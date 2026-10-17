package org.example.spotifymixer.runtime;
import android.content.SharedPreferences;import java.util.*;import org.json.*;
/** Explicit setting allowlist: never captures account, transaction, queue or listening data. */
public final class SettingsProfiles {
 private final SharedPreferences prefs;
 public SettingsProfiles(SharedPreferences prefs){this.prefs=prefs;}
 static boolean allowed(String key){if(key.equals("profiles_enabled"))return false;for(FeatureCatalog.Feature f:FeatureCatalog.ALL)if(key.equals(f.key()))return true;return key.equals("theme_enabled")||key.equals("theme_preset")||key.equals("eq_enabled")||key.equals("eq_preset")||key.matches("eq_hz_[0-9]+")||Arrays.asList("volume_steps","rules_pause","rules_resume","start_route","sleep_minutes","creator_enabled").contains(key);}
 public JSONObject all()throws JSONException{return new JSONObject(prefs.getString("settings_profiles","{}"));}
 public void save(String name)throws JSONException{String title=name.trim();if(title.isEmpty()||title.length()>60)throw new IllegalArgumentException("Use a name between 1 and 60 characters");JSONObject profiles=all(),values=new JSONObject();for(Map.Entry<String,?> e:prefs.getAll().entrySet())if(allowed(e.getKey()))values.put(e.getKey(),e.getValue());profiles.put(title,values);if(!prefs.edit().putString("settings_profiles",profiles.toString()).commit())throw new IllegalStateException("Could not save profile");}
 public void apply(String name)throws JSONException{JSONObject values=all().getJSONObject(name);SharedPreferences.Editor edit=prefs.edit();for(String key:prefs.getAll().keySet())if(allowed(key))edit.remove(key);Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();if(!allowed(key))continue;Object v=values.get(key);if(v instanceof Boolean)edit.putBoolean(key,(Boolean)v);else if(v instanceof Number)edit.putInt(key,((Number)v).intValue());else if(v instanceof String)edit.putString(key,(String)v);}if(!edit.commit())throw new IllegalStateException("Could not apply profile");}
 public void delete(String name)throws JSONException{JSONObject values=all();values.remove(name);if(!prefs.edit().putString("settings_profiles",values.toString()).commit())throw new IllegalStateException("Could not delete profile");}
}
