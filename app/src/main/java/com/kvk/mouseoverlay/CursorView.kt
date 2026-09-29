package com.kvk.mouseoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.WindowManager

class CursorView(context: Context) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    var size: Int = 60
        set(value) { field = value; requestLayout() }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.05f)
            lineTo(w * 0.12f, h * 0.85f)
            lineTo(w * 0.36f, h * 0.62f)
            lineTo(w * 0.54f, h * 0.95f)
            lineTo(w * 0.68f, h * 0.88f)
            lineTo(w * 0.51f, h * 0.56f)
            lineTo(w * 0.85f, h * 0.52f)
            close()
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, stroke)
    }

    fun updatePosition(x: Float, y: Float) {
        val params = layoutParams as? WindowManager.LayoutParams ?: return
        params.x = x.toInt()
        params.y = y.toInt()
        try {
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .updateViewLayout(this, params)
        } catch (_: Exception) {}
    }
}
