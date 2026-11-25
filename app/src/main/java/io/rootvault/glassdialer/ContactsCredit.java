package io.rootvault.glassdialer;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.graphics.Rect;
import java.lang.ref.WeakReference;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** End-of-list credit, above native tabs; never changes contact adapter or touch handling. */
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
  FrameLayout host=(FrameLayout)frame;
  if(host.findViewWithTag(TAG)!=null)return;
  TextView footer=new TextView(list.getContext());footer.setTag(TAG);footer.setText("Made by s23_hype");footer.setTextSize(12);footer.setGravity(Gravity.CENTER);
  footer.setTypeface(Typeface.create("sec",Typeface.NORMAL));footer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
  footer.setClickable(false);footer.setFocusable(false);
  float dp=list.getResources().getDisplayMetrics().density;int height=Math.round(32*dp);
  boolean night=(list.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
  footer.setTextColor(night?0xff999999:0xff676767);footer.setBackgroundColor(night?0xff000000:0xfffafafa);
  footer.setVisibility(View.INVISIBLE);
  host.addView(footer,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height,Gravity.BOTTOM));
  EndCredit state=new EndCredit(list,host,footer,height,Math.round(8*dp));
  list.addOnAttachStateChangeListener(state);state.observer=list.getViewTreeObserver();state.observer.addOnPreDrawListener(state);
 }
 private static View findTabs(View v){
  if("com.samsung.android.dialer.tab.view.TabLayout".equals(v.getClass().getName()))return v;
  if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View hit=findTabs(g.getChildAt(i));if(hit!=null)return hit;}}
  return null;
 }
 private static final class EndCredit implements ViewTreeObserver.OnPreDrawListener,View.OnAttachStateChangeListener {
  final View list;final FrameLayout host;final TextView footer;final int height,gap;
  final int[] listPos=new int[2],hostPos=new int[2],tabPos=new int[2];
  int nativeBottom,applied=-1,margin=-1;
  ViewTreeObserver observer;
  WeakReference<View> tabs=new WeakReference<>(null);
  EndCredit(View list,FrameLayout host,TextView footer,int height,int gap){this.list=list;this.host=host;this.footer=footer;this.height=height;this.gap=gap;nativeBottom=list.getPaddingBottom();}
  private void hide(){if(footer.getVisibility()!=View.INVISIBLE)footer.setVisibility(View.INVISIBLE);}
  @Override public boolean onPreDraw(){
   try{
    if(!list.isAttachedToWindow()||!list.isShown()||list.getWidth()==0||list.getHeight()==0||host.getHeight()==0){hide();return true;}
    // Respect native bottom padding and the actual top of Samsung's floating tab bar.
    if(applied<0||list.getPaddingBottom()!=applied)nativeBottom=list.getPaddingBottom();
    list.getLocationOnScreen(listPos);host.getLocationOnScreen(hostPos);
    View tab=tabs.get();if(tab==null||!tab.isAttachedToWindow()){tab=findTabs(list.getRootView());tabs=new WeakReference<>(tab);}
    int listBottom=listPos[1]+list.getHeight(),safeBottom=listBottom-nativeBottom;
    if(tab!=null&&tab.isShown()&&tab.getHeight()>0){tab.getLocationOnScreen(tabPos);safeBottom=Math.min(safeBottom,tabPos[1]-gap);}
    int hostBottom=hostPos[1]+host.getHeight();
    safeBottom=Math.min(safeBottom,hostBottom);
    if(safeBottom-height<=Math.max(listPos[1],hostPos[1])){hide();return true;}
    int wanted=Geometry.creditPadding(nativeBottom,listBottom,safeBottom,height);
    int newMargin=Math.max(0,hostBottom-safeBottom);
    if(applied!=wanted||list.getPaddingBottom()!=wanted||margin!=newMargin){
     hide();applied=wanted;list.setPaddingRelative(list.getPaddingStart(),list.getPaddingTop(),list.getPaddingEnd(),wanted);
     FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)footer.getLayoutParams();lp.bottomMargin=newMargin;footer.setLayoutParams(lp);margin=newMargin;
     return true;
    }
    // Do not display on intermediate rows; only when the list cannot scroll any further down.
    boolean atEnd=!list.canScrollVertically(1)&&!list.isLayoutRequested()&&list.getAlpha()>.01f;
    int visibility=atEnd?View.VISIBLE:View.INVISIBLE;
    if(footer.getVisibility()!=visibility)footer.setVisibility(visibility);
   }catch(Throwable t){hide();}
   return true;
  }
  @Override public void onViewAttachedToWindow(View v){}
  @Override public void onViewDetachedFromWindow(View v){
   if(observer!=null&&observer.isAlive())observer.removeOnPreDrawListener(this);
   else if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnPreDrawListener(this);
   v.removeOnAttachStateChangeListener(this);
   if(applied>=0&&v.getPaddingBottom()==applied)v.setPaddingRelative(v.getPaddingStart(),v.getPaddingTop(),v.getPaddingEnd(),nativeBottom);
   hide();host.post(()->{if(footer.getParent()==host)host.removeView(footer);});
  }
 }
}
