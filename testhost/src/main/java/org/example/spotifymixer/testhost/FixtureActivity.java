package org.example.spotifymixer.testhost;
import android.app.Activity;
import android.os.*;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.example.spotifymixer.runtime.Mixer;

/** Own synthetic playlist host. This is not Spotify and never accesses an account/network. */
public final class FixtureActivity extends Activity {
    private FrameLayout root;private p.gs10 fragment;
    public void onCreate(Bundle saved){super.onCreate(saved);root=new FrameLayout(this);root.setBackgroundColor(0xff121212);setContentView(root);library();}
    protected void onResume(){super.onResume();Mixer.attach(this);}
    protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);library();}
    private LinearLayout content(String heading){
        root.removeAllViews();LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(24,100,24,24);
        TextView title=new TextView(this);title.setText(heading);if(android.os.Build.VERSION.SDK_INT>=28)title.setAccessibilityHeading(true);title.setTextSize(26);title.setTextColor(Color.WHITE);box.addView(title);
        TextView notice=new TextView(this);notice.setText("Synthetic UI fixture · not Spotify");notice.setTextColor(Color.LTGRAY);box.addView(notice);root.addView(box);return box;
    }
    private void library(){LinearLayout box=content("Test library");button(box,"Playlist Alpha",()->playlist("Playlist Alpha","37i9dQZF1DXcBWIGoYBM5M"));button(box,"Playlist Beta",()->playlist("Playlist Beta","37i9dQZF1DX0XUsuxWHRQd"));Bundle args=new Bundle();args.putString("page_key","test.library");fragment=new p.gs10(args,root);Mixer.fragmentResumed(new p.av10(fragment));}
    private void playlist(String title,String id){LinearLayout box=content(title);TextView detail=new TextView(this);detail.setText("Original fixture data for testing selection only");detail.setTextColor(Color.WHITE);box.addView(detail);button(box,"Return to test library",()->library());Bundle args=new Bundle();args.putString("page_key","test.playlist");args.putParcelable("parameters",new Parameters("spotify:playlist:"+id));fragment=new p.gs10(args,root);Mixer.fragmentResumed(new p.av10(fragment));}
    private void button(LinearLayout box,String title,Runnable click){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setOnClickListener(v->click.run());box.addView(b);}
    public static final class Parameters implements Parcelable {
        private final String uri;
        Parameters(String uri){this.uri=uri;}
        public int describeContents(){return 0;}
        public void writeToParcel(Parcel out,int flags){out.writeString(uri);}
        public static final Creator<Parameters> CREATOR=new Creator<Parameters>(){public Parameters createFromParcel(Parcel in){return new Parameters(in.readString());}public Parameters[] newArray(int n){return new Parameters[n];}};
    }
}
