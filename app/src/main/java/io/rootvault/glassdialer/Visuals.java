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
  float glow=0,recoil=0; ValueAnimator anim,motion; boolean pressed;
  Runnable frame;
  final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Bitmap halo; int haloSize; float haloRadius,commonRadius;
  Shader fillGlow,edgeGlow;float shaderX=Float.NaN,shaderY=Float.NaN,shaderR=Float.NaN;
  void prepareShaders(float x,float y,float radius){if(fillGlow!=null&&shaderX==x&&shaderY==y&&shaderR==radius)return;shaderX=x;shaderY=y;shaderR=radius;fillGlow=new LinearGradient(x-radius,y-radius,x+radius,y+radius,new int[]{0xff344334,0xff2b342b,0xff253332},null,Shader.TileMode.CLAMP);edgeGlow=new RadialGradient(x,y,radius,new int[]{0x00000000,0x00000000,0x31000000,0x61000000},new float[]{0,.58f,.90f,1},Shader.TileMode.CLAMP);}
  void radius(float r){if(Math.abs(commonRadius-r)>.25f){commonRadius=r;halo=null;invalidateSelf();}}
  Key(float dp,boolean dark){super(dp,dark);}
  void change(){invalidateSelf();if(frame!=null)frame.run();}
  void press(boolean down){if(pressed==down)return;pressed=down;
   if(anim!=null){anim.removeAllUpdateListeners();anim.cancel();anim=null;}if(motion!=null){motion.removeAllUpdateListeners();motion.cancel();motion=null;}
   if(!ValueAnimator.areAnimatorsEnabled()){glow=down?1:0;recoil=0;change();return;}
   float start=glow;
   // Keep very short taps visible, even if released before the next display frame.
   if(down){anim=ValueAnimator.ofFloat(start,1f);anim.setDuration(100);}
   else if(start<.35f){anim=ValueAnimator.ofFloat(Math.max(start,.25f),.75f,0f);anim.setDuration(350);}
   else {anim=ValueAnimator.ofFloat(start,0f);anim.setDuration(350);}
   anim.setInterpolator(down?new PathInterpolator(.22f,0f,.25f,1f):new PathInterpolator(.18f,.55f,.24f,1f));
   anim.addUpdateListener(a->{glow=(float)a.getAnimatedValue();change();});anim.start();
   motion=down?ValueAnimator.ofFloat(recoil,1f):(recoil<.35f?ValueAnimator.ofFloat(Math.max(.25f,recoil),.75f,0f):ValueAnimator.ofFloat(recoil,0f));motion.setDuration(down?100:350);
   motion.setInterpolator(down?new PathInterpolator(.25f,0f,.25f,1f):new PathInterpolator(.18f,.86f,.24f,1.24f));
   motion.addUpdateListener(a->{recoil=(float)a.getAnimatedValue();change();});motion.start();
  }
  void stop(){if(anim!=null){anim.removeAllUpdateListeners();anim.cancel();}if(motion!=null){motion.removeAllUpdateListeners();motion.cancel();}anim=motion=null;pressed=false;glow=recoil=0;halo=null;change();}
  void prepareHalo(int size,float radius){
   if(halo!=null&&haloSize==size&&Math.abs(haloRadius-radius)<.5f)return;
   haloSize=size;haloRadius=radius;int pixels=Math.min(512,size);halo=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);
   Canvas c=new Canvas(halo);c.scale(pixels/(float)size,pixels/(float)size);float center=size*.5f;
   int first=0xffc7f9b3,end=0xff99ded2;
   Paint q=new Paint(Paint.ANTI_ALIAS_FLAG);
   q.setShader(new LinearGradient(0,0,size,size,new int[]{first,end},null,Shader.TileMode.CLAMP));
   c.drawRect(0,0,size,size,q);
   float edge=Math.min(.91f,radius/center);
   q.setShader(new RadialGradient(center,center,center,new int[]{0x08000000,0x18000000,0x39000000,0x8c000000,0x47000000,0x00000000},
    Geometry.haloStops(edge),Shader.TileMode.CLAMP));
   q.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));c.drawRect(0,0,size,size,q);
  }
  @Override protected void onBoundsChange(Rect bounds){super.onBoundsChange(bounds);halo=null;}
  @Override public void draw(Canvas c){
   Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY(),available=Math.min(b.width(),b.height());
   if(available<8)return;
   float radius=Math.min(Geometry.safeRadius(b.width(),b.height(),dp),commonRadius>0?commonRadius:39*dp);
   if(radius<1)return;
   int checkpoint=c.save();float scale=Geometry.recoilScale(recoil);c.scale(scale,scale,x,y);
   int base=dark?0xff171717:0xfffcfcff;
   p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(base);c.drawCircle(x,y,radius,p);
   if(glow>.002f){
    prepareShaders(x,y,radius);
    p.setColor(Color.WHITE);p.setShader(fillGlow);p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,radius,p);p.setShader(null);
    p.setColor(Color.WHITE);p.setShader(edgeGlow);p.setAlpha(Math.round(255*glow));c.drawCircle(x,y,radius,p);p.setShader(null);
   }
   if(glow>.002f){
    int size=Math.max(8,(int)Math.floor(Math.min(available/1.02f,2*(radius+4*dp))));
    prepareHalo(size,radius);
    bitmapPaint.setAlpha(Math.round(255*clamp(glow,0,1)));
    r.set(x-size*.5f,y-size*.5f,x+size*.5f,y+size*.5f);c.drawBitmap(halo,null,r,bitmapPaint);
   }
   p.setAlpha(255);c.restoreToCount(checkpoint);
  }
 }
}
