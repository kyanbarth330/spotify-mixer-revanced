package org.example.spotifymixer.runtime;
import java.io.*;import java.nio.charset.StandardCharsets;import java.util.*;
/** Minimal bounded protobuf wire adapter for the SHA-pinned playlist Get schema. No Spotify source. */
public final class PlaylistWire {
    private PlaylistWire(){}
    public static final class Page {
        public final List<String> tracks;public final String title,cover;public final int rows,total;public final boolean loading;
        Page(List<String> tracks,String title,String cover,int rows,int total,boolean loading){this.tracks=tracks;this.title=title;this.cover=cover;this.rows=rows;this.total=total;this.loading=loading;}
    }
    public static byte[] request(String uri,int offset,int length){
        if(uri==null||!uri.matches("spotify:playlist:[A-Za-z0-9]{22}")||offset<0||length<1||length>500)throw new IllegalArgumentException("Invalid playlist page");
        byte[] range=join(number(1,offset),number(2,length));
        byte[] query=join(bytes(4,range),number(8,0),number(10,0));
        byte[] playlist=join(number(3,1),number(5,1),number(7,1),number(12,1),number(49,1));
        byte[] policy=join(bytes(1,playlist),bytes(4,number(1,1)));
        return join(bytes(1,uri.getBytes(StandardCharsets.UTF_8)),bytes(2,query),bytes(3,policy));
    }
    public static Page response(byte[] raw)throws IOException{
        Message outer=new Message(raw);Message status=outer.message(1);int code=status.integer(1,-1);
        if(code<200||code>=300)throw new IOException("Playlist service rejected request ("+code+")");
        Message data=outer.message(2);List<String> tracks=new ArrayList<>();List<byte[]> rows=data.all(1);
        for(byte[] row:rows){String uri=new Message(row).string(18);if(uri.matches("spotify:track:[A-Za-z0-9]{22}"))tracks.add(uri);}
        Message metadata=data.message(2).message(1);String title=metadata.string(2);Message pictures=metadata.message(8);String cover=pictures.string(1);if(cover.isEmpty())cover=pictures.string(2);
        return new Page(Collections.unmodifiableList(tracks),title,cover,rows.size(),data.integer(4,-1),data.integer(6,0)!=0);
    }
    static byte[] join(byte[]... chunks){ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] c:chunks)out.write(c,0,c.length);return out.toByteArray();}
    static byte[] number(int field,int value){ByteArrayOutputStream out=new ByteArrayOutputStream();varint(out,field*8);varint(out,value);return out.toByteArray();}
    static byte[] bytes(int field,byte[] value){ByteArrayOutputStream out=new ByteArrayOutputStream();varint(out,field*8+2);varint(out,value.length);out.write(value,0,value.length);return out.toByteArray();}
    private static void varint(ByteArrayOutputStream out,long v){while((v&~127L)!=0){out.write((int)v&127|128);v>>>=7;}out.write((int)v);}
    static final class Message {
        final Map<Integer,List<byte[]>> blobs=new HashMap<>();final Map<Integer,Long> numbers=new HashMap<>();
        Message(byte[] raw)throws IOException{
            if(raw==null||raw.length>8*1024*1024)throw new IOException("Invalid playlist response size");int[] pos={0};int fields=0;
            while(pos[0]<raw.length){if(++fields>100000)throw new IOException("Too many protobuf fields");long tag=read(raw,pos);if(tag>>>3==0||tag>>>3>536870911)throw new IOException("Invalid protobuf tag");int f=(int)(tag>>>3),wire=(int)tag&7;
                if(wire==0)numbers.put(f,read(raw,pos));
                else if(wire==2){long n=read(raw,pos);if(n<0||n>raw.length-pos[0])throw new IOException("Truncated protobuf field");blobs.computeIfAbsent(f,k->new ArrayList<>()).add(Arrays.copyOfRange(raw,pos[0],pos[0]+(int)n));pos[0]+=(int)n;}
                else if(wire==1||wire==5){int n=wire==1?8:4;if(n>raw.length-pos[0])throw new IOException("Truncated protobuf field");pos[0]+=n;}
                else throw new IOException("Unsupported protobuf wire type");
            }
        }
        List<byte[]> all(int field){return blobs.getOrDefault(field,Collections.emptyList());}
        Message message(int field)throws IOException{List<byte[]> b=all(field);return new Message(b.isEmpty()?new byte[0]:b.get(b.size()-1));}
        int integer(int field,int fallback)throws IOException{Long n=numbers.get(field);if(n==null)return fallback;if(n<0||n>Integer.MAX_VALUE)throw new IOException("Invalid playlist count");return n.intValue();}
        String string(int field){List<byte[]> b=all(field);return b.isEmpty()?"":new String(b.get(b.size()-1),StandardCharsets.UTF_8);}
        private static long read(byte[] raw,int[] pos)throws IOException{long n=0;for(int shift=0;shift<64;shift+=7){if(pos[0]>=raw.length)throw new IOException("Truncated varint");int b=raw[pos[0]++]&255;if(shift==63&&(b&254)!=0)throw new IOException("Varint overflow");n|=(long)(b&127)<<shift;if((b&128)==0)return n;}throw new IOException("Invalid varint");}
    }
}
