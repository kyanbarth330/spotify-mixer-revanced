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
}
