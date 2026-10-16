package org.example.spotifymixer.testhost;

import android.content.*;
import android.graphics.Rect;
import android.security.keystore.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.File;
import java.security.*;
import java.security.cert.X509Certificate;
import com.android.apksig.ApkVerifier;
import org.example.spotifymixer.engine.Patcher;

@RunWith(AndroidJUnit4.class)
public class AndroidSmokeTest {
    private static final String PREFS="spotify-mixer-entry-v1";
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private UiObject2 waitText(String text){
        UiObject2 view=device.wait(Until.findObject(By.text(text)),4000);
        if(view==null || view.getVisibleBounds().isEmpty()){
            try{new UiScrollable(new UiSelector().className("android.widget.ScrollView")).scrollTextIntoView(text);}catch(UiObjectNotFoundException ignored){}
            view=device.wait(Until.findObject(By.text(text)),4000);
        }
        assertNotNull("Missing UI: "+text,view);return view;
    }
    private void screenshot(String name){
        device.waitForIdle();
        try{
            // Gradle uninstalls the fixture after tests, deleting app-scoped external files.
            // Shell-owned evidence survives that cleanup on this fresh, own-code-only emulator.
            String path="/sdcard/Download/spotenhancer-evidence/"+name+".png";
            device.executeShellCommand("mkdir -p /sdcard/Download/spotenhancer-evidence");
            device.executeShellCommand("screencap -p "+path);
            String size=device.executeShellCommand("stat -c %s "+path);
            assertTrue("Screenshot must contain PNG data",Long.parseLong(size.trim())>1000);
        }catch(java.io.IOException ex){throw new AssertionError("Cannot preserve screenshot",ex);}
    }

    @Test public void selectTwoPlaylistsAndNavigateBack()throws Exception{
        context.getSharedPreferences(PREFS,0).edit().clear().commit();
        context.startActivity(new Intent(context,FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try{
            waitText("SpotEnhancer");screenshot("01-native-entry-fixture");
            waitText("SpotEnhancer").click();waitText("Playlist Mixer");screenshot("02-spotenhancer-home");
            waitText("Playlist Mixer").click();waitText("Choose Playlist A").click();
            waitText("Test library");waitText("Playlist Alpha").click();
            UiObject2 add=waitText("Add to Mixer");assertTrue(device.wait(Until.hasObject(By.text("Add to Mixer").enabled(true)),10000));add.click();
            waitText("Playlist Mixer").click();waitText("Choose Playlist B").click();
            waitText("Test library");waitText("Playlist Beta").click();
            assertTrue(device.wait(Until.hasObject(By.text("Add to Mixer").enabled(true)),10000));waitText("Add to Mixer").click();
            waitText("Playlist Mixer").click();waitText("Playlist Alpha");waitText("Playlist Beta");
            UiObject2 slider=device.findObject(By.clazz("android.widget.SeekBar"));assertNotNull(slider);Rect bounds=slider.getVisibleBounds();device.click(bounds.left+bounds.width()*2/5,bounds.centerY());device.waitForIdle();
            assertNotEquals(70,context.getSharedPreferences(PREFS,0).getInt("ratio",70));
            assertEquals("Playlist Alpha",context.getSharedPreferences(PREFS,0).getString("a_title",null));
            assertEquals("Playlist Beta",context.getSharedPreferences(PREFS,0).getString("b_title",null));
            assertEquals("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",context.getSharedPreferences(PREFS,0).getString("a",null));
            assertEquals("spotify:playlist:37i9dQZF1DX0XUsuxWHRQd",context.getSharedPreferences(PREFS,0).getString("b",null));
            screenshot("03-playlist-mixer-two-selections");
            waitText("Back to SpotEnhancer").click();waitText("Diagnostics").click();waitText("Diagnostics");assertNotNull(device.findObject(By.textContains("keys=[")));screenshot("04-diagnostics");
            waitText("Back to Spotify").click();waitText("Playlist Beta");waitText("SpotEnhancer");screenshot("05-return-to-fixture");
        }finally{device.pressHome();}
    }
    @Test public void rejectsAmbiguousHeadings(){
        try(androidx.test.core.app.ActivityScenario<FixtureActivity> scenario=androidx.test.core.app.ActivityScenario.launch(FixtureActivity.class)){
            scenario.onActivity(activity->{
                android.widget.LinearLayout root=new android.widget.LinearLayout(activity);root.setOrientation(1);activity.setContentView(root);
                android.widget.TextView first=new android.widget.TextView(activity);first.setText("First title");first.setAccessibilityHeading(true);root.addView(first);
                assertEquals("First title",org.example.spotifymixer.runtime.PlaylistTitle.visibleHeading(root));
                android.widget.TextView second=new android.widget.TextView(activity);second.setText("Other title");second.setAccessibilityHeading(true);root.addView(second);
                assertNull(org.example.spotifymixer.runtime.PlaylistTitle.visibleHeading(root));
                android.os.Bundle metadata=new android.os.Bundle();metadata.putString("playlist_name","Explicit title");
                assertEquals("Explicit title",org.example.spotifymixer.runtime.PlaylistTitle.from(metadata,root));
            });
        }
    }
    @Test public void installerShowsSafeInitialState(){
        context.startActivity(new Intent().setClassName("org.example.spotifymixer.patcher","org.example.spotifymixer.patcher.InstallerActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try{
            waitText("SpotEnhancer");waitText("1. Select APK and build SpotEnhancer");
            assertFalse(waitText("2. Install patched Spotify").isEnabled());
            screenshot("06-installer-initial-state");
            waitText("Copy log");
            assertNotNull(device.findObject(By.textContains("[READY] Select an APK to begin. No patch has run yet.")));
            screenshot("07-installer-terminal-ready");
        }finally{device.pressHome();}
    }
    @Test public void signOwnFixtureWithAndroidKeystore()throws Exception{
        String alias="spotenhancer-emulator-test";
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(store.containsAlias(alias))store.deleteEntry(alias);
        File signed=new File(context.getCacheDir(),"signed-fixture.apk");
        try{
            KeyPairGenerator gen=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");
            gen.initialize(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY).setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA512).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).build());gen.generateKeyPair();
            java.util.List<String> events=new java.util.ArrayList<>();
            Patcher.sign(new File(context.getApplicationInfo().sourceDir),signed,(PrivateKey)store.getKey(alias,null),(X509Certificate)store.getCertificate(alias),events::add);
            assertEquals("[SIGN] APK Signature Scheme v2 + v3",events.get(0));
            assertEquals("[OK] Output signature verified",events.get(events.size()-1));
            ApkVerifier.Result result=new ApkVerifier.Builder(signed).setMinCheckedPlatformVersion(24).build().verify();assertTrue(result.isVerified());assertTrue(result.isVerifiedUsingV2Scheme());assertTrue(result.isVerifiedUsingV3Scheme());
        }finally{signed.delete();store.deleteEntry(alias);}
    }
}
