package com.romankozak.forwardappmobile.ui.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import kotlin.random.Random

class MatrixRainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint()
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var drops = mutableListOf<MatrixDrop>()
    private var animationJob: Job? = null
    
    // Matrix characters - mix of Katakana, Latin, and numbers
    private val matrixChars = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    
    private val primaryColor = Color.parseColor("#00FF41") // Classic Matrix green
    private val secondaryColor = Color.parseColor("#008F11") // Darker green
    private val fadeColor = Color.parseColor("#003300") // Very dark green
    private val glowColor = Color.parseColor("#40FF80") // Bright green glow
    
    private var columnWidth = 0f
    private var fontSize = 0f
    private var numColumns = 0
    
    // Animation properties
    private var alpha = 1f
    private var fadeStartTime = 0L
    private val fadeDuration = 300L
    
    data class MatrixDrop(
        var x: Float,
        var y: Float,
        var speed: Float,
        var chars: MutableList<Char> = mutableListOf(),
        var age: Int = 0,
        val maxLength: Int = Random.nextInt(8, 25),
        var leadingChar: Char = getRandomChar()
    ) {
        companion object {
            fun getRandomChar(): Char {
                val chars = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                return chars.random()
            }
        }
        
        fun update() {
            y += speed
            age++
            
            // Add new character at the front occasionally
            if (age % 3 == 0 && chars.size < maxLength) {
                chars.add(0, leadingChar)
                leadingChar = getRandomChar()
            }
            
            // Randomly change some characters
            if (Random.nextFloat() < 0.1f) {
                leadingChar = getRandomChar()
            }
            
            // Randomly change characters in the trail
            chars.forEachIndexed { index, _ ->
                if (Random.nextFloat() < 0.05f) {
                    chars[index] = getRandomChar()
                }
            }
        }
        
        fun shouldRemove(screenHeight: Float): Boolean {
            return y - (chars.size * 30) > screenHeight
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Calculate optimal font size and columns
        fontSize = (w / 25f).coerceIn(16f, 24f)
        columnWidth = fontSize * 0.8f
        numColumns = (w / columnWidth).toInt()
        
        setupPaints()
        initializeDrops()
        startAnimation()
    }
    
    private fun setupPaints() {
        paint.apply {
            textSize = fontSize
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
        }
        
        backgroundPaint.apply {
            color = Color.BLACK
            alpha = 200 // Slightly transparent for layering effect
        }
        
        glowPaint.apply {
            textSize = fontSize
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
            setShadowLayer(8f, 0f, 0f, glowColor)
        }
    }
    
    private fun initializeDrops() {
        drops.clear()
        
        // Create drops with staggered start times
        for (i in 0 until numColumns) {
            if (Random.nextFloat() < 0.7f) { // Not every column starts immediately
                drops.add(
                    MatrixDrop(
                        x = i * columnWidth,
                        y = -Random.nextInt(100, 500).toFloat(), // Staggered start
                        speed = Random.nextFloat() * 3f + 2f // Variable speed
                    )
                )
            }
        }
        
        // Add some random drops throughout the screen
        repeat(numColumns / 3) {
            drops.add(
                MatrixDrop(
                    x = Random.nextInt(numColumns).toFloat() * columnWidth,
                    y = Random.nextInt(height / 2).toFloat(),
                    speed = Random.nextFloat() * 2f + 1f
                )
            )
        }
    }
    
    private fun startAnimation() {
        animationJob?.cancel()
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isAttachedToWindow) {
                updateAnimation()
                invalidate()
                delay(50) // ~20 FPS for smooth but not too intensive animation
            }
        }
    }
    
    private fun updateAnimation() {
        // Update existing drops
        drops.forEach { drop ->
            drop.update()
        }
        
        // Remove drops that have left the screen
        drops.removeAll { it.shouldRemove(height.toFloat()) }
        
        // Add new drops randomly
        if (Random.nextFloat() < 0.3f && drops.size < numColumns * 2) {
            val column = Random.nextInt(numColumns)
            drops.add(
                MatrixDrop(
                    x = column * columnWidth,
                    y = -Random.nextInt(50, 200).toFloat(),
                    speed = Random.nextFloat() * 3f + 2f
                )
            )
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply fade effect if needed
        if (fadeStartTime > 0) {
            val elapsed = System.currentTimeMillis() - fadeStartTime
            alpha = 1f - (elapsed.toFloat() / fadeDuration).coerceIn(0f, 1f)
            if (alpha <= 0f) {
                visibility = GONE
                return
            }
        }
        
        // Draw black background with some transparency for layering effect
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw matrix drops
        drops.forEach { drop ->
            drawMatrixDrop(canvas, drop)
        }
    }
    
    private fun drawMatrixDrop(canvas: Canvas, drop: MatrixDrop) {
        val chars = drop.chars
        if (chars.isEmpty()) return
        
        // Draw the leading character with bright color and glow
        glowPaint.color = glowColor
        glowPaint.alpha = (255 * alpha).toInt()
        canvas.drawText(
            drop.leadingChar.toString(),
            drop.x,
            drop.y,
            glowPaint
        )
        
        // Draw the leading character again without glow for solid appearance
        paint.color = primaryColor
        paint.alpha = (255 * alpha).toInt()
        canvas.drawText(
            drop.leadingChar.toString(),
            drop.x,
            drop.y,
            paint
        )
        
        // Draw the trail with fading effect
        chars.forEachIndexed { index, char ->
            val charY = drop.y + (index + 1) * fontSize * 1.2f
            if (charY > height) return@forEachIndexed
            
            // Calculate fade based on position in trail
            val fadePercent = (index.toFloat() / chars.size.coerceAtLeast(1))
            val charAlpha = (255 * (1f - fadePercent) * alpha).toInt().coerceIn(0, 255)
            
            // Use different colors for different parts of the trail
            val color = when {
                index < 3 -> primaryColor
                index < 6 -> secondaryColor
                else -> fadeColor
            }
            
            paint.color = color
            paint.alpha = charAlpha
            
            canvas.drawText(
                char.toString(),
                drop.x,
                charY,
                paint
            )
        }
    }
    
    fun startFadeOut() {
        fadeStartTime = System.currentTimeMillis()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationJob?.cancel()
    }
}