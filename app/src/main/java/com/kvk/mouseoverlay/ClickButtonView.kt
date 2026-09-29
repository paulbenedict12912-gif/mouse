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
        strokeWidth = 6f
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
    private var dragMode = false
    private var startX = 0f
    private var startY = 0f
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
        if (dragMode) {
            canvas.drawCircle(cx, cy, r, moveRing)
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
                startX = event.x
                startY = event.y
                lastX = event.x
                lastY = event.y
                dragMode = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val idx = event.findPointerIndex(activeId)
                if (idx < 0) return false
                val dx = event.getX(idx) - startX
                val dy = event.getY(idx) - startY
                // If finger moves more than 15px, switch into drag mode
                if (!dragMode && (abs(dx) > 15f || abs(dy) > 15f)) {
                    dragMode = true
                    invalidate()
                }
                if (dragMode) {
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
                removeCallbacks(clickRunnable)
                if (!dragMode) {
                    // Fire the click on a short delay so it happens after touch release
                    postDelayed(clickRunnable, 30L)
                }
                dragMode = false
                activeId = -1
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragMode = false
                activeId = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val clickRunnable = Runnable { onClick() }
}
