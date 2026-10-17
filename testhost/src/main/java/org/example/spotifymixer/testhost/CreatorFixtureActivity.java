package org.example.spotifymixer.testhost;

import android.app.Activity;import android.os.Bundle;import android.widget.LinearLayout;import java.util.*;import java.util.function.Consumer;
import org.example.spotifymixer.runtime.*;import org.example.spotifymixer.ui.EnhancerPanel;
/** Own deterministic UI fixture only. No Spotify services, accounts or network. */
public final class CreatorFixtureActivity extends Activity {
 public static volatile int writes;public static volatile List<CreatorSession.Track> written;private CreatorPanel panel;
 public void onCreate(Bundle saved){super.onCreate(saved);LinearLayout body=EnhancerPanel.page(this,"Creator test fixture",true,this::finish,this::finish);EnhancerPanel.label(body,"SYNTHETIC FIXTURE · NOT SPOTIFY",12,EnhancerPanel.GREEN,true);
 panel=new CreatorPanel(this,body,getSharedPreferences("creator-fixture",0),new CreatorSession.Backend(){
  public List<CreatorSession.Track> suggest(CreatorSession.Request r,Consumer<String> progress){List<CreatorSession.Track> out=new ArrayList<>();for(int i=1;i<=r.count;i++)out.add(new CreatorSession.Track("spotify:track:"+String.format(Locale.ROOT,"%022d",i),"Fixture song "+i,"Fixture artist"));return out;}
  public CreatorSession.Receipt create(String name,List<CreatorSession.Track> tracks,String operation){writes++;written=new ArrayList<>(tracks);return new CreatorSession.Receipt("spotify:playlist:0000000000000000000001",name,tracks.size());}
 });}
 protected void onDestroy(){if(panel!=null)panel.close();super.onDestroy();}
}
