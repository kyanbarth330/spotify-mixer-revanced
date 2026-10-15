package org.spotify.mixer.injected;

import android.app.*;
import android.view.*;
import android.widget.*;
import android.content.SharedPreferences;

/** Resource-free payload: all classes are packaged as an additional DEX. */
public final class MixerMenu {
    private static final String TAG="spotify-mixer-menu-v1";
    public static void attach(Activity activity) {
        try {
            ViewGroup content=activity.findViewById(android.R.id.content);
            if(content==null || content.findViewWithTag(TAG)!=null)return;
            Button button=new Button(activity);button.setTag(TAG);button.setText("Mixer");
            button.setContentDescription("Open Spotify Mixer");
            button.setOnClickListener(v -> show(activity));
            FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.END);
            p.topMargin=(int)(48*activity.getResources().getDisplayMetrics().density);
            content.addView(button,p);
        } catch(RuntimeException e) { android.util.Log.e("SpotifyMixer","Menu kon niet worden toegevoegd",e); }
    }
    private static void show(Activity activity) {
        SharedPreferences prefs=activity.getSharedPreferences("spotify_mixer",0);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(1);root.setPadding(32,16,32,16);
        TextView explanation=new TextView(activity);explanation.setText("Mixer-menu prototype. Playlistselectie en afspelen zijn nog niet gekoppeld aan Spotify.");root.addView(explanation);
        EditText a=new EditText(activity);a.setSingleLine();a.setHint("Playlist A: link of naam");a.setText(prefs.getString("a",""));root.addView(a);
        EditText b=new EditText(activity);b.setSingleLine();b.setHint("Playlist B: link of naam");b.setText(prefs.getString("b",""));root.addView(b);
        TextView ratio=new TextView(activity);root.addView(ratio);
        SeekBar slider=new SeekBar(activity);slider.setMax(100);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar,int value,boolean user){ratio.setText("Playlist A: "+value+"% · Playlist B: "+(100-value)+"%");}
            public void onStartTrackingTouch(SeekBar bar){} public void onStopTrackingTouch(SeekBar bar){}
        });
        int initial=prefs.getInt("ratio",70);ratio.setText("Playlist A: "+initial+"% · Playlist B: "+(100-initial)+"%");slider.setProgress(initial);root.addView(slider);
        new AlertDialog.Builder(activity).setTitle("Spotify Mixer").setView(root)
            .setPositiveButton("Bewaar",(dialog,which)->prefs.edit().putString("a",a.getText().toString()).putString("b",b.getText().toString()).putInt("ratio",slider.getProgress()).apply())
            .setNegativeButton("Sluiten",null).show();
    }
}
