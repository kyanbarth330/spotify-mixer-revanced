package com.spotify.player.model.command;
import java.util.*;import com.spotify.player.model.ContextTrack;
/** Original command double, no actual Spotify request. */
public final class SetQueueCommand {public final String revision;public final List<ContextTrack> next,previous;private SetQueueCommand(String revision,List<ContextTrack> next,List<ContextTrack> previous){this.revision=revision;this.next=new ArrayList<>(next);this.previous=new ArrayList<>(previous);}public static SetQueueCommand create(String revision,List<ContextTrack> next,List<ContextTrack> previous){return new SetQueueCommand(revision,next,previous);}}
