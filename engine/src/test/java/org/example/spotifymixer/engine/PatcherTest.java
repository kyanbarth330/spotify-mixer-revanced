package org.example.spotifymixer.engine;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import java.io.*;
import java.util.*;

public class PatcherTest {
    @Test public void wrongInputRejected() throws Exception {
        File f=File.createTempFile("wrong", ".apk");
        try { Patcher.validate(f); fail("accepted unknown input"); }
        catch (IOException expected) { assertTrue(expected.getMessage().contains("SHA-256")); }
        finally { f.delete(); }
    }
    @Test public void hookPreservesThisAndReturn() throws Exception {
        Method method = new ImmutableMethod(Patcher.TARGET,"onResume",Collections.emptyList(),"V",1,
            Collections.emptySet(),Collections.emptySet(), new ImmutableMethodImplementation(23,
                Collections.singletonList(new ImmutableInstruction10x(Opcode.RETURN_VOID)),Collections.emptyList(),Collections.emptyList()));
        ClassDef cls = new ImmutableClassDef(Patcher.TARGET,1,"Landroid/app/Activity;",Collections.emptyList(),null,
            Collections.emptySet(),Collections.emptyList(),Collections.singletonList(method));
        MemoryDataStore store=new MemoryDataStore();
        DexPool.writeTo(store,new ImmutableDexFile(Opcodes.forApi(24),Collections.singleton(cls)));
        byte[] patched=Patcher.hook(store.getData());
        Method actual=new DexBackedDexFile(Opcodes.forApi(24),patched).getClasses().iterator().next().getMethods().iterator().next();
        Iterator<? extends Instruction> ins=actual.getImplementation().getInstructions().iterator();
        Instruction call=ins.next(); assertEquals(Opcode.INVOKE_STATIC_RANGE,call.getOpcode());
        assertEquals(22,((RegisterRangeInstruction)call).getStartRegister());
        assertEquals("attach",((MethodReference)((ReferenceInstruction)call).getReference()).getName());
        assertEquals(Opcode.RETURN_VOID,ins.next().getOpcode()); assertFalse(ins.hasNext());
    }
    private byte[] runtimeClass(String type, boolean helper) throws Exception {
        List<Method> methods = new ArrayList<>();
        if (helper) methods.add(new ImmutableMethod(type,"attach",Collections.singletonList(
            new ImmutableMethodParameter("Landroid/app/Activity;",Collections.emptySet(),null)),"V",9,
            Collections.emptySet(),Collections.emptySet(),new ImmutableMethodImplementation(1,
                Collections.singletonList(new ImmutableInstruction10x(Opcode.RETURN_VOID)),Collections.emptyList(),Collections.emptyList())));
        ClassDef cls = new ImmutableClassDef(type,1,"Ljava/lang/Object;",Collections.emptyList(),null,
            Collections.emptySet(),Collections.emptyList(),methods);
        MemoryDataStore store = new MemoryDataStore();
        DexPool.writeTo(store,new ImmutableDexFile(Opcodes.forApi(24),Collections.singleton(cls)));
        return store.getData();
    }
    @Test public void resourceOnlyRuntimeRejected() throws Exception {
        File f = File.createTempFile("runtime", ".dex");
        try {
            try (OutputStream out = new FileOutputStream(f)) { out.write(runtimeClass("Lorg/example/spotifymixer/runtime/R;",false)); }
            try { Patcher.validateRuntime(f); fail("accepted R-only runtime"); }
            catch (IOException expected) { assertTrue(expected.getMessage().contains("Mixer.attach")); }
        } finally { f.delete(); }
    }
    @Test public void helperInSecondDexIsRetained() throws Exception {
        File f = File.createTempFile("runtime", ".zip");
        try {
            try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new FileOutputStream(f))) {
                out.putNextEntry(new java.util.zip.ZipEntry("classes.dex")); out.write(runtimeClass("Lorg/example/spotifymixer/runtime/R;",false)); out.closeEntry();
                out.putNextEntry(new java.util.zip.ZipEntry("classes2.dex")); out.write(runtimeClass("Lorg/example/spotifymixer/runtime/Mixer;",true)); out.closeEntry();
            }
            List<String> events=new ArrayList<>();
            assertEquals(2,Patcher.validateRuntime(f,events::add).size());
            assertEquals("[CHECK] runtime/classes.dex",events.get(0));
            assertEquals("[CHECK] runtime/classes2.dex",events.get(1));
            assertTrue(events.get(2).startsWith("[OK] Runtime validated:"));
        } finally { f.delete(); }
    }
    private void checkPayload(String extra, boolean reject) throws Exception {
        File f=File.createTempFile("runtime", ".zip");
        try {
            try(java.util.zip.ZipOutputStream out=new java.util.zip.ZipOutputStream(new FileOutputStream(f))){
                out.putNextEntry(new java.util.zip.ZipEntry("classes.dex"));out.write(runtimeClass("Lorg/example/spotifymixer/runtime/Mixer;",true));out.closeEntry();
                out.putNextEntry(new java.util.zip.ZipEntry("classes2.dex"));out.write(runtimeClass(extra,false));out.closeEntry();
            }
            try { assertEquals(2,Patcher.validateRuntime(f).size());if(reject)fail("accepted invalid runtime"); }
            catch(IOException error){if(!reject)throw error;assertTrue(error.getMessage().contains("Invalid or duplicate"));}
        }finally{f.delete();}
    }
    @Test public void sharedThemeAndD8LambdaAccepted() throws Exception {
        checkPayload("Lorg/example/spotifymixer/ui/EnhancerTheme;",false);
        checkPayload("Lorg/example/spotifymixer/ui/EnhancerTheme$$ExternalSyntheticLambda0;",false);
    }
    @Test public void duplicateRuntimeClassRejected() throws Exception {checkPayload("Lorg/example/spotifymixer/runtime/Mixer;",true);}
    @Test public void unrelatedPackageRejected() throws Exception {checkPayload("Lunrelated/Helper;",true);}

    @Test public void failedValidationDoesNotLogPatchSuccess() throws Exception {
        File input=File.createTempFile("unsupported", ".apk"),output=File.createTempFile("unsigned", ".apk");
        List<String> events=new ArrayList<>();
        try{
            try{Patcher.patch(input,input,output,events::add);fail("accepted invalid APK");}
            catch(IOException expected){assertTrue(expected.getMessage().contains("SHA-256"));}
            assertEquals(Collections.singletonList("[CHECK] Input SHA-256 and original APK signature"),events);
        }finally{input.delete();output.delete();}
    }

}
