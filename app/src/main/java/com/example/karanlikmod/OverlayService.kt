package com.example.karanlikmod

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

/**
 * Ekranın üzerine iki bağımsız katman (overlay) çizen servis:
 *  1) Renk tersleme katmanı: beyaz zemin + DIFFERENCE blend modu -> altındaki
 *     her rengi tersine çevirir (beyaz zemin -> siyah, siyah metin -> beyaz vb).
 *  2) Karartma katmanı: yarı saydam siyah -> ekstra göz yorgunluğu azaltma.
 *
 * İkisi de SYSTEM_ALERT_WINDOW izniyle çalışır, özel/gizli bir izin gerekmez.
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

        // Karartma katmanının opaklığı (0x00 şeffaf - 0xFF tam siyah arası).
        // Şu an ~%35 siyah. Daha fazla/az karartma istenirse bu değer değiştirilebilir.
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

        val view = View(this)
        view.setBackgroundColor(0xFFFFFFFF.toInt())

        // GPU katmanı üzerinde DIFFERENCE karışım modu uygulayarak
        // altındaki içerik ile bu beyaz katmanı ters çevirecek şekilde birleştir.
        val xfermodePaint = Paint()
        xfermodePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DIFFERENCE)
        view.setLayerType(View.LAYER_TYPE_HARDWARE, xfermodePaint)

        invertView = view
        windowManager?.addView(view, buildOverlayParams())
    }

    private fun removeInvertOverlay() {
        invertView?.let {
            runCatching { windowManager?.removeView(it) }
            invertView = null
        }
    }

    // ---- Karartma katmanı ----

    private fun addDimOverlay() {
        if (dimView != null) return

        val view = View(this)
        view.setBackgroundColor((DIM_ALPHA shl 24)) // yarı saydam siyah

        dimView = view
        windowManager?.addView(view, buildOverlayParams())
    }

    private fun removeDimOverlay() {
        dimView?.let {
            runCatching { windowManager?.removeView(it) }
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
