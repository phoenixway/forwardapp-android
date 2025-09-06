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
    // --- ДОДАНО: Єдиний тег для логування всього процесу ---
    private val TAG = "ReminderFlow"

    fun schedule(goal: Goal) {
        // --- ЗМІНЕНО: Більш детальний лог на вході ---
        Log.d(TAG, "AlarmScheduler: schedule() called for goal ID: ${goal.id}, text: '${goal.text}', reminderTime: ${goal.reminderTime}")

        val reminderTime = goal.reminderTime
        if (reminderTime == null) {
            Log.w(TAG, "AlarmScheduler: Goal has no reminderTime. Aborting schedule.")
            return
        }

        // --- ДОДАНО: Лог для перевірки часу ---
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "AlarmScheduler: Current time is $currentTime. Reminder time is $reminderTime. Is in past: ${reminderTime < currentTime}")

        if (reminderTime < currentTime) {
            Log.w(TAG, "AlarmScheduler: Reminder time is in the past. Aborting schedule.")
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
            Log.e(TAG, "AlarmScheduler: Cannot schedule exact alarms. Permission denied.")
            // Тут можна показати користувачеві повідомлення про необхідність надати дозвіл
            return
        }

        try {
            val formattedTime = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(reminderTime))
            Log.i(TAG, "AlarmScheduler: Setting exact alarm for goal ID: ${goal.id} at $formattedTime")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
            Log.i(TAG, "AlarmScheduler: Alarm successfully scheduled.")
        } catch (e: SecurityException) {
            Log.e(TAG, "AlarmScheduler: SecurityException while scheduling alarm. Check permissions.", e)
        } catch (e: Exception) {
            Log.e(TAG, "AlarmScheduler: An unexpected error occurred during scheduling.", e)
        }
    }

    fun cancel(goal: Goal) {
        Log.d(TAG, "AlarmScheduler: cancel() called for goal ID: ${goal.id}")
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "AlarmScheduler: Alarm for goal ID: ${goal.id} was cancelled.")
    }
}