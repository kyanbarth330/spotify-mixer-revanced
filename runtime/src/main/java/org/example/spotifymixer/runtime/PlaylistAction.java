package org.example.spotifymixer.runtime;

import android.app.Activity;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import org.example.spotifymixer.ui.EnhancerTheme;

/** Page-scoped action attached to the verified playlist FrameLayout, not a global top bar or native overflow row. */
public final class PlaylistAction extends Button {
    private static final String TAG="spotenhancer-playlist-action";
    private WeakReference<Object> fragment;
    private final WeakReference<View> scope;
    private ViewTreeObserver observer;
    private final ViewTreeObserver.OnGlobalLayoutListener layout=this::refresh;
    private PlaylistAction(View root,WeakReference<Object> fragment){
        super(new ContextThemeWrapper(root.getContext(),android.R.style.Theme_Material_NoActionBar));this.fragment=fragment;scope=new WeakReference<>(root);setTag(TAG);
        setText("Add to Mixer");setContentDescription("Add current playlist to Mixer");setAllCaps(false);setTextSize(14);setTypeface(Typeface.DEFAULT,Typeface.BOLD);setTextColor(0xff07150c);setMinHeight(0);setMinimumHeight(0);setElevation(dp(4));setPadding(dp(18),0,dp(18),0);
        setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x33000000),EnhancerTheme.surface(EnhancerTheme.GREEN,24,getContext()),null));
        setOnClickListener(v->add());
    }
    private int dp(int value){return EnhancerTheme.dp(getContext(),value);}
    static void attach(View root,WeakReference<Object> fragment){
        if(!(root instanceof FrameLayout))return;
        View existing=owned(root);
        if(existing instanceof PlaylistAction){((PlaylistAction)existing).fragment=fragment;((PlaylistAction)existing).refresh();return;}
        PlaylistAction action=new PlaylistAction(root,fragment);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,action.dp(44),Gravity.LEFT|Gravity.BOTTOM);lp.leftMargin=action.dp(16);lp.bottomMargin=action.dp(140);((FrameLayout)root).addView(action,lp);action.refresh();
    }
    static PlaylistAction owned(View root){if(!(root instanceof FrameLayout))return null;FrameLayout frame=(FrameLayout)root;for(int i=0;i<frame.getChildCount();i++){View child=frame.getChildAt(i);if(child instanceof PlaylistAction&&((PlaylistAction)child).scope.get()==root)return (PlaylistAction)child;}return null;}
    static void remove(View root){View action=owned(root);if(action instanceof PlaylistAction&&action.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)action.getParent()).removeView(action);}
    protected void onAttachedToWindow(){super.onAttachedToWindow();observer=getViewTreeObserver();observer.addOnGlobalLayoutListener(layout);post(this::refresh);}
    protected void onDetachedFromWindow(){if(observer!=null&&observer.isAlive())observer.removeOnGlobalLayoutListener(layout);observer=null;super.onDetachedFromWindow();}
    private String[] current(boolean withTitle){View root=scope.get();Object owner=fragment.get();if(root==null||owner==null||!root.isAttachedToWindow()||!root.isShown()||!root.getGlobalVisibleRect(new android.graphics.Rect()))return null;return Mixer.readPlaylist(root,owner,withTitle);}
    private void refresh(){int next=current(false)==null?GONE:VISIBLE;if(getVisibility()!=next)setVisibility(next);}
    private Activity activity(){Context c=getContext();for(int i=0;i<12;i++){if(c instanceof Activity)return (Activity)c;if(!(c instanceof ContextWrapper))break;Context next=((ContextWrapper)c).getBaseContext();if(next==c)break;c=next;}return null;}
    private void add(){
        String[] value=current(true);Activity host=activity();if(value==null||host==null){refresh();Toast.makeText(getContext(),"Open a playlist first",Toast.LENGTH_SHORT).show();return;}
        try{
            android.content.SharedPreferences prefs=host.getSharedPreferences(Mixer.PREFS,0);MixLibrary library=new MixLibrary(prefs);MixConfig config=library.active();String pending=prefs.getString("pending_slot","");int index=pending.startsWith("source:")?Integer.parseInt(pending.substring(7)):config.sources.size();
            for(int i=0;i<config.sources.size();i++)if(config.sources.get(i).uri.equals(value[0])&&i!=index){
                if(pending.startsWith("source:"))throw new java.io.IOException("This playlist is already in your mix. Choose a different playlist.");
                Toast.makeText(host,"Playlist already in Mixer",Toast.LENGTH_SHORT).show();Mixer.launchMixer(host);return;
            }
            library.select(index,value[0],value[1]);Toast.makeText(host,"Playlist added to Mixer",Toast.LENGTH_SHORT).show();Mixer.launchMixer(host);
        }catch(Exception error){Toast.makeText(host,error.getMessage()==null?"Could not add playlist":error.getMessage(),Toast.LENGTH_LONG).show();}
    }
}
