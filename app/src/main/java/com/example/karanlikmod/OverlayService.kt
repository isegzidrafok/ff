package com.example.karanlikmod

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Ekranın üzerine iki bağımsız katman (overlay) çizen servis:
 *  1) Renk tersleme katmanı: API 29+ üzerinde DIFFERENCE blend modu ile
 *     oluşturulan beyaz katman.
 *  2) Karartma katmanı: yarı saydam siyah.
 *
 * ÖNEMLİ:
 * Normal bir TYPE_APPLICATION_OVERLAY penceresi, başka uygulamaların ayrı
 * Surface'larıyla doğrudan Porter-Duff kompozitleme yapamaz. Bu nedenle
 * aşağıdaki DIFFERENCE kullanımı yalnızca bu overlay yüzeyinin kendi
 * render'ında geçerlidir; başka uygulamaların ekranını gerçek anlamda
 * ters çevirdiği garanti edilemez.
 *
 * API 27-28'de BlendMode bulunmadığı için invert katmanı bu sürümlerde
 * oluşturulmaz. Karartma katmanı ise tüm minSdk sürümlerinde çalışır.
 */
class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var invertView: View? = null
    private var dimView: View? = null

    companion object {
        const val ACTION_INVERT_ON = "com.example.karanlikmod.action.INVERT_ON"
        const val ACTION_INVERT_OFF = "com.example.karanlikmod.action.INVERT_OFF"
        const val ACTION_DIM_ON = "com.example.karanlikmod.action.DIM_ON"
        const val ACTION_DIM_OFF = "com.example.karanlikmod.action.DIM_OFF"

        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIF_ID = 1001
        private const val TAG = "OverlayService"

        // Karartma katmanının opaklığı (0x00 şeffaf - 0xFF tam siyah).
        private const val DIM_ALPHA = 0x59
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INVERT_ON -> addInvertOverlay()
            ACTION_INVERT_OFF -> removeInvertOverlay()
            ACTION_DIM_ON -> addDimOverlay()
            ACTION_DIM_OFF -> removeDimOverlay()
        }

        updateForegroundState()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeInvertOverlay()
        removeDimOverlay()
        super.onDestroy()
    }

    // ---- Renk tersleme katmanı ----

    private fun addInvertOverlay() {
        if (invertView != null) return

        // BlendMode.DIFFERENCE API 29'da geldi.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.w(TAG, "Invert overlay requires API 29+; current API=${Build.VERSION.SDK_INT}")
            return
        }

        val view = View(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())

            // Paint'ın blendMode'u yalnızca bu View'ın kendi render katmanına
            // uygulanır. Başka uygulamaların Surface'ını doğrudan değiştirmez.
            val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                blendMode = BlendMode.DIFFERENCE
            }

            setLayerType(View.LAYER_TYPE_HARDWARE, blendPaint)
        }

        invertView = view
        runCatching {
            windowManager?.addView(view, buildOverlayParams())
        }.onFailure {
            invertView = null
            Log.e(TAG, "Failed to add invert overlay", it)
        }
    }

    private fun removeInvertOverlay() {
        invertView?.let { view ->
            runCatching { windowManager?.removeView(view) }
                .onFailure { Log.w(TAG, "Failed to remove invert overlay", it) }
            invertView = null
        }
    }

    // ---- Karartma katmanı ----

    private fun addDimOverlay() {
        if (dimView != null) return

        val view = View(this).apply {
            setBackgroundColor(DIM_ALPHA shl 24)
        }

        dimView = view
        runCatching {
            windowManager?.addView(view, buildOverlayParams())
        }.onFailure {
            dimView = null
            Log.e(TAG, "Failed to add dim overlay", it)
        }
    }

    private fun removeDimOverlay() {
        dimView?.let { view ->
            runCatching { windowManager?.removeView(view) }
                .onFailure { Log.w(TAG, "Failed to remove dim overlay", it) }
            dimView = null
        }
    }

    // ---- Ortak yardımcılar ----

    private fun buildOverlayParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun updateForegroundState() {
        if (invertView != null || dimView != null) {
            startForegroundWithNotification()
        } else {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ekran Filtresi",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_title))
            .setContentText("Filtre(ler) aktif olarak çalışıyor")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()

        startForeground(NOTIF_ID, notification)
    }
}

