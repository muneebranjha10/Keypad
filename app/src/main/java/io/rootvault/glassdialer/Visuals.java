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
  float glow=0,recoil=0; ValueAnimator anim; boolean pressed;
  final Paint digitPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);final Rect digitInk=new Rect(),referenceInk=new Rect();
  String digit;Typeface digitFont;int digitColor;float digitEnabled,digitRadius=Float.NaN;
  void setDigit(String value,Typeface font,int color,float enabled){if(java.util.Objects.equals(digit,value)&&digitFont==font&&digitColor==color&&digitEnabled==enabled)return;digit=value;digitFont=font;digitColor=color;digitEnabled=enabled;digitRadius=Float.NaN;invalidateSelf();}
  void prepareDigit(float radius){if(digit==null||digitFont==null||digitRadius==radius)return;digitRadius=radius;digitPaint.setTypeface(digitFont);digitPaint.setTextAlign(Paint.Align.LEFT);digitPaint.setTextSize(32*dp);digitPaint.getTextBounds("8",0,1,referenceInk);if(referenceInk.height()>0)digitPaint.setTextSize(32*dp*Geometry.glyphHeight(radius)/referenceInk.height());digitPaint.getTextBounds(digit,0,digit.length(),digitInk);}
  void drawDigit(Canvas c,float x,float y,float radius){if(digit==null||digitFont==null)return;prepareDigit(radius);int color=digitColor;if(digitEnabled>0){int r=Math.round(Color.red(color)+(153-Color.red(color))*glow),g=Math.round(Color.green(color)+(222-Color.green(color))*glow),b=Math.round(Color.blue(color)+(210-Color.blue(color))*glow);color=Color.argb(Color.alpha(color),r,g,b);}digitPaint.setColor(color);c.drawText(digit,x-(digitInk.left+digitInk.right)*.5f,y-(digitInk.top+digitInk.bottom)*.5f,digitPaint);}
  Runnable frame;
  final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
  Bitmap halo; int haloSize; float haloRadius,commonRadius;
  Shader fillGlow,edgeGlow;float shaderX=Float.NaN,shaderY=Float.NaN,shaderR=Float.NaN;
  void prepareShaders(float x,float y,float radius){if(fillGlow!=null&&shaderX==x&&shaderY==y&&shaderR==radius)return;shaderX=x;shaderY=y;shaderR=radius;fillGlow=new LinearGradient(x-radius,y-radius,x+radius,y+radius,new int[]{0xff344334,0xff2b342b,0xff253332},null,Shader.TileMode.CLAMP);edgeGlow=new RadialGradient(x,y,radius,new int[]{0x00000000,0x00000000,0x31000000,0x61000000},new float[]{0,.58f,.90f,1},Shader.TileMode.CLAMP);}
  void radius(float r){if(Math.abs(commonRadius-r)>.25f){commonRadius=r;halo=null;invalidateSelf();}}
  Key(float dp,boolean dark){super(dp,dark);}
  void change(){invalidateSelf();if(frame!=null)frame.run();}
  void press(boolean down){if(pressed==down)return;pressed=down;
   if(anim!=null){anim.removeAllUpdateListeners();anim.cancel();anim=null;}
   if(!ValueAnimator.areAnimatorsEnabled()){glow=down?1:0;recoil=0;change();return;}
   final float g=glow,r=recoil;final boolean quick=!down&&Math.max(g,r)<.35f;
   anim=ValueAnimator.ofFloat(0f,1f);anim.setDuration(down?100:350);anim.setInterpolator(new android.view.animation.LinearInterpolator());
   anim.addUpdateListener(a->{float t=(float)a.getAnimatedValue();glow=Geometry.keyGlow(g,t,down,quick);recoil=Geometry.keyRecoil(r,t,down,quick);change();});
   anim.start();
  }
  void stop(){if(anim!=null){anim.removeAllUpdateListeners();anim.cancel();}anim=null;pressed=false;glow=recoil=0;halo=null;change();}
  float currentRadius(){Rect b=getBounds();return Math.min(Geometry.safeRadius(b.width(),b.height(),dp),commonRadius>0?commonRadius:39*dp);}
  void warm(){Rect b=getBounds();float available=Math.min(b.width(),b.height()),radius=currentRadius();if(available<8||radius<1)return;prepareDigit(radius);prepareShaders(b.exactCenterX(),b.exactCenterY(),radius);prepareHalo(Math.max(8,(int)Math.floor(Math.min(available/1.02f,2*(radius+4*dp)))),radius);}
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
   float radius=currentRadius();
   if(radius<1)return;
   int checkpoint=c.save();float scale=Geometry.calculatorRecoilScale(recoil,radius,dp);c.scale(scale,scale,x,y);
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
   p.setAlpha(255);drawDigit(c,x,y,radius);c.restoreToCount(checkpoint);
  }
 }
}
