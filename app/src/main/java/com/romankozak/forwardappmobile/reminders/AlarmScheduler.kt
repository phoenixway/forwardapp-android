package com.romankozak.forwardappmobile.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.Goal
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val TAG = "AlarmScheduler"

    fun schedule(goal: Goal) {
        Log.d(TAG, "schedule() called for goal ID: ${goal.id} with text: '${goal.text}'")

        val reminderTime = goal.reminderTime
        if (reminderTime == null) {
            Log.w(TAG, "Goal has no reminderTime. Aborting schedule.")
            return
        }

        if (reminderTime < System.currentTimeMillis()) {
            Log.w(TAG, "Reminder time is in the past. Aborting schedule.")
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, goal.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, goal.text)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "Cannot schedule exact alarms. Permission denied or not granted by user.")
            // Тут можна показати користувачеві повідомлення про необхідність надати дозвіл
            return
        }

        try {
            val formattedTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(reminderTime))
            Log.i(TAG, "Setting exact alarm for goal ID: ${goal.id} at $formattedTime")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
            Log.i(TAG, "Alarm successfully scheduled.")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling alarm. Check permissions.", e)
        } catch (e: Exception) {
            Log.e(TAG, "An unexpected error occurred during scheduling.", e)
        }
    }

    fun cancel(goal: Goal) {
        Log.d(TAG, "cancel() called for goal ID: ${goal.id}")
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Alarm for goal ID: ${goal.id} was cancelled.")
    }
}