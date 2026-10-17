package org.example.spotifymixer.engine;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;import java.io.*;import org.jf.dexlib2.*;import org.jf.dexlib2.iface.*;import org.jf.dexlib2.iface.instruction.*;import org.jf.dexlib2.iface.reference.*;import org.jf.dexlib2.immutable.*;import org.jf.dexlib2.immutable.instruction.*;import org.jf.dexlib2.dexbacked.*;import org.jf.dexlib2.writer.io.*;import org.jf.dexlib2.writer.pool.*;
public class ServiceHooksTest {
    private byte[] fixture(boolean both)throws Exception{
        List<ClassDef> classes=new ArrayList<>();
        for(String name:both?Arrays.asList("Lp/gay;","Lp/xta0;"):Collections.singletonList("Lp/gay;")){
            List<String> types=name.equals("Lp/gay;")?Arrays.asList("Lp/jbl;","Lp/irc0;","Z","Ljava/util/List;"):Arrays.asList("Lp/iof;","Lp/pnv0;","Lp/ezl;","Lp/c77;","Lp/ck4;","Lp/c77;","Lp/c77;","Lp/y6q0;","Ljava/util/Optional;");
            List<ImmutableMethodParameter> parameters=new ArrayList<>();for(String t:types)parameters.add(new ImmutableMethodParameter(t,Collections.emptySet(),null));
            Method method=new ImmutableMethod(name,"<init>",parameters,"V",0x10001,Collections.emptySet(),Collections.emptySet(),new ImmutableMethodImplementation(30,Arrays.asList(new ImmutableInstruction10x(Opcode.NOP),new ImmutableInstruction10x(Opcode.RETURN_VOID)),Collections.emptyList(),Collections.emptyList()));
            classes.add(new ImmutableClassDef(name,1,"Ljava/lang/Object;",Collections.emptyList(),null,Collections.emptySet(),Collections.emptyList(),Collections.singletonList(method)));
        }
        MemoryDataStore out=new MemoryDataStore();DexPool.writeTo(out,new ImmutableDexFile(Opcodes.forApi(24),classes));return out.getData();
    }
    @Test public void capturesInitializedThisAtReturnWithoutChangingRegisters()throws Exception{
        DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),ServiceHooks.patch(fixture(true)));
        for(ClassDef c:dex.getClasses())for(Method m:c.getMethods()){
            assertEquals(30,m.getImplementation().getRegisterCount());Iterator<? extends Instruction> i=m.getImplementation().getInstructions().iterator();assertEquals(Opcode.NOP,i.next().getOpcode());Instruction call=i.next();assertEquals(Opcode.INVOKE_STATIC_RANGE,call.getOpcode());assertEquals(30-m.getParameterTypes().size()-1,((RegisterRangeInstruction)call).getStartRegister());assertTrue(((MethodReference)((ReferenceInstruction)call).getReference()).getName().startsWith("capture"));assertEquals(Opcode.RETURN_VOID,i.next().getOpcode());
        }
    }
    @Test(expected=IOException.class)public void rejectsMissingServiceConstructor()throws Exception{ServiceHooks.patch(fixture(false));}
}
