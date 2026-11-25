package io.rootvault.glassdialer;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b); LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(32,40,32,32); root.setGravity(Gravity.CENTER_VERTICAL); root.setBackgroundColor(0xff000000);
  TextView title=new TextView(this); title.setText("Glass Dialer"); title.setTextColor(Color.WHITE); title.setTextSize(32); root.addView(title);
  TextView info=new TextView(this); info.setText("1.4 LARGER KEYPAD + NATIVE TAB SWIPE\n\nWhole Samsung Phone app • pure black surfaces\n\n1. Turn on Samsung Dark mode.\n2. Enable Glass Dialer in LSPosed.\n3. Select Samsung Phone (com.samsung.android.dialer).\n4. Force stop Phone and reopen it.\n\nCalculator-style number keys and individual digit entry are enabled. Native Samsung tabs: drag the selected tab to switch. Red-only backspace feedback. Native call functionality remains unchanged by design. Disable the module if anything is wrong."); info.setTextColor(0xffc7d6d3); info.setTextSize(17); info.setPadding(0,24,0,0); root.addView(info); setContentView(root);
 }
}