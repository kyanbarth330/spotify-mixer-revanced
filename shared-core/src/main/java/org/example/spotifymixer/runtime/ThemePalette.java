package org.example.spotifymixer.runtime;
import java.util.*;
/** Original global semantic color presets. Artwork and arbitrary saturated colors are preserved. */
public final class ThemePalette {
 public final String id,name,category;public final int background,surface,line,text,muted,accent;
 private ThemePalette(String id,String name,String category,int bg,int card,int line,int text,int muted,int accent){this.id=id;this.name=name;this.category=category;this.background=bg;this.surface=card;this.line=line;this.text=text;this.muted=muted;this.accent=accent;}
 public static final List<ThemePalette> PRESETS=Collections.unmodifiableList(Arrays.asList(
  new ThemePalette("amoled","AMOLED Green","AMOLED",0xff000000,0xff101110,0xff343735,0xfff5f5f5,0xffb9c0bd,0xff1ed760),
  new ThemePalette("ocean","Midnight Ocean","Cool",0xff06111f,0xff102438,0xff284459,0xfff0f7ff,0xffb1c4d8,0xff6ad8ff),
  new ThemePalette("velvet","Velvet Purple","Cool",0xff140b20,0xff251735,0xff493455,0xfffaf0ff,0xffc5b5d0,0xffd3a0ff),
  new ThemePalette("rose","Rose Gold","Warm",0xff1b1014,0xff2e1c24,0xff53333e,0xfffff1e8,0xffd4b8b0,0xffffb18d),
  new ThemePalette("mint","Arctic Mint","Cool",0xff071815,0xff102c27,0xff29554a,0xffeafff6,0xffa9cec0,0xff61f0bd),
  new ThemePalette("solar","Solar Amber","Warm",0xff1a1108,0xff302315,0xff564329,0xfffff7e8,0xffd6c3a6,0xffffc268),
  new ThemePalette("obsidian","Obsidian","AMOLED",0xff000000,0xff131218,0xff38343f,0xfffaf6ff,0xffc5becd,0xffd6c0ff),
  new ThemePalette("graphite","Graphite","AMOLED",0xff000000,0xff14181b,0xff343e46,0xfff1f7ff,0xffb8c7d3,0xffb6d8ef),
  new ThemePalette("frost","Glacier Blue","Cool",0xff09151b,0xff142b36,0xff345160,0xfff0fbff,0xffb8d0dd,0xff9fe5ff),
  new ThemePalette("lavender","Lavender Haze","Cool",0xff151220,0xff29243c,0xff4b4462,0xfff9f4ff,0xffcbc1dc,0xffc9b7ff),
  new ThemePalette("cherry","Cherry Blossom","Warm",0xff200e17,0xff361c2a,0xff59354b,0xfffff3f9,0xffdcb8c9,0xffffa1cb),
  new ThemePalette("copper","Copper Glow","Warm",0xff1d120e,0xff33241c,0xff584034,0xfffff5ed,0xffd5bfae,0xffffb889),
  new ThemePalette("aurora","Aurora","Neon",0xff07191b,0xff102e31,0xff2d5156,0xffefffff,0xffacd1d0,0xff67ffe0),
  new ThemePalette("cyber","Cyber Pink","Neon",0xff1a0a1d,0xff301734,0xff543359,0xfffff1ff,0xffd5b8d9,0xffff8de1),
  new ThemePalette("electric","Electric Lime","Neon",0xff111907,0xff233114,0xff40542b,0xfff7ffe9,0xffc0d1a9,0xffcbff63),
  new ThemePalette("sakura","Sakura Night","Warm",0xff1d111b,0xff32212f,0xff553b4e,0xfffff4fb,0xffd4becd,0xffffb3db),
  new ThemePalette("indigo","Indigo Dreams","Cool",0xff0e1024,0xff1e2440,0xff3a4566,0xfff3f4ff,0xffbec6e0,0xffaebdff),
  new ThemePalette("forest","Forest Drive","Cool",0xff0b1710,0xff193023,0xff34533d,0xfff0fff3,0xffb3d0bb,0xff8ce5ad),
  new ThemePalette("emerald","Emerald City","Neon",0xff061b14,0xff103125,0xff295443,0xffecfff5,0xffadd4c2,0xff57ffad),
  new ThemePalette("sand","Desert Sand","Warm",0xff1a160f,0xff30291c,0xff534934,0xfffffaec,0xffd5cbb4,0xffe8d28f),
  new ThemePalette("ruby","Ruby Noir","Warm",0xff1d0c12,0xff341922,0xff56303e,0xfffff1f5,0xffd8b6c1,0xffff98b2),
  new ThemePalette("cobalt","Cobalt Sky","Cool",0xff091323,0xff162941,0xff304866,0xfff0f6ff,0xffb7c9e0,0xff89c5ff),
  new ThemePalette("ultraviolet","Ultraviolet","Neon",0xff160a24,0xff2a1640,0xff4b3065,0xfffcf1ff,0xffceb8df,0xffdc9eff),
  new ThemePalette("mono","Silver Screen","AMOLED",0xff000000,0xff161616,0xff383838,0xfffafafa,0xffc2c2c2,0xffededed)));
 public static ThemePalette find(String id){for(ThemePalette p:PRESETS)if(p.id.equals(id))return p;return PRESETS.get(0);}
 public boolean matches(String query,String filter){return ("All".equals(filter)||category.equals(filter))&&name.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));}
 public ThemePalette black(){return new ThemePalette(id,name,category,0xff000000,surface,line,text,muted,accent);}
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
