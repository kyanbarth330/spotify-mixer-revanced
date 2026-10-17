package org.example.spotifymixer.runtime;
public final class FeatureCatalog {
 public static final class Feature {
  public final String id,title,subtitle,category,limit;
  Feature(String id,String title,String subtitle,String category,String limit){this.id=id;this.title=title;this.subtitle=subtitle;this.category=category;this.limit=limit;}
  public String key(){return id+"_enabled";}
 }
 public static final Feature[] ALL={
  new Feature("sleep","Sleep Timer","Set your own stop time","Playback",null),
  new Feature("duplicates","Playlist Check","Find repeated track identities","Tools",null),
  new Feature("cinema","Cinema Player","Large artwork and calm controls","Playback",null),
  new Feature("artwork","Live Artwork","Animated artwork colors in Cinema","Appearance",null),
  new Feature("find","Find in Playlists","Search your selected playlist sources","Tools",null),
  new Feature("rediscover","Rediscover","Revisit locally observed listening","Tools",null),
  new Feature("amoled","AMOLED Mode","True black supported backgrounds","Appearance",null),
  new Feature("compact","Compact Lists","Compact supported Android view rows","Appearance",null),
  new Feature("queue_search","Queue Search","Search Spotify's published queue","Playback",null),
  new Feature("queue_undo","Queue Undo","Save and restore published track lists","Playback",null),
  new Feature("clean_share","Clean Share","Copy a clean current-track link","Tools",null),
  new Feature("volume","Volume Steps","Choose foreground volume key steps","Playback",null),
  new Feature("rules","Playback Rules","Headphone connection actions","Playback",null),
  new Feature("guard","Session Guard","Confirm replacements from our tools","Playback",null),
  new Feature("quick_search","Quick Search","Search Spotify from the tools button","Tools",null),
  new Feature("seek","Seek Buttons","Jump 10 or 30 seconds in Cinema","Playback",null),
  new Feature("confirm_clear","Confirm Queue Clear","Review and clear native upcoming items","Playback",null),
  new Feature("track_info","Always Show Track Info","Expand supported track text labels","Appearance",null),
  new Feature("start","Start Page","Choose a launcher landing page","Tools",null),
  new Feature("skip_guard","No Accidental Skip","Confirm repeated skip presses","Playback",null),
  new Feature("profiles","Settings Profiles","Save and restore enhancer settings","Tools",null)
 };
 public static Feature find(String id){for(Feature f:ALL)if(f.id.equals(id))return f;throw new IllegalArgumentException("Unknown enhancement");}
 private FeatureCatalog(){}
}
