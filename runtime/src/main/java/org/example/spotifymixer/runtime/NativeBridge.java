package org.example.spotifymixer.runtime;

import java.io.IOException;import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicReference;import java.util.function.Consumer;
/** SHA-pinned native service adapter. Never obtains tokens or bypasses playback restrictions. */
public final class NativeBridge implements MixSession.Backend {
    private static volatile Object playlist,player;
    public static void capturePlaylist(Object service){playlist=service;Diagnostics.event("Native playlist service captured");}
    public static void capturePlayer(Object service){player=service;Diagnostics.event("Native player service captured");}
    public static boolean ready(){return playlist!=null&&player!=null;}
    public static String state(){return "Playlist service: "+(playlist==null?"waiting":"ready")+" · Player service: "+(player==null?"waiting":"ready");}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static Class<?> type(ClassLoader loader,String name)throws Exception{return Class.forName(name,false,loader);}
    private static Object invoke(Method method,Object target,Object... args)throws Exception{
        try{return method.invoke(target,args);}catch(InvocationTargetException error){Throwable cause=error.getCause();if(cause instanceof Exception)throw (Exception)cause;throw error;}
    }
    public MixSession.Playlist load(String uri,Consumer<String> progress)throws Exception{
        Object service=playlist;if(service==null)throw new IOException("Open a playlist in Spotify first");
        ClassLoader loader=service.getClass().getClassLoader();Object client=field(service,"f"),context=field(service,"c");
        Class<?> base=type(loader,"com.spotify.esperanto.esperanto.CoroutineClientBase");Field tf=base.getDeclaredField("transport");tf.setAccessible(true);Object transport=tf.get(client);
        Class<?> continuation=type(loader,"p.uel"),transportApi=type(loader,"com.spotify.esperanto.esperanto.CoroutineTransport");
        Method call=transportApi.getMethod("callSingle",String.class,String.class,byte[].class,continuation);
        List<String> tracks=new ArrayList<>();String title="";int offset=0,total=-1;
        while(true){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            CountDownLatch done=new CountDownLatch(1);AtomicReference<Object> result=new AtomicReference<>();
            Object callback=Proxy.newProxyInstance(loader,new Class<?>[]{continuation},(proxy,method,args)->{
                switch(method.getName()){
                    case "getContext":return context;
                    case "resumeWith":result.set(args[0]);done.countDown();return null;
                    case "toString":return "SpotEnhancer playlist continuation";
                    case "hashCode":return System.identityHashCode(proxy);
                    case "equals":return proxy==args[0];
                    default:throw new UnsupportedOperationException(method.getName());
                }
            });
            progress.accept("Loading playlist items "+offset+"–"+(offset+100));
            Object immediate=invoke(call,transport,"spotify.playlist_esperanto.proto.PlaylistDataService","Get",PlaylistWire.request(uri,offset,100),callback);
            Object value=immediate;
            if(!(value instanceof byte[])){
                if(!done.await(TimeUnit.SECONDS.toNanos(30),TimeUnit.NANOSECONDS))throw new IOException("Playlist service timed out; try again");value=result.get();
            }
            if(!(value instanceof byte[])){
                // This is the same Kotlin Result failure check used by the original callers.
                invoke(type(loader,"p.yd70").getMethod("M",Object.class),null,value);
                throw new IOException("Unexpected playlist service response");
            }
            PlaylistWire.Page page=PlaylistWire.response((byte[])value);
            if(page.loading)throw new IOException("Spotify is still loading this playlist; try again");
            if(total>=0&&page.total>=0&&total!=page.total)throw new IOException("Playlist changed while loading; try again");
            if(page.total>=0)total=page.total;
            if(!page.title.isEmpty())title=page.title;tracks.addAll(page.tracks);offset+=page.rows;
            if(total>=0&&offset>=total)break;
            if(page.rows<100){if(total>offset)throw new IOException("Incomplete playlist response; try again");break;}
            if(offset>Integer.MAX_VALUE-100)throw new IOException("Playlist offset overflow");
        }
        return new MixSession.Playlist(title,tracks);
    }
    public static boolean playlistReady(){return playlist!=null;}
    static byte[] creatorCall(byte[] request)throws Exception{
        Object service=playlist;if(service==null)throw new IOException("Open a playlist in Spotify first");
        ClassLoader loader=service.getClass().getClassLoader();Object client=field(service,"f"),context=field(service,"c");Class<?> base=type(loader,"com.spotify.esperanto.esperanto.CoroutineClientBase");Field tf=base.getDeclaredField("transport");tf.setAccessible(true);Object transport=tf.get(client);
        Class<?> continuation=type(loader,"p.uel");CountDownLatch done=new CountDownLatch(1);AtomicReference<Object> result=new AtomicReference<>();
        Object callback=Proxy.newProxyInstance(loader,new Class<?>[]{continuation},(proxy,method,args)->{switch(method.getName()){case "getContext":return context;case "resumeWith":result.set(args[0]);done.countDown();return null;case "toString":return "SpotEnhancer create continuation";case "hashCode":return System.identityHashCode(proxy);case "equals":return proxy==args[0];default:throw new UnsupportedOperationException(method.getName());}});
        Object response=invoke(type(loader,"com.spotify.esperanto.esperanto.CoroutineTransport").getMethod("callSingle",String.class,String.class,byte[].class,continuation),transport,"spotify.playlist_esperanto.proto.RootlistModificationService","Modify",request,callback);
        if(!(response instanceof byte[])){if(!done.await(30,TimeUnit.SECONDS))throw new IOException("Creation timed out; check Spotify before retrying");response=result.get();}
        if(!(response instanceof byte[])){invoke(type(loader,"p.yd70").getMethod("M",Object.class),null,response);throw new IOException("Unexpected creation response");}return (byte[])response;
    }
    public void play(List<String> tracks,boolean shuffle)throws Exception{
        Object service=player;if(service==null)throw new IOException("Play a track in Spotify first");if(tracks.isEmpty())throw new IOException("The mix is empty");
        ClassLoader loader=service.getClass().getClassLoader();Class<?> list=type(loader,"p.l960"),ctx=type(loader,"com.spotify.player.model.Context"),origin=type(loader,"com.spotify.player.model.PlayOrigin"),command=type(loader,"com.spotify.player.model.command.PlayCommand");
        Object uris=invoke(list.getMethod("p",Collection.class),null,tracks);
        Object context=invoke(ctx.getMethod("fromTrackUris",String.class,list),null,"spotify:collection:tracks",uris);
        Object source=invoke(origin.getMethod("create",String.class),null,"spotenhancer");
        Class<?> overrides=type(loader,"com.spotify.player.model.command.options.PlayerOptionOverrides"),ob=type(loader,"com.spotify.player.model.command.options.PlayerOptionOverrides$Builder"),prepare=type(loader,"com.spotify.player.model.command.options.PreparePlayOptions"),pb=type(loader,"com.spotify.player.model.command.options.PreparePlayOptions$Builder"),cb=type(loader,"com.spotify.player.model.command.PlayCommand$Builder");
        Object overrideBuilder=invoke(overrides.getMethod("builder"),null);
        invoke(ob.getMethod("shufflingContext",Boolean.class),overrideBuilder,Boolean.valueOf(shuffle));
        invoke(ob.getMethod("repeatingContext",Boolean.class),overrideBuilder,Boolean.FALSE);
        invoke(ob.getMethod("repeatingTrack",Boolean.class),overrideBuilder,Boolean.FALSE);
        Object optionsBuilder=invoke(prepare.getMethod("builder"),null);
        invoke(pb.getMethod("initiallyPaused",boolean.class),optionsBuilder,false);
        invoke(pb.getMethod("playerOptionsOverride",overrides),optionsBuilder,invoke(ob.getMethod("build"),overrideBuilder));
        Object commandBuilder=invoke(command.getMethod("builder",ctx,origin),null,context,source);
        invoke(cb.getMethod("options",prepare),commandBuilder,invoke(pb.getMethod("build"),optionsBuilder));
        Object request=invoke(cb.getMethod("build"),commandBuilder);
        Object single=invoke(service.getClass().getMethod("a",command),service,request);
        Class<?> rx=type(loader,"io.reactivex.rxjava3.core.Single");single=invoke(rx.getMethod("timeout",long.class,TimeUnit.class),single,30L,TimeUnit.SECONDS);
        Object response=invoke(rx.getMethod("blockingGet"),single);
        if(!type(loader,"p.ibg").isInstance(response))throw new IOException("Spotify did not accept the play request");
    }
}
