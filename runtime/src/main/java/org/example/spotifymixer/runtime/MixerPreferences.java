package org.example.spotifymixer.runtime;

import android.content.SharedPreferences;

/** Explicit selection must reach disk before reporting success or leaving the screen. */
public final class MixerPreferences {
    private MixerPreferences(){}
    public static boolean select(SharedPreferences prefs,String slot,String uri,String title){
        if(!"a".equals(slot)&&!"b".equals(slot))throw new IllegalArgumentException("Invalid playlist slot");
        String normalized=PlaylistUri.normalize(uri);
        if(normalized==null)throw new IllegalArgumentException("Invalid playlist identity");
        return prefs.edit().putString(slot,normalized).putString(slot+"_title",title==null?"Selected playlist":title).remove("pending_slot").commit();
    }
}
