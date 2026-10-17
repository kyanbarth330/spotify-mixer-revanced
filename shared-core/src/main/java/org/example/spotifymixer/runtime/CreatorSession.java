package org.example.spotifymixer.runtime;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/** Own creator transaction. No recommendation engine or account adapter is fabricated here. */
public final class CreatorSession {
    public static final int MAX_TRACKS=500;
    public static final class Request {
        public final String name,genres,artists,description;
        public final int count;
        public Request(String name,String genres,String artists,String description,int count){
            this.name=clean(name,80,"Playlist name");this.genres=clean(genres,200,"Genres");this.artists=clean(artists,200,"Artists");this.description=clean(description,500,"Description");
            if(this.name.isEmpty())throw new IllegalArgumentException("Enter a playlist name");
            if(this.genres.isEmpty()&&this.artists.isEmpty()&&this.description.isEmpty())throw new IllegalArgumentException("Enter genres, artists or a description");
            if(count<1||count>MAX_TRACKS)throw new IllegalArgumentException("Choose 1–"+MAX_TRACKS+" tracks");this.count=count;
        }
        public String playlistName(){return "SpotEnhancer-"+name;}
        private static String clean(String value,int max,String label){String s=value==null?"":value.trim();if(s.length()>max)throw new IllegalArgumentException(label+" is too long");for(int i=0;i<s.length();i++)if(Character.isISOControl(s.charAt(i)))throw new IllegalArgumentException(label+" contains control characters");return s;}
    }
    public static final class Track {
        public final String uri,title,artist;
        public Track(String uri,String title,String artist){if(uri==null||!uri.matches("spotify:track:[A-Za-z0-9]{22}"))throw new IllegalArgumentException("Invalid track identity");this.uri=uri;this.title=title==null||title.trim().isEmpty()?"Untitled track":title;this.artist=artist==null?"":artist;}
    }
    /** Backend must return an acknowledgement only after creation AND all requested items are verified.
     * The operation ID must reconcile retries without creating a second playlist. No automatic retries here. */
    public interface Backend {
        List<Track> suggest(Request request,Consumer<String> progress)throws Exception;
        Receipt create(String name,List<Track> tracks,String operationId)throws Exception;
    }
    public static final class Receipt {
        public final String uri,name;public final int count;
        public Receipt(String uri,String name,int count){if(uri==null||!uri.matches("spotify:playlist:[A-Za-z0-9]{22}")||count<1)throw new IllegalArgumentException("Invalid creation acknowledgement");this.uri=uri;this.name=name;this.count=count;}
    }
    public static final class Preview {
        public final Request request;public final List<Track> tracks;
        private Preview(Request request,List<Track> tracks){this.request=request;this.tracks=Collections.unmodifiableList(new ArrayList<>(tracks));}
    }
    private final Backend backend;private Preview current;private long generation;private boolean writing;
    public CreatorSession(Backend backend){this.backend=Objects.requireNonNull(backend);}
    public synchronized void invalidate(){generation++;current=null;}
    public Preview prepare(Request request,Consumer<String> progress)throws Exception{
        final long token;synchronized(this){if(writing)throw new IOException("A creation request is already in progress");invalidate();token=generation;}
        List<Track> suggested=backend.suggest(request,message->{synchronized(this){if(token!=generation)throw new java.util.concurrent.CancellationException();}progress.accept(message);});
        if(suggested==null)throw new IOException("No suggestions were returned");
        LinkedHashMap<String,Track> unique=new LinkedHashMap<>();for(Track t:suggested)if(t!=null)unique.putIfAbsent(t.uri,t);
        if(unique.size()<request.count)throw new IOException("Only "+unique.size()+" unique tracks found; lower the count or broaden your preferences");
        List<Track> selected=new ArrayList<>(unique.values()).subList(0,request.count);Preview next=new Preview(request,selected);
        synchronized(this){if(token!=generation)throw new java.util.concurrent.CancellationException();current=next;}return next;
    }
    public synchronized Preview remove(Preview expected,String uri)throws IOException{
        if(expected==null||current!=expected||writing)throw new IOException("Generate a new preview first");
        List<Track> kept=new ArrayList<>(expected.tracks);if(!kept.removeIf(track->track.uri.equals(uri)))throw new IOException("Track is no longer in this preview");
        if(kept.isEmpty())throw new IOException("Keep at least one track");current=new Preview(expected.request,kept);return current;
    }
    public Receipt create(Preview expected)throws Exception{
        synchronized(this){if(expected==null||current!=expected||writing)throw new IOException("Generate and review a new preview first");current=null;writing=true;generation++;}
        try{
            Receipt receipt=backend.create(expected.request.playlistName(),expected.tracks,UUID.randomUUID().toString());
            if(receipt==null||!expected.request.playlistName().equals(receipt.name)||receipt.count!=expected.tracks.size())throw new IOException("Creation was not fully confirmed; check Spotify before trying again");return receipt;
        }finally{synchronized(this){writing=false;}}
    }
}
