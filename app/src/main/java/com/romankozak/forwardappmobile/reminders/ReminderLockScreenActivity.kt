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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.MainActivity
import com.romankozak.forwardappmobile.ui.theme.ForwardAppMobileTheme
import kotlinx.coroutines.delay

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

            val vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
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

    override fun onBackPressed() {
        // Prevent back button from closing the reminder
        Log.d(tag, "Back button pressed - ignoring")
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .border(3.dp, Color.Red, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emergency indicator
                Text(
                    text = "🚨 URGENT REMINDER 🚨",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )

                // Goal emoji
                Text(
                    text = goalEmoji,
                    fontSize = 64.sp,
                    textAlign = TextAlign.Center
                )

                // Goal text
                Text(
                    text = goalText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )

                // Time info
                Text(
                    text = "⏰ Act now to achieve your goal!",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                // Countdown
                Text(
                    text = "Auto-dismiss in: ${timeRemaining / 60}:${(timeRemaining % 60).toString().padStart(2, '0')}",
                    fontSize = 12.sp,
                    color = Color.Red
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("✅ COMPLETED!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSnooze,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text("⏰ SNOOZE\n10 MIN", fontSize = 12.sp, textAlign = TextAlign.Center)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5722)
                            )
                        ) {
                            Text("❌ SKIP", fontSize = 12.sp)
                        }
                    }
                }

                // Instructions
                Text(
                    text = "This reminder will stay until you take action",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}