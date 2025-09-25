package com.romankozak.forwardappmobile.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MatrixRainView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.GREEN
        textSize = 40f
        typeface = Typeface.MONOSPACE
    }

    private val symbols = ('0'..'9') + ('A'..'Z') + ('ぁ'..'ん')
    private val random = Random(System.currentTimeMillis())
    private val columns get() = if (paint.textSize > 0) width / paint.textSize.toInt() else 0
    private val drops = mutableListOf<Int>()

    private val handler = Handler(Looper.getMainLooper())
    private val invalidateRunnable = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 50)
        }
    }

    init {
        handler.post(invalidateRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (columns == 0) return

        if (drops.isEmpty()) {
            for (i in 0 until columns) {
                drops.add(random.nextInt(height))
            }
        }

        canvas.drawColor(Color.argb(150, 0, 0, 0))

        for (i in drops.indices) {
            val text = symbols[random.nextInt(symbols.size)].toString()
            val x = i * paint.textSize
            val y = drops[i].toFloat()
            canvas.drawText(text, x, y, paint)

            if (y > height && random.nextInt(100) > 90) {
                drops[i] = 0
            } else {
                drops[i] += 40
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(invalidateRunnable)
    }
}
