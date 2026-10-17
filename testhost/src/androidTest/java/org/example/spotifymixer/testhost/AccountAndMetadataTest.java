package org.example.spotifymixer.testhost;
import android.content.*;import android.graphics.*;import android.util.LruCache;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.uiautomator.*;import org.junit.*;import static org.junit.Assert.*;import org.example.spotifymixer.runtime.*;
/** Only own entitlement states and original synthetic cover/metadata. No live account or CDN. */
@org.junit.runner.RunWith(AndroidJUnit4.class)
public class AccountAndMetadataTest {
 private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();private final UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());private final SharedPreferences prefs=context.getSharedPreferences("spotify-mixer-entry-v1",0);
 @Before public void before(){OwnMembership.set(false);prefs.edit().clear().commit();}
 @After public void after(){NativeBridge.capturePlaylist(null);CreatorProtocolFixture.cover="";prefs.edit().clear().commit();OwnMembership.set(true);device.pressHome();}
 private void open(String screen){context.startActivity(new Intent(context,SpotEnhancerActivity.class).putExtra("screen",screen).putExtra("from_spotify",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));InstrumentationRegistry.getInstrumentation().waitForIdleSync();device.waitForIdle();}
 private UiObject2 text(String value){UiObject2 result=device.wait(Until.findObject(By.text(value)),5000);assertNotNull(value,result);return result;}
 private void shot(String name)throws Exception{device.executeShellCommand("mkdir -p /sdcard/Download/spotenhancer-evidence");device.executeShellCommand("screencap -p /sdcard/Download/spotenhancer-evidence/"+name+".png");}
 @Test public void freeLocksPremiumAndKeepsAccountAccessible()throws Exception{
  prefs.edit().putBoolean("volume_enabled",true).putBoolean("theme_enabled",true).putBoolean("eq_enabled",true).commit();assertFalse(Auth.enabled(prefs,"volume"));assertFalse(Auth.enabled(prefs,"themes"));assertFalse(Auth.enabled(prefs,"equalizer"));assertTrue(Auth.allowed("mixer"));
  open("creator");text("Open account").click();text("Your SpotEnhancer account");text("Sign in");assertNotNull(device.findObject(By.desc("SpotEnhancer Username")));shot("25-free-account-lock-own-fixture");
  OwnMembership.set(true);assertTrue(Auth.allowed("creator"));OwnMembership.set(false);assertFalse(Auth.allowed("creator"));
 }
 @Test public void stalePremiumControlsCannotSaveAfterExpiry()throws Exception{
  OwnMembership.set(true);prefs.edit().putBoolean("volume_enabled",true).commit();open("tool:volume");UiObject2 input=device.wait(Until.findObject(By.desc("Volume steps (1–10)")),5000);assertNotNull(input);input.setText("8");OwnMembership.set(false);text("Save volume steps").click();InstrumentationRegistry.getInstrumentation().waitForIdleSync();assertFalse(prefs.contains("volume_steps"));assertFalse(Auth.enabled(prefs,"volume"));
 }
 @Test public void tokenEncryptionUsesKeystoreWithoutPlaintextPersistence()throws Exception{
  java.lang.reflect.Method encrypt=Auth.class.getDeclaredMethod("encrypt",String.class),decrypt=Auth.class.getDeclaredMethod("decrypt",String.class);encrypt.setAccessible(true);decrypt.setAccessible(true);String token="a".repeat(64);String encoded=(String)encrypt.invoke(null,token);assertFalse(encoded.contains(token));assertEquals(token,decrypt.invoke(null,encoded));assertNull(context.getSharedPreferences("spotenhancer-account-secure-v1",0).getString("session",null));
 }
 @Test public void mixerLoadsNameAndOwnCoverBeforeTrackPreparation()throws Exception{
  CreatorProtocolFixture.name="Own real metadata fixture";CreatorProtocolFixture.writes=0;CreatorProtocolFixture.wrongReadback=false;CreatorProtocolFixture.cover="own-fixture://cover";CreatorProtocolFixture.tracks=new java.util.ArrayList<>(java.util.Collections.singletonList("spotify:track:0000000000000000000001"));NativeBridge.capturePlaylist(new CreatorProtocolFixture());
  java.lang.reflect.Field cache=PlaylistMetadataStore.class.getDeclaredField("IMAGES");cache.setAccessible(true);Bitmap bitmap=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xff2255aa);((LruCache<String,Bitmap>)cache.get(null)).put(CreatorProtocolFixture.cover,bitmap);
  new MixLibrary(prefs).select(0,"spotify:playlist:0000000000000000000001","Selected playlist");open("mixer");text("Own real metadata fixture");assertNotNull(device.wait(Until.findObject(By.desc("Playlist cover")),5000));assertEquals(1,CreatorProtocolFixture.lastRange);assertEquals(0,CreatorProtocolFixture.writes);assertEquals("Own real metadata fixture",new MixLibrary(prefs).active().sources.get(0).title);shot("26-playlist-name-cover-own-fixture");
 }
}
