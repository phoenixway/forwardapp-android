package com.romankozak.forwardappmobile.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.romankozak.forwardappmobile.MainActivity
import com.romankozak.forwardappmobile.R

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_GOAL_ID = "EXTRA_GOAL_ID"
        const val EXTRA_GOAL_TEXT = "EXTRA_GOAL_TEXT"
        private const val CHANNEL_ID = "goal_reminders_channel"
    }

    private val TAG = "ReminderReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "onReceive triggered! The alarm fired.")

        val goalId = intent.getStringExtra(EXTRA_GOAL_ID)
        val goalText = intent.getStringExtra(EXTRA_GOAL_TEXT)

        if (goalId == null || goalText == null) {
            Log.e(TAG, "Received broadcast with missing data. GoalId: $goalId, GoalText: $goalText")
            return
        }

        Log.d(TAG, "Received data: GoalId='$goalId', GoalText='$goalText'")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel '$CHANNEL_ID'.")
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Нагадування по цілях",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created.")
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            goalId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Нагадування по цілі")
            .setContentText(goalText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            Log.i(TAG, "Showing notification for goal ID: $goalId")
            notificationManager.notify(goalId.hashCode(), notification)
            Log.i(TAG, "Notification successfully posted.")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification. Check POST_NOTIFICATIONS permission.", e)
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during notification display.", e)
        }
    }
}