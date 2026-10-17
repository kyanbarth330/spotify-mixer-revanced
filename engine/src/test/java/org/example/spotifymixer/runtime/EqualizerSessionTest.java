package org.example.spotifymixer.runtime;
import org.junit.Test;
import static org.junit.Assert.*;
public class EqualizerSessionTest {
 static class Device implements EqualizerSession.Device {
  int releases,disable,applied;boolean control=true,fail,enabled;
  int[] gains=new int[3];
  public int bands(){return 3;}public int frequency(int band){return new int[]{60,910,14000}[band];}
  public int minimum(){return -200;}public int maximum(){return 200;}
  public boolean hasControl(){return control;}
  public void level(int band,int gain){if(fail)throw new IllegalStateException("device lost");gains[band]=gain;applied++;}
  public void enabled(boolean value){enabled=value;if(!value)disable++;}
  public void release(){releases++;}
 }
 @Test public void offAndZeroNeverAllocate(){int[] calls={0};EqualizerSession s=new EqualizerSession(id->{calls[0]++;return new Device();});s.opened(9);assertEquals(0,calls[0]);s.closed(9);s.configure(true,hz->0);s.opened(0);s.opened(-1);assertEquals(0,calls[0]);assertEquals("Waiting for local playback",s.status());}
 @Test public void clampsAppliesAndReleasesOnSwitchAndDisable(){java.util.List<Device> devices=new java.util.ArrayList<>();EqualizerSession s=new EqualizerSession(id->{Device d=new Device();devices.add(d);return d;});s.configure(true,hz->hz<100?900:-900);s.opened(2);assertArrayEquals(new int[]{200,-200,-200},devices.get(0).gains);assertTrue(devices.get(0).enabled);s.opened(3);assertEquals(1,devices.get(0).releases);assertEquals(2,devices.size());s.closed(2);assertNotNull(s.device());s.configure(false,hz->0);assertEquals(1,devices.get(1).releases);assertNull(s.device());s.opened(4);assertEquals(2,devices.size());}
 @Test public void failureReleasesAndRetryCanRecover(){Device d=new Device();d.fail=true;EqualizerSession s=new EqualizerSession(id->d);s.configure(true,hz->0);s.opened(5);assertNull(s.device());assertEquals(1,d.releases);assertTrue(s.status().contains("device lost"));d.fail=false;s.retry();assertNotNull(s.device());s.closed(5);assertNull(s.device());assertEquals(2,d.releases);}
 @Test public void controlLossDoesNotDisableAnotherOwnersEffect(){Device d=new Device();EqualizerSession s=new EqualizerSession(id->d);s.configure(true,hz->0);s.opened(5);d.control=false;s.configure(false,hz->0);assertEquals(0,d.disable);assertEquals(1,d.releases);}
 @Test public void curvesUseLogFrequencyAndFlatResets(){assertEquals(500,EqualizerSession.presetLevel(1,60));assertEquals(0,EqualizerSession.presetLevel(0,910));assertEquals(200,EqualizerSession.presetLevel(2,3600));int between=EqualizerSession.presetLevel(1,120);assertTrue(between>300&&between<500);assertEquals(-100,EqualizerSession.presetLevel(1,30000));}
}
