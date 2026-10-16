package org.example.spotifymixer.runtime;

import android.os.Parcelable;
import android.net.Uri;
import java.lang.reflect.*;
import java.util.*;

/** Reads only the explicit page-parameters payload, not an arbitrary object graph. */
public final class PageParameters {
    public static String playlist(Object parameters){
        if(!(parameters instanceof Parcelable))return null;
        Set<String> candidates=new LinkedHashSet<>();
        int count=0;
        for(Class<?> type=parameters.getClass();type!=null && type!=Object.class;type=type.getSuperclass()){
            for(Field field:type.getDeclaredFields()){
                if(Modifier.isStatic(field.getModifiers()) || field.isSynthetic())continue;
                if(++count>32)return null;
                try {
                    field.setAccessible(true);Object value=field.get(parameters);
                    String uri=scalar(value);
                    // Verified Spotify URI wrapper: p.jh71.a is android.net.Uri.
                    if(uri==null && value!=null && value.getClass().getName().equals("p.jh71")){
                        Field wrapped=value.getClass().getDeclaredField("a");wrapped.setAccessible(true);uri=scalar(wrapped.get(value));
                    }
                    if(uri!=null)candidates.add(uri);
                }catch(ReflectiveOperationException | RuntimeException ignored){}
            }
        }
        return candidates.size()==1?candidates.iterator().next():null;
    }
    private static String scalar(Object value){return value instanceof String || value instanceof Uri?PlaylistUri.normalize(value.toString()):null;}
    static String describe(Object parameters){
        if(parameters==null)return "parameters=null";
        StringBuilder out=new StringBuilder("parameters="+parameters.getClass().getName()+" fields=[");int count=0;
        for(Class<?> type=parameters.getClass();type!=null && type!=Object.class;type=type.getSuperclass()){
            for(Field field:type.getDeclaredFields()){
                if(Modifier.isStatic(field.getModifiers()) || field.isSynthetic())continue;
                if(count++==12)return out.append("…]").toString();
                out.append(field.getName()).append(':').append(field.getType().getName()).append(' ');
            }
        }
        return out.append(']').toString();
    }
}
