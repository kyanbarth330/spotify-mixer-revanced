package org.example.spotifymixer.engine;
import java.io.*;import java.util.*;import org.jf.dexlib2.*;import org.jf.dexlib2.iface.*;import org.jf.dexlib2.iface.instruction.*;import org.jf.dexlib2.dexbacked.*;import org.jf.dexlib2.builder.*;import org.jf.dexlib2.builder.instruction.*;import org.jf.dexlib2.immutable.*;import org.jf.dexlib2.immutable.reference.*;import org.jf.dexlib2.writer.io.*;import org.jf.dexlib2.writer.pool.*;
/** Hook the exact native Compose Color-to-ARGB path, relocating its full class out of full classes4. */
final class ThemeHook {
 static byte[][] patch(byte[] original)throws Exception{
  DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),original);List<ClassDef> kept=new ArrayList<>(),moved=new ArrayList<>();int matches=0;
  for(ClassDef cls:dex.getClasses()){
   if(!cls.getType().equals("Lp/k5k1;")){kept.add(cls);continue;}List<Method> methods=new ArrayList<>();
   for(Method m:cls.getMethods()){
    if(!m.getName().equals("K")||!m.getParameterTypes().equals(Collections.singletonList("J"))||!m.getReturnType().equals("I")){methods.add(m);continue;}
    if(m.getImplementation()==null||(m.getAccessFlags()&8)==0)throw new IOException("Invalid native color conversion");
    MutableMethodImplementation impl=new MutableMethodImplementation(m.getImplementation());int returns=0;
    for(int i=impl.getInstructions().size()-1;i>=0;i--)if(impl.getInstructions().get(i).getOpcode()==Opcode.RETURN){int reg=((OneRegisterInstruction)impl.getInstructions().get(i)).getRegisterA();impl.addInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,reg,1,new ImmutableMethodReference("Lorg/example/spotifymixer/runtime/ThemeRuntime;","mapColor",Collections.singletonList("I"),"I")));impl.addInstruction(i+1,new BuilderInstruction11x(Opcode.MOVE_RESULT,reg));returns++;}
    if(returns!=1)throw new IOException("Native color conversion return changed");matches++;
    methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));
   }
   moved.add(new ImmutableClassDef(cls.getType(),cls.getAccessFlags(),cls.getSuperclass(),cls.getInterfaces(),cls.getSourceFile(),cls.getAnnotations(),cls.getFields(),methods));
  }
  if(matches!=1)throw new IOException("Expected one native theme conversion hook");return new byte[][]{write(dex.getOpcodes(),kept),write(dex.getOpcodes(),moved)};
 }
 static byte[] merge(byte[] first,byte[] second)throws IOException{List<ClassDef> all=new ArrayList<>();Opcodes op=Opcodes.forApi(24);all.addAll(new DexBackedDexFile(op,first).getClasses());all.addAll(new DexBackedDexFile(op,second).getClasses());return write(op,all);}
 private static byte[] write(Opcodes op,List<ClassDef> classes)throws IOException{MemoryDataStore out=new MemoryDataStore();DexPool.writeTo(out,new ImmutableDexFile(op,classes));return out.getData();}
}
