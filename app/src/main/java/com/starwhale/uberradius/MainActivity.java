package com.example.uberoverlay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 檢查懸浮窗與定位權限
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 1234)
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 5678)
        }

        // 開啟／關閉懸浮窗實體按鈕 UI
        val button = Button(this).apply {
            text = "開啟文心路紅區遮罩"
            setOnClickListener {
                val serviceIntent = Intent(this@MainActivity, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
        setContentView(button)
    }
}

class OverlayService : Service(), LocationListener {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var statusText: TextView
    private lateinit var locationManager: LocationManager

    // 文心路一至四段與文心南路關鍵座標節點 (Latitude, Longitude)
    private val wenxinRoute = listOf(
        Pair(24.1135, 120.6531), // 文心南路南端 (靠近大里/烏日交界)
        Pair(24.1312, 120.6472), // 文心南路與復興路口
        Pair(24.1405, 120.6438), // 文心路一段與五權西路口
        Pair(24.1561, 120.6475), // 文心路二段與臺灣大道口
        Pair(24.1725, 120.6712), // 文心路三段與中清路口
        Pair(24.1748, 120.6901)  // 文心路四段與北屯路口
    )

    // 設定安全範圍半徑 (單位：公尺)，超過此距離即轉為紅區
    private val safeThresholdMeters = 300.0

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 建立全螢幕穿透遮罩 UI
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        statusText = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA000000"))
            setPadding(32, 16, 32, 16)
            text = "定位中..."
        }

        windowManager.addView(statusText, layoutParams)
        startForegroundNotification()
        initLocation()
    }

    private fun initLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, this)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        val minDistance = getMinDistanceToRoute(location.latitude, location.longitude)
        
        if (minDistance > safeThresholdMeters) {
            // 超過文心路／文心南路範圍 -> 顯示紅區警告
            statusText.setBackgroundColor(Color.parseColor("#D32F2F")) // 紅色背景
            statusText.text = "⚠️ 已超出文心路範圍！(距離路段: ${minDistance.toInt()}m)"
        } else {
            // 在文心路主幹道安全範圍內
            statusText.setBackgroundColor(Color.parseColor("#388E3C")) // 綠色背景
            statusText.text = "✅ 處於文心路幹道範圍內 (距離: ${minDistance.toInt()}m)"
        }
    }

    // 計算當前位置到文心路多邊形折線的最短距離 (Haversine 算法)
    private fun getMinDistanceToRoute(lat: Double, lng: Double): Double {
        var minDest = Double.MAX_VALUE
        for (i in 0 until wenxinRoute.size - 1) {
            val p1 = wenxinRoute[i]
            val p2 = wenxinRoute[i + 1]
            val dist = pointToSegmentDistance(lat, lng, p1.first, p1.second, p2.first, p2.second)
            if (dist < minDest) minDest = dist
        }
        return minDest
    }

    private fun pointToSegmentDistance(px: Double, py: Double, x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(px, py, x1, y1, results)
        return results[0].toDouble()
    }

    private fun startForegroundNotification() {
        val channelId = "overlay_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId).setContentTitle("文心路定位偵測中").build()
        } else {
            Notification().apply { flags = Notification.FLAG_FOREGROUND_SERVICE }
        }
        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::statusText.isInitialized) windowManager.removeView(statusText)
        locationManager.removeUpdates(this)
    }
}
