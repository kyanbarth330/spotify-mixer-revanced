package org.example.spotifymixer.engine;
import java.io.*;import java.util.*;import org.jf.dexlib2.*;import org.jf.dexlib2.iface.*;import org.jf.dexlib2.iface.instruction.*;import org.jf.dexlib2.dexbacked.*;import org.jf.dexlib2.builder.*;import org.jf.dexlib2.builder.instruction.*;import org.jf.dexlib2.immutable.*;import org.jf.dexlib2.immutable.reference.*;import org.jf.dexlib2.writer.io.*;import org.jf.dexlib2.writer.pool.*;
/** Capture fully initialized existing services; no service is created or replaced. */
final class ServiceHooks {
    static byte[] patch(byte[] data)throws Exception{
        DexBackedDexFile dex=new DexBackedDexFile(Opcodes.forApi(24),data);List<ClassDef> classes=new ArrayList<>();int count=0;
        for(ClassDef cls:dex.getClasses()){
            String name=cls.getType(),helper;List<String> params;
            if(name.equals("Lp/gay;")){helper="capturePlayer";params=Arrays.asList("Lp/jbl;","Lp/irc0;","Z","Ljava/util/List;");}
            else if(name.equals("Lp/xta0;")){helper="capturePlaylist";params=Arrays.asList("Lp/iof;","Lp/pnv0;","Lp/ezl;","Lp/c77;","Lp/ck4;","Lp/c77;","Lp/c77;","Lp/y6q0;","Ljava/util/Optional;");}
            else{classes.add(cls);continue;}
            List<Method> methods=new ArrayList<>();
            for(Method m:cls.getMethods()){
                if(!m.getName().equals("<init>")||!m.getParameterTypes().equals(params)){methods.add(m);continue;}
                if(m.getImplementation()==null)throw new IOException("Missing native service constructor");
                MutableMethodImplementation impl=new MutableMethodImplementation(m.getImplementation());int self=impl.getRegisterCount()-params.size()-1,returns=0;
                for(int i=impl.getInstructions().size()-1;i>=0;i--)if(impl.getInstructions().get(i).getOpcode()==Opcode.RETURN_VOID){impl.addInstruction(i,new BuilderInstruction3rc(Opcode.INVOKE_STATIC_RANGE,self,1,new ImmutableMethodReference("Lorg/example/spotifymixer/runtime/NativeBridge;",helper,Collections.singletonList("Ljava/lang/Object;"),"V")));returns++;}
                if(returns==0)throw new IOException("Native service constructor never returns");
                methods.add(new ImmutableMethod(name,m.getName(),m.getParameters(),m.getReturnType(),m.getAccessFlags(),m.getAnnotations(),m.getHiddenApiRestrictions(),impl));count++;
            }
            classes.add(new ImmutableClassDef(name,cls.getAccessFlags(),cls.getSuperclass(),cls.getInterfaces(),cls.getSourceFile(),cls.getAnnotations(),cls.getFields(),methods));
        }
        if(count!=2)throw new IOException("Expected exactly two native service constructor hooks; found "+count);
        MemoryDataStore out=new MemoryDataStore();DexPool.writeTo(out,new ImmutableDexFile(dex.getOpcodes(),classes));return out.getData();
    }
}
