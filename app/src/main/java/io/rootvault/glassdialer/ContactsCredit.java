package io.rootvault.glassdialer;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Footer outside the adapter, only in the main Phone Contacts layout. */
final class ContactsCredit {
 private static final String TAG="io.rootvault.glassdialer.creator";
 private static final String LIST="com.samsung.android.dialtacts.common.contactslist.view.ContactRecyclerView";
 static void install()throws Exception{
  XposedBridge.hookMethod(View.class.getDeclaredMethod("onAttachedToWindow"),new XC_MethodHook(){
   @Override protected void afterHookedMethod(MethodHookParam p){
    View v=(View)p.thisObject;if(!LIST.equals(v.getClass().getName()))return;
    v.post(()->{try{add(v);}catch(Throwable t){XposedBridge.log("GlassDialer credit: "+t);}});
   }
  });
 }
 private static String name(View v){try{return v.getResources().getResourceEntryName(v.getId());}catch(Exception e){return "";}}
 private static boolean mainPhone(Context c){for(int i=0;i<12&&c!=null;i++){if(c instanceof Activity)return c.getClass().getName().equals("com.samsung.android.dialer.DialtactsActivity");if(!(c instanceof ContextWrapper))break;Context next=((ContextWrapper)c).getBaseContext();if(next==c)break;c=next;}return false;}
 private static void add(View list){
  if(!list.isAttachedToWindow()||!mainPhone(list.getContext())||!"contact_list".equals(name(list))||!(list.getParent() instanceof FrameLayout))return;
  View frame=(View)list.getParent();if(!(frame.getParent() instanceof LinearLayout))return;
  LinearLayout column=(LinearLayout)frame.getParent();
  if(column.getOrientation()!=LinearLayout.VERTICAL||!"coordinator_layout".equals(name(column))||!(frame.getLayoutParams() instanceof LinearLayout.LayoutParams)||((LinearLayout.LayoutParams)frame.getLayoutParams()).weight<=0)return;
  if(!(column.getParent() instanceof View)||!"contact_list_content".equals(name((View)column.getParent())))return;
  if(column.findViewWithTag(TAG)!=null)return;
  TextView footer=new TextView(list.getContext());footer.setTag(TAG);footer.setText("Made by s23_hype");footer.setTextSize(12);footer.setGravity(Gravity.CENTER);
  footer.setTypeface(Typeface.create("sec",Typeface.NORMAL));footer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  float dp=list.getResources().getDisplayMetrics().density;int pad=Math.round(6*dp);footer.setPadding(pad,pad,pad,pad);
  boolean night=(list.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  footer.setTextColor(night?0xff999999:0xff676767);footer.setBackgroundColor(night?0xff000000:0xfffafafa);
  column.addView(footer,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
 }
}
