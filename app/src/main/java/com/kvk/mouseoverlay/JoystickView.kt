package com.kvk.mouseoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

class JoystickView(context: Context, private val onMove: (Float, Float) -> Unit) : View(context) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55222222")
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC4488FF")
        style = Paint.Style.FILL
    }

    var size: Int = 220
        set(value) { field = value; requestLayout() }

    private var knobX = 0f
    private var knobY = 0f
    private var activePointerId = -1
    private var resizeMode = false

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = cx - 6f
        canvas.drawCircle(cx, cy, radius, basePaint)
        canvas.drawCircle(cx, cy, radius, ringPaint)
        val knobRadius = radius * 0.35f
        val kx = (cx + knobX).coerceIn(knobRadius, width - knobRadius)
        val ky = (cy + knobY).coerceIn(knobRadius, height - knobRadius)
        canvas.drawCircle(kx, ky, knobRadius, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                postDelayed(resizeRunnable, 600L)
                if (!resizeMode) updateKnob(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activePointerId)
                if (idx < 0) return false
                if (resizeMode) {
                    val newSize = (event.x * 2f).toInt().coerceIn(120, 500)
                    size = newSize
                    Prefs.setJoystickSize(context, newSize)
                } else {
                    updateKnob(event.getX(idx), event.getY(idx))
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(resizeRunnable)
                resizeMode = false
                knobX = 0f; knobY = 0f
                onMove(0f, 0f)
                invalidate()
                activePointerId = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val resizeRunnable = Runnable {
        resizeMode = true
        knobX = 0f; knobY = 0f
        onMove(0f, 0f)
        invalidate()
    }

    private fun updateKnob(x: Float, y: Float) {
        val cx = width / 2f
        val cy = height / 2f
        var dx = x - cx
        var dy = y - cy
        val maxR = cx - 6f
        val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (d > maxR) {
            dx = dx / d * maxR
            dy = dy / d * maxR
        }
        knobX = dx; knobY = dy
        onMove(dx / maxR, dy / maxR)
        invalidate()
    }
}
