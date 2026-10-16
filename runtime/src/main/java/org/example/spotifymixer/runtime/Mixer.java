package org.example.spotifymixer.runtime;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.lang.reflect.Field;

/** Exact-version fragment bridge. Never creates a global floating control. */
public final class Mixer {
    static final String PREFS="spotify-mixer-entry-v1";
    private static final String ROW="spotenhancer-playlist-actions";
    public static void attach(Activity activity) { /* Main lifecycle hook retained; no overlay. */ }
    public static void fragmentResumed(Object manager) {
        try {
            Field f=manager.getClass().getDeclaredField("c");f.setAccessible(true);Object fragment=f.get(manager);
            Class<?> base=Class.forName("p.gs10",false,manager.getClass().getClassLoader());
            Field args=base.getDeclaredField("f"),view=base.getDeclaredField("e1");args.setAccessible(true);view.setAccessible(true);
            Bundle bundle=(Bundle)args.get(fragment);View root=(View)view.get(fragment);
            if(root==null || bundle==null)return;
            String uri=PlaylistUri.fromBundle(bundle);
            if(uri==null)return;
            String title="Selected playlist";
            for(String key:new String[]{"playlist_name","playlist_title","title","name"}) {
                Object value=bundle.get(key);if(value instanceof String && !((String)value).isEmpty()){title=(String)value;break;}
            }
            final String label=title;
            root.post(() -> addPlaylistActions(root,uri,label));
        } catch(ReflectiveOperationException | RuntimeException | LinkageError error) { Log.w("SpotEnhancer","Playlist hook unavailable",error); }
    }
    private static void addPlaylistActions(View root,String uri,String title) {
        try {
            if(!(root instanceof FrameLayout) || !root.isAttachedToWindow() || root.findViewWithTag(ROW)!=null)return;
            Activity activity=activity(root.getContext());if(activity==null)return;
            FrameLayout host=(FrameLayout)root;
            host.setClipToPadding(false);
            int height=(int)(56*root.getResources().getDisplayMetrics().density);
            LinearLayout row=new LinearLayout(activity);row.setTag(ROW);row.setOrientation(0);row.setBackgroundColor(0xff121212);
            Button add=new Button(activity);add.setText("Add to Mixer");add.setAllCaps(false);
            Button open=new Button(activity);open.setText("SpotEnhancer");open.setAllCaps(false);
            row.addView(add,new LinearLayout.LayoutParams(0,-1,1));row.addView(open,new LinearLayout.LayoutParams(0,-1,1));
            add.setOnClickListener(v -> launch(activity,uri,title));open.setOnClickListener(v -> launch(activity,null,null));
            // Reserve a row in this playlist's layout. This is not a window overlay.
            host.setPadding(host.getPaddingLeft(),host.getPaddingTop()+height,host.getPaddingRight(),host.getPaddingBottom());
            FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,height,Gravity.TOP);lp.topMargin=-height;
            host.addView(row,lp);
        }catch(RuntimeException | LinkageError error){Log.w("SpotEnhancer","Cannot attach playlist actions",error);}
    }
    private static Activity activity(Context context){for(int i=0;i<10;i++){if(context instanceof Activity)return (Activity)context;if(!(context instanceof ContextWrapper))return null;context=((ContextWrapper)context).getBaseContext();}return null;}
    static void launch(Activity a,String uri,String title){
        Intent intent=new Intent().setClassName(a.getPackageName(),SpotEnhancerActivity.class.getName());
        if(uri!=null)intent.putExtra("playlist",uri).putExtra("title",title);
        a.startActivity(intent);
    }
}
