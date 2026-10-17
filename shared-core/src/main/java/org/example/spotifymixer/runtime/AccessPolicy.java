package org.example.spotifymixer.runtime;
import java.util.*;
/** Product entitlements, unrelated to Spotify account rights. Six of 32 tools are free. */
public final class AccessPolicy {
 public static final Set<String> FREE=Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("mixer","sleep","duplicates","clean_share","quick_search","details")));
 public static final Set<String> ALL=Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("mixer","presets","history","equalizer","themes","creator","sleep","duplicates","cinema","artwork","find","rediscover","amoled","compact","queue_search","queue_undo","clean_share","volume","rules","guard","quick_search","seek","confirm_clear","track_info","start","skip_guard","profiles","bookmarks","compare","details","saved_searches","awake")));
 public static boolean allowed(String id,boolean premium){return id.equals("account")||id.equals("diagnostics")||ALL.contains(id)&&(premium||FREE.contains(id));}
 private AccessPolicy(){}
}
