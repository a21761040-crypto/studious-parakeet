package com.starwhale.uberradius;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    EditText radius;
    @Override public void onCreate(Bundle b){super.onCreate(b);
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40,50,40,30);
        TextView title=new TextView(this); title.setText("Uber Radius Overlay\n第一版"); title.setTextSize(26); title.setPadding(0,0,0,35); l.addView(title);
        TextView hint=new TextView(this); hint.setText("在其他 App 上顯示可調整的圓形範圍。第一版圓圈固定在螢幕中央，主要用於視覺輔助。\n\n建議先開啟定位與「顯示在其他 App 上層」。"); hint.setTextSize(16); l.addView(hint);
        radius=new EditText(this); radius.setHint("半徑（km），例如 3"); radius.setInputType(2|8192); l.addView(radius);
        Button start=new Button(this); start.setText("啟動浮動紅圈"); l.addView(start);
        Button stop=new Button(this); stop.setText("停止浮動紅圈"); l.addView(stop);
        start.setOnClickListener(v->{ if(!Settings.canDrawOverlays(this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));return;} if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},10); else launch(); });
        stop.setOnClickListener(v->stopService(new Intent(this,OverlayService.class)));
        setContentView(l);
    }
    void launch(){ float km=3f; try{km=Float.parseFloat(radius.getText().toString());}catch(Exception ignored){} Intent i=new Intent(this,OverlayService.class); i.putExtra("radius",Math.max(.1f,Math.min(20f,km))); if(Build.VERSION.SDK_INT>=26)startForegroundService(i); else startService(i); Toast.makeText(this,"浮動紅圈已啟動",Toast.LENGTH_SHORT).show(); }
}
