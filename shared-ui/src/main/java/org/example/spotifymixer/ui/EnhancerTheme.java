package org.example.spotifymixer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Resource-independent styling: the same original UI is also injected as DEX. */
public final class EnhancerTheme {
    public static final int BACKGROUND=0xff121212, SURFACE=0xff242424, GREEN=0xff1ed760, MUTED=0xffb3b3b3;
    private EnhancerTheme(){}
    public static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    public static void window(Activity a){a.getWindow().setStatusBarColor(BACKGROUND);a.getWindow().setNavigationBarColor(BACKGROUND);a.getWindow().getDecorView().setSystemUiVisibility(0);}
    public static GradientDrawable surface(int color,int radius,Context c){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    public static TextView text(LinearLayout box,String value,int size,boolean bold){
        Context c=box.getContext();TextView t=new TextView(c);t.setText(value);t.setTextColor(size>=20?0xffffffff:MUTED);t.setTextSize(size);t.setPadding(0,dp(c,8),0,dp(c,8));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(t);return t;
    }
    public static void styleButton(Button b,boolean primary){
        Context c=b.getContext();b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,16),dp(c,10),dp(c,16),dp(c,10));
        b.setTextColor(new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff808080,primary?0xff000000:0xffffffff}));
        StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},surface(0xff303030,28,c));states.addState(new int[]{},surface(primary?GREEN:SURFACE,28,c));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),states,null));
    }
    public static Button button(LinearLayout box,String label,boolean primary,Runnable action){Button b=new Button(box.getContext());b.setText(label);styleButton(b,primary);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(box.getContext(),8);lp.bottomMargin=dp(box.getContext(),8);box.addView(b,lp);return b;}
    public static LinearLayout card(LinearLayout parent){Context c=parent.getContext();LinearLayout card=new LinearLayout(c);card.setOrientation(1);card.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));card.setBackground(surface(SURFACE,16,c));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(c,10);lp.bottomMargin=dp(c,10);parent.addView(card,lp);return card;}
    public static LinearLayout page(Activity a,String heading){
        window(a);ScrollView scroll=new ScrollView(a);scroll.setBackgroundColor(BACKGROUND);scroll.setFillViewport(true);LinearLayout box=new LinearLayout(a);box.setOrientation(1);int gap=dp(a,20);box.setPadding(gap,gap,gap,gap);
        box.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(gap+insets.getSystemWindowInsetLeft(),gap+insets.getSystemWindowInsetTop(),gap+insets.getSystemWindowInsetRight(),gap+insets.getSystemWindowInsetBottom());return insets;});
        scroll.addView(box,new ScrollView.LayoutParams(-1,-2));a.setContentView(scroll);box.requestApplyInsets();text(box,heading,32,true);return box;
    }
}
