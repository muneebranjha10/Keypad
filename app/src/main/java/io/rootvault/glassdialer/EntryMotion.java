package io.rootvault.glassdialer;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
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

/** Animate only the newly inserted glyphs, using the native Layout; never change text, spans or selection. */
final class EntryMotion implements TextWatcher,View.OnAttachStateChangeListener {
 private static final WeakHashMap<EditText,WeakReference<EntryMotion>> instances=new WeakHashMap<>();
 private static final ThreadLocal<IdentityHashMap<XC_MethodHook.MethodHookParam,Frame>> frames=new ThreadLocal<>();
 private final EditText editor;
 private String previous="";
 private final ArrayList<Motion> active=new ArrayList<>();
 private static final long DURATION=260;
 private EntryMotion(EditText v){editor=v;v.addTextChangedListener(this);v.addOnAttachStateChangeListener(this);}
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
 @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){previous=Geometry.dialString(s);}
 @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
 @Override public void afterTextChanged(Editable e){
  String now=Geometry.dialString(e);int ordinal=Geometry.insertedOrdinal(previous,now);
  if(ordinal<0){if(!now.equals(previous))active.clear();editor.invalidate();return;}
  if(!ValueAnimator.areAnimatorsEnabled()||!editor.isAttachedToWindow())return;
  // Existing in-flight characters stay animated during fast taps. Shift their ordinals for middle insertion.
  for(Motion m:active)if(m.ordinal>=ordinal)m.ordinal++;
  active.add(new Motion(ordinal,now.charAt(ordinal),SystemClock.uptimeMillis()));
  while(active.size()>8)active.remove(0);
  editor.postInvalidateOnAnimation();
 }
 @Override public void onViewAttachedToWindow(View v){}
 @Override public void onViewDetachedFromWindow(View v){active.clear();}
 private Frame prepare(Canvas c){
  if(active.isEmpty())return null;
  if(!ValueAnimator.areAnimatorsEnabled()){active.clear();return null;}
  long time=SystemClock.uptimeMillis();CharSequence text=editor.getText();Layout layout=editor.getLayout();
  if(layout==null||text==null||layout.getText().length()!=text.length()||layout.getLineCount()==0){active.clear();return null;}
  ArrayList<Glyph> glyphs=new ArrayList<>();
  float px=editor.getCompoundPaddingLeft(),py=editor.getTotalPaddingTop(),dp=editor.getResources().getDisplayMetrics().density;
  for(int i=active.size()-1;i>=0;i--){
   Motion m=active.get(i);float f=(time-m.start)/(float)DURATION;
   if(f>=1){active.remove(i);continue;}
   int index=Geometry.dialIndex(text,m.ordinal);
   if(index<0||text.charAt(index)!=m.digit){active.remove(i);continue;}
   int line=layout.getLineForOffset(index);
   if(layout.getParagraphDirection(line)!=1||layout.getEllipsisCount(line)>0||layout.getLineForOffset(index+1)!=line){active.remove(i);continue;}
   float l=layout.getPrimaryHorizontal(index),r=layout.getPrimaryHorizontal(index+1);
   if(!Float.isFinite(l)||!Float.isFinite(r)||r-l<1){active.remove(i);continue;}
   float q=Geometry.entryEase(Math.max(0,f));
   // Layout's actual glyph cell: preserve other digits and cursor at their native positions.
   RectF box=new RectF(px+l,py+layout.getLineTop(line),px+r,py+layout.getLineBottom(line));
   glyphs.add(new Glyph(box,Math.min(14*dp,(r-l)*.75f)*(1-q),Math.round(255*(.28f+.72f*q))));
  }
  if(!active.isEmpty())editor.postInvalidateOnAnimation();
  if(glyphs.isEmpty())return null;
  int save=c.save();
  try{for(Glyph g:glyphs)c.clipOutRect(g.box);return new Frame(save,layout,px,py,glyphs);}
  catch(Throwable t){c.restoreToCount(save);return null;}
 }
 private static final class Motion {int ordinal;final char digit;final long start;Motion(int o,char d,long s){ordinal=o;digit=d;start=s;}}
 private static final class Glyph {final RectF box;final float dx;final int alpha;Glyph(RectF b,float d,int a){box=b;dx=d;alpha=a;}}
 private static final class Frame {
  final int save;final Layout layout;final float x,y;final ArrayList<Glyph> glyphs;
  Frame(int s,Layout l,float x,float y,ArrayList<Glyph> g){save=s;layout=l;this.x=x;this.y=y;glyphs=g;}
  void finish(Canvas c){
   c.restoreToCount(save);
   for(Glyph g:glyphs){int s=c.save();try{
    // Keep moving glyph inside its own cell to avoid painting over a neighboring digit.
    c.clipRect(g.box);int layer=c.saveLayerAlpha(g.box,g.alpha);
    try{c.translate(x+g.dx,y);c.clipRect(g.box.left-x,g.box.top-y,g.box.right-x,g.box.bottom-y);layout.draw(c);}
    finally{c.restoreToCount(layer);}
   }finally{c.restoreToCount(s);}}
  }
 }
}
