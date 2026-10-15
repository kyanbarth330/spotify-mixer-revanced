package org.example.spotifymixer.patcher;

import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.*;
import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import org.jf.dexlib2.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction3rc;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.MemoryDataStore;

/** Exact-input, offline patch. Does not bypass entitlement or playback checks. */
public final class PatchEngine {
    public static final String INPUT_SHA256 = "5c05366de31788fd0eec6e046c3610311acf5e63c15345e5bdaca6e8f09fb3b4";
    static final String TARGET = "Lcom/spotify/music/SpotifyMainActivity;";
    static final String PAYLOAD = "Lorg/spotify/mixer/injected/MixerMenu;";
    public static String sha256(File file) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) { byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1)d.update(b,0,n); }
        StringBuilder s=new StringBuilder(); for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255)); return s.toString();
    }
    public static void patch(File input, byte[] payload, File unsigned) throws Exception {
        if (!INPUT_SHA256.equals(sha256(input))) throw new IOException("Alleen de exact onderzochte Spotify 9.1.90.2270 APK wordt ondersteund (SHA-256 wijkt af).");
        DexBackedDexFile extra = new DexBackedDexFile(Opcodes.forApi(24),payload);
        boolean foundPayload=false; for(ClassDef c:extra.getClasses())if(c.getType().equals(PAYLOAD))foundPayload=true;
        if(!foundPayload)throw new IOException("Mixer-payload ontbreekt");
        try (ZipFile zip=new ZipFile(input)) {
            if(zip.getEntry("classes18.dex")!=null)throw new IOException("APK heeft al classes18.dex");
            byte[] patched=patchDex(read(zip.getInputStream(zip.getEntry("classes2.dex"))));
            try(AlignedZip out=new AlignedZip(new FileOutputStream(unsigned))) {
                Enumeration<? extends ZipEntry> entries=zip.entries();
                while(entries.hasMoreElements()) {
                    ZipEntry e=entries.nextElement(); String name=e.getName();
                    if(e.isDirectory() || isSignature(name) || name.equals("stamp-cert-sha256"))continue;
                    byte[] data=name.equals("classes2.dex") ? patched : read(zip.getInputStream(e));
                    // Retain native libraries and resources as STORED. Alignment is handled below.
                    write(out,name,data,name.endsWith(".so") || name.equals("resources.arsc"));
                }
                write(out,"classes18.dex",payload,false);
            }
        } catch(Exception e) { unsigned.delete(); throw e; }
    }
    static boolean isSignature(String name) {
        String upper=name.toUpperCase(Locale.ROOT);
        return upper.equals("META-INF/MANIFEST.MF") || upper.matches("META-INF/[^/]+\\.(SF|RSA|DSA|EC)");
    }
    static byte[] patchDex(byte[] bytes) throws Exception {
        DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),bytes);
        DexPool pool=new DexPool(dex.getOpcodes()); int hooks=0;
        for(ClassDef c:dex.getClasses()) {
            if(!c.getType().equals(TARGET)) { pool.internClass(c); continue; }
            List<Method> methods=new ArrayList<>();
            for(Method m:c.getMethods()) {
                if(m.getName().equals("onResume") && m.getParameterTypes().isEmpty() && m.getReturnType().equals("V")) {
                    if(m.getImplementation()==null || (m.getAccessFlags() & 8)!=0)throw new IOException("Ongeldige onResume");
                    MutableMethodImplementation impl=new MutableMethodImplementation(m.getImplementation());
                    int receiver=impl.getRegisterCount()-1;
                    for(int i=impl.getInstructions().size()-1;i>=0;i--) {
                        if(impl.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID) {
                            impl.addInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,receiver,1,
                                new ImmutableMethodReference(PAYLOAD,"attach",Collections.singletonList("Landroid/app/Activity;"),"V")));
                            hooks++;
                        }
                    }
                    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));
                } else methods.add(m);
            }
            pool.internClass(new ImmutableClassDef(c.getType(),c.getAccessFlags(),c.getSuperclass(),c.getInterfaces(),c.getSourceFile(),c.getAnnotations(),c.getFields(),methods));
        }
        if(hooks!=1)throw new IOException("Verwacht precies één onResume-return, gevonden: "+hooks);
        MemoryDataStore store=new MemoryDataStore(); pool.writeTo(store); return store.getData();
    }
    static final class Counter extends FilterOutputStream {
        long count;
        Counter(OutputStream out){super(out);}
        public void write(int b)throws IOException{out.write(b);count++;}
        public void write(byte[] b,int o,int n)throws IOException{out.write(b,o,n);count+=n;}
    }
    static final class AlignedZip extends ZipOutputStream {
        final Counter counter;
        AlignedZip(OutputStream out){this(new Counter(out));}
        private AlignedZip(Counter c){super(c);counter=c;}
    }
    static void write(ZipOutputStream out,String name,byte[] data,boolean stored) throws IOException {
        ZipEntry e=new ZipEntry(name); e.setTime(315619200000L);
        if(stored) { CRC32 crc=new CRC32();crc.update(data);e.setMethod(ZipEntry.STORED);e.setSize(data.length);e.setCompressedSize(data.length);e.setCrc(crc.getValue()); }
        if(stored) {
            int alignment=name.endsWith(".so")?16384:4;
            long offset=((AlignedZip)out).counter.count+30+name.getBytes("UTF-8").length;
            int padding=(int)((alignment-offset%alignment)%alignment);
            if(padding>0) {
                if(padding<4)padding+=alignment;
                byte[] extra=new byte[padding];extra[0]=0x35;extra[1]=(byte)0xd9;
                extra[2]=(byte)(padding-4);extra[3]=(byte)((padding-4)>>8);e.setExtra(extra);
            }
        }
        out.putNextEntry(e);out.write(data);out.closeEntry();
    }
    public static void sign(File unsigned,File output,PrivateKey key,X509Certificate certificate) throws Exception {
        ApkSigner.SignerConfig config=new ApkSigner.SignerConfig.Builder("mixer-local",key,Collections.singletonList(certificate)).build();
        try {
            new ApkSigner.Builder(Collections.singletonList(config)).setInputApk(unsigned).setOutputApk(output)
                .setMinSdkVersion(24).setV1SigningEnabled(true).setV2SigningEnabled(true).setV3SigningEnabled(false).build().sign();
            ApkVerifier.Result result=new ApkVerifier.Builder(output).setMinCheckedPlatformVersion(24).build().verify();
            if(!result.isVerified() || !result.isVerifiedUsingV2Scheme())throw new IOException("APK-handtekeningverificatie mislukt: "+result.getErrors());
        } catch(Exception e) {output.delete();throw e;}
    }
    public static byte[] read(InputStream input) throws IOException {
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()) {byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
    }
}
