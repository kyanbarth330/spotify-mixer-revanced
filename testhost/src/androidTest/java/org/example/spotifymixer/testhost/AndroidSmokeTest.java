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

    private UiObject2 tools(){UiObject2 view=device.wait(Until.findObject(By.desc("Open SpotEnhancer")),5000);assertNotNull("Missing floating tools button",view);return view;}
    @Test public void selectMultiplePlaylistsPresetAndPanel()throws Exception{
        context.getSharedPreferences(PREFS,0).edit().clear().commit();
        context.startActivity(new Intent(context,FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try{
            tools();assertFalse(device.hasObject(By.text("Add to Mixer")));screenshot("01-floating-tools-fixture");tools().click();
            waitText("Playlist Mixer");screenshot("02-feature-panel");waitText("Playlist Mixer").click();
            for(String title:new String[]{"Playlist Alpha","Playlist Beta","Playlist Gamma"}){waitText("+  Add playlist").click();waitText("Test library");waitText(title).click();tools().click();waitText(title);}
            org.example.spotifymixer.runtime.MixLibrary library=new org.example.spotifymixer.runtime.MixLibrary(context.getSharedPreferences(PREFS,0));
            assertEquals(3,library.active().sources.size());
            UiObject2 slider=device.findObject(By.desc("Weight for Playlist Alpha"));assertNotNull(slider);Rect bounds=slider.getVisibleBounds();device.click(bounds.left+bounds.width()/2,bounds.centerY());device.waitForIdle();
            assertEquals(100,library.active().sources.stream().mapToInt(source->source.weight).sum());
            waitText("Shuffle mix").click();assertTrue(device.wait(Until.hasObject(By.text("Shuffle mix").checked(true)),4000));device.waitForIdle();assertTrue(library.active().shuffle);
            screenshot("03-multi-playlist-mixer");
            waitText("Save preset").click();UiObject2 name=device.wait(Until.findObject(By.clazz("android.widget.EditText")),3000);assertNotNull(name);name.setText("Morning blend");waitText("Save").click();waitText("Sources");
            assertEquals(1,library.entries(false).size());assertEquals(3,library.entries(false).get(0).config.sources.size());
            assertFalse(waitText("Load mix").isEnabled());assertFalse(waitText("Start Mix").isEnabled());
            waitText("History").click();waitText("No mixes yet");screenshot("04-empty-history");
            device.findObject(By.desc("Back to SpotEnhancer")).click();waitText("Mix Presets").click();waitText("Morning blend");screenshot("05-saved-presets");
            waitText("Use preset").click();waitText("Playlist Alpha");waitText("Playlist Gamma");
            waitText("‹  Back to Spotify").click();waitText("Playlist Gamma");tools();
        }finally{device.pressHome();}
    }
    @Test public void reusedFragmentReadsReplacementArguments()throws Exception{
        try(androidx.test.core.app.ActivityScenario<FixtureActivity> scenario=androidx.test.core.app.ActivityScenario.launch(FixtureActivity.class)){
            final p.gs10[] fragment={null};
            scenario.onActivity(activity->{android.view.View root=activity.findViewById(android.R.id.content);android.os.Bundle args=new android.os.Bundle();args.putString("uri","spotify:playlist:37i9dQZF1DXcBWIGoYBM5M");fragment[0]=new p.gs10(args,root);org.example.spotifymixer.runtime.Mixer.fragmentResumed(new p.av10(fragment[0]));});
            device.waitForIdle();
            scenario.onActivity(activity->{try{
                java.lang.reflect.Method current=org.example.spotifymixer.runtime.Mixer.class.getDeclaredMethod("currentPlaylist");current.setAccessible(true);
                assertEquals("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",((String[])current.invoke(null))[0]);
                android.os.Bundle replacement=new android.os.Bundle();replacement.putString("uri","spotify:playlist:37i9dQZF1DX0XUsuxWHRQd");fragment[0].f=replacement;
                assertEquals("spotify:playlist:37i9dQZF1DX0XUsuxWHRQd",((String[])current.invoke(null))[0]);
            }catch(ReflectiveOperationException error){throw new AssertionError(error);}});
        }
    }
    @Test public void resumedMixerRefreshesReplacedSelections()throws Exception{
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);prefs.edit().clear().commit();org.example.spotifymixer.runtime.MixLibrary library=new org.example.spotifymixer.runtime.MixLibrary(prefs);
        library.select(0,"spotify:playlist:0000000000000000000001","Playlist Alpha");library.select(1,"spotify:playlist:0000000000000000000002","Playlist Beta");
        Intent intent=new Intent(context,org.example.spotifymixer.runtime.SpotEnhancerActivity.class).putExtra("screen","mixer");
        try(androidx.test.core.app.ActivityScenario<org.example.spotifymixer.runtime.SpotEnhancerActivity> scenario=androidx.test.core.app.ActivityScenario.launch(intent)){
            waitText("Playlist Alpha");scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            library.select(0,"spotify:playlist:0000000000000000000003","Playlist Gamma");library.select(1,"spotify:playlist:0000000000000000000004","Playlist Delta");scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            waitText("Playlist Gamma");waitText("Playlist Delta");assertFalse(waitText("Start Mix").isEnabled());
        }
    }
    @Test public void libraryMigrationPresetsHistoryAndCorruption()throws Exception{
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);prefs.edit().clear().putString("a","spotify:playlist:0000000000000000000001").putString("a_title","Alpha").putString("b","spotify:playlist:0000000000000000000002").putString("b_title","Beta").putInt("ratio",70).commit();
        org.example.spotifymixer.runtime.MixLibrary library=new org.example.spotifymixer.runtime.MixLibrary(prefs);org.example.spotifymixer.runtime.MixConfig config=library.active();assertEquals(70,config.sources.get(0).weight);assertEquals(2,config.sources.size());library.save(config);
        library.add("Fixture preset",config,false,0);library.add("Fixture accepted request",config,true,250);assertEquals(250,library.entries(true).get(0).tracks);assertEquals(2,library.entries(false).get(0).config.sources.size());
        context.startActivity(new Intent(context,org.example.spotifymixer.runtime.SpotEnhancerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitText("Mix History").click();waitText("Fixture accepted request");screenshot("08-history-fixture-entry");device.pressHome();
        library.delete(library.entries(false).get(0).id,false);assertTrue(library.entries(false).isEmpty());prefs.edit().putString("active_v2","broken").commit();try{library.active();fail();}catch(java.io.IOException expected){}assertEquals("broken",prefs.getString("active_v2",null));prefs.edit().clear().commit();
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
