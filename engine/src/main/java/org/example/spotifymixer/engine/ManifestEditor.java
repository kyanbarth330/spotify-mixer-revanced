package org.example.spotifymixer.engine;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Append one private original Activity; preserve every original string index/node. */
final class ManifestEditor {
    static final String ACTIVITY="org.example.spotifymixer.runtime.SpotEnhancerActivity";
    static byte[] addActivity(byte[] xml) throws IOException {
        ByteBuffer b=ByteBuffer.wrap(xml).order(ByteOrder.LITTLE_ENDIAN);
        if(b.getShort(0)!=3 || b.getInt(4)!=xml.length) throw new IOException("Invalid AXML");
        List<String> strings=null;byte[] pool=null;int poolStart=-1,poolEnd=-1,insert=-1;
        for(int p=8;p<xml.length;) {
            int type=b.getShort(p)&65535,size=b.getInt(p+4);
            if(size<8 || p+size>xml.length)throw new IOException("Invalid AXML chunk");
            if(type==1) {
                if(b.getInt(p+12)!=0)throw new IOException("Styled manifest string pool unsupported");
                poolStart=p;poolEnd=p+size;strings=new ArrayList<>();int count=b.getInt(p+8),start=b.getInt(p+20),head=b.getShort(p+2)&65535;
                boolean utf8=(b.getInt(p+16)&256)!=0;
                for(int i=0;i<count;i++) {
                    int pos=p+start+b.getInt(p+head+4*i);
                    if(utf8){int[] a=len8(xml,pos);a=len8(xml,a[1]);strings.add(new String(xml,a[1],a[0],StandardCharsets.UTF_8));}
                    else {int n=b.getShort(pos)&65535;pos+=2;if((n&32768)!=0){n=((n&32767)<<16)|(b.getShort(pos)&65535);pos+=2;}strings.add(new String(xml,pos,n*2,StandardCharsets.UTF_16LE));}
                }
            } else if(type==0x103 && strings!=null && strings.get(b.getInt(p+20)).equals("application"))insert=p;
            p+=size;
        }
        if(strings==null||insert<0||strings.contains(ACTIVITY))throw new IOException("Unexpected manifest structure");
        int activity=index(strings,"activity"),name=index(strings,"name"),exported=index(strings,"exported"),ns=index(strings,"http://schemas.android.com/apk/res/android"),value=index(strings,ACTIVITY);
        ByteArrayOutputStream data=new ByteArrayOutputStream();int[] offsets=new int[strings.size()];
        for(int i=0;i<strings.size();i++){offsets[i]=data.size();byte[] utf=strings.get(i).getBytes(StandardCharsets.UTF_8);writeLen(data,strings.get(i).length());writeLen(data,utf.length);data.write(utf);data.write(0);}
        while(data.size()%4!=0)data.write(0);
        ByteBuffer sp=ByteBuffer.allocate(28+4*strings.size()+data.size()).order(ByteOrder.LITTLE_ENDIAN);
        sp.putShort((short)1).putShort((short)28).putInt(sp.capacity()).putInt(strings.size()).putInt(0).putInt(256).putInt(28+4*strings.size()).putInt(0);
        for(int off:offsets)sp.putInt(off);sp.put(data.toByteArray());pool=sp.array();
        ByteArrayOutputStream nodes=new ByteArrayOutputStream();
        start(nodes,activity,new int[][]{{ns,name,value,3,value},{ns,exported,-1,18,0}});end(nodes,activity);
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(xml,0,poolStart);out.write(pool);out.write(xml,poolEnd,insert-poolEnd);out.write(nodes.toByteArray());out.write(xml,insert,xml.length-insert);
        byte[] result=out.toByteArray();ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).putInt(4,result.length);return result;
    }
    private static void start(OutputStream out,int tag,int[][] attrs)throws IOException{
        ByteBuffer b=ByteBuffer.allocate(36+20*attrs.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short)0x102).putShort((short)16).putInt(b.capacity()).putInt(0).putInt(-1).putInt(-1).putInt(tag).putShort((short)20).putShort((short)20).putShort((short)attrs.length).putShort((short)0).putShort((short)0).putShort((short)0);
        for(int[] a:attrs)attr(b,a[0],a[1],a[2],a[3],a[4]);out.write(b.array());
    }
    private static void end(OutputStream out,int tag)throws IOException{ByteBuffer b=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);b.putShort((short)0x103).putShort((short)16).putInt(24).putInt(0).putInt(-1).putInt(-1).putInt(tag);out.write(b.array());}
    private static void attr(ByteBuffer b,int ns,int name,int raw,int type,int value){b.putInt(ns).putInt(name).putInt(raw).putShort((short)8).put((byte)0).put((byte)type).putInt(value);}
    private static int index(List<String> list,String s){int i=list.indexOf(s);if(i<0){i=list.size();list.add(s);}return i;}
    private static int[] len8(byte[] b,int p){int n=b[p++]&255;if((n&128)!=0)n=((n&127)<<8)|(b[p++]&255);return new int[]{n,p};}
    private static void writeLen(OutputStream out,int n)throws IOException{if(n>32767)throw new IOException("String too long");if(n>127)out.write((n>>8)|128);out.write(n&255);}
}
