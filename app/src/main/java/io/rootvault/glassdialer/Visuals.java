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
  Key(float dp,boolean dark){super(dp,dark);}
  void press(boolean down){if(pressed==down)return;pressed=down; if(anim!=null)anim.cancel(); anim=ValueAnimator.ofFloat(glow,down?1f:0f);anim.setDuration(down?105:310);anim.setInterpolator(EASE);anim.addUpdateListener(a->{glow=(float)a.getAnimatedValue();invalidateSelf();});anim.start();}
  void stop(){if(anim!=null)anim.cancel();glow=0;pressed=false;invalidateSelf();}
  @Override public void draw(Canvas c){Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY();float radius=Math.max(0,Math.min(Math.min(b.width(),b.height())*.455f,37*dp));if(radius<1)return;
   int base=dark?0xff242527:0xffe8e9ea; float rr=radius*(1-.045f*glow);
   p.setStyle(Paint.Style.FILL);p.setColor(base);c.drawCircle(x,y,rr,p);
   if(glow>.003f){float halo=rr+Math.min(5*dp,(Math.min(b.width(),b.height())*.5f-rr));p.setColor(Color.WHITE);p.setShader(new RadialGradient(x,y,Math.max(1,halo),new int[]{color(0x51c8a6,.09f*glow),color(0x71e9ba,.12f*glow),color(0xa0f6d5,.55f*glow),color(0x60c9ac,0)},new float[]{0,.67f,.86f,1},Shader.TileMode.CLAMP));c.drawCircle(x,y,halo,p);p.setShader(null);
    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f*dp);p.setColor(color(0x99eed1,.48f*glow));c.drawCircle(x,y,rr-.5f*dp,p);p.setStyle(Paint.Style.FILL);}
  }
 }
 static final class Tabs extends Skin {
  float left=Float.NaN,right=Float.NaN, targetL,targetR, lift=0; ValueAnimator anim;
  Tabs(float dp,boolean dark){super(dp,dark);}
  void move(float l,float r,boolean animate){if(!Float.isFinite(l)||r<=l)return;if(!Float.isNaN(left)&&Math.abs(targetL-l)<.1f&&Math.abs(targetR-r)<.1f&&animate)return;
   targetL=l;targetR=r;if(anim!=null)anim.cancel();if(!animate||!Float.isFinite(left)){left=l;right=r;invalidateSelf();return;}
   float a=left,b=right;anim=ValueAnimator.ofFloat(0,1);anim.setDuration(340);anim.setInterpolator(new PathInterpolator(.22f,.9f,.25f,1f));anim.addUpdateListener(v->{float t=(float)v.getAnimatedValue();left=a+(l-a)*t;right=b+(r-b)*t;invalidateSelf();});anim.start();}
  void drag(float l,float r){if(anim!=null)anim.cancel();left=l;right=r;targetL=l;targetR=r;lift=1;invalidateSelf();}
  void stop(){if(anim!=null)anim.cancel();lift=0;left=right=Float.NaN;}
  @Override public void draw(Canvas c){Rect b=getBounds();if(b.width()<4||b.height()<4)return;float inset=dp; r.set(b.left+inset,b.top+inset,b.right-inset,b.bottom-inset);float rad=r.height()/2;
   shade(c,r,rad,new LinearGradient(0,r.top,0,r.bottom,dark?new int[]{0x454e5960,0x27323940,0x39434e53}:new int[]{0xbaffffff,0x70e6eaee,0x9cf8ffff},null,Shader.TileMode.CLAMP));
   stroke(c,r,rad,new LinearGradient(r.left,r.top,r.right,r.bottom,new int[]{dark?0x72ffffff:0xcaffffff,0x12ffffff,dark?0x4dbbddd4:0x6f8eafaa},new float[]{0,.53f,1},Shader.TileMode.CLAMP),.85f*dp);
   if(!Float.isFinite(left)||right<=left)return;
   float pad=3*dp;RectF t=new RectF(Math.max(r.left+pad,b.left+left+pad),r.top+pad,Math.min(r.right-pad,b.left+right-pad),r.bottom-pad);if(t.width()<2||t.height()<2)return;float tr=Math.min(t.width(),t.height())*.5f;
   shade(c,t,tr,new LinearGradient(0,t.top,0,t.bottom,dark?new int[]{0x8b657479,0x63515b5f,0x91434d50}:new int[]{0xf2ffffff,0xccd8e1e3,0xdaf7fcfc},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
   shade(c,t,tr,new RadialGradient(t.left+t.width()*.23f,t.top,t.width()*.85f,new int[]{dark?0x4fffffff:0x9cffffff,0x00ffffff},null,Shader.TileMode.CLAMP));
   stroke(c,t,tr,new LinearGradient(t.left,t.top,t.right,t.bottom,new int[]{0xcaffffff,0x16ffffff,0x9cc3ece1},new float[]{0,.56f,1},Shader.TileMode.CLAMP),(lift>0?1.3f:.85f)*dp);
  }
 }
}