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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.MainActivity
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import kotlinx.coroutines.delay
import androidx.activity.OnBackPressedCallback


/**
 * Improved ReminderLockScreenActivity with polished UI/UX for the lock-screen reminder.
 * - nicer gradient background
 * - larger emoji with subtle pulse animation
 * - clearer hierarchy of text
 * - accessible buttons with icons and descriptive labels
 * - haptic feedback on primary actions
 */
class ReminderLockScreenActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val tag = "LockScreenReminder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(tag, "ReminderLockScreenActivity created")

        // Make this activity appear over lock screen
        setupLockScreenFlags()

        // Keep screen on and wake up device
        acquireWakeLock()

        // Get data from intent
        val goalId = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID) ?: "unknown"
        val goalText = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT) ?: "Your Goal"
        val goalEmoji = intent.getStringExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI) ?: "🎯"

        // Start sound and vibration
        startAlarmSoundAndVibration()

        setContent {
            ForwardAppMobileTheme {
                ReminderLockScreen(
                    goalId = goalId,
                    goalText = goalText,
                    goalEmoji = goalEmoji,
                    onComplete = { handleComplete(goalId) },
                    onSnooze = { handleSnooze(goalId) },
                    onDismiss = { handleDismiss(goalId) }
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Тут нічого не робимо — ігноруємо натискання
                Log.d(tag, "Back button pressed - ignored to avoid accidental dismiss")
            }
        })

    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Additional flags for better visibility
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Make activity full screen
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        Log.d(tag, "Lock screen flags set")
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "GoalReminder:WakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes max
        }
        Log.d(tag, "Wake lock acquired")
    }

    private fun startAlarmSoundAndVibration() {
        // Start alarm sound
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

        // Start vibration
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
        // Here you can add logic to mark goal as completed
        finish()
    }

    private fun handleSnooze(goalId: String) {
        Log.d(tag, "Goal snoozed: $goalId")
        stopAlarmSoundAndVibration()
        // Here you can add logic to reschedule reminder for 10 minutes
        finish()
    }

    private fun handleDismiss(goalId: String) {
        Log.d(tag, "Goal dismissed: $goalId")
        stopAlarmSoundAndVibration()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSoundAndVibration()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        Log.d(tag, "ReminderLockScreenActivity destroyed")
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderLockScreen(
    goalId: String,
    goalText: String,
    goalEmoji: String,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var timeRemaining by remember { mutableStateOf(300) } // 5 minutes auto-dismiss

    // Countdown timer
    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        // Auto dismiss after 5 minutes
        onDismiss()
    }

    // Pulse animation for the emoji
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing)
        )
    )

    // Background gradient for better visual hierarchy
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF071033))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row with small badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reminder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827)
                    )

                    // Small dismiss shortcut (icon only)
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss reminder",
                            tint = Color(0xFF6B7280)
                        )
                    }
                }

                // Emoji with pulse + soft circular background
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0xFFF1F5F9), shape = CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = goalEmoji,
                        fontSize = 56.sp,
                        modifier = Modifier.scale(pulse),
                        textAlign = TextAlign.Center
                    )
                }

                // Goal text
                Text(
                    text = goalText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF0F172A)
                )

                // Subtext / instruction
                Text(
                    text = "You scheduled this reminder — take a moment to act.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B7280)
                )

                // Countdown and urgency
                Text(
                    text = "Auto-dismiss: ${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Primary action: Completed
                Button(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onComplete()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark completed",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completed", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // Secondary actions in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSnooze()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Snooze, contentDescription = "Snooze 10 min")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Snooze 10 min", fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Skip reminder")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Skip", fontSize = 14.sp)
                    }
                }

                // Tiny accessibility hint
                Text(
                    text = "This reminder will remain until you choose an action.",
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )

            }
        }
    }
}
