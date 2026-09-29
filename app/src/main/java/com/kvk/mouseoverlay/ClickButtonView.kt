package com.kvk.mouseoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class ClickButtonView(
    context: Context,
    private val label: String,
    private val onClick: () -> Unit,
    private val onDrag: (Int, Int) -> Unit
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
    private val moveRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFAA33")
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    var size: Int = 150
        set(value) { field = value; requestLayout() }

    private var activeId = -1
    private var adjusting = false
    private var lastX = 0f
    private var lastY = 0f

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = cx - 6f
        canvas.drawCircle(cx, cy, r, fill)
        if (adjusting) {
            canvas.drawCircle(cx, cy, r - 3f, moveRing)
            canvas.drawCircle(cx, cy, r - 13f, moveRing)
        } else {
            canvas.drawCircle(cx, cy, r, ring)
        }
        val fm = textPaint.fontMetrics
        val offset = -(fm.ascent + fm.descent) / 2f
        canvas.drawText(label, cx, cy + offset, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeId = event.getPointerId(0)
                adjusting = Prefs.isAdjustMode(context)
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activeId)
                if (idx < 0) return false
                if (adjusting) {
                    val moveX = event.getX(idx) - lastX
                    val moveY = event.getY(idx) - lastY
                    if (abs(moveX) > 0.5f || abs(moveY) > 0.5f) {
                        onDrag(moveX.toInt(), moveY.toInt())
                        lastX = event.getX(idx)
                        lastY = event.getY(idx)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!adjusting) {
                    postDelayed(clickRunnable, 30L)
                }
                adjusting = false
                activeId = -1
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                adjusting = false
                activeId = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val clickRunnable = Runnable { onClick() }
}
