package org.example.spotifymixer.runtime;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class CreatorIntentTest {
 private CreatorSession.Request request(String genres,String artists,String description){return new CreatorSession.Request("Party",genres,artists,description,20);}
 private CreatorSession.Track track(int id,String title,String artist){return new CreatorSession.Track(String.format(Locale.ROOT,"spotify:track:%022d",id),title,artist);}
 @Test public void englishDutchPartyPhraseExpandsIntoLocalSearches(){List<String> q=CreatorWire.queries(request("","","Dutch party songs"));assertEquals(6,q.size());assertTrue(q.contains("genre:\"carnaval\""));assertTrue(q.contains("Hollandse meezingers"));assertFalse(q.contains("Dutch party songs"));}
 @Test public void dutchPhraseInGenreFieldIsNotSentAsFictionalGenre(){List<String> q=CreatorWire.queries(request("Nederlandse feestmuziek","",""));assertEquals(6,q.size());assertFalse(q.get(0).contains("genre:\"Nederlandse feestmuziek\""));}
 @Test public void explicitArtistIsPreservedInEveryExpandedSearch(){for(String q:CreatorWire.queries(request("","My Artist","Dutch party songs")))assertTrue(q.contains("artist:\"My Artist\""));}
 @Test public void additionalKeywordsAndDecadeAreRetained(){for(String q:CreatorWire.queries(request("","","Dutch party songs 90s acoustic"))){assertTrue(q.contains("year:1990-1999"));assertTrue(q.contains("acoustic"));}}
 @Test public void unrelatedWordsDoNotTriggerDutchParty(){assertEquals(Collections.singletonList("third party library"),CreatorWire.queries(request("","","third party library")));}
 @Test public void selectionSpreadsArtistsAndDeduplicatesWithoutDroppingCandidates(){CreatorSession.Track a=track(1,"One","A"),b=track(2,"Two","A"),c=track(3,"Three","B");List<CreatorSession.Track> out=CreatorIntent.diversify(Arrays.asList(a,b,c,a));assertEquals(Arrays.asList(a,c,b),out);}
 @Test public void unwantedKaraokeIsFilteredButExplicitRequestWins(){CreatorSession.Track t=track(1,"Hit (Karaoke Version)","Fixture Artist");assertFalse(CreatorIntent.accept(t,request("","","Dutch party songs")));assertTrue(CreatorIntent.accept(t,request("","","karaoke")));assertTrue(CreatorIntent.accept(track(2,"Cover Me In Sunshine","Fixture Artist"),request("","","pop")));}
 @Test public void selectorCombinationsStayBoundedAndRepresented(){List<String> q=CreatorWire.queries(request("pop,rock","A,B","Dutch party songs"));assertEquals(12,q.size());assertTrue(q.stream().anyMatch(s->s.contains("genre:\"rock\" artist:\"B\"")));}
}
