package io.rootvault.glassdialer;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
 private static final String CHANNEL="https://t.me/s23_hype";
 private boolean guide;
 private LinearLayout content;
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle state){super.onCreate(state);guide=state!=null&&state.getBoolean("guide");getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);show(guide);}
 @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("guide",guide);super.onSaveInstanceState(state);}
 private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER);if(bold)v.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return v;}
 private void add(View v,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);content.addView(v,p);}
 private void button(String s,boolean primary,Runnable action){TextView t=text(s,16,primary?Color.WHITE:0xffd9e9ff,true);t.setPadding(dp(16),dp(15),dp(16),dp(15));t.setMinHeight(dp(50));t.setBackground(bg(primary?0xff1678db:0xff142030,18));t.setClickable(true);t.setFocusable(true);t.setOnClickListener(v->action.run());add(t,12);}
 private void openChannel(){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(CHANNEL)));}catch(Exception e){Toast.makeText(this,CHANNEL,Toast.LENGTH_LONG).show();}}
 private void show(boolean setup){
  guide=setup;ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.BLACK);
  content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER_HORIZONTAL);content.setPadding(dp(24),dp(18),dp(24),dp(24));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
  scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());content.setPadding(dp(24)+bars.left,dp(18)+bars.top,dp(24)+bars.right,dp(24)+bars.bottom);return insets;});
  setContentView(scroll);
  add(text("GLASS DIALER  •  v1.8",12,0xff91bdf5,true),4);
  if(!setup){
   Orb orb=new Orb();LinearLayout.LayoutParams op=new LinearLayout.LayoutParams(-1,dp(210));op.topMargin=dp(8);content.addView(orb,op);
   add(text("s23_hype",34,Color.WHITE,true),0);
   add(text("Official Telegram channel",16,0xff9cc8fc,false),6);
   add(text("Discover updates, new modules and community support.",15,0xffb6bfcc,false),14);
   button("Join Telegram channel",true,this::openChannel);
   button("Continue to setup",false,()->show(true));
  }else{
   add(text("Activate Glass Dialer",28,Color.WHITE,true),24);
   add(text("Bring Calculator-style motion to Samsung Phone.",15,0xffb6bfcc,false),8);
   step("01","Open LSPosed Manager","Go to Modules and select Glass Dialer.");
   step("02","Enable the module","Turn it on and select Samsung Phone (com.samsung.android.dialer) in the scope.");
   step("03","Apply changes","Enable Samsung Dark mode. Force stop Samsung Phone, then reopen it. Reboot if LSPosed requires it.");
   step("04","Enjoy the new dialer","Calculator-style keypad, AMOLED theme and native draggable navigation.");
   button("Open Telegram channel",false,this::openChannel);
   button("Finish",true,this::finish);
  }
  add(text("Made by s23_hype",13,0xff8c9db2,false),24);
  if(ValueAnimator.areAnimatorsEnabled()){content.setAlpha(0f);content.setTranslationY(dp(12));content.animate().alpha(1f).translationY(0).setDuration(350).start();}
 }
 private void step(String n,String title,String body){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(14),dp(16),dp(14));box.setBackground(bg(0xff111923,18));TextView h=text(n+"   "+title,16,0xffe8f2ff,true);h.setGravity(Gravity.START);box.addView(h);TextView desc=text(body,14,0xffb3bfd0,false);desc.setGravity(Gravity.START);desc.setPadding(0,dp(7),0,0);box.addView(desc);add(box,12);}
 /** Lightweight 3D-inspired orb with perspective tilt, orbit, and an original paper-plane vector. */
 private final class Orb extends View{
  private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path plane=new Path();private final RectF ring=new RectF();
  private ValueAnimator anim;private float phase;
  Orb(){super(MainActivity.this);setContentDescription("Animated Telegram paper plane");setLayerType(View.LAYER_TYPE_HARDWARE,null);}
  @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(ValueAnimator.areAnimatorsEnabled()){anim=ValueAnimator.ofFloat(0,1);anim.setDuration(4400);anim.setRepeatCount(ValueAnimator.INFINITE);anim.setInterpolator(new android.view.animation.LinearInterpolator());anim.addUpdateListener(a->{phase=(float)a.getAnimatedValue();postInvalidateOnAnimation();});anim.start();}}
  @Override protected void onDetachedFromWindow(){if(anim!=null){anim.removeAllUpdateListeners();anim.cancel();anim=null;}super.onDetachedFromWindow();}
  @Override protected void onDraw(Canvas c){
   super.onDraw(c);float x=getWidth()*.5f,y=getHeight()*.5f,r=Math.min(getWidth(),getHeight())*.31f;
   if(r<1)return;float angle=phase*6.2831853f;float bob=(float)Math.sin(angle)*r*.065f;y+=bob;
   p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(x,y,r*1.65f,new int[]{0x663f9bff,0x001e54c0},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*1.65f,p);
   p.setShader(new RadialGradient(x-r*.38f,y-r*.43f,r*1.55f,new int[]{0xff9ee9ff,0xff268fee,0xff14467e,0xff08192f},new float[]{0,.35f,.76f,1},Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);
   p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1.4f));p.setColor(0x99a1d8ff);c.drawCircle(x,y,r-dp(1),p);
   int saved=c.save();c.rotate(-18+8*(float)Math.sin(angle),x,y);ring.set(x-r*1.42f,y-r*.52f,x+r*1.42f,y+r*.52f);p.setColor(0x88a7d9ff);p.setStrokeWidth(dp(1.6f));c.drawOval(ring,p);p.setStyle(Paint.Style.FILL);p.setColor(0xffa8e9ff);c.drawCircle(x+(float)Math.cos(angle)*r*1.42f,y+(float)Math.sin(angle)*r*.52f,dp(3.2f),p);c.restoreToCount(saved);
   saved=c.save();c.translate(x,y);c.rotate(-7+5*(float)Math.sin(angle));c.scale(r/64,r/64);plane.reset();plane.moveTo(-40,-7);plane.lineTo(41,-34);plane.lineTo(26,36);plane.lineTo(2,18);plane.lineTo(-13,30);plane.lineTo(-10,7);plane.close();p.setStyle(Paint.Style.FILL);p.setColor(0xfff5fcff);c.drawPath(plane,p);plane.reset();plane.moveTo(-10,7);plane.lineTo(27,-21);plane.lineTo(2,18);plane.lineTo(-13,30);plane.close();p.setColor(0xffb6dbf6);c.drawPath(plane,p);c.restoreToCount(saved);
  }
 }
}
