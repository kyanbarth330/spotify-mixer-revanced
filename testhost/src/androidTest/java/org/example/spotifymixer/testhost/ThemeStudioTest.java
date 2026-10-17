package org.example.spotifymixer.testhost;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.example.spotifymixer.runtime.SpotEnhancerActivity;
import org.example.spotifymixer.runtime.ThemeRuntime;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ThemeStudioTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final SharedPreferences prefs=context.getSharedPreferences("spotify-mixer-entry-v1",0);
    @Before public void setup(){prefs.edit().clear().putBoolean("theme_enabled",true).putString("theme_preset","ocean").commit();OwnMembership.set(true);}
    @After public void cleanup(){prefs.edit().clear().commit();OwnMembership.set(true);InstrumentationRegistry.getInstrumentation().runOnMainSync(()->ThemeRuntime.refresh(context));device.pressHome();}
    private UiObject2 find(BySelector selector){UiObject2 view=device.wait(Until.findObject(selector),8000);assertNotNull(selector.toString(),view);return view;}
    private void awaitPreference(String key,Object expected)throws Exception{
        long deadline=android.os.SystemClock.uptimeMillis()+5000;
        do{if(java.util.Objects.equals(expected,prefs.getAll().get(key)))return;android.os.SystemClock.sleep(50);}while(android.os.SystemClock.uptimeMillis()<deadline);
        capture("failure-theme-preference");assertEquals("Saved preference "+key,expected,prefs.getAll().get(key));
    }
    private void capture(String name)throws java.io.IOException{
        device.waitForIdle();device.executeShellCommand("mkdir -p /sdcard/Download/spotenhancer-evidence");
        device.executeShellCommand("screencap -p /sdcard/Download/spotenhancer-evidence/"+name+".png");
    }
    private ActivityScenario<SpotEnhancerActivity> launch(){return ActivityScenario.launch(new Intent(context,SpotEnhancerActivity.class).putExtra("screen","themes").putExtra("from_spotify",true));}
    @Test public void previewFavoritesAndSettingsSurviveRecreationWithoutApplying()throws Exception{
        try(ActivityScenario<SpotEnhancerActivity> scenario=launch()){
            find(By.desc("Search themes")).setText("velvet");
            find(By.desc("Favorite Velvet Purple")).click();awaitPreference("theme_favorites",java.util.Collections.singleton("velvet"));
            find(By.desc("Preview Velvet Purple")).click();
            assertEquals("ocean",prefs.getString("theme_preset",""));
            assertTrue(prefs.getStringSet("theme_favorites",java.util.Collections.emptySet()).contains("velvet"));
            scenario.recreate();
            assertEquals("velvet",find(By.desc("Search themes")).getText());
            assertTrue(find(By.desc("Preview Velvet Purple")).isSelected());
            find(By.desc("Search themes")).setText("");capture("20-theme-studio-library-velvet");
            find(By.text("Settings")).click();find(By.text("Appearance"));capture("21-theme-studio-settings-velvet");find(By.desc("True black")).click();
            assertFalse(prefs.getBoolean("amoled_enabled",false));
            scenario.recreate();
            assertTrue(find(By.desc("True black")).isChecked());
            assertEquals("ocean",prefs.getString("theme_preset",""));
            find(By.text("Apply")).click();awaitPreference("theme_preset","velvet");awaitPreference("amoled_enabled",true);
            assertEquals("velvet",prefs.getString("theme_preset",""));assertTrue(prefs.getBoolean("amoled_enabled",false));
            new UiScrollable(new UiSelector().className("android.widget.ScrollView")).scrollTextIntoView("Restore defaults");
            find(By.text("Restore defaults")).click();find(By.res("android","button1")).click();
            awaitPreference("theme_enabled",false);awaitPreference("amoled_enabled",false);
            assertFalse(prefs.getBoolean("theme_enabled",true));assertFalse(prefs.getBoolean("amoled_enabled",true));
            assertTrue(prefs.getStringSet("theme_favorites",java.util.Collections.emptySet()).contains("velvet"));
        }
    }
    @Test public void filterAndEmptyStateAreRealAndPremiumCannotBeBypassed(){
        try(ActivityScenario<SpotEnhancerActivity> scenario=launch()){
            find(By.text("Warm")).click();assertTrue(find(By.desc("Preview Rose Gold")).isEnabled());
            assertFalse(device.hasObject(By.desc("Preview Midnight Ocean")));
            find(By.desc("Search themes")).setText("not-a-theme");
            find(By.text("No themes found\nTry another name or filter."));
            OwnMembership.set(false);scenario.recreate();
            assertFalse(device.hasObject(By.text("Apply")));assertFalse(device.hasObject(By.desc("Enable themes")));
            assertEquals("ocean",prefs.getString("theme_preset",""));
        }
    }
}
