package org.example.spotifymixer.runtime;
import java.util.*;
/** Immutable user configuration, shared by preparation, presets and history. */
public final class MixConfig {
    public static final class Source {
        public final String uri,title;public final int weight;
        public Source(String uri,String title,int weight){if(uri==null||!uri.matches("spotify:playlist:[A-Za-z0-9]{22}")||weight<0||weight>100)throw new IllegalArgumentException("Invalid source");this.uri=uri;this.title=title==null||title.trim().isEmpty()?"Selected playlist":title;this.weight=weight;}
    }
    public final List<Source> sources;public final boolean shuffle;
    public MixConfig(List<Source> sources,boolean shuffle){this.sources=Collections.unmodifiableList(new ArrayList<>(sources));this.shuffle=shuffle;Set<String> ids=new HashSet<>();for(Source s:sources)if(!ids.add(s.uri))throw new IllegalArgumentException("Choose different playlists");}
    public MixConfig balance(int index,int value){
        if(index<0||index>=sources.size()||value<0||value>100)throw new IllegalArgumentException("Invalid balance");
        if(sources.size()==1)value=100;List<Source> out=new ArrayList<>();int remaining=100-value,old=0;for(int i=0;i<sources.size();i++)if(i!=index)old+=sources.get(i).weight;
        int assigned=0,position=0;for(int i=0;i<sources.size();i++){Source s=sources.get(i);int n=value;if(i!=index){position++;n=position==sources.size()-1?remaining-assigned:old==0?remaining/(sources.size()-1):(int)((long)remaining*s.weight/old);assigned+=n;}out.add(new Source(s.uri,s.title,n));}return new MixConfig(out,shuffle);
    }
    public MixConfig select(int index,String uri,String title){List<Source> out=new ArrayList<>(sources);if(index==out.size())out.add(new Source(uri,title,0));else{out.set(index,new Source(uri,title,out.get(index).weight));return new MixConfig(out,shuffle);}return new MixConfig(out,shuffle).balance(index,100/out.size());}
    public MixConfig remove(int index){List<Source> out=new ArrayList<>(sources);out.remove(index);if(out.isEmpty())return new MixConfig(out,shuffle);MixConfig config=new MixConfig(out,shuffle);return config.balance(0,Math.max(1,100/out.size()));}
}
