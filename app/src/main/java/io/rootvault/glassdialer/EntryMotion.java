package io.rootvault.glassdialer;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Single-line append reflow. Native text, selection, cursor and dialing remain untouched. */
final class EntryMotion implements TextWatcher,View.OnAttachStateChangeListener {
 private static final int MAX=48;
 private static final long DURATION=250;
 private static final android.view.animation.PathInterpolator EASE=new android.view.animation.PathInterpolator(.22f,.25f,0f,1f);
 private static final WeakHashMap<EditText,WeakReference<EntryMotion>> instances=new WeakHashMap<>();
 private static final ThreadLocal<EntryMotion> drawing=new ThreadLocal<>();
 private final EditText editor;
 private final TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
 private final RectF clip=new RectF();
 private final float[] target=new float[MAX],shown=new float[MAX],from=new float[MAX],initialAlpha=new float[MAX],shownAlpha=new float[MAX];
 private final char[] chars=new char[MAX];
 private String previous="",lastDrawn="",runningText="";
 private boolean requested,running,custom;
 private long start,requestTime;
 private int saved,count;
 private float baseline,lastBaseline,lastSize,lastScroll;
 private EntryMotion(EditText v){editor=v;previous=v.getText().toString();v.addTextChangedListener(this);v.addOnAttachStateChangeListener(this);}
 static void attach(EditText v){WeakReference<EntryMotion> ref=instances.get(v);if(ref!=null&&ref.get()!=null)return;instances.put(v,new WeakReference<>(new EntryMotion(v)));}
 static void install()throws Exception{
  XposedBridge.hookMethod(android.widget.TextView.class.getDeclaredMethod("onDraw",Canvas.class),new XC_MethodHook(){
   @Override protected void beforeHookedMethod(MethodHookParam p){
    if(!(p.thisObject instanceof EditText)||drawing.get()!=null)return;
    WeakReference<EntryMotion> ref=instances.get(p.thisObject);EntryMotion m=ref==null?null:ref.get();if(m==null)return;
    try{if(m.prepare((Canvas)p.args[0]))drawing.set(m);}catch(Throwable ignored){m.reset();}
   }
   @Override protected void afterHookedMethod(MethodHookParam p){
    EntryMotion m=drawing.get();if(m==null||m.editor!=p.thisObject)return;
    drawing.remove();try{m.finish((Canvas)p.args[0]);}catch(Throwable ignored){m.reset();}
   }
  });
 }
 @Override public void beforeTextChanged(CharSequence s,int st,int count,int after){}
 @Override public void onTextChanged(CharSequence s,int st,int before,int count){}
 @Override public void afterTextChanged(Editable e){
  String now=e.toString(),old=previous;previous=now;if(now.equals(old))return;
  // Formatting changes, deletion, cursor insertion and pasted numbers use the untouched native renderer.
  requested=Geometry.appendRun(old,now)&&ValueAnimator.areAnimatorsEnabled()&&editor.isAttachedToWindow();
  if(requested)requestTime=SystemClock.uptimeMillis();else{running=false;lastDrawn="";}
  editor.postInvalidateOnAnimation();
 }
 @Override public void onViewAttachedToWindow(View v){previous=editor.getText().toString();reset();}
 @Override public void onViewDetachedFromWindow(View v){reset();}
 private void reset(){running=requested=custom=false;lastDrawn=runningText="";}
 private boolean prepare(Canvas c){
  custom=false;
  if(!ValueAnimator.areAnimatorsEnabled()){reset();return false;}
  Layout layout=editor.getLayout();CharSequence text=editor.getText();
  if(text==null||layout==null||!android.text.TextUtils.equals(layout.getText(),text)||layout.getLineCount()!=1){if(requested&&SystemClock.uptimeMillis()-requestTime<300)editor.postInvalidateOnAnimation();else reset();return false;}
  int n=text.length();
  if(n==0){reset();return false;}
  if(n>MAX||layout.getParagraphDirection(0)!=1||layout.getEllipsisCount(0)>0||!Geometry.simpleDialRun(text)){reset();return false;}
  float px=editor.getCompoundPaddingLeft(),py=editor.getTotalPaddingTop();
  baseline=py+layout.getLineBaseline(0);float size=editor.getTextSize(),scroll=editor.getScrollX();
  for(int i=0;i<n;i++){
   if(layout.isRtlCharAt(i)){reset();return false;}
   float l=layout.getPrimaryHorizontal(i),r=layout.getPrimaryHorizontal(i+1);
   if(!Float.isFinite(l)||!Float.isFinite(r)||r-l<1){reset();return false;}
   chars[i]=text.charAt(i);target[i]=px+l;
  }
  String now=text.toString();long time=SystemClock.uptimeMillis();
  if(requested&&!now.equals(runningText)){
   // Begin at first valid draw, not at the earlier text callback. Rebase from current on-screen coordinates.
   boolean canReflow=Geometry.appendRun(lastDrawn,now)&&Math.abs(lastSize-size)<.1f&&Math.abs(lastBaseline-baseline)<1f&&Math.abs(lastScroll-scroll)<.1f;
   int old=canReflow?lastDrawn.length():Math.max(0,n-1);
   for(int i=0;i<n;i++){
    if(canReflow&&i<old){from[i]=shown[i];initialAlpha[i]=shownAlpha[i];}
    else if(i<old){from[i]=target[i];initialAlpha[i]=1f;}
    else{from[i]=target[i]+Math.min(size*.23f,editor.getResources().getDisplayMetrics().density*8);initialAlpha[i]=.55f;}
   }
   start=time;runningText=now;running=true;requested=false;
  }
  if(running&&(!now.equals(runningText)||Math.abs(lastSize-size)>.1f&&lastSize>0&&now.equals(lastDrawn))){running=false;}
  float fraction=running?Math.min(1f,(time-start)/(float)DURATION):1f;
  float q=EASE.getInterpolation(fraction);
  for(int i=0;i<n;i++){shown[i]=running?Geometry.lerp(from[i],target[i],q):target[i];shownAlpha[i]=running?Geometry.lerp(initialAlpha[i],1f,q):1f;}
  lastDrawn=now;lastSize=size;lastBaseline=baseline;lastScroll=scroll;
  if(!running||fraction>=1){running=false;return false;}
  count=n;
  // Suppress just the native text run, leaving its cursor at the native end position.
  float right=px+layout.getPrimaryHorizontal(n),left=target[0];
  clip.set(left,py+layout.getLineTop(0),right,py+layout.getLineBottom(0));
  if(clip.width()<1||!RectF.intersects(clip,new RectF(c.getClipBounds()))){running=false;return false;}
  paint.set(editor.getPaint());paint.setColor(editor.getCurrentTextColor());paint.setTextAlign(Paint.Align.LEFT);
  saved=c.save();
  try{c.clipOutRect(clip);custom=true;editor.postInvalidateOnAnimation();return true;}
  catch(Throwable t){c.restoreToCount(saved);custom=false;running=false;return false;}
 }
 private void finish(Canvas c){
  if(!custom)return;custom=false;c.restoreToCount(saved);
  int original=paint.getAlpha();int save=c.save();
  try{
   // Respect native visible text area and scrolling, but no per-digit clipping or offscreen layers.
   c.clipRect(editor.getScrollX()+editor.getCompoundPaddingLeft(),editor.getScrollY()+editor.getExtendedPaddingTop(),editor.getScrollX()+editor.getWidth()-editor.getCompoundPaddingRight(),editor.getScrollY()+editor.getHeight()-editor.getExtendedPaddingBottom());
   for(int i=0;i<count;i++){paint.setAlpha(Math.round(original*shownAlpha[i]));c.drawText(chars,i,1,shown[i],baseline,paint);}
  }finally{paint.setAlpha(original);c.restoreToCount(save);}
 }
}
