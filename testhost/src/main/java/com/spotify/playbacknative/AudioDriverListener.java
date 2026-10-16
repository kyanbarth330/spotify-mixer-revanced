package com.spotify.playbacknative;
/** Handwritten fixture contract, limited to events exercised by our adapter. */
public interface AudioDriverListener {
 void onAudioTrackCreated(AudioTrackAdapter track);
 void onAudioTrackDestroyed(AudioTrackAdapter track);
 void onPaused(boolean paused,int session);
}
