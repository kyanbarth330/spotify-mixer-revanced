package org.example.spotifymixer.runtime;

import android.content.SharedPreferences;import org.json.*;import java.io.*;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.TimeUnit;import java.util.function.Consumer;
/** Uses Spotify's initialized authenticated clients; never reads credentials or bypasses restrictions. */
public final class NativeCreator implements CreatorSession.Backend {
 private static volatile Object maker;private static final Object writeLock=new Object();private static Thread writer;
 private final SharedPreferences prefs;
 public NativeCreator(SharedPreferences prefs){this.prefs=prefs;}
 public static void captureMaker(Object value){maker=value;}
 public static boolean ready(){return maker!=null&&NativeBridge.playlistReady();}
 public static String state(){return "Search: "+(maker==null?"waiting":"ready")+" · Playlist service: "+(NativeBridge.playlistReady()?"ready":"waiting");}
 private static Object invoke(Method m,Object target,Object... args)throws Exception{try{return m.invoke(target,args);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw (Exception)t;throw e;}}
 private static Object await(Object single,ClassLoader loader)throws Exception{Class<?> rx=Class.forName("io.reactivex.rxjava3.core.Single",false,loader);Object timed=invoke(rx.getMethod("timeout",long.class,TimeUnit.class),single,30L,TimeUnit.SECONDS);return invoke(rx.getMethod("blockingGet"),timed);}
 public List<CreatorSession.Track> suggest(CreatorSession.Request request,Consumer<String> progress)throws Exception{
  Auth.require("creator");if(pending())throw new IOException("Resolve the unfinished creation request before generating another playlist");Object factory=maker;if(factory==null||!NativeBridge.playlistReady())throw new IOException("Open a playlist in Spotify, then refresh the connection");
  ClassLoader loader=factory.getClass().getClassLoader();Class<?> api=Class.forName("p.sy11",false,loader);
  Object search=invoke(factory.getClass().getMethod("createWebgateService",Class.class,String.class),factory,api,"android-search-onlineproto");
  List<String> queries=CreatorWire.queries(request);List<List<CreatorSession.Track>> groups=new ArrayList<>();for(String q:queries)groups.add(new ArrayList<>());Set<String> unique=new HashSet<>();List<String> tokens=new ArrayList<>(Collections.nCopies(queries.size(),""));boolean[] exhausted=new boolean[queries.size()];int calls=0;int goal=Math.min(1000,request.count+Math.max(50,request.count));
  for(int round=0;round<20&&unique.size()<goal&&calls<48;round++){
   boolean more=false;for(int i=0;i<queries.size();i++){
    if(Thread.currentThread().isInterrupted())throw new InterruptedException();if(exhausted[i]||calls>=48)continue;calls++;more=true;progress.accept("Searching "+queries.get(i)+" · "+unique.size()+" candidates");
    Map<String,String> params=new LinkedHashMap<>();params.put("query",queries.get(i));params.put("entity_types","track");params.put("limit","50");params.put("locale",Locale.getDefault().toLanguageTag());params.put("request_id",UUID.randomUUID().toString());params.put("timestamp",Long.toString(System.currentTimeMillis()));if(!tokens.get(i).isEmpty())params.put("page_token",tokens.get(i));
    Object response=await(invoke(api.getMethod("a",Map.class,Map.class),search,params,Collections.emptyMap()),loader);
    byte[] raw=(byte[])invoke(response.getClass().getMethod("toByteArray"),response);CreatorWire.Page page=CreatorWire.search(raw);
    for(CreatorSession.Track track:page.tracks)if(CreatorIntent.accept(track,request)&&unique.add(track.uri))groups.get(i).add(track);
    exhausted[i]=page.next.isEmpty()||page.next.equals(tokens.get(i));tokens.set(i,page.next);
   }if(!more)break;
  }
  List<CreatorSession.Track> all=new ArrayList<>();for(int rank=0;;rank++){boolean added=false;for(List<CreatorSession.Track> group:groups)if(rank<group.size()){all.add(group.get(rank));added=true;}if(!added)break;}progress.accept("Balancing artists across "+all.size()+" candidates");return CreatorIntent.diversify(all);
 }
 public boolean pending(){return prefs.contains("creator_pending_write");}
 private void store(JSONObject value)throws IOException{if(!prefs.edit().putString("creator_pending_write",value.toString()).commit())throw new IOException("Could not save creation recovery state");}
 public void clearPending()throws IOException{synchronized(writeLock){if(writer!=null&&writer!=Thread.currentThread())throw new IOException("A creation request is still running");if(!prefs.edit().remove("creator_pending_write").commit())throw new IOException("Could not clear recovery state");}}
 public CreatorSession.Receipt create(String name,List<CreatorSession.Track> tracks,String operation)throws Exception{
  Auth.require("creator");if(!ready())throw new IOException("Spotify services are not ready");synchronized(writeLock){if(writer!=null||pending())throw new IOException("Resolve the previous creation request first");writer=Thread.currentThread();}
  try{
  JSONArray ids=new JSONArray();for(CreatorSession.Track t:tracks)ids.put(t.uri);
  JSONObject state=new JSONObject().put("operation",operation).put("name",name).put("tracks",ids).put("uri","");store(state);
  // Exactly one mutation. Never retry a timed-out creation automatically.
  String uri=CreatorWire.createdUri(NativeBridge.creatorCall(CreatorWire.create(name,tracks)));state.put("uri",uri);store(state);
  return verifyPending();
  }finally{synchronized(writeLock){writer=null;}}
 }
 public CreatorSession.Receipt verifyPending()throws Exception{
  synchronized(writeLock){if(writer!=null&&writer!=Thread.currentThread())throw new IOException("A creation request is still running");}
  String raw=prefs.getString("creator_pending_write",null);if(raw==null)throw new IOException("No unfinished request");JSONObject state=new JSONObject(raw);String uri=state.getString("uri");if(uri.isEmpty())throw new IOException("No playlist identity was confirmed. Check your Spotify library before clearing this request.");
  JSONArray expected=state.getJSONArray("tracks");Exception last=null;
  for(int attempt=0;attempt<3;attempt++){
   try{MixSession.Playlist actual=new NativeBridge().load(uri,m->{});if(!state.getString("name").equals(actual.title)||actual.tracks.size()!=expected.length())throw new IOException("Playlist name/count not yet confirmed");for(int i=0;i<expected.length();i++)if(!expected.getString(i).equals(actual.tracks.get(i)))throw new IOException("Playlist track order does not match the reviewed proposal");CreatorSession.Receipt receipt=new CreatorSession.Receipt(uri,actual.title,actual.tracks.size());synchronized(writeLock){if(!raw.equals(prefs.getString("creator_pending_write",null)))throw new IOException("Recovery record changed; check again");clearPending();}return receipt;}catch(Exception e){last=e;if(attempt<2)Thread.sleep(1000);}
  }throw new IOException("Playlist was created but contents could not be verified. Use Check previous creation.",last);
 }
}
