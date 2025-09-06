package com.romankozak.forwardappmobile.reminders

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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.romankozak.forwardappmobile.MainActivity
import com.romankozak.forwardappmobile.R

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_GOAL_ID = "EXTRA_GOAL_ID"
        const val EXTRA_GOAL_TEXT = "EXTRA_GOAL_TEXT"
        const val EXTRA_GOAL_DESCRIPTION = "EXTRA_GOAL_DESCRIPTION"
        const val EXTRA_GOAL_EMOJI = "EXTRA_GOAL_EMOJI"
        private const val CHANNEL_ID = "goal_reminders_channel"
        private const val ACTION_COMPLETE = "ACTION_COMPLETE"
        private const val ACTION_SNOOZE = "ACTION_SNOOZE"
        private const val ACTION_DISMISS = "ACTION_DISMISS"
        private const val tag = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(tag, "onReceive triggered! The alarm fired.")

        // Handle notification actions
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

        if ((goalId == null) || (goalText == null)) {
            Log.e(tag, "Received broadcast with missing data. GoalId: $goalId, GoalText: $goalText")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)
        showNotification(context, notificationManager, goalId, goalText, goalDescription, goalEmoji)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Goal Reminders",
                NotificationManager.IMPORTANCE_HIGH
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
        goalEmoji: String
    ) {
        // Intent для відкриття lock screen Activity
        val lockScreenIntent = Intent(context, ReminderLockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_GOAL_ID, goalId)
            putExtra(EXTRA_GOAL_TEXT, goalText)
            putExtra(EXTRA_GOAL_DESCRIPTION, goalDescription)
            putExtra(EXTRA_GOAL_EMOJI, goalEmoji)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            goalId.hashCode(),
            lockScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions
        val completeIntent = createActionIntent(context, ACTION_COMPLETE, goalId)
        val snoozeIntent = createActionIntent(context, ACTION_SNOOZE, goalId)
        val dismissIntent = createActionIntent(context, ACTION_DISMISS, goalId)

        val expandedText = buildString {
            append(goalText)
            if (!goalDescription.isNullOrBlank()) {
                append("\n\n")
                append(goalDescription)
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$goalEmoji Reminder")
            .setContentText(goalText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setSummaryText("Time to achieve your goals!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true) // ⚡ Головне місце!
            .addAction(android.R.drawable.ic_menu_save, "Done ✅", completeIntent)
            .addAction(android.R.drawable.ic_media_pause, "Snooze ⏰", snoozeIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip ❌", dismissIntent)
            .setColor(0xFF4CAF50.toInt())
            .setVibrate(longArrayOf(0, 300, 200, 300, 200, 800))
            .setLights(Color.BLUE, 1000, 500)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(goalId.hashCode(), notification)
    }

    private fun createActionIntent(context: Context, action: String, goalId: String): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_GOAL_ID, goalId)
        }
        return PendingIntent.getBroadcast(
            context,
            "$action$goalId".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleCompleteAction(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(goalId.hashCode())
    }

    private fun handleSnoozeAction(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(goalId.hashCode())
    }

    private fun handleDismissAction(context: Context, intent: Intent) {
        val goalId = intent.getStringExtra(EXTRA_GOAL_ID) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(goalId.hashCode())
    }
}
