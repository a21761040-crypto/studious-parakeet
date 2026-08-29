package com.starwhale.uberradius;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;

public class OverlayService extends Service {
    WindowManager wm; OverlayView view;
    @Override public void onCreate(){super.onCreate();
        String ch="radius"; if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(ch,"Radius Overlay",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c);}
        Notification n=new Notification.Builder(this,ch).setContentTitle("Uber Radius Overlay").setContentText("浮動紅圈運作中").setSmallIcon(android.R.drawable.ic_menu_mylocation).build(); startForeground(1,n);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE); view=new OverlayView(this); int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT); wm.addView(view,p);
    }
    @Override public int onStartCommand(Intent i,int flags,int id){if(view!=null)view.km=i.getFloatExtra("radius",3f); return START_STICKY;}
    @Override public void onDestroy(){if(view!=null)wm.removeView(view);super.onDestroy();}
    @Override public android.os.IBinder onBind(Intent i){return null;}
    static class OverlayView extends View { Paint p=new Paint(3); float km=3; OverlayView(Context c){super(c);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.RED);}
        protected void onDraw(Canvas c){super.onDraw(c);float cx=getWidth()/2f,cy=getHeight()/2f;float r=Math.min(getWidth(),getHeight())*.28f;p.setColor(Color.argb(210,220,0,0));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);c.drawCircle(cx,cy,r,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb(190,220,0,0));c.drawCircle(cx,cy,8,p);p.setTextSize(38);p.setColor(Color.RED);c.drawText(String.format("%.1f km",km),cx-r,cy-r-18,p);invalidate();}
    }
}
