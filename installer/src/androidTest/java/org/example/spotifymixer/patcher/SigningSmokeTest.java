package org.example.spotifymixer.patcher;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.core.content.FileProvider;
import android.content.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;

/** Uses only our source-built fixture APK, never Spotify. */
@RunWith(AndroidJUnit4.class)
public class SigningSmokeTest {
    @Test public void androidKeyStoreSignsAndFileProviderShares() throws Exception {
        Context test=InstrumentationRegistry.getInstrumentation().getContext();
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File input=new File(app.getCacheDir(),"smoke-fixture.apk");
        File output=new File(app.getFilesDir(),"output/smoke-signed.apk");output.getParentFile().mkdirs();
        try {
            try(OutputStream out=new FileOutputStream(input)){out.write(PatchEngine.read(test.getAssets().open("fixture.apk")));}
            LocalSigner.sign(input,output);
            assertTrue(output.length()>0);
            android.net.Uri uri=FileProvider.getUriForFile(app,app.getPackageName()+".files",output);
            assertEquals("content",uri.getScheme());
            try(InputStream in=app.getContentResolver().openInputStream(uri)){assertNotNull(in);assertEquals('P',in.read());}
            // Reuse of the same key is exercised by signing a second time.
            LocalSigner.sign(input,output);
        } finally {input.delete();output.delete();}
    }
}
