package org.example.spotifymixer.runtime;

import android.app.*;import android.content.*;import android.os.*;import android.text.*;import android.view.*;import android.widget.*;
import org.example.spotifymixer.ui.EnhancerPanel;

/** Shared real form/preview UI. Production has no backend until the native contract is verified. */
public final class CreatorPanel {
    private final Activity activity;private final SharedPreferences prefs;private final CreatorSession session;
    private final NativeCreator nativeBackend;private final LinearLayout body;private final Handler main=new Handler(Looper.getMainLooper());
    private final EditText name,genres,artists,description,count;private final Switch enabled;private final Button generate,save;
    private final TextView feedback,naming;private final LinearLayout previewBody;private CreatorSession.Preview preview;
    private int generation;private boolean closed,busy;
    public CreatorPanel(Activity activity,LinearLayout body,SharedPreferences prefs,CreatorSession.Backend backend){
        this.activity=activity;this.body=body;this.prefs=prefs;nativeBackend=backend instanceof NativeCreator?(NativeCreator)backend:null;session=backend==null?null:new CreatorSession(backend);
        label(body,"Find your next playlist",24,true);muted(body,"Choose genres, artists or describe the music you want. Uses local intent expansion and Spotify search, not AI. Review every proposal before creating a playlist.");
        enabled=new Switch(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));enabled.setText("Enable Playlist Creator");enabled.setTextColor(EnhancerPanel.WHITE);EnhancerPanel.styleSwitch(enabled);enabled.setChecked(prefs.getBoolean("creator_enabled",false));body.addView(enabled);
        LinearLayout form=EnhancerPanel.card(body);
        name=field(form,"Playlist name","e.g. Evening rock","creator_name",80,false);
        naming=muted(form,"");genres=field(form,"Genres","e.g. rock, soul","creator_genres",200,false);
        artists=field(form,"Artists","e.g. Dire Straits, Sade","creator_artists",200,false);
        description=field(form,"Music preferences","Describe a vibe, e.g. Dutch party songs","creator_description",500,false);
        count=field(form,"Number of tracks","1–500","creator_count",3,true);if(count.getText().length()==0)count.setText("30");
        feedback=muted(body,backend==null?"Spotify generation is unavailable in this build. You can save your preferences; no playlist will be created.":nativeBackend!=null&&!NativeCreator.ready()?"Open a playlist in Spotify, then refresh the connection. Nothing is created before confirmation.":"Ready to generate a proposal. Nothing is created before confirmation.");
        save=button(body,"Save draft",false,()->{if(save())feedback.setText("Draft saved on this device. No playlist was created.");});
        button(body,"Preview search plan",false,()->{try{CreatorSession.Request request=new CreatorSession.Request(name.getText().toString(),genres.getText().toString(),artists.getText().toString(),description.getText().toString(),Integer.parseInt(count.getText().toString()));new AlertDialog.Builder(activity,android.R.style.Theme_Material_Dialog_Alert).setTitle("How your playlist is searched").setMessage(CreatorIntent.explain(request)).setPositiveButton("Close",null).show();}catch(IllegalArgumentException e){feedback.setText(e.getMessage());}});
        generate=button(body,"Generate preview",true,this::generate);
        if(nativeBackend!=null){muted(body,NativeCreator.state());button(body,"Refresh connection",false,()->{feedback.setText(NativeCreator.state());refresh();});button(body,"Check previous creation",false,()->{
            if(busy)return;busy=true;refresh();final int token=generation;new Thread(()->{try{CreatorSession.Receipt receipt=nativeBackend.verifyPending();post(token,()->feedback.setText("Created "+receipt.name+" · "+receipt.count+" tracks"));}catch(Exception e){post(token,()->feedback.setText(message(e)));}finally{post(token,()->{busy=false;refresh();});}},"spotenhancer-creator-check").start();
        });button(body,"Clear unfinished request",false,()->{if(busy)return;new AlertDialog.Builder(activity,android.R.style.Theme_Material_Dialog_Alert).setTitle("Checked your Spotify library?").setMessage("An earlier request may already have created a playlist. Clearing this record permits a new creation and can cause duplicates. No playlist is deleted.").setNegativeButton("Cancel",null).setPositiveButton("I checked Spotify",(d,w)->{try{nativeBackend.clearPending();feedback.setText("Recovery record cleared. Existing playlists were not changed.");refresh();}catch(Exception e){feedback.setText(message(e));}}).show();});}
        previewBody=new LinearLayout(activity);previewBody.setOrientation(1);body.addView(previewBody);
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int n,int after){}public void onTextChanged(CharSequence s,int start,int before,int n){invalidate();updateName();}public void afterTextChanged(Editable e){}};
        for(EditText input:fields())input.addTextChangedListener(watcher);
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){private boolean reverting;public void onCheckedChanged(CompoundButton view,boolean value){if(reverting)return;if(!prefs.edit().putBoolean("creator_enabled",value).commit()){reverting=true;enabled.setChecked(!value);reverting=false;feedback.setText("Could not save the feature setting");}invalidate();refresh();}});
        updateName();refresh();
    }
    private EditText[] fields(){return new EditText[]{name,genres,artists,description,count};}
    private EditText field(LinearLayout parent,String title,String hint,String key,int max,boolean numeric){
        label(parent,title,13,true);EditText input=new EditText(activity);input.setContentDescription(title);input.setHint(hint);input.setSingleLine(true);input.setTextSize(14);input.setTextColor(EnhancerPanel.WHITE);input.setHintTextColor(EnhancerPanel.MUTED);input.setBackground(EnhancerPanel.surface(0xff101615,10,activity,true));input.setPadding(EnhancerPanel.dp(activity,12),0,EnhancerPanel.dp(activity,12),0);input.setInputType(numeric?android.text.InputType.TYPE_CLASS_NUMBER:android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});input.setText(prefs.getString(key,""));parent.addView(input,new LinearLayout.LayoutParams(-1,EnhancerPanel.dp(activity,48)));return input;
    }
    private TextView label(LinearLayout parent,String text,int size,boolean bold){return EnhancerPanel.label(parent,text,size,EnhancerPanel.WHITE,bold);}
    private TextView muted(LinearLayout parent,String text){return EnhancerPanel.label(parent,text,12,EnhancerPanel.MUTED,false);}
    private Button button(LinearLayout parent,String title,boolean primary,Runnable action){return EnhancerPanel.button(parent,title,primary,action);}
    private void updateName(){naming.setText("Playlist: SpotEnhancer-"+name.getText().toString().trim());}
    private void refresh(){boolean on=enabled.isChecked();for(EditText field:fields())field.setEnabled(on&&!busy);save.setEnabled(on&&!busy);generate.setEnabled(on&&!busy&&session!=null&&(nativeBackend==null||NativeCreator.ready()));enabled.setEnabled(!busy);}
    private void invalidate(){generation++;preview=null;if(session!=null)session.invalidate();previewBody.removeAllViews();}
    private boolean save(){boolean ok=prefs.edit().putString("creator_name",name.getText().toString()).putString("creator_genres",genres.getText().toString()).putString("creator_artists",artists.getText().toString()).putString("creator_description",description.getText().toString()).putString("creator_count",count.getText().toString()).commit();if(!ok)feedback.setText("Could not save the draft");return ok;}
    private void generate(){if(!Auth.allowed("creator")){feedback.setText("SpotEnhancer Premium is required.");return;}
        if(session==null||!enabled.isChecked()||busy)return;
        final CreatorSession.Request request;
        try{request=new CreatorSession.Request(name.getText().toString(),genres.getText().toString(),artists.getText().toString(),description.getText().toString(),Integer.parseInt(count.getText().toString()));}catch(NumberFormatException e){feedback.setText("Choose 1–500 tracks");return;}catch(IllegalArgumentException e){feedback.setText(e.getMessage());return;}
        if(!save())return;invalidate();final int token=generation;busy=true;refresh();feedback.setText("Finding matching tracks…");
        new Thread(()->{try{CreatorSession.Preview next=session.prepare(request,message->post(token,()->feedback.setText(message)));post(token,()->{preview=next;renderPreview();feedback.setText("Proposal ready. Review the tracks before creating.");});}catch(Exception e){post(token,()->feedback.setText("Could not generate: "+message(e)));}finally{post(token,()->{busy=false;refresh();});}},"spotenhancer-creator-search").start();
    }
    private void renderPreview(){
        previewBody.removeAllViews();final CreatorSession.Preview shown=preview;if(shown==null)return;
        label(previewBody,"Review proposal",20,true);label(previewBody,shown.request.playlistName(),16,true);muted(previewBody,shown.tracks.size()+" tracks · requested "+shown.request.count);muted(previewBody,CreatorIntent.explain(shown.request));
        for(CreatorSession.Track track:shown.tracks){LinearLayout row=EnhancerPanel.card(previewBody);label(row,track.title,14,true);muted(row,track.artist);Button remove=button(row,"Remove track",false,()->{try{preview=session.remove(preview,track.uri);renderPreview();}catch(Exception e){feedback.setText(message(e));}});remove.setContentDescription("Remove "+track.title);remove.setEnabled(shown.tracks.size()>1);}
        button(previewBody,"Create Playlist",true,()->new AlertDialog.Builder(activity,android.R.style.Theme_Material_Dialog_Alert).setTitle("Create playlist?").setMessage(shown.request.playlistName()+" · "+shown.tracks.size()+" tracks").setNegativeButton("Cancel",null).setPositiveButton("Create",(dialog,which)->create(shown)).show());
    }
    private void create(CreatorSession.Preview confirmed){if(!Auth.allowed("creator")){feedback.setText("SpotEnhancer Premium is required.");return;}
        if(closed||busy||preview!=confirmed||!enabled.isChecked())return;final int token=generation;busy=true;refresh();previewBody.removeAllViews();feedback.setText("Creating playlist in Spotify…");
        new Thread(()->{try{CreatorSession.Receipt receipt=session.create(confirmed);post(token,()->{preview=null;feedback.setText("Created "+receipt.name+" · "+receipt.count+" tracks");});}catch(Exception e){post(token,()->{preview=null;feedback.setText("Creation could not be confirmed. Check Spotify before retrying. "+message(e));});}finally{post(token,()->{busy=false;refresh();});}},"spotenhancer-creator-write").start();
    }
    private static String message(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private void post(int token,Runnable action){main.post(()->{if(!closed&&generation==token&&!activity.isDestroyed()&&!activity.isFinishing())action.run();});}
    public void close(){closed=true;generation++;if(session!=null)session.invalidate();main.removeCallbacksAndMessages(null);}
}
