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
  System.out.println("Geometry checks passed: "+count);
 }
}
