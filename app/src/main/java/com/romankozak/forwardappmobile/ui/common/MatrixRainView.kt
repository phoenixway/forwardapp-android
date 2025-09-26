// Enhanced MatrixRainView.kt with smoother transitions
package com.romankozak.forwardappmobile.ui.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import kotlin.math.pow
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
    
    // Dynamic colors based on theme
    private var primaryColor = Color.parseColor("#00FF41")
    private var secondaryColor = Color.parseColor("#008F11")
    private var fadeColor = Color.parseColor("#003300")
    private var glowColor = Color.parseColor("#40FF80")
    private var backgroundColor = Color.BLACK
    
    private var columnWidth = 0f
    private var fontSize = 0f
    private var numColumns = 0
    
    // Enhanced animation properties for smoother transitions
    private var alpha = 1f
    private var backgroundAlpha = 1f
    private var fadeStartTime = 0L
    private val fadeDuration = 500L // Longer fade duration
    private var isPreFadeStarted = false
    private var lastFrameTime = 0L
    
    // Performance optimization
    private val targetFPS = 30
    private val frameInterval = 1000L / targetFPS
    
    data class MatrixDrop(
        var x: Float,
        var y: Float,
        var speed: Float,
        var chars: MutableList<Char> = mutableListOf(),
        var age: Int = 0,
        val maxLength: Int = Random.nextInt(8, 25),
        var leadingChar: Char = getRandomChar(),
        var opacity: Float = 1f // Individual drop opacity for smoother fading
    ) {
        companion object {
            fun getRandomChar(): Char {
                val chars = "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                return chars.random()
            }
        }
        
        fun update(deltaTime: Float) {
            y += speed * deltaTime / 16f // Normalize for ~60fps baseline
            age++
            
            // Add new character at the front occasionally
            if (age % 3 == 0 && chars.size < maxLength) {
                chars.add(0, leadingChar)
                leadingChar = getRandomChar()
            }
            
            // Randomly change some characters
            if (Random.nextFloat() < 0.08f) {
                leadingChar = getRandomChar()
            }
            
            // Randomly change characters in the trail
            chars.forEachIndexed { index, _ ->
                if (Random.nextFloat() < 0.03f) {
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
        
        detectAndSetTheme()
        setupPaints()
        initializeDrops()
        startAnimation()
    }
    
    private fun detectAndSetTheme() {
        val isDarkMode = when (context.resources.configuration.uiMode and 
                              android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> false
            else -> {
                val typedValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true)
                val backgroundColor = typedValue.data
                val red = Color.red(backgroundColor)
                val green = Color.green(backgroundColor)
                val blue = Color.blue(backgroundColor)
                val brightness = (red + green + blue) / 3
                brightness < 128
            }
        }
        
        if (isDarkMode) {
            primaryColor = Color.parseColor("#00FF41")
            secondaryColor = Color.parseColor("#008F11")
            fadeColor = Color.parseColor("#003300")
            glowColor = Color.parseColor("#40FF80")
            backgroundColor = Color.parseColor("#000000")
        } else {
            primaryColor = Color.parseColor("#2E7D32")
            secondaryColor = Color.parseColor("#4CAF50")
            fadeColor = Color.parseColor("#A5D6A7")
            glowColor = Color.parseColor("#1B5E20")
            backgroundColor = Color.parseColor("#F8F9FA")
        }
    }
    
    private fun setupPaints() {
        paint.apply {
            textSize = fontSize
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
            isFilterBitmap = true // Smooth scaling
            isDither = true // Better color gradients
        }
        
        backgroundPaint.apply {
            color = backgroundColor
            alpha = if (backgroundColor == Color.BLACK) 200 else 240
        }
        
        glowPaint.apply {
            textSize = fontSize
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
            isFilterBitmap = true
            isDither = true
            
            if (backgroundColor == Color.BLACK) {
                setShadowLayer(12f, 0f, 0f, glowColor) // Stronger glow for dark theme
            } else {
                setShadowLayer(6f, 0f, 0f, glowColor)
            }
        }
    }
    
    private fun initializeDrops() {
        drops.clear()
        
        // Create drops with more natural staggered start times
        for (i in 0 until numColumns) {
            if (Random.nextFloat() < 0.6f) {
                drops.add(
                    MatrixDrop(
                        x = i * columnWidth,
                        y = -Random.nextInt(200, 800).toFloat(), // More varied start positions
                        speed = Random.nextFloat() * 4f + 1.5f // Slightly faster base speed
                    )
                )
            }
        }
        
        // Add some random mid-screen drops for more dynamic appearance
        repeat(numColumns / 4) {
            drops.add(
                MatrixDrop(
                    x = Random.nextInt(numColumns).toFloat() * columnWidth,
                    y = Random.nextInt(height / 3).toFloat(),
                    speed = Random.nextFloat() * 2f + 1f
                )
            )
        }
    }
    
    private fun startAnimation() {
        animationJob?.cancel()
        lastFrameTime = System.currentTimeMillis()
        
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isAttachedToWindow) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastFrameTime
                
                if (deltaTime >= frameInterval) {
                    updateAnimation(deltaTime.toFloat())
                    invalidate()
                    lastFrameTime = currentTime
                }
                
                delay(8) // Small delay to prevent excessive CPU usage
            }
        }
    }
    
    private fun updateAnimation(deltaTime: Float) {
        // Update existing drops with delta time for smooth movement
        drops.forEach { drop ->
            drop.update(deltaTime)
        }
        
        // Remove drops that have left the screen
        drops.removeAll { it.shouldRemove(height.toFloat()) }
        
        // Add new drops with controlled frequency
        if (Random.nextFloat() < 0.25f && drops.size < numColumns * 1.8f) {
            val column = Random.nextInt(numColumns)
            drops.add(
                MatrixDrop(
                    x = column * columnWidth,
                    y = -Random.nextInt(50, 300).toFloat(),
                    speed = Random.nextFloat() * 4f + 1.5f
                )
            )
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Enhanced fade effect with easing
        if (fadeStartTime > 0) {
            val elapsed = System.currentTimeMillis() - fadeStartTime
            val progress = (elapsed.toFloat() / fadeDuration).coerceIn(0f, 1f)
            
            // Ease-out function for smoother fade
            val easedProgress = 1f - (1f - progress) * (1f - progress)
            alpha = 1f - easedProgress
            
            // Fade background separately for more natural transition
            backgroundAlpha = (1f - progress * 0.7f).coerceIn(0f, 1f)
            
            if (alpha <= 0f) {
                visibility = GONE
                return
            }
        }
        
        // Draw background with smooth alpha transition
        backgroundPaint.alpha = (if (backgroundColor == Color.BLACK) 200 else 240) * backgroundAlpha.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // Draw matrix drops
        drops.forEach { drop ->
            drawMatrixDrop(canvas, drop)
        }
    }
    
    private fun drawMatrixDrop(canvas: Canvas, drop: MatrixDrop) {
        val chars = drop.chars
        if (chars.isEmpty()) return
        
        val dropAlpha = alpha * drop.opacity
        
        // Draw the leading character with enhanced glow
        glowPaint.color = glowColor
        glowPaint.alpha = (180 * dropAlpha).toInt().coerceIn(0, 255)
        canvas.drawText(
            drop.leadingChar.toString(),
            drop.x,
            drop.y,
            glowPaint
        )
        
        // Draw the leading character again for solid appearance
        paint.color = primaryColor
        paint.alpha = (255 * dropAlpha).toInt().coerceIn(0, 255)
        canvas.drawText(
            drop.leadingChar.toString(),
            drop.x,
            drop.y,
            paint
        )
        
        // Draw the trail with smooth fading
        chars.forEachIndexed { index, char ->
            val charY = drop.y + (index + 1) * fontSize * 1.15f // Tighter spacing
            if (charY > height) return@forEachIndexed
            
            // Smoother fade calculation with exponential falloff
            val fadePercent = (index.toFloat() / chars.size.coerceAtLeast(1))
            val exponentialFade = (1f - fadePercent).toDouble().pow(1.5).toFloat()
            val charAlpha = (255 * exponentialFade * dropAlpha).toInt().coerceIn(0, 255)
            
            // Enhanced color gradient
            val color = when {
                index < 2 -> primaryColor
                index < 5 -> {
                    // Blend between primary and secondary
                    val blendFactor = (index - 2) / 3f
                    blendColors(primaryColor, secondaryColor, blendFactor)
                }
                index < 8 -> secondaryColor
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
    
    // Helper function to blend colors smoothly
    private fun blendColors(color1: Int, color2: Int, factor: Float): Int {
        val factor2 = factor.coerceIn(0f, 1f)
        val factor1 = 1f - factor2
        
        val r = (Color.red(color1) * factor1 + Color.red(color2) * factor2).toInt()
        val g = (Color.green(color1) * factor1 + Color.green(color2) * factor2).toInt()
        val b = (Color.blue(color1) * factor1 + Color.blue(color2) * factor2).toInt()
        
        return Color.rgb(r, g, b)
    }
    
    fun setTheme(isDark: Boolean) {
        if (isDark) {
            primaryColor = Color.parseColor("#00FF41")
            secondaryColor = Color.parseColor("#008F11")
            fadeColor = Color.parseColor("#003300")
            glowColor = Color.parseColor("#40FF80")
            backgroundColor = Color.parseColor("#000000")
        } else {
            primaryColor = Color.parseColor("#2E7D32")
            secondaryColor = Color.parseColor("#4CAF50")
            fadeColor = Color.parseColor("#A5D6A7")
            glowColor = Color.parseColor("#1B5E20")
            backgroundColor = Color.parseColor("#F8F9FA")
        }
        setupPaints()
        invalidate()
    }
    
    // Enhanced fade out with pre-fade preparation
    fun startFadeOut() {
        if (!isPreFadeStarted) {
            isPreFadeStarted = true
            // Optionally slow down new drop generation before fade
            CoroutineScope(Dispatchers.Main).launch {
                delay(100) // Brief pause before fade
                fadeStartTime = System.currentTimeMillis()
            }
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animationJob?.cancel()
    }
}