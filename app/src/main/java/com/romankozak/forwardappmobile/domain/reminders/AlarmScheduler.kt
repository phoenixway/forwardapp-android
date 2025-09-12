package com.romankozak.forwardappmobile.domain.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
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
class AlarmScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlarmSchedulerInterface {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)
        private val tag = "ReminderFlow"

        fun schedule(goal: Goal) {
            Log.d(
                tag,
                "AlarmScheduler: schedule() called for goal ID: ${goal.id}, text: '${goal.text}', reminderTime: ${goal.reminderTime}",
            )
            val reminderTime = goal.reminderTime ?: return
            if (reminderTime <= System.currentTimeMillis()) {
                Log.w(tag, "AlarmScheduler: Reminder time is in the past or now. Aborting schedule.")
                return
            }
            if (!checkPermissions()) return

            val intent =
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, goal.id)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, goal.text)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION, goal.description)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI, "🎯")
                }
            setExactAlarm(goal.id.hashCode(), reminderTime, intent)
        }

        fun cancel(goal: Goal) {
            Log.d(tag, "AlarmScheduler: cancel() called for goal ID: ${goal.id}")
            cancelAlarm(goal.id.hashCode())
        }

        override fun scheduleNotification(
            requestCode: Int,
            triggerTime: Long,
            title: String,
            message: String,
            extraInfo: String?,
        ) {
            Log.d(tag, "AlarmScheduler: scheduleNotification() called for requestCode: $requestCode, triggerTime: $triggerTime")
            if (triggerTime <= System.currentTimeMillis()) {
                Log.w(tag, "AlarmScheduler: Trigger time is in the past. Aborting schedule.")
                return
            }
            if (!checkPermissions()) return

            val intent =
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, requestCode.toString())
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, title)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION, message)
                    putExtra("EXTRA_INFO", extraInfo)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI, "🔔")
                }
            setExactAlarm(requestCode, triggerTime, intent)
        }

        override fun cancelNotification(requestCode: Int) {
            Log.d(tag, "AlarmScheduler: cancelNotification() called for requestCode: $requestCode")
            cancelAlarm(requestCode)
        }

        private fun setExactAlarm(
            requestCode: Int,
            triggerTime: Long,
            intent: Intent,
        ) {
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            try {
                val formattedReminder = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(triggerTime))
                Log.i(tag, "AlarmScheduler: Setting exact alarm for requestCode: $requestCode at $formattedReminder")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                Log.i(tag, "AlarmScheduler: Alarm successfully scheduled.")
            } catch (e: SecurityException) {
                Log.e(tag, "AlarmScheduler: SecurityException while scheduling alarm.", e)
            }
        }

        private fun cancelAlarm(requestCode: Int) {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            alarmManager.cancel(pendingIntent)
            Log.i(tag, "AlarmScheduler: Alarm for requestCode: $requestCode was cancelled.")
        }

        private fun checkPermissions(): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                    Log.e(tag, "AlarmScheduler: POST_NOTIFICATIONS permission not granted")
                    return false
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e(tag, "AlarmScheduler: Cannot schedule exact alarms. Permission denied.")
                    Log.e(tag, "AlarmScheduler: User needs to grant SCHEDULE_EXACT_ALARM permission in system settings")
                    return false
                }
            }

            checkBatteryOptimization()

            Log.i(tag, "AlarmScheduler: All permissions are granted")
            return true
        }

        private fun checkBatteryOptimization() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)

                if (!isIgnoringBatteryOptimizations) {
                    Log.w(tag, "AlarmScheduler: App is NOT in battery optimization whitelist. Alarms might be delayed or cancelled.")
                    Log.w(tag, "AlarmScheduler: Consider asking user to disable battery optimization for this app")
                } else {
                    Log.i(tag, "AlarmScheduler: App is in battery optimization whitelist")
                }
            }
        }

        private fun verifyAlarmSet(
            goalId: String,
            reminderTime: Long,
        ) {
            val testIntent = Intent(context, ReminderBroadcastReceiver::class.java)
            val testPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    goalId.hashCode(),
                    testIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                )

            if (testPendingIntent != null) {
                Log.i(tag, "AlarmScheduler: Alarm verification successful - PendingIntent exists")
            } else {
                Log.w(tag, "AlarmScheduler: Alarm verification failed - PendingIntent not found")
            }
        }

        fun scheduleTestAlarm() {
            val testTime = System.currentTimeMillis() + 60000 
            val currentTime = System.currentTimeMillis()

            val testGoal =
                Goal(
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
