package io.rootvault.glassdialer;

import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.provider.Settings;
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
 private static final WeakHashMap<View,KeyState> keys=new WeakHashMap<>();
 private static boolean installed;
 private static void log(String s){XposedBridge.log("GlassDialer: "+s);}
 private static boolean dark(View v){return (v.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
 private static float dp(View v){return v.getResources().getDisplayMetrics().density;}
 private static Class<?> find(String s,ClassLoader l){try{return Class.forName(s,false,l);}catch(Throwable t){return null;}}
 private static Method declared(Class<?> c,String name,Class<?>... sig)throws Exception {Method m=c.getDeclaredMethod(name,sig);m.setAccessible(true);return m;}
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
  if(!PKG.equals(p.packageName)||!PKG.equals(p.processName)||installed)return;installed=true;
  try{Amoled.install();}catch(Throwable t){log("AMOLED install failed: "+t);}
  try{ContactsCredit.install();}catch(Throwable t){log("contacts credit unavailable: "+t);}
  // Keep the stock Samsung tab visuals and add a scoped swipe gesture.
  Class<?> key=find(KEY,p.classLoader);
  if(key!=null&&ViewGroup.class.isAssignableFrom(key))try{installKey(key);log("keypad hooks installed");}catch(Throwable e){log("keypad hooks unavailable: "+e);}
  else log("keypad class missing; stock UI retained");
  try{NativeTabs.install(p.classLoader);log("native Samsung tab drag installed");}catch(Throwable t){log("native tab drag unavailable: "+t);}
  try{installHintGuard();}catch(Throwable t){log("voicemail draw guard unavailable: "+t);}
  try{installDialpadDetails();}catch(Throwable e){log("dialpad detail hooks unavailable: "+e);}
  try{EntryMotion.install();installEntry(p.classLoader);}catch(Throwable e){log("entry animation unavailable: "+e);}
 }
 private static final WeakHashMap<View,Boolean> entryViews=new WeakHashMap<>();
 private static void installEntry(ClassLoader l)throws Exception {
  Class<?> c=find(PKG+".dialpad.view.widget.EllipsisEditText",l);
  if(c==null||!EditText.class.isAssignableFrom(c)){log("entry class missing; attachment fallback enabled");return;}
  XposedBridge.hookAllConstructors(c,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{attachEntry((EditText)p.thisObject);}catch(Throwable t){log("entry init: "+t);}}});
 }
 private static void attachEntry(EditText e){if(entryViews.containsKey(e))return;EntryMotion.attach(e);entryViews.put(e,true);log("native number-line animation attached");}
 private static void installKey(Class<?> c)throws Exception {
  Method pressed=declared(c,"setPressed",boolean.class);
  // Glyph and circle now share a single drawable and transform; no child-canvas scaling hook.
  java.lang.reflect.Field nativeAnimator=null;try{java.lang.reflect.Field field=c.getDeclaredField("t");if(android.animation.ValueAnimator.class.isAssignableFrom(field.getType())){field.setAccessible(true);nativeAnimator=field;}}catch(Throwable ignored){}
  final java.lang.reflect.Field stockAnimation=nativeAnimator;
  XposedBridge.hookAllConstructors(c,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;if(keys.containsKey(v)||eraseKey(v))return;KeyState s=new KeyState(v);keys.put(v,s);v.addOnAttachStateChangeListener(s);v.addOnLayoutChangeListener(s);if(v.isAttachedToWindow())s.apply(v);}catch(Throwable t){log("key init: "+t);}}});
  XposedBridge.hookMethod(pressed,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;KeyState s=keys.get(v);if(s!=null&&v.isAttachedToWindow()){if(!s.captured||v.getBackground()!=s.wrapper)s.apply(v);if(v.getBackground()==s.wrapper&&!s.failed){if(stockAnimation!=null)try{Object a=stockAnimation.get(v);if(a instanceof android.animation.ValueAnimator&&((android.animation.ValueAnimator)a).isRunning()){((android.animation.ValueAnimator)a).cancel();v.setScaleX(s.baseX);v.setScaleY(s.baseY);}}catch(Throwable ignored){}s.skin.press((boolean)p.args[0]);}}}catch(Throwable t){log("key press: "+t);}}});
 }
 private static String idName(View v){int id=v.getId();if(id==View.NO_ID)return "";try{return v.getResources().getResourceEntryName(id);}catch(Throwable t){return "";}}
 private static boolean inDialpad(View v){View x=v;while(x!=null){if("keypad_fragment".equals(idName(x)))return true;android.view.ViewParent p=x.getParent();x=p instanceof View?(View)p:null;}return false;}
 private static final String CALL_LAYOUT=PKG+".dialpad.view.widget.DialpadCallButtonLayout";
 private static View descendant(View root,String name){if(name.equals(idName(root)))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=descendant(g.getChildAt(i),name);if(found!=null)return found;}}return null;}
 private static boolean eraseKey(View v){String name=idName(v);return ("dialpad_delete_button".equals(name)||"dialpad_widget_key_delete_button_china".equals(name))&&inDialpad(v);}
 private static View deleteHost(View v){View x=v;for(int i=0;i<8&&x!=null;i++){if(CALL_LAYOUT.equals(x.getClass().getName())&&descendant(x,"delete_button_image")!=null&&descendant(x,"delete_button_background_layout")!=null)return x;android.view.ViewParent p=x.getParent();x=p instanceof View?(View)p:null;}return null;}
 private static View deleteRoot(View v){View host=deleteHost(v);return host==null?null:descendant(host,"delete_button_background_layout");}
 // The delete control keeps its native background, touch handling and dimensions.
 private static final WeakHashMap<ImageView,ColorStateList> deleteTints=new WeakHashMap<>();
 private static final WeakHashMap<View,WeakReference<Drawable>> redRipples=new WeakHashMap<>();
 private static void tintDelete(View v){
  View host=deleteHost(v);
  View image=host==null?(v instanceof ImageView&&eraseKey(v)?v:null):descendant(host,"delete_button_image");
  View background=host==null?(eraseKey(v)?v:null):descendant(host,"delete_button_background_layout");
  if(image instanceof ImageView){
   ImageView icon=(ImageView)image;ColorStateList current=icon.getImageTintList(),red=deleteTints.get(icon);
   if(red==null||!red.equals(current)){
    int disabled=current==null?0x66ff5252:current.getColorForState(new int[]{-android.R.attr.state_enabled},0x66ff5252);
    red=new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_pressed},new int[]{}},new int[]{disabled,0xffff9292,0xffff5252});
    deleteTints.put(icon,red);icon.setImageTintList(red);
   }
  }
  if(background!=null&&background.getBackground() instanceof RippleDrawable){
   Drawable d=background.getBackground();WeakReference<Drawable> old=redRipples.get(background);
   if(old==null||old.get()!=d){((RippleDrawable)d.mutate()).setColor(ColorStateList.valueOf(0x44ff4040));redRipples.put(background,new WeakReference<>(d));}
  }
 }

 private static boolean blackSurface(View v){String n=idName(v);return "keypad_fragment".equals(n)||"keypad_view".equals(n)||"dialpad_container".equals(n)||"dialpad_search_list_view".equals(n)||"toggle_container".equals(n)||"input_number_container".equals(n);}
 private static final WeakHashMap<View,Drawable> surfaces=new WeakHashMap<>();
 private static void black(View v){if(!blackSurface(v)||!inDialpad(v))return;if(dark(v)){if(!surfaces.containsKey(v))surfaces.put(v,v.getBackground());if(!(v.getBackground() instanceof android.graphics.drawable.ColorDrawable)||((android.graphics.drawable.ColorDrawable)v.getBackground()).getColor()!=Color.BLACK){int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackgroundColor(Color.BLACK);v.setPaddingRelative(a,b,c,d);}}else if(surfaces.containsKey(v)){Drawable old=surfaces.remove(v);int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackground(old);v.setPaddingRelative(a,b,c,d);}}
 private static void installDialpadDetails()throws Exception {
  XposedBridge.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){
   @Override protected void afterHookedMethod(MethodHookParam p){try{
    View v=(View)p.thisObject;if(blackSurface(v))black(v);
    if(v instanceof EditText&&v.getClass().getSimpleName().equals("EllipsisEditText")&&v.getClass().getName().startsWith(PKG+"."))attachEntry((EditText)v);
    String name=idName(v);
    if(eraseKey(v)||CALL_LAYOUT.equals(v.getClass().getName())||"delete_button_image".equals(name)||"delete_button_background_layout".equals(name)){
     tintDelete(v);v.post(()->{try{if(v.isAttachedToWindow())tintDelete(v);}catch(Throwable ignored){}});
    }
   }catch(Throwable t){log("dialpad detail: "+t);}}
  });
 }
 private static boolean oneKey(View root){
  if(Geometry.oneKeyId(idName(root)))return true;
  View digit=descendant(root,"dialpad_key_number");
  if(digit==null)digit=descendant(root,"dialpad_key_1_text_view");
  return digit instanceof TextView&&"1".contentEquals(((TextView)digit).getText());
 }
 private static void installHintGuard()throws Exception {
  // The APK uses a generic image resource for voicemail; suppress only its drawing under key 1.
  XposedBridge.hookMethod(ViewGroup.class.getDeclaredMethod("drawChild",Canvas.class,View.class,long.class),new XC_MethodHook(){
   @Override protected void beforeHookedMethod(MethodHookParam p){
    try{View child=(View)p.args[1];boolean label=child instanceof TextView, image=child instanceof ImageView;if(!label&&!image)return;
     String name=idName(child);if(image&&!Geometry.oneHintId(name))return;
     View root=(View)p.thisObject;
     for(int depth=0;root!=null&&depth<8;depth++){
      KeyState state=keys.get(root);
      if(state!=null){if(state.captured&&!state.failed&&root.getBackground()==state.wrapper){if(image&&oneKey(root))p.setResult(false);if(label&&state.skin.digit!=null&&state.digitAlphas.containsKey(child)&&state.primaryLabel((TextView)child))p.setResult(false);}return;}
      android.view.ViewParent parent=root.getParent();root=parent instanceof View?(View)parent:null;
     }
    }catch(Throwable ignored){}
   }
  });
 }
 private static boolean sameGrid(View a,View b){
  View x=a;for(int i=0;i<5&&x!=null;i++){View y=b;for(int j=0;j<5&&y!=null;j++){if(x==y&&x!=a&&x!=b)return true;android.view.ViewParent p=y.getParent();y=p instanceof View?(View)p:null;}android.view.ViewParent p=x.getParent();x=p instanceof View?(View)p:null;}return false;
 }
 private static void alignGrid(View source){
  if(!source.isAttachedToWindow()||source.getWidth()<=0||source.getHeight()<=0)return;
  float radius=Geometry.safeRadius(source.getWidth(),source.getHeight(),dp(source));ArrayList<KeyState> group=new ArrayList<>();
  for(View v:new ArrayList<>(keys.keySet())){if(v==null||!v.isAttachedToWindow()||!v.isShown()||v.getWidth()<=0||v.getHeight()<=0||!sameGrid(source,v)&&v!=source)continue;KeyState s=keys.get(v);if(s==null||s.failed||eraseKey(v))continue;group.add(s);radius=Math.min(radius,Geometry.safeRadius(v.getWidth(),v.getHeight(),dp(v)));}
  for(KeyState s:group){s.skin.radius(radius);s.skin.warm();}
 }
 private static final class KeyState implements View.OnAttachStateChangeListener,View.OnLayoutChangeListener {
  final Visuals.Key skin;final RippleDrawable wrapper;final WeakReference<View> view;
  Drawable stock;ColorStateList stockTint;boolean failed,reported,captured;
  float baseX=1,baseY=1;
  final WeakHashMap<View,Float> labelOffsets=new WeakHashMap<>(),labelY=new WeakHashMap<>(),hiddenHints=new WeakHashMap<>();
  final WeakHashMap<TextView,ColorStateList> labels=new WeakHashMap<>();
  final ArgbEvaluator evaluator=new ArgbEvaluator();
  final WeakHashMap<TextView,Float> digitAlphas=new WeakHashMap<>();
  Typeface calcFont;boolean fontBold;
  KeyState(View v){view=new WeakReference<>(v);skin=new Visuals.Key(dp(v),dark(v));wrapper=new RippleDrawable(ColorStateList.valueOf(Color.TRANSPARENT),skin,null);skin.frame=this::paintFrame;}
  boolean primaryLabel(TextView t){String s=t.getText()==null?"":t.getText().toString().trim();return "dialpad_key_number".equals(idName(t))||s.matches("[0-9*#＊＃]")||(s.length()==1&&Character.isDigit(s.charAt(0)));}
  void hideHint(View v){if(!hiddenHints.containsKey(v))hiddenHints.put(v,v.getAlpha());if(v.getAlpha()!=0f)v.setAlpha(0f);}
  void discover(View v){if(v instanceof TextView){TextView t=(TextView)v;String s=t.getText()==null?"":t.getText().toString().trim();{if(primaryLabel(t)){if(hiddenHints.containsKey(t)){t.setAlpha(hiddenHints.remove(t));}if(!labels.containsKey(t))labels.put(t,t.getTextColors());}else if(!s.isEmpty()){hideHint(t);}}}
   if(v instanceof ImageView){String n=idName(v);View root=view.get();if(n.contains("voicemail")||n.contains("letter")||n.contains("sub_")||(root!=null&&oneKey(root)&&Geometry.oneHintId(n)))hideHint(v);}
   if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)discover(g.getChildAt(i));}}
  void paintFrame(){View v=view.get();if(v==null||!captured)return;if(skin.digit!=null)return;
   // Samsung calculator recoil moves 3 dp relative to key width. Do not alter layout or hit listeners.
   // The drawable performs recoil; keep the touch receiver and its clipping bounds fixed.
   // Invalidate the full view to refresh the matching child draw transform. Hit bounds stay fixed.
   v.postInvalidateOnAnimation();
   for(TextView t:new ArrayList<>(labels.keySet())){if(t==null)continue;ColorStateList original=labels.get(t);if(original==null)continue;if(skin.glow<=.002f||!t.isEnabled()){if(t.getTextColors()!=original)t.setTextColor(original);continue;}int from=original.getColorForState(t.getDrawableState(),original.getDefaultColor()),to=skin.dark?0xff99ded2:0xff41c19f;
    int c=(int)evaluator.evaluate(Visuals.clamp(skin.glow,0,1),from,to);if(t.getCurrentTextColor()!=c)t.setTextColor(c);}
  }
  void resetAlignment(){for(View t:new ArrayList<>(labelOffsets.keySet()))if(t!=null){Float x=labelOffsets.get(t),y=labelY.get(t);if(x!=null)t.setTranslationX(x);if(y!=null)t.setTranslationY(y);}labelOffsets.clear();labelY.clear();}
  void restoreDigits(){for(TextView t:new ArrayList<>(digitAlphas.keySet()))if(t!=null){Float alpha=digitAlphas.get(t);if(alpha!=null)t.setAlpha(alpha);}digitAlphas.clear();skin.setDigit(null,null,0,0);}
  void alignLabels(View root){
   if(root.getWidth()<=0||root.getHeight()<=0)return;
   TextView selected=null;
   for(TextView t:new ArrayList<>(labels.keySet())){if(t==null||!primaryLabel(t)||!t.isShown()||t.getText()==null)continue;String s=t.getText().toString().trim();if(s.length()!=1||!(Character.isDigit(s.charAt(0))||"*#＊＃".indexOf(s.charAt(0))>=0))continue;if(selected!=null&&!selected.getText().toString().trim().equals(s)){restoreDigits();return;}selected=t;}
   if(selected==null){restoreDigits();return;}
   boolean bold=Settings.Global.getInt(root.getContext().getContentResolver(),"bold_text",0)==1;
   if(calcFont==null||fontBold!=bold){fontBold=bold;Typeface base=Typeface.create("sec",Typeface.NORMAL);calcFont=Typeface.create(base,300,false);if(bold)calcFont=Typeface.create(calcFont,Typeface.BOLD);}
   ColorStateList original=labels.get(selected);int color=original==null?(dark(root)?Color.WHITE:0xff222222):original.getColorForState(selected.getDrawableState(),original.getDefaultColor());
   skin.setDigit(selected.getText().toString().trim(),calcFont,color,root.isEnabled()&&selected.isEnabled()?1f:0f);
   if(skin.digit==null){restoreDigits();return;}
   for(TextView t:new ArrayList<>(labels.keySet()))if(t!=null&&primaryLabel(t)){if(!digitAlphas.containsKey(t))digitAlphas.put(t,t.getAlpha());if(t.getAlpha()!=0)t.setAlpha(0f);}
  }
  void apply(View v){if(failed||!v.isAttachedToWindow()||eraseKey(v))return;try{skin.dark(dark(v));discover(v);
   
   if(v.getBackground()!=wrapper){stock=v.getBackground();stockTint=v.getBackgroundTintList();if(!captured){baseX=v.getScaleX();baseY=v.getScaleY();}captured=true;int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackgroundTintList(null);v.setBackground(wrapper);v.setPaddingRelative(a,b,c,d);if(!reported){reported=true;log("calculator recoil keypad active");}}else if(v.getBackgroundTintList()!=null)v.setBackgroundTintList(null);alignLabels(v);skin.warm();paintFrame();
  }catch(Throwable e){failed=true;restore(v);log("key reverted: "+e);}}
  void restore(View v){skin.stop();restoreDigits();resetAlignment();for(View h:new ArrayList<>(hiddenHints.keySet()))if(h!=null)h.setAlpha(hiddenHints.get(h));hiddenHints.clear();for(TextView t:new ArrayList<>(labels.keySet()))if(t!=null&&labels.get(t)!=null)t.setTextColor(labels.get(t));labels.clear();if(captured&&v.getBackground()==wrapper){int a=v.getPaddingStart(),b=v.getPaddingTop(),c=v.getPaddingEnd(),d=v.getPaddingBottom();v.setBackground(stock);v.setBackgroundTintList(stockTint);v.setPaddingRelative(a,b,c,d);}captured=false;stock=null;stockTint=null;}
  @Override public void onViewAttachedToWindow(View v){apply(v);v.post(()->{if(v.isAttachedToWindow()){apply(v);alignGrid(v);}});}
  @Override public void onViewDetachedFromWindow(View v){restore(v);}
  @Override public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){apply(v);alignGrid(v);}
 }
}
