package com.spotify.player.model;
import java.util.*;
/** Original model double for adapter tests; no proprietary implementation. */
public final class PlayerQueue {private final String revision;private final ContextTrack current;private final List<ContextTrack> next,previous;public PlayerQueue(String revision,ContextTrack current,List<ContextTrack> next,List<ContextTrack> previous){this.revision=revision;this.current=current;this.next=new ArrayList<>(next);this.previous=new ArrayList<>(previous);}public String revision(){return revision;}public p.z6o0 track(){return new p.z6o0(current);}public List<ContextTrack> nextTracks(){return new ArrayList<>(next);}public List<ContextTrack> prevTracks(){return new ArrayList<>(previous);}}
