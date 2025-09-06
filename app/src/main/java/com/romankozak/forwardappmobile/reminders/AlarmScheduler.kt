package com.romankozak.forwardappmobile.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
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
    private val tag = "ReminderFlow"

    fun schedule(goal: Goal) {
        Log.d(tag, "AlarmScheduler: schedule() called for goal ID: ${goal.id}, text: '${goal.text}', reminderTime: ${goal.reminderTime}")

        val reminderTime = goal.reminderTime
        if (reminderTime == null) {
            Log.w(tag, "AlarmScheduler: Goal has no reminderTime. Aborting schedule.")
            return
        }

        // Check current time and reminder time
        val currentTime = System.currentTimeMillis()
        val formattedCurrent = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(currentTime))
        val formattedReminder = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(reminderTime))

        Log.d(tag, "AlarmScheduler: Current time: $formattedCurrent ($currentTime)")
        Log.d(tag, "AlarmScheduler: Reminder time: $formattedReminder ($reminderTime)")
        Log.d(tag, "AlarmScheduler: Time difference: ${reminderTime - currentTime}ms")

        if (reminderTime <= currentTime) {
            Log.w(tag, "AlarmScheduler: Reminder time is in the past or now. Aborting schedule.")
            return
        }

        // Check all necessary permissions
        if (!checkPermissions()) {
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, goal.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, goal.text)
            // Add additional data for better UX
            putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION, goal.description)
            putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI, "🎯")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            Log.i(tag, "AlarmScheduler: Setting exact alarm for goal ID: ${goal.id} at $formattedReminder")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }

            Log.i(tag, "AlarmScheduler: Alarm successfully scheduled.")

            // Verify the alarm was set
            verifyAlarmSet(goal.id, reminderTime)

        } catch (e: SecurityException) {
            Log.e(tag, "AlarmScheduler: SecurityException while scheduling alarm. Check permissions.", e)
        } catch (e: Exception) {
            Log.e(tag, "AlarmScheduler: An unexpected error occurred during scheduling.", e)
        }
    }

    fun cancel(goal: Goal) {
        Log.d(tag, "AlarmScheduler: cancel() called for goal ID: ${goal.id}")
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            goal.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.i(tag, "AlarmScheduler: Alarm for goal ID: ${goal.id} was cancelled.")
    }

    private fun checkPermissions(): Boolean {
        // Check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                Log.e(tag, "AlarmScheduler: POST_NOTIFICATIONS permission not granted")
                return false
            }
        }

        // Check SCHEDULE_EXACT_ALARM permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(tag, "AlarmScheduler: Cannot schedule exact alarms. Permission denied.")
                Log.e(tag, "AlarmScheduler: User needs to grant SCHEDULE_EXACT_ALARM permission in system settings")
                return false
            }
        }

        // Check if app is in battery optimization whitelist (optional but recommended)
        checkBatteryOptimization()

        Log.i(tag, "AlarmScheduler: All permissions are granted")
        return true
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)

            if (!isIgnoringBatteryOptimizations) {
                Log.w(tag, "AlarmScheduler: App is NOT in battery optimization whitelist. Alarms might be delayed or cancelled.")
                Log.w(tag, "AlarmScheduler: Consider asking user to disable battery optimization for this app")
            } else {
                Log.i(tag, "AlarmScheduler: App is in battery optimization whitelist")
            }
        }
    }

    private fun verifyAlarmSet(goalId: String, reminderTime: Long) {
        // Create a test to verify alarm was actually set
        val testIntent = Intent(context, ReminderBroadcastReceiver::class.java)
        val testPendingIntent = PendingIntent.getBroadcast(
            context,
            goalId.hashCode(),
            testIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (testPendingIntent != null) {
            Log.i(tag, "AlarmScheduler: Alarm verification successful - PendingIntent exists")
        } else {
            Log.w(tag, "AlarmScheduler: Alarm verification failed - PendingIntent not found")
        }
    }

    /**
     * Schedule a test alarm for immediate testing (1 minute from now)
     */
    fun scheduleTestAlarm() {
        val testTime = System.currentTimeMillis() + 60000 // 1 minute from now
        val currentTime = System.currentTimeMillis()

        val testGoal = Goal(
            id = "TEST_ALARM",
            text = "Test Reminder - Delete this goal after testing",
            description = "This is a test reminder to verify notifications work correctly",
            completed = false,
            reminderTime = testTime,
            createdAt = currentTime,
            updatedAt = currentTime,
        )

        Log.i(tag, "AlarmScheduler: Scheduling test alarm for 1 minute from now")
        schedule(testGoal)
    }
}