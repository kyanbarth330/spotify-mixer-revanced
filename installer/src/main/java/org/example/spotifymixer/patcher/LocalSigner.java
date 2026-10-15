package org.example.spotifymixer.patcher;

import android.security.keystore.*;
import java.io.File;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import javax.security.auth.x500.X500Principal;

final class LocalSigner {
    static void sign(File input,File output) throws Exception {
        KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);
        String alias="spotify-mixer-local-v1";
        if(!keys.containsAlias(alias)) {
            Calendar end=Calendar.getInstance();end.add(Calendar.YEAR,25);
            KeyPairGenerator generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY)
                .setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSubject(new X500Principal("CN=Spotify Mixer Local"))
                .setCertificateSerialNumber(BigInteger.ONE).setCertificateNotAfter(end.getTime()).build());generator.generateKeyPair();
        }
        PatchEngine.sign(input,output,(PrivateKey)keys.getKey(alias,null),(X509Certificate)keys.getCertificate(alias));
    }
}
