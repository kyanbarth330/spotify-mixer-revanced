package org.example.spotifymixer.ui;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Original abstract placeholder art; never presented as a fetched playlist cover. */
public final class EnhancerArtwork extends Drawable {
    private final Paint paint=new Paint(3);private final int seed;
    public EnhancerArtwork(int seed){this.seed=seed;}
    public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/40f,getBounds().height()/40f);int[][] palettes={{0xffba594e,0xff482f31},{0xff4a9fbd,0xff414676},{0xffa0a565,0xff3e6867},{0xffa776c0,0xff3e435e}};int[] colors=palettes[Math.floorMod(seed,4)];paint.setShader(new LinearGradient(0,0,40,40,colors,null,Shader.TileMode.CLAMP));c.drawRect(0,0,40,40,paint);paint.setShader(null);paint.setColor(0x88522b31);c.drawCircle(25,12,9,paint);paint.setColor(0xaa152b30);Path hills=new Path();hills.moveTo(0,32);hills.lineTo(12,15);hills.lineTo(28,30);hills.lineTo(40,22);hills.lineTo(40,40);hills.lineTo(0,40);hills.close();c.drawPath(hills,paint);c.restore();}
    public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return PixelFormat.OPAQUE;}
}
