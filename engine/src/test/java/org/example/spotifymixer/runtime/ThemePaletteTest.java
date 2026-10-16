package org.example.spotifymixer.runtime;
import org.junit.Test;import static org.junit.Assert.*;
public class ThemePaletteTest {
 @Test public void allPresetsMapSemanticColorsAndKeepAlpha(){for(ThemePalette p:ThemePalette.PRESETS){assertEquals(p.background,p.map(0xff121212));assertEquals(p.accent,p.map(0xff1ed760));assertEquals(0x80000000|(p.accent&0xffffff),p.map(0x801ed760));assertEquals(0x001ed760,p.map(0x001ed760));assertEquals(0xffee3322,p.map(0xffee3322));}}
 @Test public void mappingIsIdempotentAndFallbackStable(){assertEquals("amoled",ThemePalette.find("unknown").id);for(ThemePalette p:ThemePalette.PRESETS)for(int c:new int[]{0xff000000,0xff121212,0xff282828,0xff535353,0xffb3b3b3,0xffffffff,0xff1ed760})assertEquals(p.map(c),p.map(p.map(c)));}
 private double component(int c){double s=c/255.0;return s<=.04045?s/12.92:Math.pow((s+.055)/1.055,2.4);}
 private double luminance(int c){return .2126*component((c>>16)&255)+.7152*component((c>>8)&255)+.0722*component(c&255);}
 private double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 @Test public void readablePresetPairs(){for(ThemePalette p:ThemePalette.PRESETS){assertTrue(p.name+" text",contrast(p.text,p.background)>=7);assertTrue(p.name+" muted",contrast(p.muted,p.surface)>=4.5);assertTrue(p.name+" accent",contrast(p.accent,p.background)>=4.5);assertTrue(p.name+" button",contrast(p.accent,0xff07150c)>=4.5);}}
}
