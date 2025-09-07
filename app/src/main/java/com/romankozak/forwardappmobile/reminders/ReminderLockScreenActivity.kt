package com.romankozak.forwardappmobile.reminders

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import kotlinx.coroutines.delay
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Елегантна темна ReminderLockScreenActivity для показу нагадувань поверх екрану блокування
 */
class ReminderLockScreenActivity : ComponentActivity() {

    companion object {
        var isActive = false
    }
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val tag = "LockScreenReminder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isActive = true // Встановлюємо прапорець при створенні

        Log.d(tag, "ReminderLockScreenActivity created")

        setupLockScreenFlags()
        //acquireWakeLock()

        // Отримання даних з Intent (підтримуємо обидва формати)
        val goalId = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID) ?: "unknown"
        val goalText = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT) ?: "Ваша мета"
        val goalDescription = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION)
        val goalEmoji = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI) ?: "🎯"
        val extraInfo = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_INFO)

        startAlarmSoundAndVibration()

        setContent {
            ForwardAppMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    DarkReminderLockScreen(
                        goalId = goalId,
                        goalText = goalText,
                        goalDescription = goalDescription,
                        goalEmoji = goalEmoji,
                        extraInfo = extraInfo,
                        onComplete = { handleComplete(goalId) },
                        onSnooze = { handleSnooze(goalId) },
                        onDismiss = { handleDismiss(goalId) }
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(tag, "Back button pressed - ignored to prevent accidental dismiss")
            }
        })

        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        Log.d(tag, "Lock screen flags set")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "GoalReminder:WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 хвилин максимум
        }
        Log.d(tag, "Wake lock acquired")
    }

    private fun startAlarmSoundAndVibration() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@ReminderLockScreenActivity, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.d(tag, "Alarm sound started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start alarm sound", e)
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val vibrationPattern = longArrayOf(0, 800, 400, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(vibrationPattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(vibrationPattern, 0)
            }
            Log.d(tag, "Vibration started")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start vibration", e)
        }
    }

    private fun stopAlarmSoundAndVibration() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        Log.d(tag, "Alarm sound and vibration stopped")
    }

    private fun handleComplete(goalId: String) {
        Log.d(tag, "Goal completed: $goalId")
        stopAlarmSoundAndVibration()
        finishSafely()
    }

    private fun handleSnooze(goalId: String) {
        Log.d(tag, "Goal snoozed: $goalId")
        stopAlarmSoundAndVibration()
        // Тут можна додати логіку перепланування нагадування
        finishSafely()
    }

    private fun handleDismiss(goalId: String) {
        Log.d(tag, "Goal dismissed: $goalId")
        stopAlarmSoundAndVibration()
        finishSafely()
    }

    private fun finishSafely() {
        try {
            if (!isFinishing && !isDestroyed) {
                finish()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error finishing activity", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
        stopAlarmSoundAndVibration()
        // Блок коду для WakeLock тут більше не потрібен
        Log.d(tag, "ReminderLockScreenActivity destroyed")
    }

    override fun onPause() {
        super.onPause()
        // Звільняємо WakeLock тут
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(tag, "Wake lock released in onPause")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Отримуємо WakeLock тут
        if (wakeLock?.isHeld == false) {
            acquireWakeLock()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkReminderLockScreen(
    goalId: String,
    goalText: String,
    goalDescription: String?,
    goalEmoji: String,
    extraInfo: String?,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var timeRemaining by remember { mutableStateOf(300) } // 5 хвилин авто-відміна

    // Таймер зворотного відліку
    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        onDismiss()
    }

    // Пульсація для емодзі
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "pulse"
    )

    // Анімація фонових елементів
    val backgroundPulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing)
        ),
        label = "backgroundPulse"
    )

    // Темний елегантний фон з градієнтами
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Темно-сірий центр
                        Color(0xFF1E293B), // Трохи світліший
                        Color(0xFF0F172A)  // Знову темний по краях
                    ),
                    center = Offset(0.5f, 0.3f),
                    radius = 1200f
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        // Анімовані фонові елементи
        Box(
            modifier = Modifier
                .size(400.dp)
                .scale(backgroundPulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x20A78BFA), // Фіолетовий з прозорістю
                            Color(0x10818CF8), // Синій з прозорістю
                            Color.Transparent
                        ),
                        center = Offset(0.3f, 0.7f),
                        radius = 400f
                    ),
                    shape = CircleShape
                )
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
        )

        Box(
            modifier = Modifier
                .size(350.dp)
                .scale(1.2f - (backgroundPulse - 0.8f))
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x206366F1), // Індіго з прозорістю
                            Color(0x0F6366F1),
                            Color.Transparent
                        ),
                        center = Offset(0.8f, 0.2f),
                        radius = 350f
                    ),
                    shape = CircleShape
                )
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
        )

        // Основна картка з темним скляним ефектом
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(20.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xE61E293B) // Темно-сірий з прозорістю
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 32.dp),
            border = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x60A78BFA), // Фіолетовий border
                        Color(0x30818CF8), // Синій border
                        Color(0x20A78BFA)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Заголовок з кнопкою закриття
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "НАГАДУВАННЯ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFA78BFA), // Світло-фіолетовий
                        letterSpacing = 1.2.sp
                    )

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0x40374151), Color(0x60111827)),
                                    radius = 40f
                                ),
                                shape = CircleShape
                            )
                            .border(1.dp, Color(0x40F1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити нагадування",
                            tint = Color(0xFFCBD5E1), // Світло-сірий
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Емодзі з елегантним темним фоном
                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 24.dp,
                            shape = CircleShape,
                            ambientColor = Color(0x80A78BFA),
                            spotColor = Color(0xA0A78BFA)
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF374151), // Темно-сірий
                                    Color(0xFF1F2937), // Ще темніший
                                    Color(0xFF111827)  // Найтемніший
                                ),
                                center = Offset(0.5f, 0.3f),
                                radius = 120f
                            ),
                            shape = CircleShape
                        )
                        .border(
                            2.dp,
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x60A78BFA),
                                    Color(0x40818CF8),
                                    Color(0x30A78BFA)
                                )
                            ),
                            CircleShape
                        )
                        .size(140.dp)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = goalEmoji,
                        fontSize = 68.sp,
                        modifier = Modifier.scale(pulse),
                        textAlign = TextAlign.Center
                    )
                }

                // Контент
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = goalText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFF8FAFC), // Майже білий
                        lineHeight = 30.sp
                    )

                    // Опис якщо є
                    if (!goalDescription.isNullOrBlank()) {
                        Text(
                            text = goalDescription,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFCBD5E1), // Світло-сірий
                            lineHeight = 22.sp
                        )
                    }

                    // Додаткова інформація якщо є
                    if (!extraInfo.isNullOrBlank()) {
                        Text(
                            text = extraInfo,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF94A3B8), // Сірувато-блакитний
                            lineHeight = 20.sp
                        )
                    }

                    Text(
                        text = "Час діяти — ваше майбутнє залежить від цього моменту.",
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF64748B), // Приглушений сірий
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Прогрес-бар з темним дизайном
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            color = Color(0xFF374151),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(1.dp, Color(0x30F1F5F9), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(timeRemaining / 300f)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFA78BFA), // Фіолетовий
                                        Color(0xFF818CF8), // Синій
                                        Color(0xFF06B6D4)  // Бірюзовий
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Text(
                    text = "Авто-закриття: ${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444), // Червоний
                    fontWeight = FontWeight.Bold
                )

                // Основна дія: Виконано
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onComplete()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        2.dp,
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF059669))
                        )
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981), // Зелений
                                        Color(0xFF059669)  // Темніший зелений
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Позначити як виконане",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "ВИКОНАНО",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Вторинні дії
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Кнопка відкладання
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSnooze()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                colors = listOf(Color(0x60F59E0B), Color(0x60D97706))
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x20F59E0B),
                            contentColor = Color(0xFFD97706)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Відкласти на 10 хв",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ВІДКЛАСТИ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }

                    // Кнопка пропуску
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.5.dp,
                            Brush.horizontalGradient(
                                colors = listOf(Color(0x60EF4444), Color(0x60DC2626))
                            )
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0x20EF4444),
                            contentColor = Color(0xFFDC2626)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Пропустити нагадування",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ПРОПУСТИТИ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                }

                // Примітка внизу з елегантним стилем
                Text(
                    text = "Це нагадування не зникне, поки ви не виберете дію",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )

                // Додаткова декоративна лінія
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF475569),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(1.5.dp)
                        )
                )
            }
        }
    }
}