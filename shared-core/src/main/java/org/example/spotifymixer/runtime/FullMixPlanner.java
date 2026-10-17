package org.example.spotifymixer.runtime;

import java.util.*;
import java.util.regex.Pattern;

/** Weighted fair ordering of the complete unique union, with no output track cap. */
public final class FullMixPlanner {
    private static final Pattern TRACK=Pattern.compile("spotify:track:[A-Za-z0-9]{22}");
    public static final class Plan {
        public final List<String> tracks;
        public final List<Integer> counts;
        private Plan(List<String> tracks,List<Integer> counts){this.tracks=Collections.unmodifiableList(tracks);this.counts=Collections.unmodifiableList(counts);}
    }
    private static final class Cursor {
        final int index,weight;final Iterator<String> items;long emitted;
        Cursor(int index,int weight,List<String> tracks){this.index=index;this.weight=weight;items=tracks.iterator();}
    }
    private FullMixPlanner(){}
    public static Plan plan(List<List<String>> playlists,List<Integer> weights){
        if(playlists.size()!=weights.size()||playlists.isEmpty())throw new IllegalArgumentException("Invalid sources");
        PriorityQueue<Cursor> queue=new PriorityQueue<>((a,b)->{int cmp=Long.compare((a.emitted+1)*b.weight,(b.emitted+1)*a.weight);return cmp!=0?cmp:Integer.compare(a.index,b.index);});
        List<Integer> counts=new ArrayList<>();int sum=0;
        for(int i=0;i<weights.size();i++){
            int weight=weights.get(i);if(weight<0||weight>100)throw new IllegalArgumentException("Invalid percentage");sum+=weight;counts.add(0);
            if(weight>0){List<String> valid=new ArrayList<>();for(String uri:playlists.get(i))if(uri!=null&&TRACK.matcher(uri).matches())valid.add(uri);if(!valid.isEmpty())queue.add(new Cursor(i,weight,valid));}
        }
        if(sum!=100)throw new IllegalArgumentException("Percentages must add up to 100");
        List<String> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        while(!queue.isEmpty()){
            Cursor c=queue.remove();boolean added=false;
            while(c.items.hasNext()){String uri=c.items.next();if(seen.add(uri)){out.add(uri);c.emitted++;counts.set(c.index,counts.get(c.index)+1);added=true;break;}}
            if(added&&c.items.hasNext())queue.add(c);
        }
        return new Plan(out,counts);
    }
}
