package org.example.spotifymixer.runtime;

import android.app.Activity;
import android.content.Intent;
import android.os.*;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;

/** Dedicated private screen with a normal Activity back stack, ready for more features. */
public final class SpotEnhancerActivity extends Activity {
    private boolean mixer;
    private String pending,title;
    private SharedPreferences prefs;
    public void onCreate(Bundle state){
        setTheme(android.R.style.Theme_Material_NoActionBar);super.onCreate(state);
        prefs=getSharedPreferences(Mixer.PREFS,0);
        pending=PlaylistUri.normalize(getIntent().getStringExtra("playlist"));
        if(pending==null && Intent.ACTION_SEND.equals(getIntent().getAction())){CharSequence shared=getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT);pending=PlaylistUri.fromSharedText(shared==null?null:shared.toString());}
        title=getIntent().getStringExtra("title");
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,() -> back());
        if(pending!=null || state!=null && state.getBoolean("mixer"))showMixer();else home();
    }
    protected void onSaveInstanceState(Bundle out){out.putBoolean("mixer",mixer);super.onSaveInstanceState(out);}
    public void onBackPressed(){back();}
    private void back(){if(mixer && pending==null)home();else finish();}
    private LinearLayout page(String heading,String back,Runnable action){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setBackgroundColor(0xff121212);box.setPadding(24,24,24,24);
        box.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(24+insets.getSystemWindowInsetLeft(),24+insets.getSystemWindowInsetTop(),24+insets.getSystemWindowInsetRight(),24+insets.getSystemWindowInsetBottom());return insets;});
        Button button=new Button(this);button.setText(back);button.setAllCaps(false);button.setOnClickListener(v->action.run());box.addView(button);
        text(box,heading,30);setContentView(box);box.requestApplyInsets();return box;
    }
    private void home(){mixer=false;LinearLayout box=page("SpotEnhancer","Back to Spotify",()->returnToSpotify());
        text(box,"Make Spotify your own.",16);
        button(box,"Playlist Mixer",()->showMixer());
        button(box,"Diagnostics",()->diagnostics());
    }
    private void showMixer(){mixer=true;LinearLayout box=page("Playlist Mixer","Back to Spotify",()->returnToSpotify());
        if(pending!=null){text(box,title==null?"Selected playlist":title,20);
            button(box,"Use as Playlist A",()->save("a"));button(box,"Use as Playlist B",()->save("b"));}
        text(box,"Playlist A",16);text(box,prefs.getString("a_title",PlaylistUri.normalize(prefs.getString("a",""))==null?"Not selected":"Selected playlist"),20);
        text(box,"Playlist B",16);text(box,prefs.getString("b_title",PlaylistUri.normalize(prefs.getString("b",""))==null?"Not selected":"Selected playlist"),20);
        TextView ratio=text(box,"",18);SeekBar slider=new SeekBar(this);slider.setMax(100);slider.setProgress(prefs.getInt("ratio",70));
        ratio.setText("A: "+slider.getProgress()+"%   B: "+(100-slider.getProgress())+"%");
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){ratio.setText("A: "+n+"%   B: "+(100-n)+"%");if(user)prefs.edit().putInt("ratio",n).apply();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(slider);
        text(box,"Open a playlist in Spotify and use Share → More → Add to Mixer · SpotEnhancer. No copying or typing is needed.",16);
        button(box,"Choose a playlist in Spotify",()->returnToSpotify());
        text(box,"Playlist selection and ratio are saved. Playback mixing is not available yet.",14);
        button(box,"Back to SpotEnhancer",()->{pending=null;home();});
    }
    private void diagnostics(){
        LinearLayout box=page("Diagnostics","Back to Spotify",()->returnToSpotify());
        text(box,Diagnostics.report(),14);
        text(box,"Open a playlist in Spotify first, then return here. This report contains class names and argument keys, not playlist URLs or names.",14);
        button(box,"Back to SpotEnhancer",()->home());
    }
    private void returnToSpotify(){
        if(!getIntent().getBooleanExtra("from_spotify",false))startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(getPackageName(),"com.spotify.music.MainActivity").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
    private void save(String slot){prefs.edit().putString(slot,pending).putString(slot+"_title",title==null?"Selected playlist":title).apply();pending=null;showMixer();Toast.makeText(this,"Playlist "+slot.toUpperCase(java.util.Locale.ROOT)+" selected",Toast.LENGTH_SHORT).show();}
    private TextView text(LinearLayout box,String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,12,0,12);box.addView(t);return t;}
    private void button(LinearLayout box,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());box.addView(b);}
}
