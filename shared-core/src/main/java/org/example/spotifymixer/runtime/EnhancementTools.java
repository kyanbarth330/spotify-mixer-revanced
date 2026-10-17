package org.example.spotifymixer.runtime;
import java.util.*;
/** Original data-only tools; no account, filesystem or Android dependency. */
public final class EnhancementTools {
 private EnhancementTools(){}
 public static String track(String value){if(value==null)return null;if(value.matches("spotify:track:[A-Za-z0-9]{22}"))return value;if(value.matches("https://open\\.spotify\\.com/track/[A-Za-z0-9]{22}([?].*)?"))return "spotify:track:"+value.substring(31,53);return null;}
 public static String cleanLink(String uri){String id=track(uri);if(id!=null)return "https://open.spotify.com/track/"+id.substring(14);if(uri!=null&&uri.matches("spotify:playlist:[A-Za-z0-9]{22}"))return "https://open.spotify.com/playlist/"+uri.substring(17);throw new IllegalArgumentException("No recognized Spotify track or playlist");}
 public static Map<String,Integer> duplicates(List<String> input){LinkedHashMap<String,Integer> all=new LinkedHashMap<>(),dupes=new LinkedHashMap<>();for(String row:input){String uri=track(row);if(uri!=null)all.put(uri,all.getOrDefault(uri,0)+1);}for(Map.Entry<String,Integer> e:all.entrySet())if(e.getValue()>1)dupes.put(e.getKey(),e.getValue());return Collections.unmodifiableMap(dupes);}
 public static long seek(long position,long updated,long now,float speed,boolean playing,long duration,long delta){long elapsed=Math.max(0,now-updated);long base=position<0?0:position;if(playing&&Float.isFinite(speed)&&speed>0)base+=(long)(elapsed*speed);long next=base+delta;if(next<0)next=0;if(duration>0)next=Math.min(duration,next);return next;}
 public static int volume(int current,int maximum,int direction,int steps){if(maximum<0||steps<1||steps>10)throw new IllegalArgumentException("Invalid volume steps");return Math.max(0,Math.min(maximum,current+(direction>0?steps:-steps)));}
 public static final class Comparison {
  public final List<String> shared,onlyA,onlyB;
  private Comparison(List<String> shared,List<String> a,List<String> b){this.shared=Collections.unmodifiableList(shared);onlyA=Collections.unmodifiableList(a);onlyB=Collections.unmodifiableList(b);}
 }
 public static Comparison compare(List<String> a,List<String> b){
  LinkedHashSet<String> left=canonical(a),right=canonical(b);List<String> shared=new ArrayList<>(),onlyA=new ArrayList<>(),onlyB=new ArrayList<>();
  for(String uri:left)if(right.contains(uri))shared.add(uri);else onlyA.add(uri);
  for(String uri:right)if(!left.contains(uri))onlyB.add(uri);
  return new Comparison(shared,onlyA,onlyB);
 }
 private static LinkedHashSet<String> canonical(List<String> rows){LinkedHashSet<String> result=new LinkedHashSet<>();for(String row:rows){String uri=track(row);if(uri!=null)result.add(uri);}return result;}
 public static final class SkipGuard {private long first=-1;private int direction;public synchronized boolean allow(long now,int next,boolean enabled){if(!enabled)return true;if(first>=0&&direction==next&&now-first>=0&&now-first<=1500){first=-1;return true;}first=now;direction=next;return false;}}
}
