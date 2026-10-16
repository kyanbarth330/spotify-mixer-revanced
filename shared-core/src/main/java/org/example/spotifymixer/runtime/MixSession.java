package org.example.spotifymixer.runtime;
import java.util.*;import java.util.function.Consumer;import java.io.IOException;
/** Load/prepare/start transaction. A prepared queue is invalidated before every load attempt. */
public final class MixSession {
    public interface Backend {Playlist load(String uri,Consumer<String> progress)throws Exception;void play(List<String> tracks)throws Exception;}
    public static final class Playlist {public final String title;public final List<String> tracks;public Playlist(String title,List<String> tracks){this.title=title;this.tracks=Collections.unmodifiableList(new ArrayList<>(tracks));}}
    public static final class Prepared {
        public final Playlist a,b;public final MixPlanner.Plan plan;public final int ratio;
        private Prepared(Playlist a,Playlist b,MixPlanner.Plan plan,int ratio){this.a=a;this.b=b;this.plan=plan;this.ratio=ratio;}
    }
    private final Backend backend;private Prepared prepared;private long generation;
    public MixSession(Backend backend){this.backend=backend;}
    public synchronized void invalidate(){prepared=null;generation++;}
    public Prepared prepare(String a,String b,int ratio,Consumer<String> progress)throws Exception{
        final long token;synchronized(this){invalidate();token=generation;}if(a==null||b==null||a.equals(b))throw new IOException("Choose two different playlists");
        progress.accept("Loading Playlist A…");Playlist left=backend.load(a,progress);
        progress.accept("Loading Playlist B…");Playlist right=backend.load(b,progress);
        MixPlanner.Plan plan=MixPlanner.plan(left.tracks,right.tracks,ratio,100);
        if(plan.tracks.isEmpty())throw new IOException("No playable Spotify tracks were returned");
        Prepared next=new Prepared(left,right,plan,ratio);synchronized(this){if(token!=generation)throw new IOException("Mix loading was cancelled");prepared=next;}return next;
    }
    public void start(Prepared expected)throws Exception{
        synchronized(this){if(expected==null||prepared!=expected)throw new IOException("Load the mix again before starting");prepared=null;}
        backend.play(expected.plan.tracks);
    }
}
