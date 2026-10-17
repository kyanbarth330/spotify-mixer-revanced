package org.example.spotifymixer.runtime;

import android.app.*;import android.content.*;import android.content.res.ColorStateList;import android.graphics.drawable.*;import android.os.*;import android.view.*;import android.widget.*;import java.lang.ref.WeakReference;import java.util.*;import org.example.spotifymixer.ui.*;
/** Native Compose color remap plus bounded traditional-view styling. No bitmap filters or permissions. */
public final class ThemeRuntime {
 private static volatile ThemePalette palette;private static boolean registered;private static WeakReference<Activity> main=new WeakReference<>(null);
 private static final Map<Activity,Binding> bindings=new WeakHashMap<>();
 public static int mapColor(int original){ThemePalette p=palette;return p==null?original:p.map(original);}
 public static void attach(Activity activity){
  try{load(activity);if(activity.getClass().getName().equals("com.spotify.music.MainActivity"))main=new WeakReference<>(activity);
   if(!registered){registered=true;activity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){public void onActivityCreated(Activity a,Bundle b){attach(a);}public void onActivityResumed(Activity a){attach(a);}public void onActivityDestroyed(Activity a){Binding binding=bindings.remove(a);if(binding!=null)binding.close();}public void onActivityStarted(Activity a){}public void onActivityPaused(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}});}
   if(!bindings.containsKey(activity)){Binding binding=new Binding(activity);bindings.put(activity,binding);binding.observe();}bindings.get(activity).request(true);
  }catch(RuntimeException e){Diagnostics.event("Theme attach error: "+e.getClass().getSimpleName());}
 }
 public static void load(Context context){SharedPreferences p=context.getSharedPreferences(Mixer.PREFS,0);palette=Auth.enabled(p,"themes")?ThemePalette.find(p.getString("theme_preset","amoled")):null;if(Auth.enabled(p,"amoled"))palette=(palette==null?ThemePalette.find("amoled"):palette).black();ThemePalette value=palette;
  if(value==null){EnhancerPanel.defaults();EnhancerTheme.BACKGROUND=0xff0c0f0d;EnhancerTheme.SURFACE=0xff1b211d;EnhancerTheme.GREEN=0xff1ed760;EnhancerTheme.MUTED=0xffb3b3b3;}
  else{EnhancerPanel.colors(value.background,value.surface,value.line,value.text,value.muted,value.accent);EnhancerTheme.BACKGROUND=value.background;EnhancerTheme.SURFACE=value.surface;EnhancerTheme.GREEN=value.accent;EnhancerTheme.MUTED=value.muted;}
 }
 public static void refresh(Context context){load(context);for(Binding binding:new ArrayList<>(bindings.values()))binding.request(true);}
 public static void reopen(Activity panel){Activity host=main.get();panel.finish();if(host!=null&&!host.isFinishing()&&!host.isDestroyed())host.getWindow().getDecorView().postDelayed(host::recreate,250);}
 private static final class Saved {ThemePalette palette;ColorStateList text,lastText,tint,lastTint,bgTint,lastBgTint;Drawable background;WeakReference<Drawable> lastBackground=new WeakReference<>(null);}
 private static final class Binding implements ViewTreeObserver.OnGlobalLayoutListener,Runnable {
  final WeakReference<Activity> activity;final WeakReference<View> root;final Map<View,Saved> saved=new WeakHashMap<>();final Handler handler=new Handler(Looper.getMainLooper());ViewTreeObserver observer;long last;boolean queued,closed;final int status,navigation,flags;
  Binding(Activity a){activity=new WeakReference<>(a);root=new WeakReference<>(a.getWindow().getDecorView());status=a.getWindow().getStatusBarColor();navigation=a.getWindow().getNavigationBarColor();flags=a.getWindow().getDecorView().getSystemUiVisibility();}
  void observe(){View r=root.get();if(r!=null){observer=r.getViewTreeObserver();observer.addOnGlobalLayoutListener(this);}}
  public void onGlobalLayout(){if(palette!=null)request(false);}
  void request(boolean immediate){if(closed)return;if(immediate){handler.removeCallbacks(this);queued=false;run();}else if(!queued){queued=true;handler.postDelayed(this,Math.max(0,250-(SystemClock.uptimeMillis()-last)));}}
  public void run(){queued=false;if(closed)return;View r=root.get();Activity a=activity.get();if(r==null||a==null||a.isDestroyed())return;last=SystemClock.uptimeMillis();
   ThemePalette p=palette;if(p==null){restore();a.getWindow().setStatusBarColor(status);a.getWindow().setNavigationBarColor(navigation);r.setSystemUiVisibility(flags);return;}
   a.getWindow().setStatusBarColor(p.background);a.getWindow().setNavigationBarColor(p.background);r.setSystemUiVisibility(r.getSystemUiVisibility()&~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
   ArrayDeque<View> todo=new ArrayDeque<>();todo.add(r);int n=0;while(!todo.isEmpty()&&n++<2000){View v=todo.remove();style(v);v.invalidate();if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)todo.add(group.getChildAt(i));}}
  }
  void style(View view){Saved state=saved.get(view);if(state==null){state=new Saved();saved.put(view,state);}Drawable bg=view.getBackground();Drawable lastBg=state.lastBackground.get();Drawable source=bg==lastBg&&lastBg!=null?state.background:bg;
   boolean same=state.palette==palette;
   if(!same||bg!=lastBg){Drawable changed=background(source);if(changed!=null){state.background=source;view.setBackground(changed);state.lastBackground=new WeakReference<>(changed);}else if(bg==lastBg&&lastBg!=null){view.setBackground(source);state.lastBackground=new WeakReference<>(null);state.background=null;}}
   ColorStateList bgNow=view.getBackgroundTintList();if(!(same&&bgNow==state.lastBgTint)){ColorStateList original=bgNow==state.lastBgTint?state.bgTint:bgNow,mapped=colors(original);if(mapped!=original){state.bgTint=original;state.lastBgTint=mapped;view.setBackgroundTintList(mapped);}else if(bgNow==state.lastBgTint&&bgNow!=null){view.setBackgroundTintList(original);state.bgTint=null;state.lastBgTint=null;}}
   if(view instanceof TextView){TextView text=(TextView)view;ColorStateList now=text.getTextColors(),original=now==state.lastText?state.text:now;if(!(same&&now==state.lastText)){ColorStateList mapped=colors(original);if(mapped!=original){state.text=original;state.lastText=mapped;text.setTextColor(mapped);}else if(now==state.lastText&&now!=null){text.setTextColor(original);state.text=null;state.lastText=null;}}}
   if(view instanceof ImageView){ImageView image=(ImageView)view;ColorStateList now=image.getImageTintList(),original=now==state.lastTint?state.tint:now;if(!(same&&now==state.lastTint)){ColorStateList mapped=colors(original);if(mapped!=original){state.tint=original;state.lastTint=mapped;image.setImageTintList(mapped);}else if(now==state.lastTint&&now!=null){image.setImageTintList(original);state.tint=null;state.lastTint=null;}}}
   state.palette=palette;
  }
  void restore(){for(Map.Entry<View,Saved> entry:new ArrayList<>(saved.entrySet())){View v=entry.getKey();Saved s=entry.getValue();if(v==null)continue;if(v.getBackground()==s.lastBackground.get()&&s.background!=null)v.setBackground(s.background);if(v.getBackgroundTintList()==s.lastBgTint&&s.lastBgTint!=null)v.setBackgroundTintList(s.bgTint);if(v instanceof TextView&&((TextView)v).getTextColors()==s.lastText)((TextView)v).setTextColor(s.text);if(v instanceof ImageView&&((ImageView)v).getImageTintList()==s.lastTint)((ImageView)v).setImageTintList(s.tint);v.invalidate();}saved.clear();}
  void close(){closed=true;handler.removeCallbacksAndMessages(null);if(observer!=null&&observer.isAlive())observer.removeOnGlobalLayoutListener(this);saved.clear();}
 }
 private static Drawable background(Drawable source){
  if(source instanceof ColorDrawable){int c=((ColorDrawable)source).getColor(),mapped=mapColor(c);return c==mapped?null:new ColorDrawable(mapped);}
  if(source instanceof GradientDrawable&&source.getConstantState()!=null){GradientDrawable old=(GradientDrawable)source;int[] values=old.getColors();ColorStateList solid=old.getColor();GradientDrawable copy=null;
   if(values!=null){int[] mapped=values.clone();boolean changed=false;for(int i=0;i<values.length;i++){mapped[i]=mapColor(values[i]);changed|=mapped[i]!=values[i];}if(changed){copy=(GradientDrawable)source.getConstantState().newDrawable().mutate();copy.setColors(mapped);}}
   else if(solid!=null){ColorStateList mapped=colors(solid);if(mapped!=solid){copy=(GradientDrawable)source.getConstantState().newDrawable().mutate();copy.setColor(mapped);}}return copy;
  }return null;
 }
 private static ColorStateList colors(ColorStateList original){if(original==null)return null;if(!original.isStateful()){int c=original.getDefaultColor(),m=mapColor(c);return c==m?original:ColorStateList.valueOf(m);}
  // Preserve arbitrary custom error/focus/checked states instead of flattening native selectors.
  return original;
 }
 private ThemeRuntime(){}
}
