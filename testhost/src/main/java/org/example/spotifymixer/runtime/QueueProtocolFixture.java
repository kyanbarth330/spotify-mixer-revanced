package org.example.spotifymixer.runtime;
import java.util.*;import com.spotify.player.model.*;import com.spotify.player.model.command.*;
/** Original in-memory queue used exclusively by public fixture tests. */
public final class QueueProtocolFixture {
 public final p.jbl a=new p.jbl();public final p.irc0 b=new p.irc0();public static int revision,writes;public static boolean missingAck;private static List<ContextTrack> next,previous;private static ContextTrack current;
 public static void reset(){revision=1;writes=0;missingAck=false;current=new ContextTrack("spotify:track:0000000000000000000001","current-fixture");next=new ArrayList<>(Arrays.asList(new ContextTrack("spotify:track:0000000000000000000002","next-a"),new ContextTrack("spotify:track:0000000000000000000003","next-b")));previous=new ArrayList<>(Collections.singletonList(new ContextTrack("spotify:track:0000000000000000000004","previous-fixture")));}
 public static PlayerQueue read(){return new PlayerQueue(Integer.toString(revision),current,next,previous);}
 public static Object write(SetQueueCommand request){writes++;if(!Integer.toString(revision).equals(request.revision))return new Object();next=new ArrayList<>(request.next);previous=new ArrayList<>(request.previous);revision++;return missingAck?new Object():new p.ibg();}
}
