package org.example.spotifymixer.runtime;

import android.app.Activity;
import org.example.spotifymixer.ui.EnhancerTheme;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;
import java.util.*;

/** Floating in-app navigation and exact-version playlist context bridge. No window overlays. */
public final class Mixer {
    static final String PREFS="spotify-mixer-entry-v1";
    private static final String ROW="spotenhancer-navigation";
    private static final WeakHashMap<View,java.lang.ref.WeakReference<Object>> contexts=new WeakHashMap<>();
    public static void attach(Activity activity) {
        ThemeRuntime.attach(activity);
        activity.getWindow().getDecorView().post(() -> {
            try {
                FrameLayout host=activity.findViewById(android.R.id.content);
                if(host==null || host.findViewWithTag(ROW)!=null)return;
                ImageButton tools=new ImageButton(activity);tools.setTag(ROW);tools.setContentDescription("Open SpotEnhancer");tools.setImageDrawable(new org.example.spotifymixer.ui.EnhancerIcon("tools",0xff07150c));
                tools.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33000000),EnhancerTheme.surface(EnhancerTheme.GREEN,28,activity),null));
                int pad=EnhancerTheme.dp(activity,14);tools.setPadding(pad,pad,pad,pad);tools.setElevation(EnhancerTheme.dp(activity,6));
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(EnhancerTheme.dp(activity,52),EnhancerTheme.dp(activity,52),Gravity.RIGHT|Gravity.BOTTOM);lp.rightMargin=EnhancerTheme.dp(activity,12);lp.bottomMargin=EnhancerTheme.dp(activity,140);host.addView(tools,lp);
                tools.setOnClickListener(v->{
                    String[] current=currentPlaylist();android.content.SharedPreferences prefs=activity.getSharedPreferences(PREFS,0);String slot=prefs.getString("pending_slot","");
                    if(slot.startsWith("source:")&&current!=null){
                        try{new MixLibrary(prefs).select(Integer.parseInt(slot.substring(7)),current[0],current[1]);launchMixer(activity);}
                        catch(Exception error){Toast.makeText(activity,error.getMessage(),Toast.LENGTH_LONG).show();}
                    }else launch(activity,current==null?null:current[0],current==null?null:current[1]);
                });
                Diagnostics.event("In-app SpotEnhancer navigation added");
            } catch(RuntimeException | LinkageError error){Diagnostics.event("Navigation error: "+error.getClass().getSimpleName());Log.w("SpotEnhancer","Cannot add navigation",error);}
        });
    }
    private static String[] currentPlaylist(){
        String[] selected=null;
        for(Map.Entry<View,java.lang.ref.WeakReference<Object>> entry:new ArrayList<>(contexts.entrySet())){
            View root=entry.getKey();if(root==null || !root.isAttachedToWindow() || !root.isShown() || !root.getGlobalVisibleRect(new android.graphics.Rect()))continue;
            Object fragment=entry.getValue().get();if(fragment==null)continue;
            String[] value=readPlaylist(root,fragment);if(value==null)continue;
            if(selected!=null && !selected[0].equals(value[0]))return null;
            selected=value;
        }
        return selected;
    }
    static String[] readPlaylist(View root,Object fragment){return readPlaylist(root,fragment,true);}
    static String[] readPlaylist(View root,Object fragment,boolean withTitle){
        try{Class<?> base=Class.forName("p.gs10",false,fragment.getClass().getClassLoader());Field args=base.getDeclaredField("f"),view=base.getDeclaredField("e1");args.setAccessible(true);view.setAccessible(true);if(view.get(fragment)!=root)return null;Bundle bundle=(Bundle)args.get(fragment);if(bundle==null)return null;String uri=PlaylistUri.fromBundle(bundle);if(uri==null)return null;String title=withTitle?PlaylistTitle.from(bundle,root):null;return new String[]{uri,title==null?"Selected playlist":title};}catch(ReflectiveOperationException|RuntimeException error){return null;}
    }
    public static void fragmentResumed(Object manager) {
        try {
            Diagnostics.resume();
            Field f=Class.forName("p.av10",false,manager.getClass().getClassLoader()).getDeclaredField("c");f.setAccessible(true);Object fragment=f.get(manager);
            Class<?> base=Class.forName("p.gs10",false,manager.getClass().getClassLoader());
            Field args=base.getDeclaredField("f"),view=base.getDeclaredField("e1");args.setAccessible(true);view.setAccessible(true);
            Bundle bundle=(Bundle)args.get(fragment);View root=(View)view.get(fragment);
            if(root==null || bundle==null){if(root!=null){contexts.remove(root);PlaylistAction.remove(root);}Diagnostics.event("Skipped: "+fragment.getClass().getName()+" view="+(root==null?"null":root.getClass().getName())+" arguments="+(bundle==null?"none":"present"));return;}
            Diagnostics.event("Fragment "+fragment.getClass().getName()+" root="+root.getClass().getName()+" keys="+new ArrayList<>(bundle.keySet()));
            String uri=PlaylistUri.fromBundle(bundle);
            if(uri==null){
                contexts.remove(root);PlaylistAction.remove(root);
                Diagnostics.event(PageParameters.describe(bundle.get("parameters")));
                Diagnostics.event("No unambiguous playlist identity");return;
            }
            String title=PlaylistTitle.from(bundle,root);
            if(title==null)title="Selected playlist";
            java.lang.ref.WeakReference<Object> captured=new java.lang.ref.WeakReference<>(fragment);contexts.put(root,captured);
            root.post(()->{if(contexts.get(root)==captured)PlaylistAction.attach(root,captured);});Diagnostics.event("Playlist context captured");
        }catch(ReflectiveOperationException | RuntimeException | LinkageError error){Diagnostics.event("Hook error: "+error.getClass().getSimpleName());Log.w("SpotEnhancer","Playlist context unavailable",error);}
    }
    static void launchMixer(Activity a){a.startActivity(new Intent().setClassName(a.getPackageName(),SpotEnhancerActivity.class.getName()).putExtra("from_spotify",true).putExtra("screen","mixer"));}
    static void launch(Activity a,String uri,String title){Intent intent=new Intent().setClassName(a.getPackageName(),SpotEnhancerActivity.class.getName()).putExtra("from_spotify",true);if(uri!=null)intent.putExtra("playlist",uri).putExtra("title",title);a.startActivity(intent);}
}
