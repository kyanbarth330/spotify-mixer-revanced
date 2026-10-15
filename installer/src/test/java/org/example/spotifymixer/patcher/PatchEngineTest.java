package org.example.spotifymixer.patcher;

import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.*;

public class PatchEngineTest {
    @Test public void rejectsUnknownInputBeforeParsing() throws Exception {
        File input=File.createTempFile("invalid",".apk"),out=File.createTempFile("output",".apk");out.delete();
        try {PatchEngine.patch(input,new byte[0],out);fail("Unexpected acceptance");}
        catch(IOException expected){assertTrue(expected.getMessage().contains("SHA-256"));assertFalse(out.exists());}
        finally{input.delete();out.delete();}
    }
    @Test public void bundledPayloadContainsMenu() throws Exception {
        byte[] payload=java.nio.file.Files.readAllBytes(new File("build/generated/payloadAssets/mixer.dex").toPath());
        DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),payload);
        boolean found=false;
        for(ClassDef c:dex.getClasses())if(c.getType().equals(PatchEngine.PAYLOAD)) {
            for(Method m:c.getMethods())if(m.getName().equals("attach") && m.getParameterTypes().toString().equals("[Landroid/app/Activity;]"))found=true;
        }
        assertTrue("Bundled DEX must define MixerMenu.attach(Activity)",found);
    }
    @Test public void realApkRoundTrip() throws Exception {
        String source=System.getenv("MIXER_TEST_APK");Assume.assumeNotNull(source);
        File input=new File(source);
        byte[] payload=java.nio.file.Files.readAllBytes(new File("build/generated/payloadAssets/mixer.dex").toPath());
        File dir=new File(System.getenv("MIXER_TEST_OUTPUT"));dir.mkdirs();
        File unsigned=new File(dir,"unsigned.apk"),signed=new File(dir,"signed.apk");
        PatchEngine.patch(input,payload,unsigned);
        try(ZipFile before=new ZipFile(input);ZipFile after=new ZipFile(unsigned)) {
            assertNotNull(after.getEntry("classes18.dex"));
            assertArrayEquals(PatchEngine.read(before.getInputStream(before.getEntry("AndroidManifest.xml"))),PatchEngine.read(after.getInputStream(after.getEntry("AndroidManifest.xml"))));
            for(Enumeration<? extends ZipEntry> e=before.entries();e.hasMoreElements();) {
                ZipEntry entry=e.nextElement();String name=entry.getName();
                if(entry.isDirectory()||PatchEngine.isSignature(name)||name.equals("stamp-cert-sha256")||name.equals("classes2.dex"))continue;
                assertNotNull(name,after.getEntry(name));
                assertEquals(name,entry.getCrc(),after.getEntry(name).getCrc());
            }
            DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),PatchEngine.read(after.getInputStream(after.getEntry("classes2.dex"))));
            int hooks=0;for(ClassDef c:dex.getClasses())for(Method m:c.getMethods())if(c.getType().equals(PatchEngine.TARGET)&&m.getName().equals("onResume")) {
                for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction) {
                    Reference r=((ReferenceInstruction)i).getReference();
                    if(r instanceof MethodReference && ((MethodReference)r).getDefiningClass().equals(PatchEngine.PAYLOAD))hooks++;
                }
            }
            assertEquals(1,hooks);
        }
        KeyStore ks=KeyStore.getInstance("PKCS12");
        try(InputStream in=new FileInputStream(System.getenv("MIXER_TEST_KEYSTORE"))){ks.load(in,"test-only".toCharArray());}
        PatchEngine.sign(unsigned,signed,(PrivateKey)ks.getKey("test","test-only".toCharArray()),(X509Certificate)ks.getCertificate("test"));
        assertTrue(signed.isFile());
        System.out.println("REAL_APK_PATCH_SIGN_OK "+PatchEngine.sha256(signed));
    }
}
