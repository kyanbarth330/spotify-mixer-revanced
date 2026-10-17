package org.example.spotifymixer.runtime;
import java.util.*;
import java.util.regex.Pattern;
/** Original two-source queue planner. Does not access Spotify or start playback. */
public final class MixPlanner {
    private static final Pattern TRACK=Pattern.compile("spotify:track:[A-Za-z0-9]{22}");
    private MixPlanner(){}
    public static final class Plan {
        public final List<String> tracks; public final int fromA,fromB;
        private Plan(List<String> tracks,int a,int b){this.tracks=Collections.unmodifiableList(tracks);fromA=a;fromB=b;}
    }
    public static Plan plan(List<String> a,List<String> b,int percentA,int count){
        if(percentA<0||percentA>100||count<0||count>10000)throw new IllegalArgumentException("Invalid ratio or queue length");
        LinkedHashSet<String> aa=valid(a),bb=valid(b),union=new LinkedHashSet<>(aa);union.addAll(bb);count=Math.min(count,union.size());
        int target=(int)Math.round(count*percentA/100.0);
        int qa=Math.max(Math.max(0,count-bb.size()),Math.min(target,aa.size())),qb=count-qa;
        int commonBudget=bb.size()-qb;
        List<String> left=new ArrayList<>(),right=new ArrayList<>();
        for(String s:aa){if(left.size()==qa)break;if(bb.contains(s)){if(commonBudget==0)continue;commonBudget--;}left.add(s);}
        Set<String> selected=new HashSet<>(left);for(String s:bb)if(!selected.contains(s)&&right.size()<qb)right.add(s);
        List<String> out=new ArrayList<>();int ia=0,ib=0;
        while(ia<left.size()||ib<right.size()){
            boolean useA=ia<left.size()&&(ib==right.size()||(long)(ia+1)*qb<=(long)(ib+1)*qa);
            out.add(useA?left.get(ia++):right.get(ib++));
        }
        return new Plan(out,left.size(),right.size());
    }
    private static LinkedHashSet<String> valid(List<String> input){LinkedHashSet<String> out=new LinkedHashSet<>();if(input!=null)for(String s:input)if(s!=null&&TRACK.matcher(s).matches())out.add(s);return out;}
}
