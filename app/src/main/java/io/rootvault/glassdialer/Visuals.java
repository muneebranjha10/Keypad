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
  float glow=0; ValueAnimator anim; boolean pressed;
  Runnable frame;
  Key(float dp,boolean dark){super(dp,dark);}
  void change(float amount){glow=amount;invalidateSelf();if(frame!=null)frame.run();}
  void press(boolean down){if(pressed==down)return;pressed=down;if(anim!=null)anim.cancel();
   float start=glow;
   if(down){anim=ValueAnimator.ofFloat(start,1f);anim.setDuration(85);}
   else if(start<.50f){anim=ValueAnimator.ofFloat(start,.58f,0f);anim.setDuration(340);}
   else {anim=ValueAnimator.ofFloat(start,0f);anim.setDuration(310);}
   anim.setInterpolator(EASE);anim.addUpdateListener(a->change((float)a.getAnimatedValue()));anim.start();
  }
  void stop(){if(anim!=null)anim.cancel();pressed=false;change(0);}
  @Override public void draw(Canvas c){
   Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY();
   float radius=Math.max(0,Math.min(Math.min(b.width(),b.height())*.435f,37*dp));if(radius<1)return;
   float extent=Math.min(Math.min(b.width(),b.height())*.5f,radius+5*dp);
   int base=dark?0xff141414:0xffefefef;
   float rr=radius*(1-.025f*glow);
   if(glow>.003f){
    p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);
    p.setShader(new RadialGradient(x,y,extent,new int[]{0x005d8657,0x005d8657,0x745d8657,0x245d8657,0x005d8657},new float[]{0,.60f,.81f,.92f,1},Shader.TileMode.CLAMP));
    p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,extent,p);p.setShader(null);
    p.setColor(Color.WHITE);
    p.setShader(new RadialGradient(x+rr*.16f,y+rr*.12f,extent,new int[]{0x004a9e97,0x004a9e97,0x624a9e97,0x234a9e97,0x004a9e97},new float[]{0,.56f,.77f,.90f,1},Shader.TileMode.CLAMP));
    p.setAlpha(Math.round(200*glow));c.save();c.clipRect(b);c.drawCircle(x,y,extent,p);c.restore();p.setShader(null);
   }
   p.setColor(base);c.drawCircle(x,y,rr,p);
   if(glow>.003f){
    p.setColor(Color.WHITE);p.setShader(new LinearGradient(x-rr,y-rr,x+rr,y+rr,
     dark?new int[]{0xff374337,0xff283630,0xff202929}:new int[]{0xffdce9d5,0xffe0eee5,0xffd7e7e6},null,Shader.TileMode.CLAMP));
    p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,rr,p);p.setShader(null);
    p.setColor(Color.WHITE);p.setShader(new RadialGradient(x,y,rr,new int[]{0x00000000,0x00000000,0x25000000,0x58000000},new float[]{0,.60f,.89f,1},Shader.TileMode.CLAMP));
    p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,rr,p);p.setShader(null);
   }
   p.setAlpha(255);
  }
 }
 static final class Tabs extends Skin {
  float left=Float.NaN,right=Float.NaN,targetL,targetR,lift=0,stretch=0; ValueAnimator anim;
  final RectF t=new RectF(),inner=new RectF();
  Tabs(float dp,boolean dark){super(dp,dark);}
  void move(float l,float r,boolean animate){
   if(!Float.isFinite(l)||r<=l)return;
   if(Float.isFinite(left)&&Math.abs(targetL-l)<.1f&&Math.abs(targetR-r)<.1f&&lift==0&&Math.abs(stretch)<.01f&&(!animate||anim!=null&&anim.isRunning()||Math.abs(left-l)<.1f)){invalidateSelf();return;}
   targetL=l;targetR=r;if(anim!=null)anim.cancel();
   if(!animate||!Float.isFinite(left)){left=l;right=r;stretch=0;invalidateSelf();return;}
   float a=left,b=right;anim=ValueAnimator.ofFloat(0,1);anim.setDuration(365);
   anim.setInterpolator(new PathInterpolator(.19f,.76f,.23f,1f));
   anim.addUpdateListener(v->{float f=(float)v.getAnimatedValue();left=a+(l-a)*f;right=b+(r-b)*f;stretch=(float)Math.sin(Math.PI*f)*Math.min(3*dp,Math.abs(l-a)*.06f);invalidateSelf();});anim.start();
  }
  void drag(float l,float r){if(anim!=null)anim.cancel();left=l;right=r;targetL=l;targetR=r;lift=1;stretch=2*dp;invalidateSelf();}
  void stop(){if(anim!=null)anim.cancel();lift=stretch=0;left=right=Float.NaN;invalidateSelf();}
  @Override public void draw(Canvas c){
   Rect b=getBounds();if(b.width()<4||b.height()<4)return;
   float inset=.75f*dp;r.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);float rad=r.height()/2;
   shade(c,r,rad,new LinearGradient(0,r.top,0,r.bottom,dark?new int[]{0xff303236,0xff1b1c20,0xff25272c}:new int[]{0xfffafbfc,0xffe4e7ec,0xfff1f3f6},new float[]{0,.53f,1},Shader.TileMode.CLAMP));
   stroke(c,r,rad,new LinearGradient(r.left,r.top,r.right,r.bottom,dark?new int[]{0x91ffffff,0x24ffffff,0x55ffffff}:new int[]{0xffffffff,0x40828b96,0xaaffffff},new float[]{0,.50f,1},Shader.TileMode.CLAMP),.85f*dp);
   if(!Float.isFinite(left)||right<=left)return;
   float pad=3*dp;t.set(Math.max(r.left+pad,b.left+left+pad-stretch),r.top+pad-(lift*.45f*dp),Math.min(r.right-pad,b.left+right-pad+stretch),r.bottom-pad+(lift*.45f*dp));if(t.width()<2||t.height()<2)return;float tr=Math.min(t.width(),t.height())*.5f;
   shade(c,t,tr,new LinearGradient(0,t.top,0,t.bottom,dark?new int[]{0xff73777f,0xff444850,0xff35383e,0xff656a72}:new int[]{0xfffefefe,0xffe0e4e8,0xffd0d5dc,0xfff7f8fa},new float[]{0,.36f,.72f,1},Shader.TileMode.CLAMP));
   shade(c,t,tr,new RadialGradient(t.left+t.width()*.18f,t.top,t.width()*.75f,new int[]{0x58ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));
   stroke(c,t,tr,new LinearGradient(t.left,t.top,t.right,t.bottom,new int[]{0xecffffff,0x36ffffff,0x94ffffff},new float[]{0,.51f,1},Shader.TileMode.CLAMP),1.1f*dp);
   inner.set(t);inner.inset(1.6f*dp,1.6f*dp);
   if(inner.width()>0&&inner.height()>0)stroke(c,inner,Math.min(inner.width(),inner.height())/2,new LinearGradient(0,inner.top,0,inner.bottom,new int[]{0x4cffffff,0x00ffffff,0x35ffffff},new float[]{0,.5f,1},Shader.TileMode.CLAMP),.6f*dp);
  }
 }
}