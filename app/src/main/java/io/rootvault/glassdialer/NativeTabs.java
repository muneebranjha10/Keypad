package io.rootvault.glassdialer;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Swipe only on the selected native tab. Never replace its background, indicator, or click listener. */
final class NativeTabs {
 private static final String INDICATOR="com.google.android.material.tabs.SeslTabRoundRectIndicator";
 private static final WeakHashMap<View,Gesture> gestures=new WeakHashMap<>();
 static void install(ClassLoader loader)throws Exception {
  Class<?> cls=Class.forName("com.samsung.android.dialer.tab.view.TabLayout",false,loader);
  if(!HorizontalScrollView.class.isAssignableFrom(cls))return;
  final Method selected=cls.getMethod("getSelectedTabPosition");
  XposedBridge.hookMethod(cls.getMethod("dispatchTouchEvent",MotionEvent.class),new XC_MethodHook(){
   @Override protected void beforeHookedMethod(MethodHookParam p){
    if(!cls.isInstance(p.thisObject))return;
    View v=(View)p.thisObject;MotionEvent e=(MotionEvent)p.args[0];int action=e.getActionMasked();
    Gesture g=gestures.get(v);
    try{
     if(action==MotionEvent.ACTION_DOWN){if(g!=null)g.finish(v);g=Gesture.start((HorizontalScrollView)v,selected,e);if(g!=null){gestures.put(v,g);v.addOnAttachStateChangeListener(g);}return;}
     if(g==null)return;
     if(g.owned){
      // The native touch stream has already been cancelled; consume through UP or CANCEL.
      p.setResult(true);
      if(action==MotionEvent.ACTION_UP){View destination=g.cancelled?null:g.destination((HorizontalScrollView)v,e);g.finish(v);if(destination!=null&&destination.isShown()&&destination.isEnabled())destination.performClick();return;}
      if(action==MotionEvent.ACTION_CANCEL){g.finish(v);return;}
      if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP||e.findPointerIndex(g.pointer)<0){g.cancelled=true;g.resetIndicator();return;}
      if(action==MotionEvent.ACTION_MOVE&&!g.cancelled)g.move((HorizontalScrollView)v,e);
      return;
     }
     if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){g.finish(v);return;}
     if(action!=MotionEvent.ACTION_MOVE)return;
     int index=e.findPointerIndex(g.pointer);if(index<0){g.finish(v);return;}
     float dx=e.getX(index)-g.startX,dy=e.getY(index)-g.startY;
     if(Math.abs(dy)>g.slop&&Math.abs(dy)>Math.abs(dx)){g.finish(v);return;}
     if(!Geometry.horizontalDrag(dx,dy,g.slop))return;
     if(!g.valid((HorizontalScrollView)v)){g.finish(v);return;}
     g.owned=true;
     MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
     try{XposedBridge.invokeOriginalMethod(p.method,v,new Object[]{cancel});}finally{cancel.recycle();}
     p.setResult(true);
     if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(true);
     g.move((HorizontalScrollView)v,e);
    }catch(Throwable t){
     if(g!=null){boolean owned=g.owned;g.resetIndicator();if(owned&&action!=MotionEvent.ACTION_UP&&action!=MotionEvent.ACTION_CANCEL){g.cancelled=true;p.setResult(true);}else{g.finish(v);if(owned)p.setResult(true);}}
     XposedBridge.log("GlassDialer native tabs: "+t);
    }
   }
  });
 }
 private static ArrayList<View> tabs(ViewGroup strip){ArrayList<View> list=new ArrayList<>(3);for(int i=0;i<strip.getChildCount();i++){View c=strip.getChildAt(i);if(c.getVisibility()==View.VISIBLE&&c.isEnabled()&&c.getWidth()>0)list.add(c);}return list;}
 private static View indicator(View root){
  if(INDICATOR.equals(root.getClass().getName())&&root.isShown()&&root.getAlpha()>.01f&&root.getWidth()>0)return root;
  if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View hit=indicator(g.getChildAt(i));if(hit!=null)return hit;}}
  return null;
 }
 private static final class Gesture implements View.OnAttachStateChangeListener {
  final java.lang.ref.WeakReference<ViewGroup> stripRef;final java.lang.ref.WeakReference<View> selectedRef;
  final float startX,startY,slop;final int pointer;
  final WeakHashMap<ViewGroup,Boolean> clipChildren=new WeakHashMap<>(),clipPadding=new WeakHashMap<>();
  java.lang.ref.WeakReference<View> indicatorRef=new java.lang.ref.WeakReference<>(null);
  float originalTranslation,initialIndicatorX;boolean owned,cancelled,translated;
  Gesture(ViewGroup strip,View selected,MotionEvent e,float slop){stripRef=new java.lang.ref.WeakReference<>(strip);selectedRef=new java.lang.ref.WeakReference<>(selected);startX=e.getX();startY=e.getY();pointer=e.getPointerId(0);this.slop=slop;}
  static Gesture start(HorizontalScrollView host,Method selected,MotionEvent e)throws Exception{
   if(host.getChildCount()!=1||!(host.getChildAt(0) instanceof ViewGroup))return null;
   ViewGroup strip=(ViewGroup)host.getChildAt(0);ArrayList<View> list=tabs(strip);if(list.size()!=3)return null;
   int idx=(Integer)selected.invoke(host);if(idx<0||idx>=strip.getChildCount())return null;
   View tab=strip.getChildAt(idx);if(!list.contains(tab))return null;
   int[] a=new int[2],b=new int[2];host.getLocationOnScreen(a);tab.getLocationOnScreen(b);
   if(!Geometry.hit(e.getX()+a[0]-b[0],e.getY()+a[1]-b[1],tab.getWidth(),tab.getHeight(),0))return null;
   return new Gesture(strip,tab,e,ViewConfiguration.get(host.getContext()).getScaledTouchSlop());
  }
  boolean valid(HorizontalScrollView host){ViewGroup strip=stripRef.get();View selected=selectedRef.get();return strip!=null&&selected!=null&&host.isAttachedToWindow()&&host.getChildCount()==1&&host.getChildAt(0)==strip&&selected.getParent()==strip&&tabs(strip).size()==3;}
  void move(HorizontalScrollView host,MotionEvent e){
   if(!valid(host)){cancelled=true;resetIndicator();return;}
   int i=e.findPointerIndex(pointer);if(i<0){cancelled=true;resetIndicator();return;}
   View selected=selectedRef.get();ViewGroup strip=stripRef.get();
   float dx=e.getX(i)-startX;
   View nativeIndicator=indicatorRef.get();
   if(!translated){
    nativeIndicator=indicator(selected);
    if(nativeIndicator==null)return; // Preserve native appearance; selection still works on release.
    int[] a=new int[2],b=new int[2];strip.getLocationOnScreen(a);nativeIndicator.getLocationOnScreen(b);
    initialIndicatorX=b[0]-a[0];originalTranslation=nativeIndicator.getTranslationX();
    indicatorRef=new java.lang.ref.WeakReference<>(nativeIndicator);
    for(ViewParent parent=nativeIndicator.getParent();parent instanceof ViewGroup;parent=parent.getParent()){
     ViewGroup vg=(ViewGroup)parent;clipChildren.put(vg,vg.getClipChildren());clipPadding.put(vg,vg.getClipToPadding());vg.setClipChildren(false);vg.setClipToPadding(false);if(vg==strip)break;
    }
    translated=true;
   }
   if(nativeIndicator==null||!nativeIndicator.isAttachedToWindow()){cancelled=true;resetIndicator();return;}
   float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;for(View c:tabs(strip)){min=Math.min(min,c.getLeft());max=Math.max(max,c.getRight());}
   nativeIndicator.setTranslationX(originalTranslation+Geometry.tabOffset(initialIndicatorX,nativeIndicator.getWidth(),dx,min,max));
  }
  View destination(HorizontalScrollView host,MotionEvent e){
   if(!valid(host))return null;int i=e.findPointerIndex(pointer);if(i<0)return null;
   ViewGroup strip=stripRef.get();View selected=selectedRef.get();ArrayList<View> list=tabs(strip);float[] centers=new float[list.size()];
   for(int k=0;k<centers.length;k++){View c=list.get(k);centers[k]=(c.getLeft()+c.getRight())*.5f;}
   float center=(selected.getLeft()+selected.getRight())*.5f+e.getX(i)-startX;
   int k=Geometry.nearest(center,centers);View target=k<0?null:list.get(k);
   return target==selected?null:target;
  }
  void resetIndicator(){
   View indicator=indicatorRef.get();if(translated&&indicator!=null)indicator.setTranslationX(originalTranslation);
   translated=false;indicatorRef.clear();
   for(ViewGroup v:new ArrayList<>(clipChildren.keySet())){if(v!=null){Boolean children=clipChildren.get(v),padding=clipPadding.get(v);if(children!=null)v.setClipChildren(children);if(padding!=null)v.setClipToPadding(padding);}}
   clipChildren.clear();clipPadding.clear();
  }
  void finish(View v){resetIndicator();if(owned&&v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(false);gestures.remove(v);v.removeOnAttachStateChangeListener(this);}
  @Override public void onViewAttachedToWindow(View v){}
  @Override public void onViewDetachedFromWindow(View v){finish(v);}
 }
}
