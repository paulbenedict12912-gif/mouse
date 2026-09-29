package com.kvk.mouseoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

class ClickButtonView(
    context: Context,
    private val label: String,
    private val onClick: () -> Unit
) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (label == "L") Color.parseColor("#CC224488")
                else Color.parseColor("#CC882222")
        style = Paint.Style.FILL
    }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    var size: Int = 150
        set(value) { field = value; requestLayout() }

    private var resizeMode = false
    private var activeId = -1

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = cx - 6f
        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, ring)
        val fm = textPaint.fontMetrics
        val offset = -(fm.ascent + fm.descent) / 2f
        canvas.drawText(label, cx, cy + offset, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeId = event.getPointerId(0)
                postDelayed(resizeRunnable, 600L)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (resizeMode) {
                    val newSize = (event.x * 2f).toInt().coerceIn(80, 400)
                    size = newSize
                    Prefs.setButtonSize(context, newSize)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                removeCallbacks(resizeRunnable)
                if (!resizeMode) onClick()
                resizeMode = false
                activeId = -1
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(resizeRunnable)
                resizeMode = false
                activeId = -1
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val resizeRunnable = Runnable { resizeMode = true }
}
