package org.example.spotifymixer.engine;

import java.util.*;
import java.util.regex.Pattern;

/** Pure queue planning; does not fetch tracks or start Spotify playback. */
public final class MixPlanner {
    private static final Pattern TRACK=Pattern.compile("spotify:track:[A-Za-z0-9]{22}");
    private MixPlanner(){}
    public static final class Plan {
        public final List<String> tracks;
        public final int fromA,fromB;
        private Plan(List<String> tracks,int a,int b){this.tracks=Collections.unmodifiableList(tracks);fromA=a;fromB=b;}
    }
    /** Stable input order; unique canonical tracks only. Shortages fall back to the other source. */
    public static Plan plan(List<String> a,List<String> b,int percentA,int count){
        if(percentA<0 || percentA>100 || count<0 || count>10000)throw new IllegalArgumentException("Invalid ratio or queue length");
        Deque<String> left=valid(a),right=valid(b);Set<String> used=new HashSet<>();List<String> output=new ArrayList<>();int na=0,nb=0;
        int targetA=(int)Math.round(count*percentA/100.0),targetB=count-targetA;
        while(output.size()<count){
            boolean preferA=targetA>na && (targetB<=nb || (long)(na+1)*targetB<=(long)(nb+1)*targetA);
            String next=take(preferA?left:right,used);boolean sourceA=preferA;
            if(next==null){next=take(preferA?right:left,used);sourceA=!preferA;}
            if(next==null)break;
            used.add(next);output.add(next);if(sourceA)na++;else nb++;
        }
        return new Plan(output,na,nb);
    }
    private static Deque<String> valid(List<String> input){Deque<String> out=new ArrayDeque<>();if(input!=null)for(String value:input)if(value!=null && TRACK.matcher(value).matches())out.add(value);return out;}
    private static String take(Deque<String> queue,Set<String> used){while(!queue.isEmpty()){String value=queue.removeFirst();if(!used.contains(value))return value;}return null;}
}
