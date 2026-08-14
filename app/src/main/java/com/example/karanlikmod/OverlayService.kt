package com.example.karanlikmod

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var invertView: View? = null
    private var dimView: View? = null

    companion object {
        const val ACTION_INVERT_ON =
            "com.example.karanlikmod.action.INVERT_ON"

        const val ACTION_INVERT_OFF =
            "com.example.karanlikmod.action.INVERT_OFF"

        const val ACTION_DIM_ON =
            "com.example.karanlikmod.action.DIM_ON"

        const val ACTION_DIM_OFF =
            "com.example.karanlikmod.action.DIM_OFF"

        const val ACTION_DIM_SET =
            "com.example.karanlikmod.action.DIM_SET"

        const val EXTRA_DIM_PERCENT =
            "com.example.karanlikmod.extra.DIM_PERCENT"

        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIF_ID = 1001
        private const val TAG = "OverlayService"

        private const val DEFAULT_DIM_PERCENT = 35
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when (intent?.action) {

            ACTION_INVERT_ON -> {
                addInvertOverlay()
            }

            ACTION_INVERT_OFF -> {
                removeInvertOverlay()
            }

            ACTION_DIM_ON -> {
                addDimOverlay()
            }

            ACTION_DIM_OFF -> {
                removeDimOverlay()
            }

            ACTION_DIM_SET -> {
                val percent = intent.getIntExtra(
                    EXTRA_DIM_PERCENT,
                    DEFAULT_DIM_PERCENT
                ).coerceIn(0, 100)

                setDimPercent(percent)
            }
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

    // --------------------------------------------------
    // Renk tersleme
    // --------------------------------------------------

    private fun addInvertOverlay() {
        if (invertView != null) return

        // Şimdilik renk tersleme özelliğine dokunmuyoruz.
        // Daha sonra ayrı olarak gerçek bir invert yöntemi
        // üzerinde çalışacağız.
        Log.d(TAG, "Invert overlay requested")
    }

    private fun removeInvertOverlay() {
        invertView?.let { view ->
            runCatching {
                windowManager?.removeView(view)
            }.onFailure {
                Log.w(TAG, "Failed to remove invert overlay", it)
            }

            invertView = null
        }
    }

    // --------------------------------------------------
    // Karartma
    // --------------------------------------------------

    private fun addDimOverlay() {
        if (dimView != null) return

        val percent = getSavedDimPercent()

        val view = View(this)

        dimView = view

        applyDimPercent(view, percent)

        runCatching {
            windowManager?.addView(
                view,
                buildOverlayParams()
            )
        }.onFailure {
            dimView = null
            Log.e(TAG, "Failed to add dim overlay", it)
        }
    }

    private fun removeDimOverlay() {
        dimView?.let { view ->
            runCatching {
                windowManager?.removeView(view)
            }.onFailure {
                Log.w(TAG, "Failed to remove dim overlay", it)
            }

            dimView = null
        }
    }

    private fun setDimPercent(percent: Int) {

        val safePercent = percent.coerceIn(0, 100)

        saveDimPercent(safePercent)

        if (safePercent == 0) {
            /*
             * %0 karartma:
             * Aktif bir overlay tutmak yerine tamamen kaldırıyoruz.
             */
            if (dimView != null) {
                removeDimOverlay()
            }

            return
        }

        if (dimView == null) {
            addDimOverlay()
        }

        dimView?.let {
            applyDimPercent(it, safePercent)
        }
    }

    private fun applyDimPercent(
        view: View,
        percent: Int
    ) {
        /*
         * %0  -> alpha 0
         * %100 -> alpha 255
         */
        val alpha = ((percent / 100f) * 255f)
            .toInt()
            .coerceIn(0, 255)

        val color = alpha shl 24

        view.setBackgroundColor(color)
    }

    // --------------------------------------------------
    // Ayarların saklanması
    // --------------------------------------------------

    private fun getSavedDimPercent(): Int {
        return getSharedPreferences(
            "settings",
            MODE_PRIVATE
        ).getInt(
            "dim_percent",
            DEFAULT_DIM_PERCENT
        ).coerceIn(0, 100)
    }

    private fun saveDimPercent(percent: Int) {
        getSharedPreferences(
            "settings",
            MODE_PRIVATE
        ).edit()
            .putInt("dim_percent", percent)
            .apply()
    }

    // --------------------------------------------------
    // Window
    // --------------------------------------------------

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

    // --------------------------------------------------
    // Foreground service
    // --------------------------------------------------

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

            val manager =
                getSystemService(NotificationManager::class.java)

            manager?.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    getString(R.string.app_title)
                )
                .setContentText(
                    "Ekran filtresi aktif"
                )
                .setSmallIcon(
                    android.R.drawable.ic_menu_view
                )
                .setOngoing(true)
                .build()

        startForeground(
            NOTIF_ID,
            notification
        )
    }
}
