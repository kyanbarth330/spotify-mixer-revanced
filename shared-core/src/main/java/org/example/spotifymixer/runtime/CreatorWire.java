package org.example.spotifymixer.runtime;

import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Original bounded wire adapter for locally verified 9.1.90 search/create contracts. */
public final class CreatorWire {
 private static byte[] text(int f,String s){return PlaylistWire.bytes(f,s.getBytes(StandardCharsets.UTF_8));}
 public static final class Page {public final List<CreatorSession.Track> tracks;public final String next;Page(List<CreatorSession.Track> t,String n){tracks=Collections.unmodifiableList(t);next=n;}}
 public static Page search(byte[] raw)throws IOException{PlaylistWire.Message root=new PlaylistWire.Message(raw);List<CreatorSession.Track> tracks=new ArrayList<>();for(byte[] entity:root.all(1))entity(entity,tracks,0);return new Page(tracks,root.string(4));}
 private static void entity(byte[] raw,List<CreatorSession.Track> out,int depth)throws IOException{
  if(depth>4)throw new IOException("Search nesting exceeds supported schema");PlaylistWire.Message e=new PlaylistWire.Message(raw);
  String uri=e.string(1),title=e.string(2);
  if(uri.matches("spotify:track:[A-Za-z0-9]{22}")&&!e.all(5).isEmpty()){
   PlaylistWire.Message track=e.message(5);List<String> artists=new ArrayList<>();for(byte[] a:track.all(4)){String n=new PlaylistWire.Message(a).string(2);if(!n.isEmpty())artists.add(n);}
   if(!title.isEmpty())out.add(new CreatorSession.Track(uri,title,String.join(", ",artists)));
  }
  for(byte[] section:e.all(14))for(byte[] child:new PlaylistWire.Message(section).all(2))entity(child,out,depth+1);
 }
 public static byte[] create(String name,List<CreatorSession.Track> tracks){
  if(name==null||!name.startsWith("SpotEnhancer-")||tracks.isEmpty()||tracks.size()>CreatorSession.MAX_TRACKS)throw new IllegalArgumentException("Invalid creator request");
  List<byte[]> parts=new ArrayList<>();parts.add(text(1,"create"));parts.add(text(2,"start"));parts.add(text(4,name));parts.add(PlaylistWire.number(5,1));
  for(CreatorSession.Track t:tracks)parts.add(text(7,t.uri));
  // Rootlist request folder is empty, as in Spotify's normal root creation path.
  return PlaylistWire.join(text(1,""),PlaylistWire.bytes(2,PlaylistWire.join(parts.toArray(new byte[0][]))));
 }
 public static String createdUri(byte[] raw)throws IOException{
  PlaylistWire.Message response=new PlaylistWire.Message(raw);int code=response.message(1).integer(1,-1);if(code<200||code>=300)throw new IOException("Spotify rejected creation ("+code+")");
  PlaylistWire.Message result=response.message(2);String uri=result.string(2);if(result.integer(1,0)!=1||!uri.matches("spotify:playlist:[A-Za-z0-9]{22}"))throw new IOException("Spotify did not confirm a new playlist identity");return uri;
 }
 /** Genre/artist selectors use Spotify search syntax; description is literal search keywords, not AI. */
 public static List<String> queries(CreatorSession.Request r){
  List<String> genres=split(r.genres),artists=split(r.artists);if(genres.isEmpty())genres.add("");if(artists.isEmpty())artists.add("");List<String> out=new ArrayList<>();
  if(genres.size()*artists.size()>12)throw new IllegalArgumentException("Use at most 12 genre/artist combinations");
  for(String g:genres)for(String a:artists){String q=(g.isEmpty()?"":"genre:"+quoted(g)+" ")+(a.isEmpty()?"":"artist:"+quoted(a)+" ")+r.description;out.add(q.trim());}return Collections.unmodifiableList(out);
 }
 private static List<String> split(String s){List<String> values=new ArrayList<>();for(String x:s.split(","))if(!x.trim().isEmpty()&&!values.contains(x.trim()))values.add(x.trim());return values;}
 private static String quoted(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
 private CreatorWire(){}
}
