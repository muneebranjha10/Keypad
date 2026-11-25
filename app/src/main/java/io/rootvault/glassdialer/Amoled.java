package io.rootvault.glassdialer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Theme only, scoped to Samsung Phone, and only when its native night mode is active. */
final class Amoled {
 private static final String PKG="com.samsung.android.dialer";
 // Names verified in SamsungDialer.apk. Do not modify shared palette tokens,
 // on-surface colors, icons, call buttons, ripple colors, or selector states.
 private static final Set<String> BLACK=new HashSet<>(Arrays.asList(
  "dialtacts_background_color","dialtacts_background_round_corner_color","dialtacts_status_bar_color",
  "dialpad_search_list_bg_color","dialpad_status_bar_color","dialpad_tab_background_color",
  "contacts_navigation_bar_color","contacts_status_bar_color","contact_list_section_header_background_color",
  "contact_list_sub_header_background_color","bottom_bar_background_color",
  "basic_token_sys_background","basic_token_sys_background_dark","basic_token_sys_surface","basic_token_sys_surface_dark",
  "basic_token_sys_surface_container","basic_token_sys_surface_container_dark",
  "basic_token_sys_surface_container_low","basic_token_sys_surface_container_low_dark",
  "basic_token_sys_surface_container_lowest","basic_token_sys_surface_container_lowest_dark",
  "background_material_dark","sesl_background_color_dark","sesl_action_bar_background_color_dark",
  "sesl_status_bar_color_dark","sesl_viewpager_background_dark","sesl_viewpager_item_background_dark",
  "sesl_bottom_navigation_background_dark","design_dark_default_color_background","design_dark_default_color_surface",
  "m3_sys_color_dark_background","m3_sys_color_dark_surface","m3_sys_color_dynamic_dark_background","m3_sys_color_dynamic_dark_surface",
  "card_view_background_color","cardview_dark_background","contact_detail_card_background_color",
  "detail_card_view_background_color_with_bg","detail_view_card_background_color","expand_view_background_color",
  "bottom_search_bar_background_color","calllog_group_call_background_color",
  "sesl_menu_popup_background_color_dark","sesl_dialog_window_background_color_material_dark",
  "background_floating_material_dark","sesl_background_floating_dark","sesl_floating_pane_menu_background_dark"));
 private static boolean night(Resources r){return (r.getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
 private static boolean own(Context c){return c!=null&&PKG.equals(c.getPackageName())&&night(c.getResources());}
 private static boolean target(Resources r,int id){if(id==0||!night(r))return false;try{return PKG.equals(r.getResourcePackageName(id))&&"color".equals(r.getResourceTypeName(id))&&BLACK.contains(r.getResourceEntryName(id));}catch(Exception e){return false;}}
 private static void log(String s){XposedBridge.log("GlassDialer AMOLED: "+s);}
 private static void hook(Class<?> c,String name,XC_MethodHook h,Class<?>... args){try{XposedBridge.hookMethod(c.getDeclaredMethod(name,args),h);}catch(Throwable t){log("optional "+name+": "+t);}}
 static void install(){
  XC_MethodHook resColor=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{if(target((Resources)p.thisObject,(int)p.args[0])&&p.getResult() instanceof Integer)p.setResult(Color.BLACK);}catch(Throwable ignored){}}};
  hook(Resources.class,"getColor",resColor,int.class);hook(Resources.class,"getColor",resColor,int.class,Resources.Theme.class);
  XC_MethodHook resList=new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{if(target((Resources)p.thisObject,(int)p.args[0])&&p.getResult() instanceof ColorStateList&&!((ColorStateList)p.getResult()).isStateful())p.setResult(ColorStateList.valueOf(Color.BLACK));}catch(Throwable ignored){}}};
  hook(Resources.class,"getColorStateList",resList,int.class);hook(Resources.class,"getColorStateList",resList,int.class,Resources.Theme.class);
  hook(Resources.class,"getValue",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{TypedValue v=(TypedValue)p.args[1];if(v.type>=TypedValue.TYPE_FIRST_COLOR_INT&&v.type<=TypedValue.TYPE_LAST_COLOR_INT&&target((Resources)p.thisObject,(int)p.args[0]))v.data=Color.BLACK;}catch(Throwable ignored){}}},int.class,TypedValue.class,boolean.class);
  hook(TypedArray.class,"getColor",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{TypedArray a=(TypedArray)p.thisObject;if(target(a.getResources(),a.getResourceId((int)p.args[0],0)))p.setResult(Color.BLACK);}catch(Throwable ignored){}}},int.class,int.class);
  hook(TypedArray.class,"getColorStateList",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{TypedArray a=(TypedArray)p.thisObject;Object result=p.getResult();if(result instanceof ColorStateList&&!((ColorStateList)result).isStateful()&&target(a.getResources(),a.getResourceId((int)p.args[0],0)))p.setResult(ColorStateList.valueOf(Color.BLACK));}catch(Throwable ignored){}}},int.class);
  hook(Activity.class,"onPostResume",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Activity a=(Activity)p.thisObject;if(own(a))window(a.getWindow(),false);}catch(Throwable t){log("activity: "+t);}}});
  hook(Activity.class,"onCreate",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Activity a=(Activity)p.thisObject;if(own(a)){Window w=a.getWindow();if(w!=null)w.getDecorView().post(()->{try{window(w,false);}catch(Throwable ignored){}});}}catch(Throwable ignored){}}},android.os.Bundle.class);
  hook(Dialog.class,"show",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{Dialog d=(Dialog)p.thisObject;if(own(d.getContext()))window(d.getWindow(),true);}catch(Throwable ignored){}}});
  hook(View.class,"onAttachedToWindow",new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;if(own(v.getContext()))surface(v);}catch(Throwable ignored){}}});
  // Runtime recoloring of newly bound rows; never replaces layout, listener, or foreground.
  hook(View.class,"setBackgroundColor",new XC_MethodHook(){@Override protected void beforeHookedMethod(MethodHookParam p){try{View v=(View)p.thisObject;if(own(v.getContext())&&surfaceView(v)&&neutral((int)p.args[0]))p.args[0]=Color.BLACK;}catch(Throwable ignored){}}},int.class);
  log("theme-only hooks installed; native night mode required");
 }
 private static boolean neutral(int c){int r=Color.red(c),g=Color.green(c),b=Color.blue(c);return Color.alpha(c)==255&&Math.max(r,Math.max(g,b))<=82&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=9;}
 private static String name(View v){try{return v.getId()==View.NO_ID?"":v.getResources().getResourceEntryName(v.getId()).toLowerCase(java.util.Locale.ROOT);}catch(Exception e){return "";}}
 private static boolean surfaceView(View v){
  if(!(v instanceof ViewGroup)||v instanceof RadioGroup||v instanceof Spinner||v.isClickable()||v.isLongClickable()||v.isFocusable())return false;
  String n=name(v),c=v.getClass().getName().toLowerCase(java.util.Locale.ROOT);
  if(n.matches(".*(button|btn|icon|avatar|badge|indicator|switch|checkbox|radio|ripple|tab_item|chip|call_button).*")||c.contains("tablayout")||c.contains("keypadbutton")||c.contains("slidingtabindicator")||c.contains("dialpadcallbutton"))return false;
  return true;
 }
 private static boolean recolor(Drawable d,int depth){
  if(d==null||depth>5)return false;
  if(d instanceof ColorDrawable){ColorDrawable c=(ColorDrawable)d;if(neutral(c.getColor())&&c.getColor()!=Color.BLACK){c.mutate();c.setColor(Color.BLACK);return true;}return false;}
  if(d instanceof GradientDrawable){GradientDrawable g=(GradientDrawable)d;ColorStateList c=g.getColor();if(c!=null&&!c.isStateful()&&neutral(c.getDefaultColor())&&c.getDefaultColor()!=Color.BLACK){g.mutate();g.setColor(Color.BLACK);return true;}return false;}
  // Keep pressed selectors, masks and ripple semantics intact.
  if(d instanceof RippleDrawable)return false;
  if(d instanceof InsetDrawable)return recolor(((InsetDrawable)d).getDrawable(),depth+1);
  if(d instanceof LayerDrawable){LayerDrawable l=(LayerDrawable)d;boolean changed=false;for(int i=0;i<l.getNumberOfLayers();i++)changed|=recolor(l.getDrawable(i),depth+1);return changed;}
  return false;
 }
 private static void surface(View v){if(surfaceView(v)&&recolor(v.getBackground(),0))v.invalidate();}
 private static void walk(View v){surface(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)walk(g.getChildAt(i));}}
 @SuppressWarnings("deprecation") private static void window(Window w,boolean dialog){
  if(w==null)return;View root=w.getDecorView();if(!own(root.getContext()))return;
  if(dialog){recolor(root.getBackground(),0);walk(root);return;}
  // Preserve floating/translucent window shapes and system insets.
  TypedArray a=root.getContext().obtainStyledAttributes(new int[]{android.R.attr.windowIsFloating,android.R.attr.windowIsTranslucent});
  boolean floating;try{floating=a.getBoolean(0,false)||a.getBoolean(1,false);}finally{a.recycle();}
  if(!floating){w.setBackgroundDrawable(new ColorDrawable(Color.BLACK));w.setStatusBarColor(Color.BLACK);w.setNavigationBarColor(Color.BLACK);
   w.setNavigationBarContrastEnforced(false);WindowInsetsController controller=w.getInsetsController();if(controller!=null)controller.setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);}
  walk(root);
  log("window applied "+root.getContext().getClass().getSimpleName());
 }
}
