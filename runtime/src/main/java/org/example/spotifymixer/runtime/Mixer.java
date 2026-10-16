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

/** Fixed in-app navigation and exact-version playlist context bridge. No window overlays. */
public final class Mixer {
    static final String PREFS="spotify-mixer-entry-v1";
    private static final String ROW="spotenhancer-navigation";
    private static final WeakHashMap<View,String[]> contexts=new WeakHashMap<>();
    public static void attach(Activity activity) {
        activity.getWindow().getDecorView().post(() -> {
            try {
                FrameLayout host=activity.findViewById(android.R.id.content);
                if(host==null || host.findViewWithTag(ROW)!=null)return;
                int height=(int)(48*host.getResources().getDisplayMetrics().density);
                int[] position=new int[2];host.getLocationOnScreen(position);
                WindowInsets insets=host.getRootWindowInsets();
                int safeTop=insets==null?0:Math.max(0,insets.getSystemWindowInsetTop()-position[1]);
                LinearLayout row=new LinearLayout(activity);row.setTag(ROW);row.setOrientation(0);row.setBackgroundColor(0xff121212);
                Button open=new Button(activity);open.setText("SpotEnhancer");open.setAllCaps(false);
                Button add=new Button(activity);add.setAllCaps(false);add.setText("Add to Mixer");
                EnhancerTheme.styleButton(open,false);EnhancerTheme.styleButton(add,true);
                row.addView(open,new LinearLayout.LayoutParams(0,-1,1));row.addView(add,new LinearLayout.LayoutParams(0,-1,1));
                open.setOnClickListener(v -> launch(activity,null,null));
                add.setOnClickListener(v -> {
                    String[] current=currentPlaylist();
                    if(current==null){Toast.makeText(activity,"No playlist detected. Open SpotEnhancer → Diagnostics.",Toast.LENGTH_LONG).show();return;}
                    String slot=activity.getSharedPreferences(PREFS,0).getString("pending_slot","");
                    if(slot.equals("a") || slot.equals("b")){
                        activity.getSharedPreferences(PREFS,0).edit().putString(slot,current[0]).putString(slot+"_title",current[1]).remove("pending_slot").apply();
                        Toast.makeText(activity,"Playlist "+slot.toUpperCase(Locale.ROOT)+" selected",Toast.LENGTH_SHORT).show();launch(activity,null,null);
                    } else launch(activity,current[0],current[1]);
                });
                host.setClipToPadding(false);host.setPadding(host.getPaddingLeft(),host.getPaddingTop()+height+safeTop,host.getPaddingRight(),host.getPaddingBottom());
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,height,Gravity.TOP);lp.topMargin=-height;
                host.addView(row,lp);
                host.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                    boolean ready=currentPlaylist()!=null;
                    if(add.isEnabled()!=ready)add.setEnabled(ready);
                    String label=ready?"Add to Mixer":"Open a playlist";
                    if(!label.contentEquals(add.getText()))add.setText(label);
                });
                Diagnostics.event("In-app SpotEnhancer navigation added");
            } catch(RuntimeException | LinkageError error){Diagnostics.event("Navigation error: "+error.getClass().getSimpleName());Log.w("SpotEnhancer","Cannot add navigation",error);}
        });
    }
    private static String[] currentPlaylist(){
        String[] selected=null;
        for(Map.Entry<View,String[]> entry:new ArrayList<>(contexts.entrySet())){
            View root=entry.getKey();if(root==null || !root.isAttachedToWindow() || !root.isShown())continue;
            String[] value=entry.getValue();
            if("Selected playlist".equals(value[1])){String title=PlaylistTitle.visibleHeading(root);if(title!=null)value[1]=title;}
            if(selected!=null && !selected[0].equals(value[0]))return null;
            selected=value;
        }
        return selected;
    }
    public static void fragmentResumed(Object manager) {
        try {
            Diagnostics.resume();
            Field f=Class.forName("p.av10",false,manager.getClass().getClassLoader()).getDeclaredField("c");f.setAccessible(true);Object fragment=f.get(manager);
            Class<?> base=Class.forName("p.gs10",false,manager.getClass().getClassLoader());
            Field args=base.getDeclaredField("f"),view=base.getDeclaredField("e1");args.setAccessible(true);view.setAccessible(true);
            Bundle bundle=(Bundle)args.get(fragment);View root=(View)view.get(fragment);
            if(root==null || bundle==null){Diagnostics.event("Skipped: "+fragment.getClass().getName()+" view="+(root==null?"null":root.getClass().getName())+" arguments="+(bundle==null?"none":"present"));return;}
            Diagnostics.event("Fragment "+fragment.getClass().getName()+" root="+root.getClass().getName()+" keys="+bundle.keySet());
            String uri=PlaylistUri.fromBundle(bundle);
            if(uri==null){
                contexts.remove(root);
                Diagnostics.event(PageParameters.describe(bundle.get("parameters")));
                Diagnostics.event("No unambiguous playlist identity");return;
            }
            String title=PlaylistTitle.from(bundle,root);
            if(title==null)title="Selected playlist";
            contexts.put(root,new String[]{uri,title});Diagnostics.event("Playlist context captured");
        }catch(ReflectiveOperationException | RuntimeException | LinkageError error){Diagnostics.event("Hook error: "+error.getClass().getSimpleName());Log.w("SpotEnhancer","Playlist context unavailable",error);}
    }
    static void launch(Activity a,String uri,String title){Intent intent=new Intent().setClassName(a.getPackageName(),SpotEnhancerActivity.class.getName()).putExtra("from_spotify",true);if(uri!=null)intent.putExtra("playlist",uri).putExtra("title",title);a.startActivity(intent);}
}
