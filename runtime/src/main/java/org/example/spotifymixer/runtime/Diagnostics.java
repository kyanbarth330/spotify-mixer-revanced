package org.example.spotifymixer.runtime;
import android.content.Context;
import java.util.*;
final class Diagnostics {
    private static final ArrayDeque<String> events=new ArrayDeque<>();
    private static int resumes;
    static synchronized void resume(){resumes++;}
    static synchronized void event(String text){if(events.size()==12)events.removeFirst();events.addLast(text);}
    static synchronized String report(){StringBuilder out=new StringBuilder("SpotEnhancer 0.20\nFragment resume calls: "+resumes+"\n");for(String line:events)out.append(line).append('\n');return out.toString();}
}
