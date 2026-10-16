package org.example.spotifymixer.engine;
import java.io.*;import java.util.*;
import org.jf.dexlib2.*;import org.jf.dexlib2.iface.*;import org.jf.dexlib2.dexbacked.*;
import org.jf.dexlib2.builder.*;import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.immutable.*;import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.writer.io.*;import org.jf.dexlib2.writer.pool.*;

final class PlaylistHook {
    static byte[][] patch(byte[] original)throws Exception {
        DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),original);
        List<ClassDef> kept=new ArrayList<>();ClassDef moved=null;int count=0;
        for(ClassDef cls:dex.getClasses()){
            if(!cls.getType().equals("Lp/av10;")){kept.add(cls);continue;}
            List<Method> methods=new ArrayList<>();
            for(Method m:cls.getMethods()){
                if(!m.getName().equals("n") || !m.getParameterTypes().isEmpty() || !m.getReturnType().equals("V")){methods.add(m);continue;}
                if(m.getImplementation()==null || (m.getAccessFlags()&8)!=0)throw new IOException("Invalid fragment resume hook");
                MutableMethodImplementation impl=new MutableMethodImplementation(m.getImplementation());
                impl.addInstruction(0,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,impl.getRegisterCount()-1,1,
                    new ImmutableMethodReference("Lorg/example/spotifymixer/runtime/Mixer;","fragmentResumed",Collections.singletonList("Ljava/lang/Object;"),"V")));
                methods.add(new ImmutableMethod(m.getDefiningClass(),m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));count++;
            }
            moved=new ImmutableClassDef(cls.getType(),cls.getAccessFlags(),cls.getSuperclass(),cls.getInterfaces(),cls.getSourceFile(),cls.getAnnotations(),cls.getFields(),methods);
        }
        if(count!=1 || moved==null)throw new IOException("Expected one fragment resume hook");
        // classes.dex has 65,536 method references. Move the patched class to its own DEX
        // rather than overflowing its 16-bit method-reference table.
        return new byte[][]{write(dex.getOpcodes(),kept),write(dex.getOpcodes(),Collections.singletonList(moved))};
    }
    private static byte[] write(Opcodes op,List<? extends ClassDef> classes)throws IOException{MemoryDataStore out=new MemoryDataStore();DexPool.writeTo(out,new ImmutableDexFile(op,classes));return out.getData();}
}
