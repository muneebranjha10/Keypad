package io.rootvault.glassdialer;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.EditText;

/** Animates only a newly appended dial character. Never modifies the actual text or selection. */
final class EntryMotion implements TextWatcher,View.OnAttachStateChangeListener {
 private final EditText editor;
 private int start,before,added;
 private boolean pending;
 private ValueAnimator animator;
 private Glyph span;
 private Editable owner;
 private EntryMotion(EditText v){editor=v;v.addTextChangedListener(this);v.addOnAttachStateChangeListener(this);}
 static void attach(EditText v){new EntryMotion(v);}
 @Override public void beforeTextChanged(CharSequence s,int st,int count,int after){start=st;before=count;added=after;pending=false;}
 @Override public void onTextChanged(CharSequence s,int st,int old,int count){
  pending=old==0&&count==1&&st==s.length()-1&&"0123456789*#+".indexOf(s.charAt(st))>=0;
  start=st;
 }
 @Override public void afterTextChanged(Editable e){
  boolean animate=pending;pending=false;int index=start;
  // Wait until native formatter, layout, and cursor handling have finished.
  editor.post(()->{if(editor.getText()!=e||!editor.isAttachedToWindow())return;clear();
   if(!animate||index<0||index!=e.length()-1||editor.getSelectionStart()!=e.length()||editor.getSelectionEnd()!=e.length())return;
   if(editor.getLayout()!=null&&editor.getLayout().getParagraphDirection(0)<0)return;
   // Do not replace an existing native replacement span (ellipsis/formatting).
   if(e.getSpans(index,index+1,ReplacementSpan.class).length!=0)return;
   try{Glyph g=new Glyph(editor.getResources().getDisplayMetrics().density);span=g;owner=e;e.setSpan(g,index,index+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(220);
    animator.setInterpolator(new PathInterpolator(.2f,.8f,.2f,1f));
    animator.addUpdateListener(a->{g.progress=(float)a.getAnimatedValue();editor.invalidate();});
    animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(span==g)remove();}});
    animator.start();
   }catch(Throwable ignored){clear();}
  });
 }
 private void remove(){Glyph g=span;Editable e=owner;span=null;owner=null;animator=null;if(g!=null&&e!=null)e.removeSpan(g);editor.invalidate();}
 private void clear(){ValueAnimator a=animator;if(a!=null){a.removeAllListeners();a.cancel();}remove();}
 @Override public void onViewAttachedToWindow(View v){}
 @Override public void onViewDetachedFromWindow(View v){clear();}
 private static final class Glyph extends ReplacementSpan {
  float progress;final float dp;Glyph(float dp){this.dp=dp;}
  @Override public int getSize(Paint p,CharSequence text,int start,int end,Paint.FontMetricsInt fm){if(fm!=null)p.getFontMetricsInt(fm);return (int)Math.ceil(p.measureText(text,start,end));}
  @Override public void draw(Canvas c,CharSequence text,int start,int end,float x,int top,int y,int bottom,Paint p){
   int alpha=p.getAlpha();float size=p.getTextSize();int save=c.save();
   try{float offset=(1-progress)*Math.min(12*dp,size*.35f);p.setAlpha(Math.round(alpha*(.18f+.82f*progress)));c.drawText(text,start,end,x+offset,y,p);}
   finally{p.setAlpha(alpha);c.restoreToCount(save);}
  }
 }
}
