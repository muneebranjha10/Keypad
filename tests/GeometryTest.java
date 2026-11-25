package io.rootvault.glassdialer;
public final class GeometryTest {
 static int count;
 static void check(boolean c,String s){count++;if(!c)throw new AssertionError(s);}
 static void close(float a,float b){check(Math.abs(a-b)<.001f,a+" != "+b);}
 public static void main(String[] args){
  close(Geometry.radius(120,100,1),36);close(Geometry.radius(100,60,1),25.5f);close(Geometry.radius(0,100,1),0);
  close(Geometry.radius(300,200,3),85);close(Geometry.radius(60,100,1),25.5f);
  close(Geometry.dragLeft(-20,20,0,300,100),0);close(Geometry.dragLeft(350,20,0,300,100),200);close(Geometry.dragLeft(150,20,0,300,100),130);
  close(Geometry.dragLeft(20,5,10,90,100),10);
  check(Geometry.nearest(10,new float[]{50,150,250})==0,"left");check(Geometry.nearest(160,new float[]{50,150,250})==1,"middle");check(Geometry.nearest(290,new float[]{50,150,250})==2,"right");
  check(Geometry.nearest(10,new float[]{250,150,50})==2,"RTL");check(Geometry.nearest(10,new float[]{})==-1,"empty");
  close(Geometry.projection(150,100000,1),182);close(Geometry.projection(150,-100000,1),118);
  close(Geometry.spring(0),0);close(Geometry.spring(1),1);
  for(int i=0;i<=100;i++){float t=Geometry.spring(i/100f);check(Float.isFinite(t)&&t>=0&&t<=1.15f,"spring bounds");}
  for(int i=1;i<=100;i++){float[] stops=Geometry.haloStops(i/100f);for(int j=1;j<stops.length;j++)check(stops[j]>stops[j-1],"gradient ordering");}
  check(Geometry.appended("123",2,0,1),"append");check(!Geometry.appended("",-1,0,1),"empty");check(!Geometry.appended("123",1,0,1),"middle edit");check(!Geometry.appended("12 ",2,0,1),"formatter");
  check(Geometry.matchesAppend("123",2,'3'),"current digit");check(!Geometry.matchesAppend("124",2,'3'),"stale digit");check(!Geometry.matchesAppend("12",2,'3'),"deleted digit");
  close(Geometry.fitLeft(-20,100,0,300),0);close(Geometry.fitLeft(350,100,0,300),200);close(Geometry.fitLeft(150,100,0,300),100);
  close(Geometry.centerShift(10,70,100,20),10);close(Geometry.centerShift(0,120,100,20),0);close(Geometry.centerShift(10,90,100,20),0);
  for(int w=8;w<=240;w+=4)for(int h=8;h<=180;h+=4){float a=Math.min(w,h),r=Geometry.safeRadius(w,h,2);check(r>=0&&r<=a/2.04f,"safe radius");}
  for(int i=-20;i<=100;i++){float sc=Geometry.recoilScale(i/100f);check(sc>=.924f&&sc<=1.016f,"bounded recoil");}
  check(Geometry.hit(20,20,40,40,0),"delete inside");check(!Geometry.hit(-1,20,40,40,0),"delete outside");check(Geometry.hit(-1,20,40,40,4),"touch slop");check(!Geometry.hit(Float.NaN,20,40,40,4),"invalid pointer");check(!Geometry.hit(0,0,0,0,4),"unlaid out target");
  for(int w=10;w<=500;w+=10)for(int x=-50;x<=550;x+=10){float fitted=Geometry.fitLeft(x,w,0,500);check(fitted>=0&&fitted+w<=500.001f,"capsule bounds");}
  System.out.println("Geometry checks passed: "+count);
 }
}
