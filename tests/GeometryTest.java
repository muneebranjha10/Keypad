package io.rootvault.glassdialer;
public final class GeometryTest {
 static int count;
 static void check(boolean c,String s){count++;if(!c)throw new AssertionError(s);}
 static void close(float a,float b){check(Math.abs(a-b)<.001f,a+" != "+b);}
 public static void main(String[] args){
  close(Geometry.radius(120,100,1),39);close(Geometry.radius(100,60,1),27f);close(Geometry.radius(0,100,1),0);
  close(Geometry.radius(300,200,3),90);close(Geometry.radius(60,100,1),27f);
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
  for(int i=-20;i<=100;i++){float sc=Geometry.recoilScale(i/100f);check(sc>=.869f&&sc<=1.018f,"bounded recoil");}
  check(Geometry.hit(20,20,40,40,0),"delete inside");check(!Geometry.hit(-1,20,40,40,0),"delete outside");check(Geometry.hit(-1,20,40,40,4),"touch slop");check(!Geometry.hit(Float.NaN,20,40,40,4),"invalid pointer");check(!Geometry.hit(0,0,0,0,4),"unlaid out target");
  for(int w=10;w<=500;w+=10)for(int x=-50;x<=550;x+=10){float fitted=Geometry.fitLeft(x,w,0,500);check(fitted>=0&&fitted+w<=500.001f,"capsule bounds");}
  check(Geometry.dialCount("1234 567")==7,"formatted number");check(Geometry.dialCount("+1 (234)")==5,"formatted international");check(Geometry.dialCount("12#*")==4,"dial symbols");check(Geometry.dialCount("")==0,"empty input");check(Geometry.dialCount("١٢٣")==3,"localized digits");
  close(Geometry.recoilScale(1),.87f);close(Geometry.recoilScale(0),1);
  check(Geometry.dialString("9,898").equals("9898"),"normalized number");
  check(Geometry.insertedOrdinal("989","9898")==3,"append");
  check(Geometry.insertedOrdinal("12","132")==1,"middle");
  check(Geometry.insertedOrdinal("12","1")==-1,"delete");
  check(Geometry.insertedOrdinal("12","34")==-1,"replace");
  check(Geometry.dialIndex("9,898",1)==2,"formatted glyph index");
  close(Geometry.entryEase(0),0);close(Geometry.entryEase(1),1);
  float last=0;for(int n=0;n<=100;n++){float q=Geometry.entryEase(n/100f);check(q>=last&&q<=1,"smooth monotonic entry");last=q;}
  close(Geometry.safeRadius(120,100,1),39);check(Geometry.safeRadius(60,60,1)>25,"larger compact key");
  check(Geometry.horizontalDrag(30,5,8),"horizontal drag");check(!Geometry.horizontalDrag(5,30,8),"vertical ignored");check(!Geometry.horizontalDrag(5,0,8),"tap ignored");check(Geometry.horizontalDrag(-30,5,8),"reverse drag");
  close(Geometry.tabOffset(100,80,-200,0,300),-100);close(Geometry.tabOffset(100,80,300,0,300),120);close(Geometry.tabOffset(100,80,30,0,300),30);
  for(int w=10;w<=300;w+=10)for(int dx=-400;dx<=400;dx+=10){float start=(300-w)*.5f;float left=start+Geometry.tabOffset(start,w,dx,0,300);check(left>=-.001f&&left+w<=300.001f,"native indicator bounds");}
  check(Geometry.oneKeyId("one"),"native one id");check(Geometry.oneKeyId("dialpad_key_1_layout"),"alternate one id");check(!Geometry.oneKeyId("two"),"not other keys");
  check(Geometry.oneHintId("dialpad_key_image"),"generic voicemail icon");check(Geometry.oneHintId("dialpad_key_1_letter_text_view"),"alternate voicemail icon");check(Geometry.oneHintId(""),"idless inflated image");check(!Geometry.oneHintId("delete_button_image"),"never hide backspace");check(!Geometry.oneHintId("dialpad_key_number"),"never hide digit");
  close(Geometry.glyphHeight(68),45.56f);close(Geometry.calculatorRecoilScale(1,39,1),1-3f/39);close(Geometry.calculatorRecoilScale(0,39,1),1);
  for(boolean quick:new boolean[]{false,true}){close(Geometry.keyGlow(.1f,0,false,quick),.1f);close(Geometry.keyGlow(.1f,1,false,quick),0);close(Geometry.keyRecoil(.1f,0,false,quick),.1f);close(Geometry.keyRecoil(.1f,1,false,quick),0);for(int z=0;z<=100;z++){float t=z/100f,g=Geometry.keyGlow(.1f,t,false,quick),r=Geometry.keyRecoil(.1f,t,false,quick);check(g>=0&&g<=1&&r>=-.15f&&r<=1,"continuous key animation bounds");}}
  check(Geometry.appendRun("123","1234"),"append raw");check(Geometry.appendRun("","*"),"first symbol");check(!Geometry.appendRun("123","12"),"deletion fallback");check(!Geometry.appendRun("12","132"),"middle fallback");check(!Geometry.appendRun("123","1,234"),"formatting fallback");check(Geometry.simpleDialRun("+1 (23)-4"),"safe run");check(!Geometry.simpleDialRun("1A"),"unsupported run");
  close(Geometry.lerp(50,30,0),50);close(Geometry.lerp(50,30,1),30);close(Geometry.lerp(50,30,.5f),40);
  for(int k=0;k<=100;k++){float t=k/100f;float x=Geometry.lerp(100,80,t);check(x>=80&&x<=100,"reflow bounds");}
  System.out.println("Geometry checks passed: "+count);
 }
}
