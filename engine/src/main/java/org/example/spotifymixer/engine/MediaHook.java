package org.example.spotifymixer.engine;
import java.io.*;import java.util.*;import org.jf.dexlib2.*;import org.jf.dexlib2.iface.*;import org.jf.dexlib2.iface.instruction.*;import org.jf.dexlib2.iface.reference.*;import org.jf.dexlib2.dexbacked.*;import org.jf.dexlib2.builder.*;import org.jf.dexlib2.builder.instruction.*;import org.jf.dexlib2.immutable.*;import org.jf.dexlib2.immutable.reference.*;import org.jf.dexlib2.writer.io.*;import org.jf.dexlib2.writer.pool.*;
/** Exact-input media-session wrapper capture. Complete relocation avoids the original method limit. */
final class MediaHook {
 static byte[][] patch(byte[] original)throws Exception{
  DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),original);List<ClassDef> kept=new ArrayList<>(),moved=new ArrayList<>();int constructors=0;
  for(ClassDef cls:dex.getClasses()){
   if(!cls.getType().equals("Lp/bqf0;")){kept.add(cls);continue;}
   boolean field=false;for(Field f:cls.getFields())if(f.getName().equals("a")&&f.getType().equals("Landroid/media/session/MediaSession;"))field=true;if(!field)throw new IOException("Media session field changed");
   List<Method> methods=new ArrayList<>();for(Method m:cls.getMethods()){
    if(!m.getName().equals("<init>")){methods.add(m);continue;}
    if(m.getImplementation()==null||!(m.getParameterTypes().equals(Arrays.asList("Landroid/content/Context;","Ljava/lang/String;"))||m.getParameterTypes().equals(Collections.singletonList("Landroid/media/session/MediaSession;"))))throw new IOException("Media session constructor changed");
    MutableMethodImplementation impl=new MutableMethodImplementation(m.getImplementation());int writes=0;
    // Capture the session at its field assignment. The incoming this register may be
    // reused for an integer before RETURN_VOID in the supported Spotify constructor.
    for(int i=impl.getInstructions().size()-1;i>=0;i--){Instruction instruction=impl.getInstructions().get(i);if(instruction.getOpcode()!=Opcode.IPUT_OBJECT)continue;FieldReference target=(FieldReference)((ReferenceInstruction)instruction).getReference();if(!target.getDefiningClass().equals(cls.getType())||!target.getName().equals("a")||!target.getType().equals("Landroid/media/session/MediaSession;"))continue;int session=((TwoRegisterInstruction)instruction).getRegisterA();impl.addInstruction(i+1,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,session,1,new ImmutableMethodReference("Lorg/example/spotifymixer/runtime/MediaToolsRuntime;","captureSession",Collections.singletonList("Landroid/media/session/MediaSession;"),"V")));writes++;}
    if(writes!=1)throw new IOException("Expected one media session assignment per constructor");constructors++;methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));
   }
   moved.add(new ImmutableClassDef(cls.getType(),cls.getAccessFlags(),cls.getSuperclass(),cls.getInterfaces(),cls.getSourceFile(),cls.getAnnotations(),cls.getFields(),methods));
  }
  if(constructors!=2)throw new IOException("Expected two media session constructors");return new byte[][]{write(dex.getOpcodes(),kept),write(dex.getOpcodes(),moved)};
 }
 private static byte[] write(Opcodes op,List<ClassDef> classes)throws IOException{MemoryDataStore out=new MemoryDataStore();DexPool.writeTo(out,new ImmutableDexFile(op,classes));return out.getData();}
}
