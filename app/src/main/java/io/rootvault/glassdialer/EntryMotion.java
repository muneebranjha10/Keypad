package io.rootvault.glassdialer;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Animate only the newly inserted glyphs, using native Layout positions and direct glyph drawing; never change text, spans or selection. */
final class EntryMotion implements TextWatcher,View.OnAttachStateChangeListener {
 private static final WeakHashMap<EditText,WeakReference<EntryMotion>> instances=new WeakHashMap<>();
 private static final ThreadLocal<IdentityHashMap<XC_MethodHook.MethodHookParam,Frame>> frames=new ThreadLocal<>();
 private final EditText editor;
 private String previous="";
 private final ArrayList<Motion> active=new ArrayList<>();
 private static final long DURATION=210;
 private final TextPaint glyphPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
 private EntryMotion(EditText v){editor=v;previous=Geometry.dialString(v.getText());v.addTextChangedListener(this);v.addOnAttachStateChangeListener(this);}
 static void attach(EditText v){if(instances.containsKey(v))return;EntryMotion m=new EntryMotion(v);instances.put(v,new WeakReference<>(m));}
 static void install()throws Exception {
  XposedBridge.hookMethod(android.widget.TextView.class.getDeclaredMethod("onDraw",Canvas.class),new XC_MethodHook(){
   @Override protected void beforeHookedMethod(MethodHookParam p){
    if(!(p.thisObject instanceof EditText))return;WeakReference<EntryMotion> ref=instances.get(p.thisObject);EntryMotion m=ref==null?null:ref.get();if(m==null)return;
    try{Frame frame=m.prepare((Canvas)p.args[0]);if(frame!=null){IdentityHashMap<MethodHookParam,Frame> map=frames.get();if(map==null){map=new IdentityHashMap<>();frames.set(map);}map.put(p,frame);}}catch(Throwable ignored){}
   }
   @Override protected void afterHookedMethod(MethodHookParam p){
    IdentityHashMap<MethodHookParam,Frame> map=frames.get();if(map==null)return;Frame f=map.remove(p);if(map.isEmpty())frames.remove();if(f!=null)try{f.finish((Canvas)p.args[0]);}catch(Throwable ignored){}
   }
  });
 }
 @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
 @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
 @Override public void afterTextChanged(Editable e){
  String now=Geometry.dialString(e);String old=previous;previous=now;if(now.equals(old))return;int ordinal=Geometry.insertedOrdinal(old,now);
  if(ordinal<0){active.clear();editor.invalidate();return;}
  if(!ValueAnimator.areAnimatorsEnabled()||!editor.isAttachedToWindow()){active.clear();return;}
  // Existing in-flight characters stay animated during fast taps. Shift their ordinals for middle insertion.
  for(Motion m:active)if(m.ordinal>=ordinal)m.ordinal++;
  active.add(new Motion(ordinal,now.charAt(ordinal),SystemClock.uptimeMillis()));
  while(active.size()>8)active.remove(0);
  editor.postInvalidateOnAnimation();
 }
 @Override public void onViewAttachedToWindow(View v){previous=Geometry.dialString(editor.getText());active.clear();}
 @Override public void onViewDetachedFromWindow(View v){active.clear();}
 private Frame prepare(Canvas c){
  if(active.isEmpty())return null;
  if(!ValueAnimator.areAnimatorsEnabled()){active.clear();return null;}
  long time=SystemClock.uptimeMillis();CharSequence text=editor.getText();Layout layout=editor.getLayout();
  if(layout==null||text==null||!android.text.TextUtils.equals(layout.getText(),text)||layout.getLineCount()==0){if(time-active.get(active.size()-1).start<DURATION)editor.postInvalidateOnAnimation();else active.clear();return null;}
  glyphPaint.set(editor.getPaint());glyphPaint.setColor(editor.getCurrentTextColor());glyphPaint.setTextAlign(Paint.Align.LEFT);
  ArrayList<Glyph> glyphs=new ArrayList<>();
  float px=editor.getCompoundPaddingLeft(),py=editor.getTotalPaddingTop(),dp=editor.getResources().getDisplayMetrics().density;
  for(int i=active.size()-1;i>=0;i--){
   Motion m=active.get(i);float f=(time-m.start)/(float)DURATION;
   if(f>=1){active.remove(i);continue;}
   int index=Geometry.dialIndex(text,m.ordinal);
   if(index<0||text.charAt(index)!=m.digit){active.remove(i);continue;}
   int line=layout.getLineForOffset(index);
   if(layout.getParagraphDirection(line)!=1||layout.getEllipsisCount(line)>0||layout.getLineForOffset(index+1)!=line||layout.isRtlCharAt(index)){active.remove(i);continue;}
   float l=layout.getPrimaryHorizontal(index),r=layout.getPrimaryHorizontal(index+1);
   if(!Float.isFinite(l)||!Float.isFinite(r)||r-l<1){active.remove(i);continue;}
   float q=Geometry.entryEase(Math.max(0,f));
   // Layout's actual glyph cell: preserve other digits and cursor at their native positions.
   RectF box=new RectF(px+l,py+layout.getLineTop(line),px+r,py+layout.getLineBottom(line));
   glyphs.add(new Glyph(box,text.charAt(index),px+l,py+layout.getLineBaseline(line),Math.min(7*dp,(r-l)*.36f)*(1-q),Math.round(glyphPaint.getAlpha()*(.38f+.62f*q))));
  }
  if(!active.isEmpty())editor.postInvalidateOnAnimation();
  if(glyphs.isEmpty())return null;
  int save=c.save();
  try{for(Glyph g:glyphs)c.clipOutRect(g.box);return new Frame(save,glyphPaint,glyphs);}
  catch(Throwable t){c.restoreToCount(save);return null;}
 }
 private static final class Motion {int ordinal;final char digit;final long start;Motion(int o,char d,long s){ordinal=o;digit=d;start=s;}}
 private static final class Glyph {
  final RectF box;final char[] digit;final float x,y,dx;final int alpha;
  Glyph(RectF b,char d,float x,float y,float dx,int a){box=b;digit=new char[]{d};this.x=x;this.y=y;this.dx=dx;alpha=a;}
 }
 private static final class Frame {
  final int save;final TextPaint paint;final ArrayList<Glyph> glyphs;
  Frame(int s,TextPaint p,ArrayList<Glyph> g){save=s;paint=p;glyphs=g;}
  void finish(Canvas c){
   c.restoreToCount(save);int original=paint.getAlpha();
   try{for(Glyph g:glyphs){int s=c.save();try{
     c.clipRect(g.box);paint.setAlpha(g.alpha);
     c.drawText(g.digit,0,1,g.x+g.dx,g.y,paint);
    }finally{c.restoreToCount(s);}}}
   finally{paint.setAlpha(original);}
  }
 }

}
