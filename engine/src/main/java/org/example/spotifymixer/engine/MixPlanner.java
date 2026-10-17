package org.example.spotifymixer.engine;
import java.util.List;
/** Source-compatible facade for the shared runtime planner. */
public final class MixPlanner {
    public static final class Plan {public final List<String> tracks;public final int fromA,fromB;private Plan(org.example.spotifymixer.runtime.MixPlanner.Plan p){tracks=p.tracks;fromA=p.fromA;fromB=p.fromB;}}
    public static Plan plan(List<String> a,List<String> b,int percentA,int count){return new Plan(org.example.spotifymixer.runtime.MixPlanner.plan(a,b,percentA,count));}
}
