package io.rootvault.glassdialer;

import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.animation.PathInterpolator;

final class Visuals {
 static final PathInterpolator EASE = new PathInterpolator(.2f,.8f,.2f,1f);
 static float clamp(float x,float a,float b){return Math.max(a,Math.min(b,x));}
 static int color(int rgb,float opacity){return (Math.round(clamp(opacity,0,1)*255)<<24)|(rgb&0xffffff);}
 static abstract class Skin extends Drawable {
  final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final RectF r=new RectF(); final float dp; boolean dark;
  Skin(float dp,boolean dark){this.dp=dp;this.dark=dark;}
  void dark(boolean d){if(dark!=d){dark=d;invalidateSelf();}}
  @Override public void setAlpha(int a){} @Override public void setColorFilter(ColorFilter f){} @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
  void fill(Canvas c,RectF r,float radius,int col){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(col);c.drawRoundRect(r,radius,radius,p);}
  void shade(Canvas c,RectF r,float radius,Shader s){p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setShader(s);c.drawRoundRect(r,radius,radius,p);p.setShader(null);}
  void stroke(Canvas c,RectF r,float radius,Shader s,float width){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setColor(Color.WHITE);p.setShader(s);c.drawRoundRect(r,radius,radius,p);p.setShader(null);p.setStyle(Paint.Style.FILL);}
 }
 static final class Key extends Skin {
  float glow=0,recoil=0; ValueAnimator anim,motion; boolean pressed,erase;
  Runnable frame;
  final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Bitmap halo; int haloSize; float haloRadius,commonRadius;
  void radius(float r){if(Math.abs(commonRadius-r)>.25f){commonRadius=r;halo=null;invalidateSelf();}}
  Key(float dp,boolean dark){super(dp,dark);}
  void erase(boolean e){if(erase!=e){erase=e;halo=null;invalidateSelf();}}
  void change(){invalidateSelf();if(frame!=null)frame.run();}
  void press(boolean down){if(pressed==down)return;pressed=down;
   if(anim!=null)anim.cancel();if(motion!=null)motion.cancel();
   float start=glow;
   // Keep very short taps visible, even if released before the next display frame.
   if(down){anim=ValueAnimator.ofFloat(start,1f);anim.setDuration(100);}
   else if(start<.35f){anim=ValueAnimator.ofFloat(Math.max(start,.25f),.75f,0f);anim.setDuration(350);}
   else {anim=ValueAnimator.ofFloat(start,0f);anim.setDuration(350);}
   anim.setInterpolator(down?new PathInterpolator(.22f,0f,.25f,1f):new PathInterpolator(.18f,.55f,.24f,1f));
   anim.addUpdateListener(a->{glow=(float)a.getAnimatedValue();change();});anim.start();
   motion=ValueAnimator.ofFloat(recoil,down?1f:0f);motion.setDuration(down?100:350);
   motion.setInterpolator(down?new PathInterpolator(.25f,0f,.25f,1f):new PathInterpolator(.18f,.86f,.24f,1.24f));
   motion.addUpdateListener(a->{recoil=(float)a.getAnimatedValue();change();});motion.start();
  }
  void stop(){if(anim!=null)anim.cancel();if(motion!=null)motion.cancel();anim=motion=null;pressed=false;glow=recoil=0;halo=null;change();}
  void prepareHalo(int size,float radius){
   if(halo!=null&&haloSize==size&&Math.abs(haloRadius-radius)<.5f)return;
   haloSize=size;haloRadius=radius;halo=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
   Canvas c=new Canvas(halo);float center=size*.5f;
   int first=erase?0xfffce0ff:0xffc7f9b3,end=erase?0xffffc8c8:0xff99ded2;
   Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);
   q.setShader(new LinearGradient(0,0,size,size,new int[]{first,end},null,Shader.TileMode.CLAMP));
   c.drawCircle(center,center,center,q);
   float edge=Math.min(.91f,radius/center);
   q.setShader(new RadialGradient(center,center,center,new int[]{0x08000000,0x14000000,0x35000000,0x79000000,0x43000000,0x00000000},
    Geometry.haloStops(edge),Shader.TileMode.CLAMP));
   q.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));c.drawCircle(center,center,center,q);
  }
  @Override public void draw(Canvas c){
   Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY(),available=Math.min(b.width(),b.height());
   if(available<8)return;
   float radius=Math.min(Geometry.radius(b.width(),b.height(),dp),commonRadius>0?commonRadius:36*dp);
   int base=dark?0xff171717:0xfffcfcff;
   p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(base);c.drawCircle(x,y,radius,p);
   if(glow>.002f){
    int a=erase?0xff493538:0xff344334,mid=erase?0xff34272d:0xff2b342b,end=erase?0xff2d2528:0xff253332;
    p.setColor(Color.WHITE);p.setShader(new LinearGradient(x-radius,y-radius,x+radius,y+radius,new int[]{a,mid,end},null,Shader.TileMode.CLAMP));p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,radius,p);p.setShader(null);
    p.setColor(Color.WHITE);p.setShader(new RadialGradient(x,y,radius,new int[]{0x00000000,0x00000000,0x31000000,0x61000000},new float[]{0,.58f,.90f,1},Shader.TileMode.CLAMP));p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,radius,p);p.setShader(null);
   }
   if(glow>.002f){
    int size=Math.max(8,(int)Math.min(available,2*(radius+8*dp)));
    prepareHalo(size,radius);
    bitmapPaint.setAlpha(Math.round(255*clamp(glow,0,1)));
    c.drawBitmap(halo,x-size*.5f,y-size*.5f,bitmapPaint);
   }
   p.setAlpha(255);
  }
 }
 static final class Tabs extends Skin {
  float left=Float.NaN,right=Float.NaN,targetL,targetR,lift=0,stretch=0; ValueAnimator anim;
  final RectF t=new RectF(),inner=new RectF();
  Tabs(float dp,boolean dark){super(dp,dark);}
  void move(float l,float r,boolean animate){
   if(!Float.isFinite(l)||!Float.isFinite(r)||r<=l)return;
   if(Float.isFinite(left)&&Math.abs(targetL-l)<.1f&&Math.abs(targetR-r)<.1f&&lift==0){if(animate&&anim!=null&&anim.isRunning())return;if(Math.abs(left-l)<.1f&&Math.abs(right-r)<.1f&&Math.abs(stretch)<.01f)return;}
   targetL=l;targetR=r;if(anim!=null)anim.cancel();
   if(!animate||!Float.isFinite(left)){left=l;right=r;stretch=0;invalidateSelf();return;}
   float a=left,b=right;anim=ValueAnimator.ofFloat(0,1);anim.setDuration(390);
   anim.setInterpolator(new android.view.animation.LinearInterpolator());
   anim.addUpdateListener(v->{float f=(float)v.getAnimatedValue();float t=Geometry.spring(f);left=a+(l-a)*t;right=b+(r-b)*t;stretch=(float)Math.sin(Math.PI*f)*Math.min(2.5f*dp,Math.abs(l-a)*.045f);invalidateSelf();});anim.start();
  }
  void drag(float l,float r){if(anim!=null)anim.cancel();left=l;right=r;targetL=l;targetR=r;lift=1;stretch=1.3f*dp;invalidateSelf();}
  void stop(){if(anim!=null)anim.cancel();lift=stretch=0;left=right=Float.NaN;invalidateSelf();}
  @Override public void draw(Canvas c){
   Rect b=getBounds();if(b.width()<4||b.height()<4)return;
   float inset=.75f*dp;r.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);float rad=r.height()/2;
   shade(c,r,rad,new LinearGradient(0,r.top,0,r.bottom,dark?new int[]{0xff1d1d1d,0xff171717,0xff1b1b1b}:new int[]{0xfffafbfc,0xffe4e7ec,0xfff1f3f6},new float[]{0,.53f,1},Shader.TileMode.CLAMP));
   stroke(c,r,rad,new LinearGradient(r.left,r.top,r.right,r.bottom,dark?new int[]{0x21ffffff,0x08ffffff,0x17ffffff}:new int[]{0xffffffff,0x40828b96,0xaaffffff},new float[]{0,.50f,1},Shader.TileMode.CLAMP),.85f*dp);
   if(!Float.isFinite(left)||right<=left)return;
   float pad=3*dp;t.set(Math.max(r.left+pad,b.left+left+pad-stretch),r.top+pad-(lift*.45f*dp),Math.min(r.right-pad,b.left+right-pad+stretch),r.bottom-pad+(lift*.45f*dp));if(t.width()<2||t.height()<2)return;float tr=Math.min(t.width(),t.height())*.5f;
   shade(c,t,tr,new LinearGradient(0,t.top,0,t.bottom,dark?new int[]{0xff494949,0xff353535,0xff303030,0xff363636}:new int[]{0xfffefefe,0xffe0e4e8,0xffd0d5dc,0xfff7f8fa},new float[]{0,.36f,.72f,1},Shader.TileMode.CLAMP));
   shade(c,t,tr,new RadialGradient(t.left+t.width()*.18f,t.top,t.width()*.75f,new int[]{0x22ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));
   stroke(c,t,tr,new LinearGradient(t.left,t.top,t.right,t.bottom,new int[]{0x58ffffff,0x12ffffff,0x24ffffff},new float[]{0,.51f,1},Shader.TileMode.CLAMP),1.1f*dp);
   inner.set(t);inner.inset(1.6f*dp,1.6f*dp);
   if(inner.width()>0&&inner.height()>0)stroke(c,inner,Math.min(inner.width(),inner.height())/2,new LinearGradient(0,inner.top,0,inner.bottom,new int[]{0x12ffffff,0x00ffffff,0x09ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP),.6f*dp);
  }
 }
}