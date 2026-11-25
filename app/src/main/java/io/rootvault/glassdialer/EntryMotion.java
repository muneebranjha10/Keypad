package io.rootvault.glassdialer;
import android.animation.ValueAnimator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.PathInterpolator;
import android.widget.EditText;

/** Draw-only animation of the native dialed-number line; no spans, no cursor or text mutation. */
final class EntryMotion implements TextWatcher, View.OnAttachStateChangeListener {
 private final EditText editor;
 private int previousCount, generation;
 private Animation owned;
 private EntryMotion(EditText v){editor=v;v.addTextChangedListener(this);v.addOnAttachStateChangeListener(this);}
 static void attach(EditText v){new EntryMotion(v);}
 @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){previousCount=Geometry.dialCount(s);generation++;}
 @Override public void onTextChanged(CharSequence s,int start,int before,int count){}
 @Override public void afterTextChanged(Editable e){
  final int token=generation;
  final int delta=Geometry.dialCount(e)-previousCount;
  if(delta!=1){if(delta<0)clear();return;}
  editor.post(()->{
   if(token!=generation||!editor.isAttachedToWindow()||!editor.isShown()||!ValueAnimator.areAnimatorsEnabled())return;
   try{
    if(editor.getAnimation()!=null&&editor.getAnimation()!=owned&&!editor.getAnimation().hasEnded())return;
    clear();
    float dp=editor.getResources().getDisplayMetrics().density;
    AnimationSet set=new AnimationSet(true);
    set.addAnimation(new TranslateAnimation(Math.min(20*dp,editor.getWidth()*.07f),0f,0f,0f));
    set.addAnimation(new AlphaAnimation(.55f,1f));
    set.setDuration(240);set.setInterpolator(new PathInterpolator(.18f,.75f,.24f,1f));
    set.setFillAfter(false);owned=set;
    set.setAnimationListener(new Animation.AnimationListener(){
     public void onAnimationStart(Animation a){}
     public void onAnimationRepeat(Animation a){}
     public void onAnimationEnd(Animation a){if(owned==a)owned=null;}
    });
    editor.startAnimation(set);
   }catch(RuntimeException ignored){clear();}
  });
 }
 private void clear(){Animation a=owned;owned=null;if(a!=null&&editor.getAnimation()==a)editor.clearAnimation();}
 @Override public void onViewAttachedToWindow(View v){}
 @Override public void onViewDetachedFromWindow(View v){generation++;clear();}
}
