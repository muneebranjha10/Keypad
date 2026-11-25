package io.rootvault.glassdialer;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.animation.ArgbEvaluator;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Hooks implements IXposedHookLoadPackage {
 private static final String PKG="com.samsung.android.dialer";
 private static final String KEY=PKG+".dialpad.view.keypadbutton.binder.DialpadKeypadButtonLayout";
 private static final String TAB=PKG+".tab.view.TabLayout";
 private static final String INDICATOR="com.google.android.material.tabs.SeslTabRoundRectIndicator";
 private static final WeakHashMap<View,KeyState> keys=new WeakHashMap<>();
 private static final WeakHashMap<View,TabState> tabs=new WeakHashMap<>();
 private static boolean installed;
 private static void log(String s){XposedBridge.log("GlassDialer: "+s);}
 private static boolean dark(View v){return (v.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
 private static float dp(View v){return v.getResources().getDisplayMetrics().density;}
 private static Class<?> find(String s,ClassLoader l){try{return Class.forName(s,false,l);}catch(Throwable t){return null;}}
 private static Method declared(Class<?> c,String name,Class<?>... sig)throws Exception {Method m=c.getDeclaredMethod(name,sig);m.setAccessible(true);return m;}
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
  if(!PKG.equals(p.packageName)||!PKG.equals(p.processName)||installed)return;installed=true;
  try{Amoled.install();}catch(Throwable t){log("AMOLED install failed: "+t);}
  // Keep AMOLED active while re-enabling the improved keypad and native tab gestures.
  if(themeOnly())return;
  Class<?> key=find(KEY,p.classLoader),tab=find(TAB,p.classLoader);
  if(key!=null&&ViewGroup.class.isAssignableFrom(key))try{installKey(key);log("keypad hooks installed");}catch(Throwable e){log("keypad hooks unavailable: "+e);}
  else log("keypad class missing; stock UI retained");
  if(tab!=null&&HorizontalScrollView.class.isAssignableFrom(tab))try{installTabs(tab);log("tab hooks installed");}catch(Throwable e){log("tab hooks unavailable: "+e);}
  else log("tab class missing; stock UI retained");
  try{installDialpadDetails();}catch(Throwable e){log("dialpad detail hooks unavailable: "+e);}
 }
 private static boolean themeOnly(){return false;}
 private static void installKey(Class<?> c)throws Exception {
  Method pressed=declared(c,"setPressed",boolean.class);
  java.lang.reflect.Field nativeAnimator=null;try{java.lang.reflect.Field field=c.getDeclaredField("t");if(android.animation.ValueAnimator.class.isAssignableFrom(field.getType())){field.setAccessible(true);nativeAnimator=field;}}catch(Throwable ignored){}
  final java.lang.reflect.Field stockAnimation=nativeAnimator;
  XposedBridge.hookAllConstructors(c,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;if(keys.containsKey(v))return;KeyState s=new KeyState(v);keys.put(v,s);v.addOnAttachStateChangeListener(s);v.addOnLayoutChangeListener(s);if(v.isAttachedToWindow())s.apply(v);}catch(Throwable t){log("key init: "+t);}}});
  XposedBridge.hookMethod(pressed,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;KeyState s=keys.get(v);if(s!=null&&v.isAttachedToWindow()){s.apply(v);if(v.getBackground()==s.wrapper){if(stockAnimation!=null){Object a=stockAnimation.get(v);if(a instanceof android.animation.ValueAnimator&&((android.animation.ValueAnimator)a).isRunning())((android.animation.ValueAnimator)a).cancel();}s.skin.press((boolean)p.args[0]);}}}catch(Throwable t){log("key press: "+t);}}});
 }
 private static String idName(View v){int id=v.getId();if(id==View.NO_ID)return "";try{return v.getResources().getResourceEntryName(id);}catch(Throwable t){return "";}}
 private static boolean inDialpad(View v){View x=v;while(x!=null){if("keypad_fragment".equals(idName(x)))return true;android.view.ViewParent p=x.getParent();x=p instanceof View?(View)p:null;}return false;}
 private static final String CALL_LAYOUT=PKG+".dialpad.view.widget.DialpadCallButtonLayout";
 private static View descendant(View root,String name){if(name.equals(idName(root)))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=descendant(g.getChildAt(i),name);if(found!=null)return found;}}return null;}
 private static boolean eraseKey(View v){String name=idName(v);return ("dialpad_delete_button".equals(name)||"dialpad_widget_key_delete_button_china".equals(name))&&inDialpad(v);}
 private static View deleteRoot(View v){View x=v;for(int i=0;i<4&&x!=null;i++){if(CALL_LAYOUT.equals(x.getClass().getName())&&descendant(x,"delete_button_image")!=null&&descendant(x,"delete_button_background_layout")!=null)return x;android.view.ViewParent p=x.getParent();x=p instanceof View?(View)p:null;}return null;}
 private static void installDelete(View v){if(v==null||!v.isAttachedToWindow())return;KeyState s=keys.get(v);if(s==null){s=new KeyState(v);s.skin.erase(true);keys.put(v,s);v.addOnAttachStateChangeListener(s);v.addOnLayoutChangeListener(s);}s.apply(v);}

 private static boolean blackSurface(View v){String n=idName(v);return "keypad_fragment".equals(n)||"keypad_view".equals(n)||"dialpad_container".equals(n)||"dialpad_search_list_view".equals(n)||"toggle_container".equals(n)||"input_number_container".equals(n);}
 private static final WeakHashMap<View,Drawable> surfaces=new WeakHashMap<>();
 private static void black(View v){if(!blackSurface(v)||!inDialpad(v))return;if(dark(v)){if(!surfaces.containsKey(v))surfaces.put(v,v.getBackground());if(!(v.getBackground() instanceof android.graphics.drawable.ColorDrawable)||((android.graphics.drawable.ColorDrawable)v.getBackground()).getColor()!=Color.BLACK){int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackgroundColor(Color.BLACK);v.setPaddingRelative(a,b,c,d);}}else if(surfaces.containsKey(v)){Drawable old=surfaces.remove(v);int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackground(old);v.setPaddingRelative(a,b,c,d);}}
 private static void installDialpadDetails()throws Exception {
  XposedBridge.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;if(blackSurface(v))black(v);if(eraseKey(v))installDelete(v);
   String name=idName(v);if(CALL_LAYOUT.equals(v.getClass().getName())||"delete_button_image".equals(name)||"delete_button_background_layout".equals(name)){View root=deleteRoot(v);if(root!=null)root.post(()->{try{installDelete(root);}catch(Throwable e){log("delete root: "+e);}});}}catch(Throwable t){log("dialpad detail: "+t);}}});
  XposedBridge.hookMethod(View.class.getDeclaredMethod("setPressed",boolean.class),new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;KeyState s=keys.get(v);if(s==null&&("delete_button_image".equals(idName(v))||"delete_button_background_layout".equals(idName(v)))){View root=deleteRoot(v);if(root!=null){installDelete(root);v=root;s=keys.get(root);}}if(s!=null&&s.skin.erase&&v.isAttachedToWindow()){s.apply(v);s.skin.press((boolean)p.args[0]);}}catch(Throwable t){log("delete feedback: "+t);}}});
 }
 private static boolean sameGrid(View a,View b){
  View x=a;for(int i=0;i<5&&x!=null;i++){View y=b;for(int j=0;j<5&&y!=null;j++){if(x==y&&x!=a&&x!=b)return true;android.view.ViewParent p=y.getParent();y=p instanceof View?(View)p:null;}android.view.ViewParent p=x.getParent();x=p instanceof View?(View)p:null;}return false;
 }
 private static void alignGrid(View source){
  if(!source.isAttachedToWindow()||source.getWidth()<=0||source.getHeight()<=0)return;
  float radius=36*dp(source);ArrayList<KeyState> group=new ArrayList<>();
  for(View v:new ArrayList<>(keys.keySet())){if(v==null||!v.isAttachedToWindow()||!v.isShown()||v.getWidth()<=0||v.getHeight()<=0||!sameGrid(source,v)&&v!=source)continue;KeyState s=keys.get(v);if(s==null||s.skin.erase)continue;group.add(s);radius=Math.min(radius,.425f*Math.min(v.getWidth(),v.getHeight()));}
  for(KeyState s:group)s.skin.radius(radius);
 }
 private static final class KeyState implements View.OnAttachStateChangeListener,View.OnLayoutChangeListener {
  final Visuals.Key skin;final RippleDrawable wrapper;final WeakReference<View> view;
  Drawable stock;ColorStateList stockTint;boolean failed,reported,captured;
  float baseX=1,baseY=1;
  View innerDelete; Drawable innerStock; boolean innerCaptured;
  final WeakHashMap<TextView,Float> labelOffsets=new WeakHashMap<>();
  final WeakHashMap<TextView,ColorStateList> labels=new WeakHashMap<>();
  final WeakHashMap<ImageView,ColorStateList> icons=new WeakHashMap<>();
  final ArgbEvaluator evaluator=new ArgbEvaluator();
  KeyState(View v){view=new WeakReference<>(v);skin=new Visuals.Key(dp(v),dark(v));wrapper=new RippleDrawable(ColorStateList.valueOf(Color.TRANSPARENT),skin,null);skin.frame=this::paintFrame;}
  void discover(View v){if(v instanceof TextView){TextView t=(TextView)v;String s=t.getText()==null?"":t.getText().toString().trim();if(s.matches("[0-9*#＊＃]")&&!labels.containsKey(t))labels.put(t,t.getTextColors());}
   if(skin.erase&&v instanceof ImageView){ImageView i=(ImageView)v;if(!icons.containsKey(i))icons.put(i,i.getImageTintList());}
   if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)discover(g.getChildAt(i));}}
  void paintFrame(){View v=view.get();if(v==null)return;
   // Samsung calculator recoil moves 3 dp relative to key width. Do not alter layout or hit listeners.
   float width=Math.max(1,Math.min(v.getWidth(),v.getHeight()));
   float scale=1-Visuals.clamp(3.5f*dp(v)/width,0,.11f)*skin.recoil;
   v.setScaleX(baseX*scale);v.setScaleY(baseY*scale);
   for(TextView t:new ArrayList<>(labels.keySet())){if(t==null)continue;int from=skin.dark?0xfffcfcff:0xff3a3a3d,to=(skin.dark?0xff99ded2:0xff41c19f);
    int c=(int)evaluator.evaluate(Visuals.clamp(skin.glow,0,1),from,to);if(t.getCurrentTextColor()!=c)t.setTextColor(c);}
   if(skin.erase)for(ImageView i:new ArrayList<>(icons.keySet())){if(i==null)continue;int from=skin.dark?0xffed6968:0xfff56962,to=0xffffb3b3;int c=(int)evaluator.evaluate(Visuals.clamp(skin.glow,0,1),from,to);ColorStateList old=i.getImageTintList();if(old==null||old.getDefaultColor()!=c)i.setImageTintList(ColorStateList.valueOf(c));}
  }
  void alignLabels(View root){if(skin.erase||root.getWidth()==0)return;for(TextView t:new ArrayList<>(labels.keySet())){if(t==null||t.getWidth()==0)continue;float x=t.getLeft()+t.getWidth()*.5f;android.view.ViewParent p=t.getParent();while(p instanceof View&&p!=root){x+=((View)p).getLeft();p=((View)p).getParent();}if(p!=root)continue;if(!labelOffsets.containsKey(t))labelOffsets.put(t,t.getTranslationX());float delta=root.getWidth()*.5f-x;if(Math.abs(delta)<=8*dp(root))t.setTranslationX(delta);}}
  void apply(View v){if(failed||!v.isAttachedToWindow())return;try{skin.dark(dark(v));discover(v);
   if(skin.erase&&CALL_LAYOUT.equals(v.getClass().getName())){View inner=descendant(v,"delete_button_background_layout");if(inner!=null&&inner.getBackground()!=null){if(!innerCaptured){innerDelete=inner;innerStock=inner.getBackground();innerCaptured=true;}int a=inner.getPaddingStart(),b=inner.getPaddingTop(),c=inner.getPaddingEnd(),d=inner.getPaddingBottom();inner.setBackground(null);inner.setPaddingRelative(a,b,c,d);}}
   if(v.getBackground()!=wrapper){stock=v.getBackground();stockTint=v.getBackgroundTintList();if(!captured){baseX=v.getScaleX();baseY=v.getScaleY();}captured=true;int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackgroundTintList(null);v.setBackground(wrapper);v.setPaddingRelative(a,b,c,d);if(!reported){reported=true;log(skin.erase?"calculator red delete feedback active":"calculator recoil keypad v3 active");}}else if(v.getBackgroundTintList()!=null)v.setBackgroundTintList(null);alignLabels(v);paintFrame();
  }catch(Throwable e){failed=true;restore(v);log("key reverted: "+e);}}
  void restore(View v){skin.stop();for(TextView t:new ArrayList<>(labelOffsets.keySet()))if(t!=null)t.setTranslationX(labelOffsets.get(t));labelOffsets.clear();if(innerCaptured&&innerDelete!=null&&innerDelete.getBackground()==null){int a=innerDelete.getPaddingStart(),b=innerDelete.getPaddingTop(),c=innerDelete.getPaddingEnd(),d=innerDelete.getPaddingBottom();innerDelete.setBackground(innerStock);innerDelete.setPaddingRelative(a,b,c,d);}innerCaptured=false;innerDelete=null;innerStock=null;for(TextView t:new ArrayList<>(labels.keySet()))if(t!=null&&labels.get(t)!=null)t.setTextColor(labels.get(t));for(ImageView i:new ArrayList<>(icons.keySet()))if(i!=null)i.setImageTintList(icons.get(i));labels.clear();icons.clear();if(captured&&v.getBackground()==wrapper){int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackground(stock);v.setBackgroundTintList(stockTint);v.setPaddingRelative(a,b,c,d);}v.setScaleX(baseX);v.setScaleY(baseY);captured=false;}
  @Override public void onViewAttachedToWindow(View v){apply(v);if(!skin.erase)v.post(()->{if(v.isAttachedToWindow())alignGrid(v);});}
  @Override public void onViewDetachedFromWindow(View v){restore(v);}
  @Override public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){apply(v);if(!skin.erase)alignGrid(v);}
 }
 private static void installTabs(Class<?> c)throws Exception {
  Method selected=c.getMethod("getSelectedTabPosition");
  Method selection=null;for(Method m:c.getDeclaredMethods())if(m.getName().equals("setSelectedButton")&&m.getParameterCount()==1){selection=m;break;}
  if(selection==null)throw new NoSuchMethodException("setSelectedButton");
  XposedBridge.hookAllConstructors(c,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{HorizontalScrollView v=(HorizontalScrollView)p.thisObject;if(tabs.containsKey(v))return;TabState s=new TabState(v,selected);tabs.put(v,s);v.addOnAttachStateChangeListener(s);v.addOnLayoutChangeListener(s);if(v.isAttachedToWindow())s.sync(false);}catch(Throwable t){log("tab init: "+t);}}});
  XposedBridge.hookMethod(selection,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){TabState s=tabs.get(p.thisObject);if(s!=null)s.safeSync(true);}});
  try{Method m=declared(c.getSuperclass(),"v",selection.getParameterTypes()[0],boolean.class);
   XposedBridge.hookMethod(m,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){TabState s=tabs.get(p.thisObject);if(s!=null)s.safeSync(true);}});
  }catch(Throwable ignored){}
  XposedBridge.hookMethod(c.getMethod("dispatchTouchEvent",MotionEvent.class),new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){TabState s=tabs.get(p.thisObject);if(s==null)return;try{s.touch(p);}catch(Throwable t){s.abort();s.safeSync(false);log("tab gesture reverted: "+t);}}
   @Override protected void afterHookedMethod(MethodHookParam p){TabState s=tabs.get(p.thisObject);if(s==null)return;MotionEvent e=(MotionEvent)p.args[0];if(e.getActionMasked()==MotionEvent.ACTION_UP){View v=(View)p.thisObject;v.post(()->s.safeSync(true));}}
  });
 }
 private static final class TabState implements View.OnAttachStateChangeListener,View.OnLayoutChangeListener {
  final WeakReference<HorizontalScrollView> host;final Method selected;final Visuals.Tabs skin;final int slop;
  final android.view.VelocityTracker[] velocity={null};
  WeakReference<ViewGroup> stripRef=new WeakReference<>(null);Drawable oldStrip;boolean active,tracking,dragging,failed,logged;
  float startX,startY,grab;int pointer=-1;final int[] origin=new int[2],stripPos=new int[2];
  final WeakHashMap<View,Float> hidden=new WeakHashMap<>();final WeakHashMap<View,ColorStateList> tints=new WeakHashMap<>();
  TabState(HorizontalScrollView v,Method selected){host=new WeakReference<>(v);this.selected=selected;skin=new Visuals.Tabs(dp(v),dark(v));slop=ViewConfiguration.get(v.getContext()).getScaledTouchSlop();}
  ArrayList<View> children(ViewGroup strip){ArrayList<View> out=new ArrayList<>();for(int i=0;i<strip.getChildCount();i++){View v=strip.getChildAt(i);if(v.getVisibility()==View.VISIBLE&&v.getWidth()>0&&v.isEnabled())out.add(v);}return out;}
  int nativeIndex(HorizontalScrollView v){try{return (int)selected.invoke(v);}catch(Throwable t){return -1;}}
  View selectedView(HorizontalScrollView v,ViewGroup strip,ArrayList<View> list){int i=nativeIndex(v);if(i>=0&&i<strip.getChildCount()){View x=strip.getChildAt(i);if(list.contains(x))return x;}for(View x:list)if(x.isSelected())return x;return null;}
  void sync(boolean animate){HorizontalScrollView v=host.get();if(v==null||failed||!v.isAttachedToWindow()||dragging)return;
   if(v.getChildCount()!=1||!(v.getChildAt(0) instanceof ViewGroup)){restore();return;}
   ViewGroup strip=(ViewGroup)v.getChildAt(0);ArrayList<View> list=children(strip);
   if(list.size()!=3||strip.getWidth()<=0||strip.getHeight()<=0){restore();return;}
   View target=selectedView(v,strip,list);if(target==null){restore();return;}
   ViewGroup prev=stripRef.get();if(prev!=strip){restore();stripRef=new WeakReference<>(strip);}
   skin.dark(dark(v));if(strip.getBackground()!=skin){oldStrip=strip.getBackground();int a=strip.getPaddingStart(),b=strip.getPaddingTop(),c=strip.getPaddingEnd(),d=strip.getPaddingBottom();strip.setBackground(skin);strip.setPaddingRelative(a,b,c,d);}
   for(View child:list){hideNativeIndicator(child);if(child.getBackground()!=null){if(!tints.containsKey(child))tints.put(child,child.getBackgroundTintList());child.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));}}
   skin.lift=0;skin.move(target.getLeft(),target.getRight(),animate&&active);active=true;if(!logged){logged=true;log("glass navigation active (3 native tabs)");}
  }
  void hideNativeIndicator(View x){if(INDICATOR.equals(x.getClass().getName())){if(!hidden.containsKey(x))hidden.put(x,x.getAlpha());x.setAlpha(0f);return;}if(x instanceof ViewGroup){ViewGroup g=(ViewGroup)x;for(int i=0;i<g.getChildCount();i++)hideNativeIndicator(g.getChildAt(i));}}
  void safeSync(boolean animate){try{sync(animate);}catch(Throwable e){failed=true;restore();log("navigation reverted: "+e);}}
  void restore(){active=false;abort();skin.stop();ViewGroup strip=stripRef.get();if(strip!=null&&strip.getBackground()==skin){int a=strip.getPaddingStart(),b=strip.getPaddingTop(),c=strip.getPaddingEnd(),d=strip.getPaddingBottom();strip.setBackground(oldStrip);strip.setPaddingRelative(a,b,c,d);}for(View x:new ArrayList<>(hidden.keySet()))if(x!=null)x.setAlpha(hidden.get(x));hidden.clear();for(View x:new ArrayList<>(tints.keySet()))if(x!=null)x.setBackgroundTintList(tints.get(x));tints.clear();stripRef=new WeakReference<>(null);oldStrip=null;}
  void abort(){tracking=false;dragging=false;pointer=-1;skin.lift=0;if(velocity[0]!=null){velocity[0].recycle();velocity[0]=null;}HorizontalScrollView v=host.get();if(v!=null&&v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(false);}
  float localX(HorizontalScrollView v,ViewGroup strip,float x){v.getLocationOnScreen(origin);strip.getLocationOnScreen(stripPos);return x+origin[0]-stripPos[0];}
  View closest(ArrayList<View> list,float x){View best=null;float dist=Float.MAX_VALUE;for(View child:list){float d=Math.abs(x-(child.getLeft()+child.getRight())*.5f);if(d<dist){best=child;dist=d;}}return best;}
  void touch(XC_MethodHook.MethodHookParam p)throws Throwable {
   HorizontalScrollView v=host.get();ViewGroup strip=stripRef.get();if(!active||failed||v==null||strip==null)return;
   MotionEvent e=(MotionEvent)p.args[0];int action=e.getActionMasked();
   if(action==MotionEvent.ACTION_DOWN){abort();ArrayList<View> list=children(strip);if(list.size()!=3)return;View current=selectedView(v,strip,list);if(current==null)return;float x=localX(v,strip,e.getX());v.getLocationOnScreen(origin);strip.getLocationOnScreen(stripPos);float y=e.getY()+origin[1]-stripPos[1];
    if(x<current.getLeft()||x>current.getRight()||y<0||y>strip.getHeight())return;
    tracking=true;pointer=e.getPointerId(0);startX=x;startY=e.getY();grab=x-current.getLeft();velocity[0]=android.view.VelocityTracker.obtain();velocity[0].addMovement(e);if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(true);return;
   }
   if(!tracking)return;
   if(velocity[0]!=null)velocity[0].addMovement(e);
   if(action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP){boolean was=dragging;abort();safeSync(true);if(was)p.setResult(true);return;}
   int idx=e.findPointerIndex(pointer);if(idx<0){boolean was=dragging;abort();safeSync(true);if(was)p.setResult(true);return;}
   float x=localX(v,strip,e.getX(idx));float dx=x-startX,dy=e.getY(idx)-startY;
   if(action==MotionEvent.ACTION_MOVE){if(!dragging){if(Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx)){abort();return;}if(Math.abs(dx)<=slop||Math.abs(dx)<=Math.abs(dy)*1.25f)return;
      MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);try{XposedBridge.invokeOriginalMethod(p.method,v,new Object[]{cancel});}finally{cancel.recycle();}dragging=true;if(v.getParent()!=null)v.getParent().requestDisallowInterceptTouchEvent(true);
    }
    ArrayList<View> list=children(strip);if(list.size()!=3){abort();safeSync(false);p.setResult(true);return;}
    float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;for(View child:list){min=Math.min(min,child.getLeft());max=Math.max(max,child.getRight());}float w=(max-min)/3f;if(!Float.isFinite(w)||w<=0){abort();safeSync(false);p.setResult(true);return;}float left=Visuals.clamp(x-grab,min,max-w);skin.drag(left,left+w);p.setResult(true);return;
   }
   if(action==MotionEvent.ACTION_UP){boolean was=dragging;if(!was){abort();return;}ArrayList<View> list=children(strip);float projected=(skin.left+skin.right)*.5f;if(velocity[0]!=null){velocity[0].computeCurrentVelocity(1000);projected+=Visuals.clamp(velocity[0].getXVelocity(pointer)*.045f,-32*dp(v),32*dp(v));}View target=closest(list,projected);abort();if(target!=null&&target!=selectedView(v,strip,list)){
      int desired=strip.indexOfChild(target);target.performClick();
      if(nativeIndex(v)!=desired){try{Class<?> parent=v.getClass().getSuperclass();Method get=declared(parent,"r",int.class);Object model=get.invoke(v,desired);if(model!=null){Method select=declared(parent,"v",get.getReturnType(),boolean.class);select.invoke(v,model,true);}}catch(Throwable ex){log("native tab fallback: "+ex);}}
    }safeSync(true);v.post(()->safeSync(true));p.setResult(true);}
  }
  @Override public void onViewAttachedToWindow(View v){v.post(()->safeSync(false));}
  @Override public void onViewDetachedFromWindow(View v){restore();}
  @Override public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){safeSync(active);}
 }
}
