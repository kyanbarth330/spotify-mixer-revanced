package org.example.spotifymixer.engine;

import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import org.jf.dexlib2.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.*;

/** Exact-input, UI-only patch; shared by Android and the local validation harness. */
public final class Patcher {
    public static final String INPUT_SHA256 = "5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4";
    public static final String TARGET = "Lcom/spotify/music/SpotifyMainActivity;";
    public static String hash(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] b = new byte[65536]; int n;
            while ((n = in.read(b)) != -1) md.update(b, 0, n);
        }
        StringBuilder s = new StringBuilder();
        for (byte b : md.digest()) s.append(String.format(Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }
    public static void validate(File input) throws Exception {
        if (!INPUT_SHA256.equals(hash(input))) throw new IOException("This is not the supported 9.1.90.2270 APK (SHA-256 mismatch).");
        ApkVerifier.Result result = new ApkVerifier.Builder(input).setMinCheckedPlatformVersion(24).build().verify();
        if (!result.isVerified()) throw new IOException("Invalid input APK signature: " + result.getErrors());
    }
    static byte[] hook(byte[] bytes) throws Exception {
        DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forApi(24), bytes);
        List<ClassDef> classes = new ArrayList<>(); int matches = 0;
        for (ClassDef cls : dex.getClasses()) {
            if (!TARGET.equals(cls.getType())) { classes.add(cls); continue; }
            List<Method> methods = new ArrayList<>();
            for (Method m : cls.getMethods()) {
                if (!m.getName().equals("onResume") || !m.getParameterTypes().isEmpty() || !m.getReturnType().equals("V")) { methods.add(m); continue; }
                if (m.getImplementation() == null || (m.getAccessFlags() & 8) != 0) throw new IOException("Invalid onResume hook");
                MutableMethodImplementation impl = new MutableMethodImplementation(m.getImplementation());
                impl.addInstruction(0, new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,
                    impl.getRegisterCount()-1, 1, new ImmutableMethodReference(
                    "Lorg/example/spotifymixer/runtime/Mixer;", "attach", Collections.singletonList("Landroid/app/Activity;"), "V")));
                methods.add(new ImmutableMethod(m.getDefiningClass(), m.getName(), m.getParameters(), m.getReturnType(),
                    m.getAccessFlags(), m.getAnnotations(), m.getHiddenApiRestrictions(), impl));
                matches++;
            }
            classes.add(new ImmutableClassDef(cls.getType(), cls.getAccessFlags(), cls.getSuperclass(), cls.getInterfaces(),
                cls.getSourceFile(), cls.getAnnotations(), cls.getFields(), methods));
        }
        if (matches != 1) throw new IOException("Expected one onResume hook; found: " + matches);
        MemoryDataStore store = new MemoryDataStore();
        DexPool.writeTo(store, new ImmutableDexFile(dex.getOpcodes(), classes));
        return store.getData();
    }
    public static List<byte[]> validateRuntime(File runtime) throws Exception { return validateRuntime(runtime, message -> {}); }
    public static List<byte[]> validateRuntime(File runtime, Consumer<String> log) throws Exception {
        List<byte[]> payloads = new ArrayList<>();
        try (InputStream in = new FileInputStream(runtime)) {
            if (in.read() == 'd') {
                try (InputStream dex = new FileInputStream(runtime)) { payloads.add(read(dex)); }
            } else {
                try (ZipFile zip = new ZipFile(runtime)) {
                    List<String> names = new ArrayList<>();
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (!name.matches("classes([0-9]+)?\\.dex")) throw new IOException("Unexpected runtime entry: " + name);
                        names.add(name);
                    }
                    Collections.sort(names);
                    for (String name : names) try (InputStream dex = zip.getInputStream(zip.getEntry(name))) { payloads.add(read(dex)); }
                }
            }
        }
        boolean attach = false;
        Set<String> types = new HashSet<>();
        int dexIndex=0;
        for (byte[] bytes : payloads) {
            log.accept("[CHECK] runtime/"+(dexIndex++==0?"classes.dex":"classes"+dexIndex+".dex"));
            DexBackedDexFile dex = new DexBackedDexFile(Opcodes.forApi(24), bytes);
            for (ClassDef cls : dex.getClasses()) {
                if (!(cls.getType().startsWith("Lorg/example/spotifymixer/runtime/") || cls.getType().startsWith("Lorg/example/spotifymixer/ui/")) || !types.add(cls.getType()))
                    throw new IOException("Invalid or duplicate runtime class: " + cls.getType());
                if (cls.getType().equals("Lorg/example/spotifymixer/runtime/Mixer;")) {
                    for (Method m : cls.getMethods()) {
                        if (m.getName().equals("attach") && m.getReturnType().equals("V")
                            && m.getParameterTypes().equals(Collections.singletonList("Landroid/app/Activity;"))
                            && (m.getAccessFlags() & 9) == 9 && m.getImplementation() != null) attach = true;
                    }
                }
            }
        }
        if (!attach) throw new IOException("Runtime missing Mixer.attach(Activity); build stopped");
        log.accept("[OK] Runtime validated: "+payloads.size()+" DEX, "+types.size()+" unique classes");
        return payloads;
    }
    public static void patch(File input, File runtime, File unsigned) throws Exception {
        patch(input,runtime,unsigned,message -> {});
    }
    public static void patch(File input, File runtime, File unsigned, Consumer<String> log) throws Exception {
        log.accept("[CHECK] Input SHA-256 and original APK signature");
        validate(input);
        log.accept("[OK] Supported Spotify APK verified");
        List<byte[]> runtimeDex = validateRuntime(runtime,log);
        try (ZipFile zip = new ZipFile(input)) {
            byte[] patched;
            byte[][] playlist;
            byte[] manifest;
            log.accept("[PATCH] classes.dex · playlist lifecycle hook");
            try (InputStream in = zip.getInputStream(zip.getEntry("classes.dex"))) { playlist = PlaylistHook.patch(read(in)); }
            log.accept("[PATCH] AndroidManifest.xml · private SpotEnhancer Activity");
            try (InputStream in = zip.getInputStream(zip.getEntry("AndroidManifest.xml"))) { manifest = ManifestEditor.addActivity(read(in)); }
            log.accept("[PATCH] classes2.dex · SpotifyMainActivity.onResume");
            try (InputStream in = zip.getInputStream(zip.getEntry("classes2.dex"))) { patched = hook(read(in)); }
            for (int i = 0; i < runtimeDex.size()+1; i++)
                if (zip.getEntry("classes" + (18+i) + ".dex") != null) throw new IOException("Runtime DEX slot already occupied");
            log.accept("[BUILD] Rebuilding APK · preserve resources/native libraries · 16 KiB alignment");
            try (CountOutput counter = new CountOutput(new FileOutputStream(unsigned)); ZipOutputStream out = new ZipOutputStream(counter)) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                byte[] buffer = new byte[65536];
                while (entries.hasMoreElements()) {
                    ZipEntry old = entries.nextElement(); String name = old.getName();
                    String upper = name.toUpperCase(Locale.ROOT);
                    if (upper.startsWith("META-INF/") && (upper.endsWith(".RSA") || upper.endsWith(".DSA") || upper.endsWith(".EC") || upper.endsWith(".SF") || upper.equals("META-INF/MANIFEST.MF"))) continue;
                    // Source stamp identifies the original distribution and is invalid after modification.
                    if (name.equals("stamp-cert-sha256")) continue;
                    if (name.equals("classes.dex")) { put(out, name, playlist[0]); log.accept("[OK] classes.dex patched"); continue; }
                    if (name.equals("AndroidManifest.xml")) { put(out, name, manifest); log.accept("[OK] AndroidManifest.xml patched"); continue; }
                    if (name.equals("classes2.dex")) { put(out, name, patched); log.accept("[OK] classes2.dex patched"); continue; }
                    ZipEntry entry = new ZipEntry(name); entry.setTime(315532800000L);
                    if (old.getMethod() == ZipEntry.STORED) {
                        entry.setMethod(ZipEntry.STORED); entry.setSize(old.getSize()); entry.setCrc(old.getCrc());
                        // Valid extra field, every uncompressed entry aligned to 16 KiB.
                        int base = (int)((counter.count + 30 + name.getBytes("UTF-8").length) % 16384);
                        int pad = (16384 - base) % 16384;
                        if (pad > 0 && pad < 4) pad += 16384;
                        if (pad > 0) { byte[] extra = new byte[pad]; extra[0] = 0x35; extra[1] = (byte)0xd9;
                            extra[2] = (byte)(pad-4); extra[3] = (byte)((pad-4)>>8); entry.setExtra(extra); }
                    }
                    out.putNextEntry(entry);
                    try (InputStream in = zip.getInputStream(old)) { int n; while ((n=in.read(buffer))!=-1) out.write(buffer,0,n); }
                    out.closeEntry();
                    if(name.matches("classes([0-9]+)?\\.dex"))log.accept("[KEEP] "+name+" unchanged");
                }
                for (int i = 0; i < runtimeDex.size(); i++) {String name="classes"+(18+i)+".dex";put(out,name,runtimeDex.get(i));log.accept("[ADD] "+name+" · SpotEnhancer runtime");}
                put(out, "classes" + (18+runtimeDex.size()) + ".dex", playlist[1]);
                log.accept("[ADD] classes"+(18+runtimeDex.size())+".dex · relocated playlist hook");
            }
        } catch (Exception e) { log.accept("[ERROR] Patching stopped: "+e.getClass().getSimpleName());unsigned.delete(); throw e; }
    }
    public static void sign(File unsigned, File signed, PrivateKey key, X509Certificate cert) throws Exception {sign(unsigned,signed,key,cert,message -> {});}
    public static void sign(File unsigned, File signed, PrivateKey key, X509Certificate cert, Consumer<String> log) throws Exception {
        try {
            log.accept("[SIGN] APK Signature Scheme v2 + v3");
            ApkSigner.SignerConfig config = new ApkSigner.SignerConfig.Builder("mixer", key, Collections.singletonList(cert)).build();
            new ApkSigner.Builder(Collections.singletonList(config)).setInputApk(unsigned).setOutputApk(signed)
                .setMinSdkVersion(24).setV1SigningEnabled(false).setV2SigningEnabled(true).setV3SigningEnabled(true).setV4SigningEnabled(false).build().sign();
            log.accept("[CHECK] Signed APK integrity and signature");
            ApkVerifier.Result result = new ApkVerifier.Builder(signed).setMinCheckedPlatformVersion(24).build().verify();
            if (!result.isVerified() || !result.isVerifiedUsingV2Scheme()) throw new IOException("Invalid output signature: " + result.getErrors());
            log.accept("[OK] Output signature verified");
        } catch (Exception e) { log.accept("[ERROR] Signing/verification stopped");signed.delete(); throw e; }
    }
    private static byte[] read(InputStream in) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[65536]; int n; while ((n=in.read(b))!=-1) out.write(b,0,n); return out.toByteArray(); }
    private static void put(ZipOutputStream out, String name, byte[] bytes) throws IOException { ZipEntry e = new ZipEntry(name); e.setTime(315532800000L); out.putNextEntry(e); out.write(bytes); out.closeEntry(); }
    private static final class CountOutput extends FilterOutputStream {
        long count;
        CountOutput(OutputStream out) { super(out); }
        public void write(int b) throws IOException { out.write(b); count++; }
        public void write(byte[] b,int o,int n) throws IOException { out.write(b,o,n); count+=n; }
    }
}
