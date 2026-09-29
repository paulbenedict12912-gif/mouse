package com.kvk.mouseoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    companion object {
        var instance: OverlayService? = null
        fun refresh() { instance?.rebuild() }
    }

    private lateinit var wm: WindowManager
    private var joystickView: JoystickView? = null
    private var cursorView: CursorView? = null
    private var leftBtn: ClickButtonView? = null
    private var rightBtn: ClickButtonView? = null

    private val handler = Handler(Looper.getMainLooper())
    private var cursorX = 200f
    private var cursorY = 400f
    private var vx = 0f
    private var vy = 0f
    private val MAX_SPEED = 22f

    private val ticker = object : Runnable {
        override fun run() {
            if (vx != 0f || vy != 0f) {
                cursorX += vx
                cursorY += vy
                val dm = resources.displayMetrics
                if (cursorX < 0f) cursorX = 0f
                if (cursorY < 0f) cursorY = 0f
                if (cursorX > dm.widthPixels) cursorX = dm.widthPixels.toFloat()
                if (cursorY > dm.heightPixels) cursorY = dm.heightPixels.toFloat()
                cursorView?.updatePosition(cursorX, cursorY)
            }
            handler.postDelayed(this, 16L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundCompat()
        rebuild()
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startForegroundCompat() {
        val chId = "mouse_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(chId, "Mouse Overlay",
                NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
        val notif: Notification = NotificationCompat.Builder(this, chId)
            .setContentTitle("Mouse Overlay")
            .setContentText("Running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notif)
        }
    }

    fun rebuild() {
        joystickView?.let { safeRemove(it) }
        cursorView?.let { safeRemove(it) }
        leftBtn?.let { safeRemove(it) }
        rightBtn?.let { safeRemove(it) }

        val joySize = Prefs.getJoystickSize(this)
        val curSize = Prefs.getCursorSize(this)
        val btnSize = Prefs.getButtonSize(this)

        joystickView = JoystickView(this) { dx, dy ->
            vx = dx * MAX_SPEED
            vy = dy * MAX_SPEED
        }.also {
            it.size = joySize
            safeAdd(it, baseParams(joySize, joySize,
                Gravity.BOTTOM or Gravity.START, 60, 160))
        }

        cursorView = CursorView(this).also {
            it.size = curSize
            val p = WindowManager.LayoutParams(
                curSize, curSize,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = cursorX.toInt()
                y = cursorY.toInt()
            }
            safeAdd(it, p)
        }

        leftBtn = ClickButtonView(this, "L") { doClick(false) }.also {
            it.size = btnSize
            safeAdd(it, baseParams(btnSize, btnSize,
                Gravity.BOTTOM or Gravity.END, 300, 160))
        }

        rightBtn = ClickButtonView(this, "R") { doClick(true) }.also {
            it.size = btnSize
            safeAdd(it, baseParams(btnSize, btnSize,
                Gravity.BOTTOM or Gravity.END, 60, 160))
        }
    }

    private fun doClick(right: Boolean) {
        MouseAccessibilityService.instance?.tap(cursorX.toInt(), cursorY.toInt(), right)
    }

    private fun baseParams(w: Int, h: Int, gravity: Int, x: Int, y: Int) =
        WindowManager.LayoutParams(
            w, h,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = x
            this.y = y
        }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun safeAdd(v: android.view.View, p: WindowManager.LayoutParams) {
        try { wm.addView(v, p) } catch (_: Exception) {}
    }

    private fun safeRemove(v: android.view.View) {
        try { wm.removeView(v) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(ticker)
        joystickView?.let { safeRemove(it) }
        cursorView?.let { safeRemove(it) }
        leftBtn?.let { safeRemove(it) }
        rightBtn?.let { safeRemove(it) }
        instance = null
    }
}
