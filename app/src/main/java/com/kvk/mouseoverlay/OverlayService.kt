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
import android.view.View
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
    private var cursorX = 300f
    private var cursorY = 500f
    private var vx = 0f
    private var vy = 0f
    private val MAX_SPEED = 22f
    private var screenW = 1080
    private var screenH = 1920

    // Auto-scroll state
    private var scrollCooldown = 0L

    private val ticker = object : Runnable {
        override fun run() {
            if (vx != 0f || vy != 0f) {
                cursorX += vx
                cursorY += vy
                var clamped = false
                if (cursorX < 0f) { cursorX = 0f; clamped = true }
                if (cursorY < 0f) { cursorY = 0f; clamped = true }
                if (cursorX > screenW - 20) { cursorX = (screenW - 20).toFloat(); clamped = true }
                if (cursorY > screenH - 20) { cursorY = (screenH - 20).toFloat(); clamped = true }
                cursorView?.updatePosition(cursorX, cursorY)

                // Auto-scroll when stuck at edge and still pushing
                val now = System.currentTimeMillis()
                if (clamped && now - scrollCooldown > 120) {
                    scrollCooldown = now
                    doScroll(vx, vy)
                }
            }
            handler.postDelayed(this, 16L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics
        screenW = dm.widthPixels
        screenH = dm.heightPixels
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

        // Joystick position: relative to bottom-left corner
        val joyX = Prefs.getJoyX(this)
        val joyY = Prefs.getJoyY(this)

        joystickView = JoystickView(this,
            { dx, dy -> vx = dx * MAX_SPEED; vy = dy * MAX_SPEED },
            { dX, dY ->
                val p = joystickView?.layoutParams as? WindowManager.LayoutParams
                if (p != null) {
                    p.x += dX; p.y += dY
                    try { wm.updateViewLayout(joystickView, p) } catch (_: Exception) {}
                    Prefs.setJoyPos(this, p.x, p.y)
                }
            }
        ).also {
            it.size = joySize
            safeAdd(it, baseParams(joySize, joySize, joyX, joyY))
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

        val leftX = Prefs.getLeftX(this)
        val leftY = Prefs.getLeftY(this)

        leftBtn = ClickButtonView(this, "L",
            { doClick(false) },
            { dX, dY ->
                val p = leftBtn?.layoutParams as? WindowManager.LayoutParams
                if (p != null) {
                    p.x += dX; p.y += dY
                    try { wm.updateViewLayout(leftBtn, p) } catch (_: Exception) {}
                    Prefs.setLeftPos(this, p.x, p.y)
                }
            }
        ).also {
            it.size = btnSize
            safeAdd(it, baseParams(btnSize, btnSize, leftX, leftY))
        }

        val rightX = Prefs.getRightX(this)
        val rightY = Prefs.getRightY(this)

        rightBtn = ClickButtonView(this, "R",
            { doClick(true) },
            { dX, dY ->
                val p = rightBtn?.layoutParams as? WindowManager.LayoutParams
                if (p != null) {
                    p.x += dX; p.y += dY
                    try { wm.updateViewLayout(rightBtn, p) } catch (_: Exception) {}
                    Prefs.setRightPos(this, p.x, p.y)
                }
            }
        ).also {
            it.size = btnSize
            safeAdd(it, baseParams(btnSize, btnSize, rightX, rightY))
        }
    }

    private fun doClick(right: Boolean) {
        // Click at cursor's top-left (matches visual tip since CursorView draws from 0,0)
        val tipX = cursorX.toInt()
        val tipY = cursorY.toInt()
        MouseAccessibilityService.instance?.tap(tipX, tipY, right)
    }

    private fun doScroll(dx: Float, dy: Float) {
        val svc = MouseAccessibilityService.instance ?: return
        val cx = cursorX.toInt()
        val cy = cursorY.toInt()
        // Determine dominant axis
        if (Math.abs(dy) > Math.abs(dx)) {
            if (dy > 0) svc.swipe(cx, cy, cx, cy - 300, 200L)
            else svc.swipe(cx, cy, cx, cy + 300, 200L)
        } else {
            if (dx > 0) svc.swipe(cx, cy, cx - 300, cy, 200L)
            else svc.swipe(cx, cy, cx + 300, cy, 200L)
        }
    }

    private fun baseParams(w: Int, h: Int, x: Int, y: Int) =
        WindowManager.LayoutParams(
            w, h,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = Gravity.BOTTOM or Gravity.START
            this.x = x
            this.y = y
        }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    private fun safeAdd(v: View, p: WindowManager.LayoutParams) {
        try { wm.addView(v, p) } catch (_: Exception) {}
    }

    private fun safeRemove(v: View) {
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
