package org.example.spotifymixer.runtime;
import java.util.*;import java.io.IOException;
/** One confirmed revision-bound write followed by reads, never automatic write retries. */
public final class QueueClearSession {
 public static final class Snapshot {
  public final String revision,current;public final List<String> next,previous;public final Object handle;
  public Snapshot(String revision,String current,List<String> next,List<String> previous,Object handle){if(revision==null||revision.isEmpty()||current==null)throw new IllegalArgumentException("Missing queue identity");this.revision=revision;this.current=current;this.next=Collections.unmodifiableList(new ArrayList<>(next));this.previous=Collections.unmodifiableList(new ArrayList<>(previous));this.handle=handle;}
 }
 public interface Backend{Snapshot read()throws Exception;void clear(Snapshot expected)throws Exception;}
 public interface Recovery{String pending();void begin(String current)throws Exception;default void begin(Snapshot snapshot)throws Exception{begin(snapshot.current);}default List<String> previous()throws Exception{return null;}void complete()throws Exception;}
 private final Backend backend;private final Recovery recovery;private Snapshot preview;
 public QueueClearSession(Backend backend,Recovery recovery){this.backend=backend;this.recovery=recovery;}
 public synchronized Snapshot preview()throws Exception{if(recovery.pending()!=null)throw new IOException("Verify the unfinished clear request first");preview=backend.read();return preview;}
 public synchronized void clear(Snapshot expected)throws Exception{if(expected==null||expected!=preview)throw new IOException("Refresh queue preview first");preview=null;if(recovery.pending()!=null)throw new IOException("Verify the unfinished clear request first");Snapshot now=backend.read();if(!now.revision.equals(expected.revision)||!now.current.equals(expected.current)||!now.next.equals(expected.next)||!now.previous.equals(expected.previous))throw new IOException("Queue changed; review a fresh preview");if(now.next.isEmpty())return;recovery.begin(now);backend.clear(now);verify();}
 public synchronized Snapshot verify()throws Exception{String current=recovery.pending();if(current==null)throw new IOException("No unfinished clear request");Snapshot now=backend.read();List<String> previous=recovery.previous();if(!now.current.equals(current)||!now.next.isEmpty()||(previous!=null&&!previous.equals(now.previous)))throw new IOException("Clear is unconfirmed. Check Spotify; only read-only verification is available");recovery.complete();return now;}
}
