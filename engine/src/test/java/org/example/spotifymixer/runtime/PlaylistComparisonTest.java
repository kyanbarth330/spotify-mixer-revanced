package org.example.spotifymixer.runtime;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class PlaylistComparisonTest {
 private final String A="spotify:track:0000000000000000000001",B="spotify:track:0000000000000000000002",C="spotify:track:0000000000000000000003";
 @Test public void overlapUsesUniqueCanonicalIdentitiesAndStableOrder(){EnhancementTools.Comparison c=EnhancementTools.compare(Arrays.asList(B,A,B,null,"invalid"),Arrays.asList(A,C,C));assertEquals(Collections.singletonList(A),c.shared);assertEquals(Collections.singletonList(B),c.onlyA);assertEquals(Collections.singletonList(C),c.onlyB);}
 @Test public void cleanLinksAndUrisAreTheSameTrack(){EnhancementTools.Comparison c=EnhancementTools.compare(Arrays.asList(A,"https://open.spotify.com/track/0000000000000000000001?si=tracking"),Collections.singletonList(A));assertEquals(1,c.shared.size());assertTrue(c.onlyA.isEmpty());assertTrue(c.onlyB.isEmpty());}
 @Test public void emptySourcesDoNotInventMatches(){EnhancementTools.Comparison c=EnhancementTools.compare(Collections.emptyList(),Arrays.asList(B,A));assertTrue(c.shared.isEmpty());assertTrue(c.onlyA.isEmpty());assertEquals(Arrays.asList(B,A),c.onlyB);}
 @Test(expected=UnsupportedOperationException.class) public void resultCannotBeMutated(){EnhancementTools.compare(Collections.singletonList(A),Collections.singletonList(A)).shared.clear();}
}
