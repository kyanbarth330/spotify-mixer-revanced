package org.example.spotifymixer.runtime;
import java.util.*;import java.util.function.Consumer;import java.io.IOException;
/** Full-source load/prepare/start transaction; old preparations never survive a reload. */
public final class MixSession {
    public interface Backend {Playlist load(String uri,Consumer<String> progress)throws Exception;void play(List<String> tracks,boolean shuffle)throws Exception;}
    public static final class Playlist {public final String title;public final List<String> tracks;public Playlist(String title,List<String> tracks){this.title=title;this.tracks=Collections.unmodifiableList(new ArrayList<>(tracks));}}
    public static final class Prepared {
        public final List<Playlist> playlists;public final MixConfig config;public final FullMixPlanner.Plan plan;
        private Prepared(List<Playlist> playlists,MixConfig config,FullMixPlanner.Plan plan){this.playlists=Collections.unmodifiableList(playlists);this.config=config;this.plan=plan;}
    }
    private final Backend backend;private Prepared prepared;private long generation;
    public MixSession(Backend backend){this.backend=backend;}
    public synchronized void invalidate(){prepared=null;generation++;}
    public Prepared prepare(MixConfig config,Consumer<String> progress)throws Exception{
        final long token;synchronized(this){invalidate();token=generation;}
        if(config.sources.size()<2)throw new IOException("Add at least two playlists");
        List<MixConfig.Source> named=new ArrayList<>();List<Playlist> playlists=new ArrayList<>();List<List<String>> tracks=new ArrayList<>();List<Integer> weights=new ArrayList<>();
        Consumer<String> checked=message->{synchronized(this){if(token!=generation)throw new java.util.concurrent.CancellationException("Mix loading was cancelled");}progress.accept(message);};
        for(int i=0;i<config.sources.size();i++){
            MixConfig.Source source=config.sources.get(i);checked.accept("Loading source "+(i+1)+" of "+config.sources.size()+"…");
            Playlist p=source.weight==0?new Playlist(source.title,Collections.emptyList()):backend.load(source.uri,checked);
            named.add(new MixConfig.Source(source.uri,p.title.isEmpty()?source.title:p.title,source.weight));playlists.add(p);tracks.add(p.tracks);weights.add(source.weight);
        }
        FullMixPlanner.Plan plan=FullMixPlanner.plan(tracks,weights);if(plan.tracks.isEmpty())throw new IOException("No Spotify tracks were returned");
        Prepared next=new Prepared(playlists,new MixConfig(named,config.shuffle),plan);synchronized(this){if(token!=generation)throw new IOException("Mix loading was cancelled");prepared=next;}return next;
    }
    public void start(Prepared expected)throws Exception{synchronized(this){if(expected==null||prepared!=expected)throw new IOException("Load the mix again before starting");prepared=null;}backend.play(expected.plan.tracks,expected.config.shuffle);}
}
