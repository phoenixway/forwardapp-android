package com.romankozak.forwardappmobile.domain.reminders

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class ReminderBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_GOAL_ID = "EXTRA_GOAL_ID"
        const val EXTRA_GOAL_TEXT = "EXTRA_GOAL_TEXT"
        const val EXTRA_GOAL_DESCRIPTION = "EXTRA_GOAL_DESCRIPTION"
        const val EXTRA_GOAL_EMOJI = "EXTRA_GOAL_EMOJI"
        const val EXTRA_INFO = "EXTRA_INFO"
        private const val CHANNEL_ID = "goal_reminders_channel"
        private const val ACTION_COMPLETE = "ACTION_COMPLETE"
        private const val ACTION_SNOOZE = "ACTION_SNOOZE"
        private const val ACTION_DISMISS = "ACTION_DISMISS"
        private const val tag = "ReminderReceiver"
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.i(tag, "onReceive triggered! Action: ${intent.action}")

        when (intent.action) {
            ACTION_COMPLETE -> {
                handleCompleteAction(context, intent)
                return
            }
            ACTION_SNOOZE -> {
                handleSnoozeAction(context, intent)
                return
            }
            ACTION_DISMISS -> {
                handleDismissAction(context, intent)
                return
            }
        }

        val goalId = intent.getStringExtra(EXTRA_GOAL_ID)
        val goalText = intent.getStringExtra(EXTRA_GOAL_TEXT)
        val goalDescription = intent.getStringExtra(EXTRA_GOAL_DESCRIPTION)
        val goalEmoji = intent.getStringExtra(EXTRA_GOAL_EMOJI) ?: "🎯"
        val extraInfo = intent.getStringExtra(EXTRA_INFO)

        if (goalId == null || goalText == null) {
            Log.e(tag, "Received broadcast with missing data. GoalId: $goalId, GoalText: $goalText")
            return
        }

        Log.d(tag, "Starting reminder for goal: $goalText")

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        if (!isScreenOn) {
            val wl =
                pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "ForwardApp:ReminderWakeLock",
                )
            wl.acquire(3000)
            startLockScreenActivity(context, goalId, goalText, goalDescription, goalEmoji, extraInfo)
        } else {
            val notificationText =
                buildString {
                    if (!goalDescription.isNullOrBlank()) append(goalDescription)
                    if (!extraInfo.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(extraInfo)
                    }
                }
            showNotification(context, notificationManager, goalId, goalText, notificationText, goalEmoji)
        }
    }

    private fun startLockScreenActivity(
        context: Context,
        goalId: String,
        goalText: String,
        goalDescription: String?,
        goalEmoji: String,
        extraInfo: String?,
    ) {
        try {
            val lockScreenIntent =
                Intent(context, ReminderLockScreenActivity::class.java).apply {
                    putExtra(EXTRA_GOAL_ID, goalId)
                    putExtra(EXTRA_GOAL_TEXT, goalText)
                    putExtra(EXTRA_GOAL_DESCRIPTION, goalDescription)
                    putExtra(EXTRA_GOAL_EMOJI, goalEmoji)
                    putExtra(EXTRA_INFO, extraInfo)

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }

            context.startActivity(lockScreenIntent)
            Log.d(tag, "Lock screen activity started successfully for goal: $goalId")
        } catch (e: Exception) {
            Log.e(tag, "Error starting lock screen activity", e)
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes =
                AudioAttributes
                    .Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Goal Reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Critical goal reminders that appear on lock screen"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 1000)
                    enableLights(true)
                    lightColor = Color.RED
                    setSound(soundUri, audioAttributes)
                    setBypassDnd(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        context: Context,
        notificationManager: NotificationManager,
        goalId: String,
        goalText: String,
        goalDescription: String?,
        goalEmoji: String,
    ) {
        val completeIntent = createActionIntent(context, ACTION_COMPLETE, goalId)
        val snoozeIntent = createActionIntent(context, ACTION_SNOOZE, goalId)
        val dismissIntent = createActionIntent(context, ACTION_DISMISS, goalId)

        val expandedText =
            buildString {
                append(goalText)
                if (!goalDescription.isNullOrBlank()) {
                    append("\n\n")
                    append(goalDescription)
                }
            }

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_dialog_info)
                .setContentTitle("$goalEmoji $goalText")
                .setContentText(goalDescription)
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .bigText(expandedText)
                        .setSummaryText("Час досягати ваших цілей!"),
                ).setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(R.drawable.ic_menu_save, "Готово ✅", completeIntent)
                .addAction(R.drawable.ic_media_pause, "Відкласти ⏰", snoozeIntent)
                .addAction(R.drawable.ic_menu_close_clear_cancel, "Пропустити ❌", dismissIntent)
                .setColor(0xFF6366F1.toInt())
                .setVibrate(longArrayOf(0, 300, 200, 300, 200, 800))
                .setLights(Color.BLUE, 1000, 500)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

        notificationManager.notify(goalId.hashCode(), notification)
    }

    private fun createActionIntent(
        context: Context,
        action: String,
        goalId: String,
    ): PendingIntent {
        val intent =
            Intent(context, ReminderBroadcastReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_GOAL_ID, goalId)
            }
        return PendingIntent.getBroadcast(
            context,
            "$action$goalId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun handleCompleteAction(
        context: Context,
        intent: Intent,
    ) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        Log.d(tag, "Goal completed via notification: $goalId")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(goalId.hashCode())
    }

    private fun handleSnoozeAction(
        context: Context,
        intent: Intent,
    ) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        Log.d(tag, "Goal snoozed via notification: $goalId")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(goalId.hashCode())
    }

    private fun handleDismissAction(
        context: Context,
        intent: Intent,
    ) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        Log.d(tag, "Goal dismissed via notification: $goalId")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(goalId.hashCode())
    }
}
