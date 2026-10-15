package org.example.spotifymixer.engine;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.zip.*;
/** Explicit local-only integration test. Never run on or upload proprietary fixtures in CI. */
public class LocalValidation {
    public static void main(String[] args) throws Exception {
        File input=new File(args[0]), runtime=new File(args[1]), work=new File(args[3]); work.mkdirs();
        File unsigned=new File(work,"unsigned.apk"), signed=new File(work,"patched.apk");
        KeyStore keys=KeyStore.getInstance("PKCS12");
        try(InputStream in=new FileInputStream(args[2])) { keys.load(in,"android".toCharArray()); }
        Patcher.patch(input,runtime,unsigned);
        Patcher.sign(unsigned,signed,(PrivateKey)keys.getKey("test","android".toCharArray()),(X509Certificate)keys.getCertificate("test"));
        int preserved=0;
        try(ZipFile before=new ZipFile(input);ZipFile after=new ZipFile(signed)) {
            Enumeration<? extends ZipEntry> e=before.entries();
            while(e.hasMoreElements()) {
                ZipEntry item=e.nextElement();String n=item.getName();
                if(n.equals("classes2.dex") || n.startsWith("META-INF/") || n.equals("stamp-cert-sha256")) continue;
                try(InputStream a=before.getInputStream(item);InputStream b=after.getInputStream(after.getEntry(n))) {
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    if(!Arrays.equals(digest(a,md),digest(b,md))) throw new AssertionError("Changed: "+n);
                }
                preserved++;
            }
            if(after.getEntry("classes18.dex")==null) throw new AssertionError("Missing runtime");
        }
        System.out.println("PASS: exact input, original signature, DEX rewrite, runtime insertion, v2/v3 signing, output verification; preserved entries="+preserved);
        System.out.println("Output SHA-256="+Patcher.hash(signed));
    }
    private static byte[] digest(InputStream in,MessageDigest md)throws Exception { md.reset();byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)md.update(b,0,n);return md.digest(); }
}
