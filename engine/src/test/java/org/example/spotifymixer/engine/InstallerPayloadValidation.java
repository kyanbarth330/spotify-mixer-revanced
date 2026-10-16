package org.example.spotifymixer.engine;
import java.io.*;
import java.nio.file.Files;
import java.util.zip.*;
/** Validate the delivered asset with the Android patcher's exact acceptance rules. */
public final class InstallerPayloadValidation {
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Expected installer APK path");
        File payload=File.createTempFile("installer-runtime", ".zip");
        try(ZipFile apk=new ZipFile(args[0])){
            ZipEntry entry=apk.getEntry("assets/mixer-runtime.zip");
            if(entry==null)throw new IOException("Missing runtime asset");
            try(InputStream in=apk.getInputStream(entry)){Files.copy(in,payload.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
            System.out.println("PASS: patcher accepts delivered runtime: "+Patcher.validateRuntime(payload).size()+" DEX files");
        }finally{payload.delete();}
    }
}
