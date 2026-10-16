package org.example.spotifymixer.runtime;
import android.os.Bundle;
import java.util.regex.*;
/** Accept only playlist identities, never an arbitrary Bundle.toString(). */
public final class PlaylistUri {
    public static String normalize(String value){
        if(value==null)return null;
        Matcher legacy=Pattern.compile("^spotify:user:[^:]+:playlist:([A-Za-z0-9]{22})$").matcher(value);
        if(legacy.matches())return "spotify:playlist:"+legacy.group(1);
        Matcher m=Pattern.compile("^(?:spotify:playlist:|https://open\\.spotify\\.com/(?:intl-[^/]+/)?playlist/)([A-Za-z0-9]{22})(?:\\?.*)?$").matcher(value);
        return m.matches()?"spotify:playlist:"+m.group(1):null;
    }
    static String fromBundle(Bundle bundle){return fromBundle(bundle,0);}
    private static String fromBundle(Bundle bundle,int depth){
        for(String key:new String[]{"playlist_uri","playlistUri","uri","view_uri","entity_uri"}){
            Object value=bundle.get(key);String uri=value instanceof String || value instanceof android.net.Uri?normalize(value.toString()):null;if(uri!=null)return uri;
        }
        if(depth<2)for(String key:bundle.keySet()){Object value=bundle.get(key);if(value instanceof Bundle){String uri=fromBundle((Bundle)value,depth+1);if(uri!=null)return uri;}}
        return null;
    }
}
