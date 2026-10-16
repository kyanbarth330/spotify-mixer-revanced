package org.example.spotifymixer.runtime;

import android.app.Activity;
import org.example.spotifymixer.ui.EnhancerTheme;
import android.content.Intent;
import android.os.*;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.view.*;
import android.widget.*;

/** Dedicated private screen with a normal Activity back stack, ready for more features. */
public final class SpotEnhancerActivity extends Activity {
    private boolean mixer;
    private int generation;
    private final MixSession session=new MixSession(new NativeBridge());
    private String pending,title;
    private SharedPreferences prefs;
    public void onCreate(Bundle state){
        setTheme(android.R.style.Theme_Material_NoActionBar);super.onCreate(state);
        prefs=getSharedPreferences(Mixer.PREFS,0);
        pending=PlaylistUri.normalize(getIntent().getStringExtra("playlist"));
        title=getIntent().getStringExtra("title");
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,() -> back());
        if(pending!=null || state!=null && state.getBoolean("mixer"))showMixer();else home();
    }
    protected void onSaveInstanceState(Bundle out){out.putBoolean("mixer",mixer);super.onSaveInstanceState(out);}
    public void onBackPressed(){back();}
    private void back(){if(mixer && pending==null)home();else finish();}
    private LinearLayout page(String heading,String back,Runnable action){
        LinearLayout box=EnhancerTheme.page(this,heading);
        EnhancerTheme.button(box,back,false,action);return box;
    }
    private void home(){generation++;session.invalidate();mixer=false;LinearLayout box=page("SpotEnhancer","Back to Spotify",()->returnToSpotify());
        text(box,"Make Spotify your own.",16);
        button(box,"Playlist Mixer",()->showMixer());
        button(box,"Diagnostics",()->diagnostics());
    }
    private void showMixer(){
        final int token=++generation;session.invalidate();mixer=true;
        LinearLayout box=page("Playlist Mixer","Back to Spotify",()->returnToSpotify());
        if(pending!=null){text(box,title==null?"Selected playlist":title,20);button(box,"Use as Playlist A",()->save("a"));button(box,"Use as Playlist B",()->save("b"));}
        LinearLayout cardA=EnhancerTheme.card(box);text(cardA,"Playlist A",16);TextView aName=text(cardA,prefs.getString("a_title",prefs.contains("a")?"Selected playlist":"Not selected"),20);
        LinearLayout cardB=EnhancerTheme.card(box);text(cardB,"Playlist B",16);TextView bName=text(cardB,prefs.getString("b_title",prefs.contains("b")?"Selected playlist":"Not selected"),20);
        TextView ratio=text(box,"",18);SeekBar slider=new SeekBar(this);slider.setProgressTintList(android.content.res.ColorStateList.valueOf(EnhancerTheme.GREEN));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(EnhancerTheme.GREEN));slider.setMax(100);slider.setProgress(prefs.getInt("ratio",70));
        ratio.setText("A: "+slider.getProgress()+"%   B: "+(100-slider.getProgress())+"%");box.addView(slider);
        text(box,"Open a playlist in Spotify and tap Add to Mixer to select it.",16);
        Button chooseA=EnhancerTheme.button(box,"Choose Playlist A",true,()->choose("a"));
        Button chooseB=EnhancerTheme.button(box,"Choose Playlist B",true,()->choose("b"));
        text(box,"Experimental native playback · up to 100 unique tracks",14);
        text(box,NativeBridge.state(),14);
        final TextView state=text(box,NativeBridge.ready()?"Load both playlists to prepare the mix.":"Open a playlist and play a track in Spotify once, then return here and refresh the connection.",14);
        Button load=EnhancerTheme.button(box,"Load mix",true,()->{});
        Button start=EnhancerTheme.button(box,"Start Mix",true,()->{});start.setEnabled(false);
        MixSession.Prepared[] prepared={null};
        Runnable allowLoad=()->load.setEnabled(NativeBridge.ready()&&PlaylistUri.normalize(prefs.getString("a",null))!=null&&PlaylistUri.normalize(prefs.getString("b",null))!=null);
        allowLoad.run();
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int n,boolean user){ratio.setText("A: "+n+"%   B: "+(100-n)+"%");if(user){prefs.edit().putInt("ratio",n).apply();session.invalidate();prepared[0]=null;start.setEnabled(false);state.setText("Ratio changed. Load the mix again.");}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        load.setOnClickListener(v->{
            load.setEnabled(false);start.setEnabled(false);slider.setEnabled(false);chooseA.setEnabled(false);chooseB.setEnabled(false);prepared[0]=null;
            String a=prefs.getString("a",null),b=prefs.getString("b",null);int percent=slider.getProgress();
            new Thread(()->{
                try{
                    MixSession.Prepared next=session.prepare(a,b,percent,message->onMixUi(token,()->state.setText(message)));
                    onMixUi(token,()->{
                        prepared[0]=next;
                        if(!next.a.title.isEmpty()){aName.setText(next.a.title);prefs.edit().putString("a_title",next.a.title).apply();}
                        if(!next.b.title.isEmpty()){bName.setText(next.b.title);prefs.edit().putString("b_title",next.b.title).apply();}
                        String note=next.plan.fromA!=(int)Math.round(next.plan.tracks.size()*percent/100.0)?" Source limits changed the final ratio.":"";
                        state.setText("Ready: "+next.plan.tracks.size()+" tracks · A: "+next.plan.fromA+" / B: "+next.plan.fromB+note);start.setEnabled(true);
                    });
                }catch(Exception error){onMixUi(token,()->state.setText("Could not load mix: "+errorMessage(error)));}
                finally{onMixUi(token,()->{slider.setEnabled(true);chooseA.setEnabled(true);chooseB.setEnabled(true);allowLoad.run();});}
            },"spotenhancer-load").start();
        });
        start.setOnClickListener(v->{
            MixSession.Prepared next=prepared[0];prepared[0]=null;start.setEnabled(false);load.setEnabled(false);slider.setEnabled(false);chooseA.setEnabled(false);chooseB.setEnabled(false);state.setText("Sending the mix to Spotify…");
            new Thread(()->{
                try{session.start(next);onMixUi(token,()->state.setText("Spotify accepted the mix request. Return to Spotify to check playback."));}
                catch(Exception error){onMixUi(token,()->state.setText("Could not start mix: "+errorMessage(error)));}
                finally{onMixUi(token,()->{slider.setEnabled(true);chooseA.setEnabled(true);chooseB.setEnabled(true);allowLoad.run();});}
            },"spotenhancer-play").start();
        });
        button(box,"Refresh connection",()->showMixer());
        button(box,"Back to SpotEnhancer",()->{pending=null;home();});
    }
    private void onMixUi(int token,Runnable action){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed()&&mixer&&generation==token)action.run();});}
    private static String errorMessage(Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();return cause.getMessage()==null?cause.getClass().getSimpleName():cause.getMessage();}
    private void diagnostics(){generation++;session.invalidate();mixer=false;
        LinearLayout box=page("Diagnostics","Back to Spotify",()->returnToSpotify());
        text(box,Diagnostics.report()+"\n"+NativeBridge.state(),14);
        text(box,"Open a playlist in Spotify first, then return here. This report contains class names and argument keys, not playlist URLs or names.",14);
        button(box,"Back to SpotEnhancer",()->home());
    }
    private void choose(String slot){
        prefs.edit().putString("pending_slot",slot).apply();
        startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("spotify:collection:playlists")).setPackage(getPackageName()).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
        Toast.makeText(this,"Open a playlist, then tap Add to Mixer",Toast.LENGTH_LONG).show();
    }
    private void returnToSpotify(){
        if(!getIntent().getBooleanExtra("from_spotify",false))startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(getPackageName(),"com.spotify.music.MainActivity").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
    private void save(String slot){prefs.edit().remove("pending_slot").putString(slot,pending).putString(slot+"_title",title==null?"Selected playlist":title).apply();pending=null;showMixer();Toast.makeText(this,"Playlist "+slot.toUpperCase(java.util.Locale.ROOT)+" selected",Toast.LENGTH_SHORT).show();}
    private TextView text(LinearLayout box,String value,int size){return EnhancerTheme.text(box,value,size,size>=20);}
    private void button(LinearLayout box,String label,Runnable action){EnhancerTheme.button(box,label,label.startsWith("Choose") || label.equals("Playlist Mixer"),action);}
}
