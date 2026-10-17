package org.example.spotifymixer.runtime;
import java.util.*;
import java.text.Normalizer;
/** Transparent local query expansion, not an AI model or language detector. */
public final class CreatorIntent {
 private CreatorIntent(){}
 private static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]"," ").replaceAll("\\s+"," ").trim();}
 private static boolean word(String text,String choices){return text.matches(".*\\b("+choices+")\\b.*");}
 public static boolean dutchParty(String value){String t=normalized(value);return word(t,"dutch|nederlands|nederlandse|nederlandstalig|hollands|hollandse")&&word(t,"party|feest|feestmuziek|feestnummers|feestliedjes|partyhits|carnaval|meezingers|singalong");}
 public static List<String> terms(String value){
  String t=normalized(value);
  if(dutchParty(value))return qualify(t,Arrays.asList("genre:\"carnaval\"","genre:\"levenslied\"","genre:\"dutch pop\" feest","Nederlandse feestmuziek","Hollandse meezingers","carnaval feest"));
  if(word(t,"nederlands|nederlandse|nederlandstalig|dutch")&&word(t,"chill|rustig|rustige|acoustic|akoestisch"))return Arrays.asList("genre:\"dutch pop\" acoustic","Nederlandstalig akoestisch","Nederlandse rustige liedjes");
  if(word(t,"workout|gym|sport|hardlopen")&&word(t,"rock|metal"))return Arrays.asList("genre:\"hard rock\"","genre:\"alternative metal\"","rock workout");
  return Collections.singletonList(value.trim());
 }
 public static List<String> queries(CreatorSession.Request r){
  boolean naturalGenre=dutchParty(r.genres);String description=naturalGenre?(r.genres+" "+r.description).trim():r.description;
  List<String> genres=split(naturalGenre?"":r.genres),artists=split(r.artists),terms=terms(description);List<String> out=new ArrayList<>();
  if(genres.isEmpty())genres.add("");if(artists.isEmpty())artists.add("");
  if(genres.size()*artists.size()>12)throw new IllegalArgumentException("Use at most 12 genre/artist combinations");
  // Interleave expansions across explicit selectors. Keep at least one query for every selector.
  int per=Math.max(1,12/(genres.size()*artists.size()));
  for(int variant=0;variant<Math.min(per,terms.size());variant++)for(String g:genres)for(String a:artists){String term=terms.get(variant);String q=((g.isEmpty()?"":"genre:"+quote(g)+" ")+(a.isEmpty()?"":"artist:"+quote(a)+" ")+term).trim();if(!out.contains(q))out.add(q);}
  return Collections.unmodifiableList(out);
 }
 private static List<String> qualify(String text,List<String> queries){
  String rest=text.replaceAll("\\b(dutch|nederlands|nederlandse|nederlandstalig|hollands|hollandse|party|feest|feestmuziek|feestnummers|feestliedjes|partyhits|carnaval|meezingers|singalong|songs|song|music|muziek|nummers|liedjes|playlist|please)\\b", " ").replaceAll("\\s+"," ").trim();
  rest=rest.replaceAll("\\b90s\\b","year:1990-1999").replaceAll("\\b80s\\b","year:1980-1989").replaceAll("\\b00s\\b","year:2000-2009");
  List<String> out=new ArrayList<>();for(String q:queries)out.add(rest.isEmpty()?q:q+" "+rest);return out;
 }
 private static List<String> split(String text){List<String> out=new ArrayList<>();for(String part:text.split(","))if(!part.trim().isEmpty()&&!out.contains(part.trim()))out.add(part.trim());return out;}
 private static String quote(String text){return "\""+text.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
 public static boolean accept(CreatorSession.Track track,CreatorSession.Request request){String wanted=normalized(request.description+" "+request.artists+" "+request.genres);if(word(wanted,"karaoke|tribute"))return true;String title=normalized(track.title),artist=normalized(track.artist);return !title.contains("karaoke version")&&!title.contains("tribute to")&&!title.contains("made famous by")&&!word(artist,"karaoke|tribute");}
 /** Preserve search relevance within each artist, while spreading leading artists across passes. */
 public static List<CreatorSession.Track> diversify(List<CreatorSession.Track> input){
  LinkedHashMap<String,List<CreatorSession.Track>> groups=new LinkedHashMap<>();Set<String> seen=new HashSet<>();
  for(CreatorSession.Track t:input)if(t!=null&&seen.add(t.uri)){String key=normalized(t.artist);if(key.isEmpty())key=t.uri;groups.computeIfAbsent(key,k->new ArrayList<>()).add(t);}
  List<CreatorSession.Track> out=new ArrayList<>();for(int rank=0;;rank++){boolean more=false;for(List<CreatorSession.Track> group:groups.values())if(rank<group.size()){out.add(group.get(rank));more=true;}if(!more)return out;}
 }
 public static String explain(CreatorSession.Request r){List<String> q=queries(r);return "Search plan ("+q.size()+" queries):\n"+String.join("\n",q)+"\nLocal keyword expansion; track language and mood are not verified. Review the results.";}
}
