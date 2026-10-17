package org.example.spotifymixer.testhost;

import android.content.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Two separate instrumentation processes; no Spotify account or playback involved. */
@RunWith(AndroidJUnit4.class)
public class RestartPersistenceTest {
    @Test public void replacementSurvivesProcessStop(){
        String phase=InstrumentationRegistry.getArguments().getString("persistence");
        Assume.assumeTrue("Run explicitly in write/read phases",phase!=null);
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs=context.getSharedPreferences("spotify-mixer-entry-v1",0);
        org.example.spotifymixer.runtime.MixLibrary library=new org.example.spotifymixer.runtime.MixLibrary(prefs);
        try{
            if("write".equals(phase)){
                prefs.edit().clear().commit();library.select(0,"spotify:playlist:0000000000000000000001","Gamma");library.select(1,"spotify:playlist:0000000000000000000002","Delta");library.select(2,"spotify:playlist:0000000000000000000003","Indie");
                org.example.spotifymixer.runtime.MixConfig config=new org.example.spotifymixer.runtime.MixConfig(library.active().sources,true).balance(0,50);library.save(config);library.add("Restart preset",config,false,0);library.add("Restart fixture history",config,true,12501);
                assertTrue(prefs.edit().putBoolean("theme_enabled",true).putString("theme_preset","ocean").putBoolean("eq_enabled",true).putInt("eq_preset",5).putInt("eq_hz_60",350).putBoolean("volume_enabled",true).putInt("volume_steps",4).putBoolean("guard_enabled",true).putBoolean("sleep_enabled",true).putInt("sleep_minutes",25).putString("queue_clear_pending","own-fixture-current").commit());new org.example.spotifymixer.runtime.SettingsProfiles(prefs).save("Restart tools");InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            }else{
                assertEquals("read",phase);assertTrue(prefs.getBoolean("volume_enabled",false));assertEquals(4,prefs.getInt("volume_steps",0));assertTrue(prefs.getBoolean("guard_enabled",false));assertEquals(25,prefs.getInt("sleep_minutes",0));assertEquals("own-fixture-current",prefs.getString("queue_clear_pending",null));assertTrue(prefs.getString("settings_profiles","").contains("Restart tools"));assertFalse(prefs.getString("settings_profiles","").contains("own-fixture-current"));assertEquals(0,org.example.spotifymixer.runtime.MediaToolsRuntime.remaining());assertTrue(prefs.getBoolean("eq_enabled",false));assertEquals(5,prefs.getInt("eq_preset",-1));assertEquals(350,prefs.getInt("eq_hz_60",0));assertTrue(prefs.getBoolean("theme_enabled",false));assertEquals("ocean",prefs.getString("theme_preset",""));org.example.spotifymixer.runtime.MixConfig config=library.active();assertEquals(3,config.sources.size());assertEquals("Gamma",config.sources.get(0).title);assertEquals("spotify:playlist:0000000000000000000002",config.sources.get(1).uri);assertEquals(50,config.sources.get(0).weight);assertTrue(config.shuffle);assertEquals(3,library.entries(false).get(0).config.sources.size());assertEquals(12501,library.entries(true).get(0).tracks);
            }
        }catch(java.io.IOException|org.json.JSONException error){throw new AssertionError(error);}
    }
}
