package org.example.spotifymixer.runtime;
import java.nio.charset.StandardCharsets;import java.util.*;
/** Entirely original in-memory protocol fixture. No network/account writes. */
public final class CreatorProtocolFixture {
 public static int writes;public static String name;public static List<String> tracks=new ArrayList<>();public static boolean wrongReadback;
 public final Object c=new Object();public final Object f=new com.spotify.esperanto.esperanto.CoroutineClientBase((service,method,data,continuation)->{
  try{PlaylistWire.Message request=new PlaylistWire.Message(data);
   if(service.endsWith("RootlistModificationService")){writes++;PlaylistWire.Message modification=request.message(2);name=modification.string(4);tracks.clear();for(byte[] uri:modification.all(7))tracks.add(new String(uri,StandardCharsets.UTF_8));return PlaylistWire.join(PlaylistWire.bytes(1,PlaylistWire.number(1,200)),PlaylistWire.bytes(2,PlaylistWire.join(PlaylistWire.number(1,1),str(2,"spotify:playlist:0000000000000000000001"))));}
   List<byte[]> items=new ArrayList<>();for(String uri:tracks)items.add(PlaylistWire.bytes(1,str(18,uri)));items.add(PlaylistWire.bytes(2,PlaylistWire.bytes(1,str(2,wrongReadback?"Mismatch":name))));items.add(PlaylistWire.number(4,tracks.size()));return PlaylistWire.join(PlaylistWire.bytes(1,PlaylistWire.number(1,200)),PlaylistWire.bytes(2,PlaylistWire.join(items.toArray(new byte[0][]))));
  }catch(Exception e){throw new IllegalStateException(e);}
 });
 static byte[] str(int field,String value){return PlaylistWire.bytes(field,value.getBytes(StandardCharsets.UTF_8));}
 public static final class SearchResponse {public byte[] toByteArray(){List<byte[]> out=new ArrayList<>();for(int i=1;i<=4;i++)out.add(PlaylistWire.bytes(1,PlaylistWire.join(str(1,"spotify:track:"+String.format(Locale.ROOT,"%022d",i)),str(2,"Protocol song "+i),PlaylistWire.bytes(5,PlaylistWire.bytes(4,str(2,"Protocol artist"))))));return PlaylistWire.join(out.toArray(new byte[0][]));}}
}
