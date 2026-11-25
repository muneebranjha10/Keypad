package io.rootvault.glassdialer;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
public final class MainActivity extends Activity {
 @Override public void onCreate(Bundle b) {
  super.onCreate(b); LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(32,40,32,32); root.setGravity(Gravity.CENTER_VERTICAL); root.setBackgroundColor(0xff101215);
  TextView title=new TextView(this); title.setText("Glass Dialer"); title.setTextColor(Color.WHITE); title.setTextSize(32); root.addView(title);
  TextView info=new TextView(this); info.setText("0.4 BETA\n\nCalculator-style keypad • Draggable glass tabs\n\n1. Enable Glass Dialer in LSPosed.\n2. Select Samsung Phone (com.samsung.android.dialer).\n3. Force stop Phone and reopen it.\n\nIf anything is wrong, disable the module and reopen Phone.\n\nThis screen does not verify hook status. See LSPosed logs for GlassDialer messages."); info.setTextColor(0xffc7d6d3); info.setTextSize(17); info.setPadding(0,24,0,0); root.addView(info); setContentView(root);
 }
}