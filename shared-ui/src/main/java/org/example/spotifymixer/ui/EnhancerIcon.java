package org.example.spotifymixer.ui;
import android.graphics.*;import android.graphics.drawable.Drawable;
/** Original line icons, independent of Android/Spotify resources. */
public final class EnhancerIcon extends Drawable {
    private final String kind;private final Paint p=new Paint(3);private final int color;
    public EnhancerIcon(String kind,int color){this.kind=kind;this.color=color;}
    public void draw(Canvas canvas){canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(kind.equals("tools")){Path path=new Path();path.moveTo(20,4);path.lineTo(17,7);path.lineTo(14,7);path.lineTo(14,4);path.lineTo(17,1);path.cubicTo(9,-1,8,6,10,9);path.lineTo(3,16);path.cubicTo(-1,20,4,24,7,20);path.lineTo(14,13);path.cubicTo(19,15,24,10,20,4);canvas.drawPath(path,p);canvas.drawCircle(5,18,0.7f,p);}
        else if(kind.equals("history")){canvas.drawArc(4,4,22,22,-130,300,false,p);canvas.drawLine(4,3,4,9,p);canvas.drawLine(4,9,10,9,p);canvas.drawLine(13,8,13,13,p);canvas.drawLine(13,13,17,15,p);}
        else if(kind.equals("presets")){for(int y=5;y<=19;y+=7){canvas.drawLine(2,y,22,y,p);p.setStyle(Paint.Style.FILL);canvas.drawCircle(y==12?16:8,y,2.8f,p);p.setStyle(Paint.Style.STROKE);}}
        else if(kind.equals("mix")){canvas.drawLine(3,5,7,5,p);canvas.drawLine(7,5,17,19,p);canvas.drawLine(17,19,22,19,p);canvas.drawLine(19,16,22,19,p);canvas.drawLine(22,19,19,22,p);canvas.drawLine(3,19,7,19,p);canvas.drawLine(7,19,17,5,p);canvas.drawLine(17,5,22,5,p);canvas.drawLine(19,2,22,5,p);canvas.drawLine(22,5,19,8,p);}
        else if(kind.equals("back")){canvas.drawLine(15,5,8,12,p);canvas.drawLine(8,12,15,19,p);}
        else if(kind.equals("close")){canvas.drawLine(6,6,18,18,p);canvas.drawLine(18,6,6,18,p);}
        else if(kind.equals("chevron")){canvas.drawLine(9,6,15,12,p);canvas.drawLine(15,12,9,18,p);}
        else if(kind.equals("plus")){canvas.drawLine(12,4,12,20,p);canvas.drawLine(4,12,20,12,p);}
        else{canvas.drawCircle(10,10,6,p);canvas.drawLine(15,15,21,21,p);}canvas.restore();}
    public void setAlpha(int alpha){p.setAlpha(alpha);}public void setColorFilter(ColorFilter filter){p.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
