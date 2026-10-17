package org.example.spotifymixer.runtime;

import android.content.*;
import android.media.audiofx.Equalizer;
import android.media.audiofx.AudioEffect;
import android.os.*;
import java.lang.reflect.*;

/** Pins the native listener contract to the supported input; no external services or session guessing. */
public final class EqualizerRuntime {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final EqualizerSession SESSION=new EqualizerSession(id->new AndroidDevice(id));
    private static SharedPreferences prefs;
    private static Object listener;
    private static String connection="Waiting for Spotify audio renderer";
    private EqualizerRuntime(){}
    public static void attach(Context context){
        if(Looper.myLooper()!=Looper.getMainLooper()){MAIN.post(()->attach(context.getApplicationContext()));return;}
        if(prefs==null)prefs=context.getApplicationContext().getSharedPreferences(Mixer.PREFS,0);
        if(listener==null)try{
            ClassLoader loader=context.getClassLoader();
            Class<?> renderer=Class.forName("com.spotify.playbacknative.AudioRenderer",false,loader);
            Class<?> contract=Class.forName("com.spotify.playbacknative.AudioDriverListener",false,loader);
            Object proxy=Proxy.newProxyInstance(loader,new Class<?>[]{contract},(self,method,args)->{
                if(method.getDeclaringClass()==Object.class){if(method.getName().equals("hashCode"))return System.identityHashCode(self);if(method.getName().equals("equals"))return self==args[0];return "SpotEnhancer audio listener";}
                try {
                String name=method.getName();
                if((name.equals("onAudioTrackCreated")||name.equals("onAudioTrackDestroyed"))&&args!=null&&args.length==1){
                    Class<?> adapter=Class.forName("com.spotify.playbacknative.AudioTrackAdapter",false,loader);
                    int id=(Integer)adapter.getMethod("getAudioSessionId").invoke(args[0]);
                    MAIN.post(()->{if(name.equals("onAudioTrackCreated"))SESSION.opened(id);else SESSION.closed(id);});
                }else if(name.equals("onPaused")&&args!=null&&args.length==2){int id=(Integer)args[1];if(Boolean.FALSE.equals(args[0]))MAIN.post(()->SESSION.opened(id));}
                }catch(ReflectiveOperationException|RuntimeException|LinkageError ignored){/* Never propagate a feature failure into the native audio callback. */}
                return null;
            });
            renderer.getMethod("addListener",contract).invoke(null,proxy);listener=proxy;connection="Connected to Spotify audio renderer";
        }catch(ReflectiveOperationException|RuntimeException|LinkageError e){connection="Audio renderer unavailable: "+e.getClass().getSimpleName();}
        refresh();
    }
    public static void refresh(){if(prefs!=null)SESSION.configure(Auth.enabled(prefs,"equalizer"),hz->level(hz));}
    public static int level(int hz){int preset=prefs==null?0:prefs.getInt("eq_preset",0);return preset==5?prefs.getInt("eq_hz_"+hz,0):EqualizerSession.presetLevel(preset,hz);}
    public static EqualizerSession.Device device(){return SESSION.device();}
    public static String status(){return SESSION.status()+"\n"+connection;}
    public static void retry(){SESSION.retry();}
    private static final class AndroidDevice implements EqualizerSession.Device {
        final Equalizer eq;
        AndroidDevice(int id){if(id<=0)throw new IllegalArgumentException("No local audio session");eq=new Equalizer(0,id);}
        public int bands(){return eq.getNumberOfBands();}
        public int frequency(int b){return eq.getCenterFreq((short)b)/1000;}
        public int minimum(){return eq.getBandLevelRange()[0];}
        public int maximum(){return eq.getBandLevelRange()[1];}
        public boolean hasControl(){return eq.hasControl();}
        public void level(int band,int value){eq.setBandLevel((short)band,(short)value);}
        public void enabled(boolean value){check(eq.setEnabled(value));}
        private static void check(int result){if(result!=AudioEffect.SUCCESS)throw new IllegalStateException("Audio effect rejected request ("+result+")");}
        public void release(){eq.release();}
    }
}
