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
        long deadline=android.os.SystemClock.uptimeMillis()+10000;
        do{try{
            device.waitForIdle();UiObject2 view=device.wait(Until.findObject(By.text(text)),1000);
            if(view==null||view.getVisibleBounds().isEmpty()){try{new UiScrollable(new UiSelector().className("android.widget.ScrollView")).scrollTextIntoView(text);}catch(UiObjectNotFoundException ignored){}view=device.wait(Until.findObject(By.text(text)),1000);}
            if(view!=null&&!view.getVisibleBounds().isEmpty())return view;
        }catch(StaleObjectException ignored){/* Activity transition: resolve the locator again. */}}
        while(android.os.SystemClock.uptimeMillis()<deadline);
        throw new AssertionError("Missing UI: "+text);
    }
    private UiObject2 field(String description)throws Exception{new UiScrollable(new UiSelector().className("android.widget.ScrollView")).scrollIntoView(new UiSelector().description(description));UiObject2 input=device.wait(Until.findObject(By.desc(description)),4000);assertNotNull(description,input);return input;}
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

    private UiObject2 tools(){device.waitForIdle();UiObject2 view=device.wait(Until.findObject(By.desc("Open SpotEnhancer")),10000);if(view==null)screenshot("failure-missing-tools");assertNotNull("Missing floating tools button",view);return view;}
    @Test public void selectMultiplePlaylistsPresetAndPanel()throws Exception{
        context.getSharedPreferences(PREFS,0).edit().clear().commit();
        context.startActivity(new Intent(context,FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try{
            waitText("Test library");tools();assertFalse(device.hasObject(By.text("Add to Mixer")));screenshot("01-floating-tools-fixture");tools().click();
            waitText("Playlist Mixer");screenshot("02-feature-panel");UiObject2 search=device.findObject(By.clazz("android.widget.EditText"));assertNotNull(search);search.setText("history");device.waitForIdle();waitText("Mix History");assertFalse(device.hasObject(By.text("Playlist Mixer")));search.setText("");waitText("Playlist Mixer").click();
            for(String title:new String[]{"Playlist Alpha","Playlist Beta","Playlist Gamma"}){waitText("+  Add playlist").click();waitText("Test library");waitText(title).click();waitText("Add to Mixer").click();waitText("Sources");waitText(title);}
            org.example.spotifymixer.runtime.MixLibrary library=new org.example.spotifymixer.runtime.MixLibrary(context.getSharedPreferences(PREFS,0));
            assertEquals(3,library.active().sources.size());
            UiObject2 slider=device.findObject(By.desc("Weight for Playlist Alpha"));assertNotNull(slider);Rect bounds=slider.getVisibleBounds();device.click(bounds.left+bounds.width()/2,bounds.centerY());device.waitForIdle();
            assertEquals(100,library.active().sources.stream().mapToInt(source->source.weight).sum());
            waitText("Shuffle mix").click();assertTrue(device.wait(Until.hasObject(By.text("Shuffle mix").checked(true)),4000));device.waitForIdle();assertTrue(library.active().shuffle);
            // Finish Android's transient selection toast before preserving the design screenshot.
            android.os.SystemClock.sleep(3500);screenshot("03-multi-playlist-mixer");
            waitText("Save preset").click();UiObject2 name=device.wait(Until.findObject(By.clazz("android.widget.EditText")),3000);assertNotNull(name);name.setText("Morning blend");UiObject2 confirm=device.wait(Until.findObject(By.res("android:id/button1")),4000);assertNotNull("Missing preset confirmation",confirm);confirm.click();waitText("Morning blend  ⌄");waitText("Sources");
            assertEquals(1,library.entries(false).size());assertEquals(3,library.entries(false).get(0).config.sources.size());
            assertFalse(waitText("Load mix").isEnabled());assertFalse(device.hasObject(By.text("Start Mix")));
            waitText("History").click();waitText("No mixes yet");screenshot("04-empty-history");
            device.findObject(By.desc("Back to SpotEnhancer")).click();waitText("Mix Presets").click();waitText("Morning blend");screenshot("05-saved-presets");
            waitText("Use preset").click();waitText("Playlist Alpha");waitText("Playlist Gamma");
            waitText("‹  Back to Spotify").click();waitText("Playlist Gamma");tools();
        }finally{device.pressHome();}
    }
    @Test public void playlistPageActionAddsDirectlyAndDeduplicates()throws Exception{
        context.getSharedPreferences(PREFS,0).edit().clear().commit();context.startActivity(new Intent(context,FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try{waitText("Test library");assertFalse(device.hasObject(By.text("Add to Mixer")));waitText("Playlist Alpha").click();waitText("Add to Mixer");screenshot("09-playlist-page-add-action");waitText("Add to Mixer").click();waitText("Sources");
            org.example.spotifymixer.runtime.MixLibrary library=new org.example.spotifymixer.runtime.MixLibrary(context.getSharedPreferences(PREFS,0));assertEquals(1,library.active().sources.size());
            waitText("‹  Back to Spotify").click();waitText("Add to Mixer").click();waitText("Sources");assertEquals(1,library.active().sources.size());waitText("‹  Back to Spotify").click();waitText("Return to test library").click();waitText("Test library");device.waitForIdle();assertFalse(device.hasObject(By.text("Add to Mixer")));
        }finally{device.pressHome();}
    }
    @Test public void creatorDraftPersistsAndProductionCannotFakeGeneration()throws Exception{
        context.getSharedPreferences(PREFS,0).edit().clear().commit();Intent intent=new Intent(context,org.example.spotifymixer.runtime.SpotEnhancerActivity.class).putExtra("screen","creator");
        try(androidx.test.core.app.ActivityScenario<org.example.spotifymixer.runtime.SpotEnhancerActivity> scenario=androidx.test.core.app.ActivityScenario.launch(intent)){
            waitText("Enable Playlist Creator");assertFalse(waitText("Generate preview").isEnabled());waitText("Enable Playlist Creator").click();
            field("Playlist name").setText("Night drive");field("Genres").setText("Rock");field("Number of tracks").setText("42");waitText("Save draft").click();waitText("Draft saved on this device. No playlist was created.");assertFalse(waitText("Generate preview").isEnabled());screenshot("10-creator-production-blocked");
            scenario.recreate();waitText("Playlist: SpotEnhancer-Night drive");assertEquals("42",field("Number of tracks").getText());assertTrue(waitText("Enable Playlist Creator").isChecked());waitText("Enable Playlist Creator").click();assertFalse(waitText("Save draft").isEnabled());assertFalse(waitText("Generate preview").isEnabled());
        }
    }
    @Test public void creatorOwnFixtureReviewsRemovesAndConfirms()throws Exception{
        context.getSharedPreferences("creator-fixture",0).edit().clear().commit();CreatorFixtureActivity.writes=0;CreatorFixtureActivity.written=null;
        try(androidx.test.core.app.ActivityScenario<CreatorFixtureActivity> scenario=androidx.test.core.app.ActivityScenario.launch(CreatorFixtureActivity.class)){
            waitText("Enable Playlist Creator").click();field("Playlist name").setText("Evening");field("Genres").setText("Rock");field("Number of tracks").setText("0");waitText("Generate preview").click();waitText("Choose 1–500 tracks");field("Number of tracks").setText("3");waitText("Generate preview").click();waitText("Review proposal");waitText("Fixture song 2");assertEquals(0,CreatorFixtureActivity.writes);screenshot("11-creator-synthetic-preview");
            UiObject2 remove=device.wait(Until.findObject(By.desc("Remove Fixture song 2")),4000);assertNotNull(remove);remove.click();waitText("2 tracks · requested 3");waitText("Create Playlist").click();UiObject2 cancel=device.wait(Until.findObject(By.res("android:id/button2")),4000);assertNotNull(cancel);cancel.click();assertEquals(0,CreatorFixtureActivity.writes);
            waitText("Create Playlist").click();UiObject2 confirm=device.wait(Until.findObject(By.res("android:id/button1")),4000);assertNotNull(confirm);confirm.click();waitText("Created SpotEnhancer-Evening · 2 tracks");assertEquals(1,CreatorFixtureActivity.writes);assertEquals(2,CreatorFixtureActivity.written.size());screenshot("12-creator-synthetic-receipt");
        }
    }
    @Test public void nativeCreatorUsesOriginalAdaptersAndVerifiesReadback()throws Exception{
        android.content.SharedPreferences prefs=context.getSharedPreferences("creator-native-protocol",0);prefs.edit().clear().commit();
        org.example.spotifymixer.runtime.CreatorProtocolFixture.writes=0;org.example.spotifymixer.runtime.CreatorProtocolFixture.wrongReadback=false;
        org.example.spotifymixer.runtime.NativeCreator.captureMaker(new com.spotify.connectivity.httpretrofit.RetrofitMaker());org.example.spotifymixer.runtime.NativeBridge.capturePlaylist(new org.example.spotifymixer.runtime.CreatorProtocolFixture());
        try{
            org.example.spotifymixer.runtime.NativeCreator backend=new org.example.spotifymixer.runtime.NativeCreator(prefs);org.example.spotifymixer.runtime.CreatorSession session=new org.example.spotifymixer.runtime.CreatorSession(backend);
            org.example.spotifymixer.runtime.CreatorSession.Preview proposal=session.prepare(new org.example.spotifymixer.runtime.CreatorSession.Request("Protocol test","rock","","",3),m->{});assertEquals("Protocol song 1",proposal.tracks.get(0).title);assertEquals(0,org.example.spotifymixer.runtime.CreatorProtocolFixture.writes);
            proposal=session.remove(proposal,proposal.tracks.get(1).uri);org.example.spotifymixer.runtime.CreatorSession.Receipt receipt=session.create(proposal);assertEquals("SpotEnhancer-Protocol test",receipt.name);assertEquals(2,receipt.count);assertEquals(1,org.example.spotifymixer.runtime.CreatorProtocolFixture.writes);assertFalse(backend.pending());
            proposal=session.prepare(new org.example.spotifymixer.runtime.CreatorSession.Request("Mismatch test","rock","","",2),m->{});org.example.spotifymixer.runtime.CreatorProtocolFixture.wrongReadback=true;try{session.create(proposal);fail();}catch(java.io.IOException expected){}assertTrue(backend.pending());assertEquals(2,org.example.spotifymixer.runtime.CreatorProtocolFixture.writes);
            try{session.prepare(new org.example.spotifymixer.runtime.CreatorSession.Request("Retry","rock","","",2),m->{});fail();}catch(java.io.IOException expected){}assertEquals(2,org.example.spotifymixer.runtime.CreatorProtocolFixture.writes);org.example.spotifymixer.runtime.CreatorProtocolFixture.wrongReadback=false;assertEquals(2,backend.verifyPending().count);assertFalse(backend.pending());assertEquals(2,org.example.spotifymixer.runtime.CreatorProtocolFixture.writes);
        }finally{org.example.spotifymixer.runtime.NativeCreator.captureMaker(null);org.example.spotifymixer.runtime.NativeBridge.capturePlaylist(null);}
    }
    @Test public void equalizerSavesPresetsAndControlsOwnAudioSession()throws Exception{
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);prefs.edit().clear().commit();
        context.startActivity(new Intent(context,FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        android.media.AudioTrack track=null;
        try{
            waitText("Test library");tools().click();waitText("Equalizer").click();assertFalse(waitText("Enable equalizer").isChecked());
            UiObject2 spinner=device.wait(Until.findObject(By.desc("Equalizer preset")),4000);assertNotNull(spinner);spinner.click();waitText("Bass Boost").click();waitText("Apply preset").click();assertEquals(1,prefs.getInt("eq_preset",-1));
            waitText("Enable equalizer").click();assertTrue(prefs.getBoolean("eq_enabled",false));
            int buffer=android.media.AudioTrack.getMinBufferSize(48000,android.media.AudioFormat.CHANNEL_OUT_MONO,android.media.AudioFormat.ENCODING_PCM_16BIT);
            track=new android.media.AudioTrack.Builder().setAudioAttributes(new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_MEDIA).setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new android.media.AudioFormat.Builder().setSampleRate(48000).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(Math.max(4096,buffer)).setTransferMode(android.media.AudioTrack.MODE_STREAM).build();
            final int id=track.getAudioSessionId();assertTrue(id>0);
            com.spotify.playbacknative.AudioRenderer.created(id);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue(org.example.spotifymixer.runtime.EqualizerRuntime.status(),org.example.spotifymixer.runtime.EqualizerRuntime.status().startsWith("Active on local audio session"));
            assertNotNull(org.example.spotifymixer.runtime.EqualizerRuntime.device());
            waitText("Refresh connection").click();waitText("Frequency bands");screenshot("15-equalizer-own-audio-session");
            UiObject2 band=device.findObject(By.clazz("android.widget.SeekBar"));assertNotNull(band);Rect bounds=band.getVisibleBounds();device.click(bounds.left+bounds.width()*3/4,bounds.centerY());device.waitForIdle();assertEquals(5,prefs.getInt("eq_preset",-1));assertTrue(prefs.getAll().keySet().stream().anyMatch(k->k.startsWith("eq_hz_")));
            waitText("Reset to Flat").click();assertEquals(0,prefs.getInt("eq_preset",-1));
            waitText("Enable equalizer").click();assertFalse(prefs.getBoolean("eq_enabled",true));assertNull(org.example.spotifymixer.runtime.EqualizerRuntime.device());
            com.spotify.playbacknative.AudioRenderer.destroyed(id);InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }finally{prefs.edit().putBoolean("eq_enabled",false).commit();org.example.spotifymixer.runtime.EqualizerRuntime.refresh();if(track!=null)track.release();device.pressHome();}
    }
    @Test public void themeStudioSavesPresetAndRestoresDefaults()throws Exception{
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);prefs.edit().clear().commit();context.startActivity(new Intent(context,FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try{waitText("Test library");tools().click();waitText("Theme Studio").click();assertFalse(waitText("Enable themes").isChecked());assertFalse(waitText("Apply theme").isEnabled());waitText("Enable themes").click();UiObject2 spinner=device.wait(Until.findObject(By.desc("Theme preset")),4000);assertNotNull(spinner);spinner.click();waitText("Midnight Ocean").click();waitText("Apply theme").click();assertTrue(prefs.getBoolean("theme_enabled",false));assertEquals("ocean",prefs.getString("theme_preset",""));waitText("Give Spotify a new mood");android.os.SystemClock.sleep(2500);screenshot("13-theme-studio-ocean");
            waitText("‹  Back to Spotify").click();waitText("Test library");screenshot("14-themed-native-view-fixture");tools().click();waitText("Theme Studio").click();waitText("Restore Spotify defaults").click();assertFalse(prefs.getBoolean("theme_enabled",true));assertFalse(waitText("Enable themes").isChecked());
        }finally{prefs.edit().remove("theme_enabled").remove("theme_preset").commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(context);device.pressHome();}
    }
    @Test public void themesMapAndRestoreOwnViewsWithoutTintingArtwork(){
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);prefs.edit().clear().commit();
        try(androidx.test.core.app.ActivityScenario<FixtureActivity> scenario=androidx.test.core.app.ActivityScenario.launch(FixtureActivity.class)){
            scenario.onActivity(activity->{android.widget.LinearLayout layout=new android.widget.LinearLayout(activity);layout.setOrientation(1);layout.setBackgroundColor(0xff121212);android.widget.TextView text=new android.widget.TextView(activity);text.setText("Original fixture text");text.setTextColor(0xffffffff);layout.addView(text);android.widget.ImageView image=new android.widget.ImageView(activity);android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff1ed760);image.setImageBitmap(bitmap);android.graphics.drawable.Drawable artwork=image.getDrawable();layout.addView(image);activity.setContentView(layout);
                prefs.edit().putBoolean("theme_enabled",true).putString("theme_preset","velvet").commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(activity);org.example.spotifymixer.runtime.ThemePalette p=org.example.spotifymixer.runtime.ThemePalette.find("velvet");assertEquals(p.background,((android.graphics.drawable.ColorDrawable)layout.getBackground()).getColor());assertEquals(p.text,text.getCurrentTextColor());assertSame(artwork,image.getDrawable());assertNull(image.getColorFilter());assertEquals(p.accent,org.example.spotifymixer.runtime.ThemeRuntime.mapColor(0xff1ed760));
                prefs.edit().putString("theme_preset","rose").commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(activity);assertEquals(org.example.spotifymixer.runtime.ThemePalette.find("rose").text,text.getCurrentTextColor());prefs.edit().putBoolean("theme_enabled",false).commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(activity);assertEquals(0xff121212,((android.graphics.drawable.ColorDrawable)layout.getBackground()).getColor());assertEquals(0xffffffff,text.getCurrentTextColor());assertEquals(0xff1ed760,org.example.spotifymixer.runtime.ThemeRuntime.mapColor(0xff1ed760));
            });
        }finally{prefs.edit().clear().commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(context);}
    }
    @Test public void nativeThemeHookExecutesOwnDexOnAndroid()throws Exception{
        String type="Lp/k5k1;";org.jf.dexlib2.iface.Method method=new org.jf.dexlib2.immutable.ImmutableMethod(type,"K",java.util.Collections.singletonList(new org.jf.dexlib2.immutable.ImmutableMethodParameter("J",java.util.Collections.emptySet(),null)),"I",9,java.util.Collections.emptySet(),java.util.Collections.emptySet(),new org.jf.dexlib2.immutable.ImmutableMethodImplementation(3,java.util.Arrays.asList(new org.jf.dexlib2.immutable.instruction.ImmutableInstruction31i(org.jf.dexlib2.Opcode.CONST,0,0xff121212),new org.jf.dexlib2.immutable.instruction.ImmutableInstruction11x(org.jf.dexlib2.Opcode.RETURN,0)),java.util.Collections.emptyList(),java.util.Collections.emptyList()));
        org.jf.dexlib2.iface.ClassDef cls=new org.jf.dexlib2.immutable.ImmutableClassDef(type,1,"Ljava/lang/Object;",java.util.Collections.emptyList(),null,java.util.Collections.emptySet(),java.util.Collections.emptyList(),java.util.Collections.singletonList(method));org.jf.dexlib2.writer.io.MemoryDataStore data=new org.jf.dexlib2.writer.io.MemoryDataStore();org.jf.dexlib2.writer.pool.DexPool.writeTo(data,new org.jf.dexlib2.immutable.ImmutableDexFile(org.jf.dexlib2.Opcodes.forApi(24),java.util.Collections.singletonList(cls)));
        java.lang.reflect.Method patch=Class.forName("org.example.spotifymixer.engine.ThemeHook").getDeclaredMethod("patch",byte[].class);patch.setAccessible(true);byte[][] transformed=(byte[][])patch.invoke(null,(Object)data.getData());java.io.File dex=java.io.File.createTempFile("own-theme-hook-",".dex",context.getCodeCacheDir());android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);
        try{
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(dex)){assertTrue(dex.setReadOnly());out.write(transformed[1]);}
            dalvik.system.DexClassLoader loader=new dalvik.system.DexClassLoader(dex.getAbsolutePath(),context.getCodeCacheDir().getAbsolutePath(),null,context.getClassLoader());java.lang.reflect.Method color=loader.loadClass("p.k5k1").getMethod("K",long.class);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{prefs.edit().putBoolean("theme_enabled",true).putString("theme_preset","ocean").commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(context);});assertEquals(org.example.spotifymixer.runtime.ThemePalette.find("ocean").background,((Integer)color.invoke(null,0L)).intValue());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{prefs.edit().putBoolean("theme_enabled",false).commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(context);});assertEquals(0xff121212,((Integer)color.invoke(null,0L)).intValue());
        }finally{dex.delete();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{prefs.edit().remove("theme_enabled").remove("theme_preset").commit();org.example.spotifymixer.runtime.ThemeRuntime.refresh(context);});}
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
                assertEquals("spotify:playlist:37i9dQZF1DX0XUsuxWHRQd",((String[])current.invoke(null))[0]);fragment[0].f=null;assertNull(current.invoke(null));
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
            waitText("Playlist Gamma");waitText("Playlist Delta");assertFalse(device.hasObject(By.text("Start Mix")));
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
