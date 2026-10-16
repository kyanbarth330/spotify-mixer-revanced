package org.example.spotifymixer.engine;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class MixPlannerTest {
    private List<String> tracks(int offset,int count){List<String> result=new ArrayList<>();for(int i=offset;i<offset+count;i++)result.add("spotify:track:"+String.format(java.util.Locale.ROOT,"%022d",i));return result;}
    @Test public void respectsQuotaAndIsDeterministic(){MixPlanner.Plan p=MixPlanner.plan(tracks(0,20),tracks(100,20),70,10);assertEquals(7,p.fromA);assertEquals(3,p.fromB);assertEquals(p.tracks,MixPlanner.plan(tracks(0,20),tracks(100,20),70,10).tracks);}
    @Test public void fallsBackWhenOneSourceIsExhausted(){MixPlanner.Plan p=MixPlanner.plan(tracks(0,2),tracks(100,20),70,10);assertEquals(10,p.tracks.size());assertEquals(2,p.fromA);assertEquals(8,p.fromB);}
    @Test public void deduplicatesAcrossSourcesAndIgnoresInvalidEntries(){List<String> a=new ArrayList<>(tracks(0,2));a.add(null);a.add("spotify:local:unavailable");a.add(a.get(0));MixPlanner.Plan p=MixPlanner.plan(a,tracks(0,3),50,10);assertEquals(3,p.tracks.size());assertEquals(3,new HashSet<>(p.tracks).size());}
    @Test public void handlesEndpointsEmptyAndRounding(){assertEquals(5,MixPlanner.plan(tracks(0,5),tracks(100,5),100,5).fromA);assertEquals(5,MixPlanner.plan(tracks(0,5),tracks(100,5),0,5).fromB);assertTrue(MixPlanner.plan(null,null,50,10).tracks.isEmpty());assertTrue(MixPlanner.plan(tracks(0,5),null,50,0).tracks.isEmpty());assertEquals(2,MixPlanner.plan(tracks(0,5),tracks(100,5),50,3).fromA);}
    @Test(expected=IllegalArgumentException.class)public void rejectsInvalidRatio(){MixPlanner.plan(null,null,101,10);}
}
