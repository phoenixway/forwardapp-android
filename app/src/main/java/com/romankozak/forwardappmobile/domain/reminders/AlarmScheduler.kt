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
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Provider
import javax.inject.Inject
import javax.inject.Singleton
import android.provider.Settings


@Singleton
class AlarmScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val projectRepositoryProvider: Provider<ProjectRepository>,
        private val dayManagementRepository: com.romankozak.forwardappmobile.data.repository.DayManagementRepository,
        private val goalRepositoryProvider: Provider<com.romankozak.forwardappmobile.data.repository.GoalRepository>,
    ) : AlarmSchedulerInterface {
        private val alarmManager = context.getSystemService(AlarmManager::class.java)
        private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository by lazy { goalRepositoryProvider.get() }
        private val tag = "ReminderFlow"

        suspend fun schedule(reminder: Reminder) {
            val projectRepository = projectRepositoryProvider.get()
            Log.d(
                tag,
                "AlarmScheduler: schedule() called for reminder ID: ${reminder.id}, entityId: ${reminder.entityId}, entityType: ${reminder.entityType}, reminderTime: ${reminder.reminderTime}",
            )
            val reminderTime = reminder.reminderTime
            val currentTime = System.currentTimeMillis()
            
            // Додаємо буфер в 2 секунди для snooze
            if (reminderTime < currentTime - 2000) {
                Log.w(tag, "AlarmScheduler: Reminder time is too far in the past (${currentTime - reminderTime}ms ago). Aborting schedule.")
                return
            }
            
            // Якщо час у минулому але менше 2 секунд - плануємо на +5 секунд від зараз
            var adjustedTime = if (reminderTime <= currentTime) {
                val newTime = currentTime + 5000 // +5 секунд
                Log.w(tag, "AlarmScheduler: Reminder time adjusted from ${reminderTime} to ${newTime} (now + 5s)")
                newTime
            } else {
                reminderTime
            }

            if (BuildConfig.DEBUG) {
                adjustedTime = System.currentTimeMillis() + 20000 // 20 seconds
            }
            
            if (!checkPermissions()) return

            val intent =
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_ID, reminder.entityId)
                    val (title, description, emoji) = when (reminder.entityType) {
                        "GOAL" -> {
                            val goal = goalRepository.getGoalById(reminder.entityId)
                            Triple(goal?.text, goal?.description, "🎯")
                        }
                        "PROJECT" -> {
                            val project = projectRepository.getProjectById(reminder.entityId)
                            Triple(project?.name, project?.description, "📂")
                        }
                        "TASK" -> {
                            val task = dayManagementRepository.getTaskById(reminder.entityId)
                            Triple(task?.title, task?.description, "📅")
                        }
                        else -> Triple("Reminder", "You have a reminder", "🔔")
                    }
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_TEXT, title)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_DESCRIPTION, description)
                    putExtra(ReminderBroadcastReceiver.EXTRA_GOAL_EMOJI, emoji)
                }
            setExactAlarm(ReminderBroadcastReceiver.Companion.getNotificationId(reminder.id), adjustedTime, intent)
        }

        fun cancel(reminder: Reminder) {
            Log.d(tag, "AlarmScheduler: cancel() called for reminder ID: ${reminder.id}")
            cancelAlarm(ReminderBroadcastReceiver.Companion.getNotificationId(reminder.id))
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
                    ReminderBroadcastReceiver.Companion.getNotificationId(goalId),
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

        }



        suspend fun snooze(reminder: Reminder) {
            Log.d(tag, "AlarmScheduler: snooze() called for reminder ID: ${reminder.id}")
            val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000 // 15 minutes
            val snoozedReminder = reminder.copy(reminderTime = snoozeTime, status = "SNOOZED", snoozeUntil = snoozeTime)
            schedule(snoozedReminder)
        }
    }
