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
        if("write".equals(phase)){
            prefs.edit().clear().putString("a","spotify:playlist:37i9dQZF1DXcBWIGoYBM5M").putString("b","spotify:playlist:37i9dQZF1DX0XUsuxWHRQd").commit();
            assertTrue(prefs.edit().putBoolean("shuffle",true).putInt("ratio",40).commit());
            assertTrue(org.example.spotifymixer.runtime.MixerPreferences.select(prefs,"a","spotify:playlist:0000000000000000000001","Playlist Gamma"));
            assertTrue(org.example.spotifymixer.runtime.MixerPreferences.select(prefs,"b","spotify:playlist:0000000000000000000002","Playlist Delta"));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }else{
            assertEquals("read",phase);
            assertEquals("spotify:playlist:0000000000000000000001",prefs.getString("a",null));
            assertEquals("spotify:playlist:0000000000000000000002",prefs.getString("b",null));
            assertEquals("Playlist Gamma",prefs.getString("a_title",null));assertEquals("Playlist Delta",prefs.getString("b_title",null));
            assertEquals(40,prefs.getInt("ratio",0));assertTrue(prefs.getBoolean("shuffle",false));
        }
    }
}
