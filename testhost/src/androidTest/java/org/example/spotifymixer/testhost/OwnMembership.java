package org.example.spotifymixer.testhost;
import org.example.spotifymixer.runtime.Auth;import androidx.test.platform.app.InstrumentationRegistry;
/** Test-source-only membership state. No account/network, credentials or production bypass API. */
final class OwnMembership {
 static void set(boolean premium){InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{try{java.lang.reflect.Field flag=Auth.class.getDeclaredField("premium"),deadline=Auth.class.getDeclaredField("verifiedUntil");flag.setAccessible(true);deadline.setAccessible(true);flag.setBoolean(null,premium);deadline.setLong(null,premium?android.os.SystemClock.elapsedRealtime()+3600000:0);}catch(Exception e){throw new AssertionError(e);}});}
}
