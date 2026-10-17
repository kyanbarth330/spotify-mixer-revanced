package org.example.spotifymixer.runtime;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import java.io.IOException;

/** Local configuration only: no credentials or track caches in presets/history. */
public final class MixLibrary {
    private final SharedPreferences prefs;
    public MixLibrary(SharedPreferences prefs){this.prefs=prefs;}
    public static final class Entry {
        public final String id,name;public final long time;public final int tracks;public final MixConfig config;
        Entry(JSONObject value)throws JSONException{id=value.getString("id");name=value.getString("name");time=value.getLong("time");tracks=value.optInt("tracks",0);config=decode(value.getJSONObject("config"));}
    }
    static JSONObject encode(MixConfig config)throws JSONException{
        JSONObject out=new JSONObject();out.put("shuffle",config.shuffle);JSONArray sources=new JSONArray();
        for(MixConfig.Source s:config.sources){JSONObject row=new JSONObject();row.put("uri",s.uri);row.put("title",s.title);row.put("weight",s.weight);sources.put(row);}out.put("sources",sources);return out;
    }
    static MixConfig decode(JSONObject value)throws JSONException{
        List<MixConfig.Source> sources=new ArrayList<>();JSONArray rows=value.getJSONArray("sources");for(int i=0;i<rows.length();i++){JSONObject s=rows.getJSONObject(i);sources.add(new MixConfig.Source(s.getString("uri"),s.getString("title"),s.getInt("weight")));}return new MixConfig(sources,value.getBoolean("shuffle"));
    }
    public MixConfig active()throws IOException{
        try{
            String saved=prefs.getString("active_v2",null);if(saved!=null)return decode(new JSONObject(saved));
            List<MixConfig.Source> sources=new ArrayList<>();String a=PlaylistUri.normalize(prefs.getString("a",null)),b=PlaylistUri.normalize(prefs.getString("b",null));int ratio=prefs.getInt("ratio",70);
            if(a!=null)sources.add(new MixConfig.Source(a,prefs.getString("a_title",null),b==null||a.equals(b)?100:ratio));
            if(b!=null&&!b.equals(a))sources.add(new MixConfig.Source(b,prefs.getString("b_title",null),a==null?100:100-ratio));
            return new MixConfig(sources,prefs.getBoolean("shuffle",false));
        }catch(JSONException|IllegalArgumentException e){throw new IOException("Saved mix could not be read",e);}
    }
    public void save(MixConfig config)throws IOException{try{if(!prefs.edit().putString("active_v2",encode(config).toString()).commit())throw new IOException("Could not save mix");}catch(JSONException e){throw new IOException(e);}}
    public String signature(){return prefs.getString("active_v2","")+"|"+prefs.getString("a","")+"|"+prefs.getString("b","");}
    public void select(int index,String uri,String title)throws IOException{MixConfig current=active();if(index<0||index>current.sources.size())throw new IOException("Selection request expired. Choose a playlist again.");try{save(current.select(index,uri,title));if(!prefs.edit().remove("pending_slot").commit())throw new IOException("Could not finish selection");}catch(IllegalArgumentException e){throw new IOException(e.getMessage(),e);}}
    private JSONArray array(String key)throws JSONException{return new JSONArray(prefs.getString(key,"[]"));}
    public List<Entry> entries(boolean history)throws IOException{try{JSONArray rows=array(history?"history_v2":"presets_v2");List<Entry> out=new ArrayList<>();for(int i=0;i<rows.length();i++)out.add(new Entry(rows.getJSONObject(i)));return out;}catch(JSONException|IllegalArgumentException e){throw new IOException("Saved library could not be read",e);}}
    public void add(String name,MixConfig config,boolean history,int tracks)throws IOException{
        if(name==null||name.trim().isEmpty()||name.trim().length()>80)throw new IOException("Use a name between 1 and 80 characters");
        if(config.sources.size()<2)throw new IOException("Add at least two playlists first");
        String key=history?"history_v2":"presets_v2";
        try{
            JSONArray previous=array(key),next=new JSONArray();JSONObject row=new JSONObject();row.put("id",UUID.randomUUID().toString());row.put("name",name.trim());row.put("time",System.currentTimeMillis());row.put("tracks",tracks);row.put("config",encode(config));next.put(row);
            for(int i=0;i<previous.length()&&(!history||next.length()<50);i++)next.put(previous.getJSONObject(i));
            if(!prefs.edit().putString(key,next.toString()).commit())throw new IOException("Could not save library");
        }catch(JSONException e){throw new IOException(e);}
    }
    public void delete(String id,boolean history)throws IOException{try{String key=history?"history_v2":"presets_v2";JSONArray rows=array(key),next=new JSONArray();for(int i=0;i<rows.length();i++)if(!id.equals(rows.getJSONObject(i).getString("id")))next.put(rows.getJSONObject(i));if(!prefs.edit().putString(key,next.toString()).commit())throw new IOException("Could not remove entry");}catch(JSONException e){throw new IOException(e);}}
}
