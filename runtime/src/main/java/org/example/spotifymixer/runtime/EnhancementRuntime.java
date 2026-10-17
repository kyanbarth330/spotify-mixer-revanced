package org.example.spotifymixer.runtime;
import android.app.*;import android.content.*;import android.media.*;import android.media.session.*;import android.os.*;import android.view.*;import android.widget.*;import java.lang.ref.WeakReference;import java.lang.reflect.*;import java.util.*;
/** Foreground window-key tweaks and opt-in local headphone rules. No system input interception. */
public final class EnhancementRuntime {
 private static SharedPreferences prefs;private static Context app;private static boolean registered,started;
 private static final Map<Activity,WeakReference<Window.Callback>> windows=new WeakHashMap<>();private static final Map<Activity,ViewTweaks> views=new WeakHashMap<>();
 private static AudioDeviceCallback devices;private static final Set<Integer> headphones=new HashSet<>();
 public static void attach(Activity a){
  try{
   if(prefs==null){app=a.getApplicationContext();prefs=app.getSharedPreferences(Mixer.PREFS,0);}MediaToolsRuntime.attach(a);
   if(!registered){registered=true;a.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity a,Bundle b){}public void onActivityResumed(Activity a){bind(a);}public void onActivityDestroyed(Activity a){ViewTweaks v=views.remove(a);if(v!=null)v.close();windows.remove(a);}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivityPaused(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}});}
   bind(a);refresh();
   if(!started&&a.getClass().getName().equals("com.spotify.music.MainActivity")){started=true;if(prefs.getBoolean("start_enabled",false)&&Intent.ACTION_MAIN.equals(a.getIntent().getAction())&&a.getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)&&a.getIntent().getData()==null){String route=prefs.getString("start_route","spotify:home");if(Arrays.asList("spotify:home","spotify:collection:playlists","spotify:search").contains(route))a.getWindow().getDecorView().post(()->navigate(a,route));}}
  }catch(RuntimeException error){Diagnostics.event("Enhancement attach unavailable: "+error.getClass().getSimpleName());}
 }
 private static void bind(Activity a){
  if(!windows.containsKey(a)||windows.get(a).get()!=a.getWindow().getCallback()){Window w=a.getWindow();Window.Callback original=w.getCallback();if(original!=null){WeakReference<Activity> weak=new WeakReference<>(a);Window.Callback proxy=(Window.Callback)Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),new Class<?>[]{Window.Callback.class},(self,method,args)->{
    if(method.getName().equals("dispatchKeyEvent")&&args!=null&&args[0] instanceof KeyEvent){Activity host=weak.get();if(host!=null&&key(host,(KeyEvent)args[0]))return true;}
    try{return method.invoke(original,args);}catch(InvocationTargetException e){throw e.getCause();}
   });windows.put(a,new WeakReference<>(proxy));w.setCallback(proxy);}}
  if(!views.containsKey(a))views.put(a,new ViewTweaks(a));views.get(a).refresh();
 }
 private static boolean key(Activity a,KeyEvent event){int code=event.getKeyCode();if(prefs.getBoolean("volume_enabled",false)&&(code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_VOLUME_DOWN)&&MediaToolsRuntime.local(MediaToolsRuntime.controller())){if(event.getAction()==KeyEvent.ACTION_DOWN){AudioManager am=(AudioManager)a.getSystemService(Context.AUDIO_SERVICE);int value=EnhancementTools.volume(am.getStreamVolume(AudioManager.STREAM_MUSIC),am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),code==KeyEvent.KEYCODE_VOLUME_UP?1:-1,Math.max(1,Math.min(10,prefs.getInt("volume_steps",1))));try{am.setStreamVolume(AudioManager.STREAM_MUSIC,value,AudioManager.FLAG_SHOW_UI);}catch(SecurityException ignored){return false;}}return true;}
  if(prefs.getBoolean("skip_guard_enabled",false)&&(code==KeyEvent.KEYCODE_MEDIA_NEXT||code==KeyEvent.KEYCODE_MEDIA_PREVIOUS)){if(event.getAction()!=KeyEvent.ACTION_DOWN||event.getRepeatCount()>0)return true;int direction=code==KeyEvent.KEYCODE_MEDIA_NEXT?1:-1;try{MediaToolsRuntime.skip(direction);}catch(RuntimeException e){Toast.makeText(a,e.getMessage(),Toast.LENGTH_SHORT).show();}return true;}
  return false;
 }
 public static void navigate(Activity a,String uri){try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(uri)).setPackage(a.getPackageName()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP));}catch(RuntimeException error){Toast.makeText(a,"Spotify could not open this screen",Toast.LENGTH_LONG).show();}}
 private static boolean headphone(AudioDeviceInfo d){int type=d.getType();return type==AudioDeviceInfo.TYPE_WIRED_HEADSET||type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_USB_HEADSET||(Build.VERSION.SDK_INT>=31&&(type==AudioDeviceInfo.TYPE_BLE_HEADSET));}
 public static void refresh(){
  if(prefs==null)return;MediaToolsRuntime.refresh();for(ViewTweaks v:new ArrayList<>(views.values()))v.refresh();
  AudioManager am=(AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
  if(!prefs.getBoolean("rules_enabled",false)){if(devices!=null)am.unregisterAudioDeviceCallback(devices);devices=null;headphones.clear();return;}
  if(devices!=null)return;for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(headphone(d))headphones.add(d.getId());
  devices=new AudioDeviceCallback(){public void onAudioDevicesAdded(AudioDeviceInfo[] added){boolean had=!headphones.isEmpty(),fresh=false;for(AudioDeviceInfo d:added)if(d.isSink()&&headphone(d))fresh|=headphones.add(d.getId());if(!had&&fresh&&prefs.getBoolean("rules_resume",false)&&MediaToolsRuntime.supports(PlaybackState.ACTION_PLAY))try{MediaToolsRuntime.play();}catch(RuntimeException ignored){}}
   public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){boolean had=!headphones.isEmpty();for(AudioDeviceInfo d:removed)headphones.remove(d.getId());if(had&&headphones.isEmpty()&&prefs.getBoolean("rules_pause",true)&&MediaToolsRuntime.supports(PlaybackState.ACTION_PAUSE))try{MediaToolsRuntime.pause();}catch(RuntimeException ignored){}}
  };am.registerAudioDeviceCallback(devices,MediaToolsRuntime.MAIN);
 }
 private EnhancementRuntime(){}
}
