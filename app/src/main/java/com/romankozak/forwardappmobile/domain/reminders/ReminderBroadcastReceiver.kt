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
import com.romankozak.forwardappmobile.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.romankozak.forwardappmobile.data.repository.ReminderRepository

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var reminderRepository: ReminderRepository

    companion object {
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
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

        // Базові ID для різних типів сповіщень
        private const val BASE_NOTIFICATION_ID = 1000
        private const val BASE_FULLSCREEN_ID = 2000
        private const val BASE_ACTION_ID = 3000

        fun getNotificationId(reminderId: String): Int {
            return BASE_NOTIFICATION_ID + reminderId.hashCode()
        }

        private fun getFullScreenId(reminderId: String): Int {
            return BASE_FULLSCREEN_ID + reminderId.hashCode()
        }

        private fun getActionId(action: String, reminderId: String): Int {
            return BASE_ACTION_ID + "$action$reminderId".hashCode()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(Companion.tag, "onReceive triggered! Action: ${intent.action}")

        when (intent.action) {
            Companion.ACTION_COMPLETE -> {
                handleCompleteAction(context, intent)
                return
            }
            Companion.ACTION_SNOOZE -> {
                handleSnoozeAction(context, intent)
                return
            }
            Companion.ACTION_DISMISS -> {
                handleDismissAction(context, intent)
                return
            }
        }

        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID)
        val goalId = intent.getStringExtra(Companion.EXTRA_GOAL_ID)
        val goalText = intent.getStringExtra(Companion.EXTRA_GOAL_TEXT)
        val goalDescription = intent.getStringExtra(Companion.EXTRA_GOAL_DESCRIPTION)
        val goalEmoji = intent.getStringExtra(Companion.EXTRA_GOAL_EMOJI) ?: "🎯"
        val extraInfo = intent.getStringExtra(Companion.EXTRA_INFO)

        if (reminderId == null || goalId == null || goalText == null) {
            Log.e(Companion.tag, "Received broadcast with missing data. ReminderId: $reminderId, GoalId: $goalId, GoalText: $goalText")
            return
        }

        Log.d(Companion.tag, "Starting reminder for goal: $goalText")

        // Перевіряємо, чи активність вже запущена
        if (ReminderLockScreenActivity.isActive) {
            Log.d(Companion.tag, "Lock screen activity already active, skipping duplicate")
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = powerManager.isInteractive

        if (isScreenOn) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            createNotificationChannel(notificationManager)

            val notificationText = buildString {
                if (!goalDescription.isNullOrBlank()) append(goalDescription)
                if (!extraInfo.isNullOrBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(extraInfo)
                }
            }

            // Показуємо тільки одне сповіщення з full-screen intent
            showNotification(context, notificationManager, reminderId, goalId, goalText, notificationText, goalEmoji, extraInfo)
        } else {
            startLockScreenActivity(context, reminderId, goalId, goalText, goalDescription, goalEmoji, extraInfo)
        }
    }

    private fun startLockScreenActivity(
        context: Context,
        reminderId: String,
        goalId: String,
        goalText: String,
        goalDescription: String?,
        goalEmoji: String,
        extraInfo: String?,
    ) {
        try {
            val lockScreenIntent = 
                Intent(context, ReminderLockScreenActivity::class.java).apply {
                    putExtra(Companion.EXTRA_REMINDER_ID, reminderId)
                    putExtra(Companion.EXTRA_GOAL_ID, goalId)
                    putExtra(Companion.EXTRA_GOAL_TEXT, goalText)
                    putExtra(Companion.EXTRA_GOAL_DESCRIPTION, goalDescription)
                    putExtra(Companion.EXTRA_GOAL_EMOJI, goalEmoji)
                    putExtra(Companion.EXTRA_INFO, extraInfo)

                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }

            context.startActivity(lockScreenIntent)
            Log.d(Companion.tag, "Lock screen activity started successfully for goal: $goalId")
        } catch (e: Exception) {
            Log.e(Companion.tag, "Error starting lock screen activity", e)
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                Companion.CHANNEL_ID,
                "Goal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical goal reminders that appear on lock screen"
                //enableVibration(true)
               // vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 1000)
                enableLights(true)
                lightColor = Color.RED
                //setSound(soundUri, audioAttributes)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC

                // Критично важливі налаштування для показу на екрані блокування
                canBypassDnd()
                canShowBadge()
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    private fun showNotification(
        context: Context,
        notificationManager: NotificationManager,
        reminderId: String,
        goalId: String,
        goalText: String,
        goalDescription: String?,
        goalEmoji: String,
        extraInfo: String?,
    ) {
        val completeIntent = createActionIntent(context, Companion.ACTION_COMPLETE, reminderId)
        val snoozeIntent = createActionIntent(context, Companion.ACTION_SNOOZE, reminderId)
        val dismissIntent = createActionIntent(context, Companion.ACTION_DISMISS, reminderId)

        val expandedText = buildString {
            append(goalText)
            if (!goalDescription.isNullOrBlank()) {
                append("\n\n")
                append(goalDescription)
            }
        }

        // Створюємо Full Screen Intent для показу на екрані блокування
        val fullScreenIntent = Intent(context, ReminderLockScreenActivity::class.java).apply {
            putExtra(Companion.EXTRA_GOAL_ID, goalId)
            putExtra(Companion.EXTRA_GOAL_TEXT, goalText)
            putExtra(Companion.EXTRA_GOAL_DESCRIPTION, goalDescription)
            putExtra(Companion.EXTRA_GOAL_EMOJI, goalEmoji)
            putExtra(Companion.EXTRA_INFO, extraInfo)

            // Важливі флаги для роботи на екрані блокування
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            getFullScreenId(reminderId),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, Companion.CHANNEL_ID)
            .setFullScreenIntent(fullScreenPendingIntent, true) // true = показувати навіть якщо екран заблокований
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("$goalEmoji $goalText")
            .setContentText(goalDescription)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
                    .setSummaryText("Час досягати ваших цілей!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX) // Максимальний пріоритет
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(R.drawable.ic_menu_save, "Готово ✅", completeIntent)
            .addAction(R.drawable.ic_media_pause, "Відкласти ⏰", snoozeIntent)
            .addAction(R.drawable.ic_menu_close_clear_cancel, "Пропустити ❌", dismissIntent)
            .setColor(0xFF6366F1.toInt())
          //  .setVibrate(longArrayOf(0, 300, 200, 300, 200, 800))
            .setLights(Color.BLUE, 1000, 500)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent) // Додано основний інтент
            .build()

        // Додаємо флаги для показу на екрані блокування
        notification.flags = notification.flags or
                NotificationCompat.FLAG_INSISTENT or
                NotificationCompat.FLAG_NO_CLEAR

        notificationManager.notify(Companion.getNotificationId(reminderId), notification)

        Log.d(Companion.tag, "Full-screen notification created for goal: $goalId")
    }

    private fun createActionIntent(context: Context, action: String, reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            this.action = action
            putExtra(Companion.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            Companion.getActionId(action, reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private fun handleCompleteAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID) ?: return
        Log.d(Companion.tag, "Goal completed via notification: $reminderId")

        CoroutineScope(Dispatchers.IO).launch {
            reminderRepository.markAsCompleted(reminderId)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Companion.getNotificationId(reminderId))

        // Закриваємо активність, якщо вона відкрита
        if (ReminderLockScreenActivity.isActive) {
            val closeIntent = Intent(context, ReminderLockScreenActivity::class.java).apply {
                putExtra("ACTION", "CLOSE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(closeIntent)
            }
            catch (e: Exception) {
                Log.w(Companion.tag, "Could not close lock screen activity", e)
            }
        }
    }

    private fun handleSnoozeAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID) ?: return
        Log.d(Companion.tag, "Goal snoozed via notification: $reminderId")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Companion.getNotificationId(reminderId))

        CoroutineScope(Dispatchers.IO).launch {
            reminderRepository.snoozeReminder(reminderId)
        }

        // Закриваємо активність
        if (ReminderLockScreenActivity.isActive) {
            val closeIntent = Intent(context, ReminderLockScreenActivity::class.java).apply {
                putExtra("ACTION", "CLOSE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(closeIntent)
            } catch (e: Exception) {
                Log.w(Companion.tag, "Could not close lock screen activity", e)
            }
        }
    }

    private fun handleDismissAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID) ?: return
        Log.d(Companion.tag, "Goal dismissed via notification: $reminderId")

        CoroutineScope(Dispatchers.IO).launch {
            reminderRepository.dismissReminder(reminderId)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Companion.getNotificationId(reminderId))

        // Закриваємо активність
        if (ReminderLockScreenActivity.isActive) {
            val closeIntent = Intent(context, ReminderLockScreenActivity::class.java).apply {
                putExtra("ACTION", "CLOSE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(closeIntent)
            } catch (e: Exception) {
                Log.w(Companion.tag, "Could not close lock screen activity", e)
            }
        }
    }

}