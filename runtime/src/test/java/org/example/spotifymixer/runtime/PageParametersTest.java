package org.example.spotifymixer.runtime;
import android.os.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class PageParametersTest {
    private static final String A="spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",B="spotify:playlist:37i9dQZF1DX0XUsuxWHRQd";
    static class Parameters implements Parcelable {
        private final String a,b;
        private static final String unrelated=B;
        Parameters(String uri,String extra){a=uri;b=extra;}
        public int describeContents(){return 0;}public void writeToParcel(Parcel p,int flags){}
    }
    static class Child extends Parameters{Child(String uri){super(uri,"A title");}}
    @Test public void readsPlaylistFromOpaqueParcelable(){assertEquals(A,PageParameters.playlist(new Parameters(A,"Playlist title")));}
    @Test public void readsInheritedParameterFields(){assertEquals(A,PageParameters.playlist(new Child(A)));}
    @Test public void rejectsConflictingIdentities(){assertNull(PageParameters.playlist(new Parameters(A,B)));}
    @Test public void acceptsRepeatedIdentityAndIgnoresStaticFields(){assertEquals(A,PageParameters.playlist(new Parameters(A,A)));}
    @Test public void rejectsBareIdsTracksAndArbitraryObjects(){assertNull(PageParameters.playlist(new Parameters("37i9dQZF1DXcBWIGoYBM5M","spotify:track:37i9dQZF1DXcBWIGoYBM5M")));assertNull(PageParameters.playlist(new Object()));}
    @Test public void diagnosticDescribesTypesWithoutValues(){String text=PageParameters.describe(new Parameters(A,"Private playlist title"));assertTrue(text.contains("java.lang.String"));assertFalse(text.contains(A));assertFalse(text.contains("Private playlist title"));}
}
