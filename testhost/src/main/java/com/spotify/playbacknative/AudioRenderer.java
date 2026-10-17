package com.spotify.playbacknative;
/** Original in-memory fixture event bus, not Spotify audio code. */
public final class AudioRenderer {
 private static final java.util.concurrent.CopyOnWriteArrayList<AudioDriverListener> listeners=new java.util.concurrent.CopyOnWriteArrayList<>();
 public static void addListener(AudioDriverListener listener){listeners.addIfAbsent(listener);}
 public static void created(int session){for(AudioDriverListener listener:listeners)listener.onAudioTrackCreated(()->session);}
 public static void destroyed(int session){for(AudioDriverListener listener:listeners)listener.onAudioTrackDestroyed(()->session);}
 public static void resumed(int session){for(AudioDriverListener listener:listeners)listener.onPaused(false,session);}
}
