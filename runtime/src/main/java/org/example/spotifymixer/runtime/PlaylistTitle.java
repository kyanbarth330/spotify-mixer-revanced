package org.example.spotifymixer.runtime;

import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import java.util.LinkedHashSet;

/** Titles never establish playlist identity. Only explicit metadata or one visible heading. */
public final class PlaylistTitle {
    private PlaylistTitle(){}
    public static String from(Bundle args,View root){
        for(String key:new String[]{"playlist_name","playlist_title","title","name"}){
            Object value=args.get(key);if(value instanceof String){String title=clean((String)value);if(title!=null)return title;}
        }
        return visibleHeading(root);
    }
    public static String visibleHeading(View root){
        if(Build.VERSION.SDK_INT<28 || root==null)return null;
        LinkedHashSet<String> candidates=new LinkedHashSet<>();int[] budget={256};
        headings(root,candidates,budget,0);
        return budget[0]>0 && candidates.size()==1?candidates.iterator().next():null;
    }
    private static void headings(View v,LinkedHashSet<String> values,int[] budget,int depth){
        if(--budget[0]<=0 || depth>12 || !v.isShown())return;
        if(v instanceof TextView && v.isAccessibilityHeading() && !v.isClickable()){
            String title=clean(((TextView)v).getText().toString());if(title!=null)values.add(title);
        }
        if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount() && budget[0]>0;i++)headings(group.getChildAt(i),values,budget,depth+1);}
    }
    private static String clean(String value){String title=value.trim();return title.isEmpty() || title.length()>300 || PlaylistUri.normalize(title)!=null?null:title;}
}
