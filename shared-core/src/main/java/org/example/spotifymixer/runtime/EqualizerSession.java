package org.example.spotifymixer.runtime;

/** Own session-scoped equalizer lifecycle. Never uses the deprecated global session zero. */
public final class EqualizerSession {
    public interface Device {
        int bands(); int frequency(int band); int minimum(); int maximum();
        boolean hasControl(); void level(int band, int millibels); void enabled(boolean value); void release();
    }
    public interface Factory { Device create(int session) throws Exception; }
    public interface Levels { int at(int frequency); }
    private final Factory factory;
    private Device device;
    private int session;
    private boolean wanted;
    private Levels levels;
    private String status="Disabled";
    public EqualizerSession(Factory factory){this.factory=factory;}
    public synchronized void configure(boolean enabled, Levels levels){this.wanted=enabled;this.levels=levels;if(!enabled){release();status="Disabled";}else apply();}
    public synchronized void opened(int id){if(id<=0)return;if(id!=session){release();session=id;}apply();}
    public synchronized void closed(int id){if(id==session){release();session=0;status=wanted?"Waiting for local playback":"Disabled";}}
    public synchronized void retry(){release();apply();}
    private void apply(){
        if(!wanted)return;
        if(session<=0){status="Waiting for local playback";return;}
        try{
            if(device==null)device=factory.create(session);
            if(device.bands()<1||device.bands()>64||device.minimum()>device.maximum())throw new IllegalStateException("Invalid equalizer capabilities");
            if(!device.hasControl())throw new IllegalStateException("Another equalizer controls this session");
            for(int i=0;i<device.bands();i++)device.level(i,Math.max(device.minimum(),Math.min(device.maximum(),levels.at(device.frequency(i)))));
            device.enabled(true);status="Active on local audio session";
        }catch(Exception|LinkageError error){release();status="Unavailable: "+(error.getMessage()==null?error.getClass().getSimpleName():error.getMessage());}
    }
    private void release(){if(device!=null){try{if(device.hasControl())device.enabled(false);}catch(RuntimeException ignored){}try{device.release();}catch(RuntimeException ignored){}device=null;}}
    public synchronized Device device(){return device;}
    public synchronized String status(){try{if(device!=null&&!device.hasControl())return "Unavailable: another equalizer controls this session";}catch(RuntimeException error){release();status="Unavailable: audio session changed";}return status;}
    public static final String[] PRESETS={"Flat","Bass Boost","Vocal","Treble Boost","Warm","Custom"};
    private static final int[] FREQUENCIES={60,230,910,3600,14000};
    private static final int[][] CURVES={{0,0,0,0,0},{500,300,0,-100,-100},{-200,-100,300,200,0},{-100,-100,0,200,400},{200,100,0,-100,-200},{0,0,0,0,0}};
    public static int presetLevel(int preset,int hz){
        int[] curve=CURVES[Math.max(0,Math.min(CURVES.length-1,preset))];
        if(hz<=FREQUENCIES[0])return curve[0];
        for(int i=1;i<FREQUENCIES.length;i++)if(hz<=FREQUENCIES[i]){double t=Math.log((double)hz/FREQUENCIES[i-1])/Math.log((double)FREQUENCIES[i]/FREQUENCIES[i-1]);return (int)Math.round(curve[i-1]+t*(curve[i]-curve[i-1]));}
        return curve[curve.length-1];
    }
}
