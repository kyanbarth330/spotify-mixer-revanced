package org.example.spotifymixer.runtime;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaylistUriTest {
    private static final String ID="37i9dQZF1DXcBWIGoYBM5M";
    @Test public void acceptsCanonicalPlaylistIdentity(){assertEquals("spotify:playlist:"+ID,PlaylistUri.normalize("spotify:playlist:"+ID));}
    @Test public void normalizesSharedAndLocalizedLinks(){assertEquals("spotify:playlist:"+ID,PlaylistUri.normalize("https://open.spotify.com/playlist/"+ID+"?si=abc"));assertEquals("spotify:playlist:"+ID,PlaylistUri.normalize("https://open.spotify.com/intl-en/playlist/"+ID));}
    @Test public void rejectsNonPlaylistAndMalformedIdentities(){assertNull(PlaylistUri.normalize("spotify:album:"+ID));assertNull(PlaylistUri.normalize("spotify:playlist:short"));assertNull(PlaylistUri.normalize("https://evil.example/playlist/"+ID));assertNull(PlaylistUri.normalize(null));}
}
