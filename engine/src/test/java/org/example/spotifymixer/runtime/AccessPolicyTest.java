package org.example.spotifymixer.runtime;
import org.junit.Test;import static org.junit.Assert.*;
public class AccessPolicyTest {
 @Test public void splitIsSixFreeOfThirtyTwo(){assertEquals(32,AccessPolicy.ALL.size());assertEquals(6,AccessPolicy.FREE.size());assertTrue(AccessPolicy.ALL.containsAll(AccessPolicy.FREE));}
 @Test public void freeCannotOpenCreatorOrEnableThemes(){assertFalse(AccessPolicy.allowed("creator",false));assertFalse(AccessPolicy.allowed("themes",false));assertTrue(AccessPolicy.allowed("mixer",false));assertTrue(AccessPolicy.allowed("details",false));}
 @Test public void premiumOnlyUnlocksKnownTools(){for(String id:AccessPolicy.ALL)assertTrue(AccessPolicy.allowed(id,true));assertFalse(AccessPolicy.allowed("unknown",true));}
 @Test public void accountAndDiagnosticsAreNeverPaywalled(){assertTrue(AccessPolicy.allowed("account",false));assertTrue(AccessPolicy.allowed("diagnostics",false));}
}
