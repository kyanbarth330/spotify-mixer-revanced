package org.example.spotifymixer.runtime;
import org.junit.Test;import static org.junit.Assert.*;import java.io.*;import java.nio.charset.StandardCharsets;
public class PlaylistWireTest {
    private byte[] text(int f,String value){return PlaylistWire.bytes(f,value.getBytes(StandardCharsets.UTF_8));}
    @Test public void requestEncodesUriRangeAndMetadataPolicy()throws Exception{
        PlaylistWire.Message msg=new PlaylistWire.Message(PlaylistWire.request("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M",100,100));assertEquals(100,msg.message(2).message(4).integer(1,-1));assertEquals(100,msg.message(2).message(4).integer(2,-1));assertEquals(1,msg.message(3).message(4).integer(1,0));assertEquals(1,msg.message(3).message(1).integer(3,0));assertEquals(1,msg.message(3).message(1).integer(12,0));
    }
    @Test public void parsesTitleTracksCountsAndSkipsUnsupportedRows()throws Exception{
        byte[] data=PlaylistWire.join(PlaylistWire.bytes(1,text(18,"spotify:track:0000000000000000000001")),PlaylistWire.bytes(1,text(18,"spotify:episode:0000000000000000000002")),PlaylistWire.number(4,2),PlaylistWire.bytes(2,PlaylistWire.bytes(1,text(2,"Real title"))));
        PlaylistWire.Page p=PlaylistWire.response(PlaylistWire.join(PlaylistWire.bytes(1,PlaylistWire.number(1,200)),PlaylistWire.bytes(2,data)));assertEquals("Real title",p.title);assertEquals(2,p.rows);assertEquals(2,p.total);assertEquals(1,p.tracks.size());
    }
    @Test public void parsesNativeCoverWithoutNeedingTracks()throws Exception{byte[] metadata=PlaylistWire.join(text(2,"Real playlist"),PlaylistWire.bytes(8,text(1,"spotify:image:0123456789012345678901234567890123456789")));byte[] data=PlaylistWire.bytes(2,PlaylistWire.bytes(1,metadata));PlaylistWire.Page page=PlaylistWire.response(PlaylistWire.join(PlaylistWire.bytes(1,PlaylistWire.number(1,200)),PlaylistWire.bytes(2,data)));assertEquals("Real playlist",page.title);assertEquals("spotify:image:0123456789012345678901234567890123456789",page.cover);assertTrue(page.tracks.isEmpty());}
    @Test(expected=IOException.class)public void rejectsServiceError()throws Exception{PlaylistWire.response(PlaylistWire.bytes(1,PlaylistWire.number(1,403)));}
    @Test(expected=IOException.class)public void rejectsTruncatedProtobuf()throws Exception{PlaylistWire.response(new byte[]{10,100,1});}
    @Test(expected=IOException.class)public void rejectsMissingStatus()throws Exception{PlaylistWire.response(new byte[0]);}
}
