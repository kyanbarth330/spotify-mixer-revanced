package org.example.spotifymixer.runtime;
import java.util.*;
/** Original global semantic color presets. Artwork and arbitrary saturated colors are preserved. */
public final class ThemePalette {
 public final String id,name;public final int background,surface,line,text,muted,accent;
 private ThemePalette(String id,String name,int bg,int card,int line,int text,int muted,int accent){this.id=id;this.name=name;this.background=bg;this.surface=card;this.line=line;this.text=text;this.muted=muted;this.accent=accent;}
 public static final List<ThemePalette> PRESETS=Collections.unmodifiableList(Arrays.asList(
  new ThemePalette("amoled","AMOLED Green",0xff000000,0xff101110,0xff343735,0xfff5f5f5,0xffb9c0bd,0xff1ed760),
  new ThemePalette("ocean","Midnight Ocean",0xff06111f,0xff102438,0xff284459,0xfff0f7ff,0xffb1c4d8,0xff6ad8ff),
  new ThemePalette("velvet","Velvet Purple",0xff140b20,0xff251735,0xff493455,0xfffaf0ff,0xffc5b5d0,0xffd3a0ff),
  new ThemePalette("rose","Rose Gold",0xff1b1014,0xff2e1c24,0xff53333e,0xfffff1e8,0xffd4b8b0,0xffffb18d),
  new ThemePalette("mint","Arctic Mint",0xff071815,0xff102c27,0xff29554a,0xffeafff6,0xffa9cec0,0xff61f0bd)));
 public static ThemePalette find(String id){for(ThemePalette p:PRESETS)if(p.id.equals(id))return p;return PRESETS.get(0);}
 public int map(int color){
  int alpha=color&0xff000000,rgb=color&0xffffff;if(alpha==0)return color;
  // An already themed color must not drift when it passes multiple UI conversion layers.
  if((background&0xffffff)==rgb||(surface&0xffffff)==rgb||(line&0xffffff)==rgb||(text&0xffffff)==rgb||(muted&0xffffff)==rgb||(accent&0xffffff)==rgb)return color;
  int red=rgb>>16,green=(rgb>>8)&255,blue=rgb&255,target;
  if(rgb==0x1ed760||rgb==0x1db954||rgb==0x1fdf64||rgb==0x169c46||rgb==0x14833b||rgb==0x15883e)target=accent;
  else if(Math.max(red,Math.max(green,blue))-Math.min(red,Math.min(green,blue))<=8){
   int shade=(red+green+blue)/3;if(shade<=18)target=background;else if(shade<=48)target=surface;else if(shade<=95)target=line;else if(shade<230)target=muted;else target=text;
  }else return color;
  return alpha|(target&0xffffff);
 }
}
