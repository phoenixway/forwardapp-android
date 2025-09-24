package com.romankozak.forwardappmobile.domain.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.Project
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import android.provider.Settings


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
                Log.w(tag, "AlarmScheduler: Reminder time is in the past or now for goal. Aborting schedule.")
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

        fun scheduleForProject(project: Project) {
            Log.d(
                tag,
                "AlarmScheduler: scheduleForProject() called for project ID: ${project.id}, name: '${project.name}', reminderTime: ${project.reminderTime}",
            )
            val reminderTime = project.reminderTime ?: return
            if (reminderTime <= System.currentTimeMillis()) {
                Log.w(tag, "AlarmScheduler: Reminder time is in the past or now for project. Aborting schedule.")
                return
            }
            if (!checkPermissions()) return

            val intent =
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, project.id)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, project.name)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION, project.description)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI, "📂")
                }
            setExactAlarm(project.id.hashCode(), reminderTime, intent)
        }

        fun cancelForProject(project: Project) {
            Log.d(tag, "AlarmScheduler: cancelForProject() called for project ID: ${project.id}")
            cancelAlarm(project.id.hashCode())
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
        // Перевірка дозволу на сповіщення для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                Log.e(tag, "AlarmScheduler: POST_NOTIFICATIONS permission not granted")
                Toast.makeText(context, "Please grant notification permission to schedule reminders.", Toast.LENGTH_LONG).show()
                return false
            }
        }

        // Перевірка дозволу на точні будильники для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(tag, "AlarmScheduler: Cannot schedule exact alarms. Permission denied.")
                Log.e(tag, "AlarmScheduler: User needs to grant SCHEDULE_EXACT_ALARM permission in system settings")
                Toast.makeText(context, "Please grant permission to schedule exact alarms.", Toast.LENGTH_LONG).show()
                return false
            }
        }

        // Перевірка дозволу на показ поверх інших додатків для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!Settings.canDrawOverlays(context)) {
                Log.e(tag, "AlarmScheduler: Cannot draw over other apps. Permission denied.")
                Toast.makeText(context, "Please grant permission to display over other apps.", Toast.LENGTH_LONG).show()
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

        fun scheduleForTask(task: DayTask) {
            Log.d(
                tag,
                "AlarmScheduler: scheduleForTask() called for task ID: ${task.id}, text: '${task.title}', reminderTime: ${task.reminderTime}",
            )
            val reminderTime = task.reminderTime ?: return
            if (reminderTime <= System.currentTimeMillis()) {
                Log.w(tag, "AlarmScheduler: Reminder time for task is in the past. Aborting schedule.")
                return
            }
            if (!checkPermissions()) return

            val intent =
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, task.id)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, task.title)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION, task.description)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI, "📅")
                }
            setExactAlarm(task.id.hashCode(), reminderTime, intent)
        }

        fun cancelForTask(task: DayTask) {
            Log.d(tag, "AlarmScheduler: cancelForTask() called for task ID: ${task.id}")
            cancelAlarm(task.id.hashCode())
        }
    }
