package com.romankozak.forwardappmobile.domain.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.BroadcastReceiver.PendingResult
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import android.view.View
import android.widget.RemoteViews
import android.R as AndroidR
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    companion object {
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_GOAL_ID = "EXTRA_GOAL_ID"
        const val EXTRA_GOAL_TEXT = "EXTRA_GOAL_TEXT"
        const val EXTRA_GOAL_DESCRIPTION = "EXTRA_GOAL_DESCRIPTION"
        const val EXTRA_GOAL_EMOJI = "EXTRA_GOAL_EMOJI"
        const val EXTRA_INFO = "EXTRA_INFO"
        private const val CHANNEL_ID_SCREEN_OFF = "goal_reminders_channel_v2"
        private const val CHANNEL_ID_SCREEN_ON = "goal_reminders_channel_v4_custom"
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

        val pendingResult: PendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Companion.ACTION_COMPLETE -> {
                        handleCompleteAction(context, intent)
                        return@launch
                    }
                    Companion.ACTION_SNOOZE -> {
                        handleSnoozeAction(context, intent)
                        return@launch
                    }
                    Companion.ACTION_DISMISS -> {
                        handleDismissAction(context, intent)
                        return@launch
                    }
                }

                val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID)
                val goalId = intent.getStringExtra(Companion.EXTRA_GOAL_ID)
                val goalText = intent.getStringExtra(Companion.EXTRA_GOAL_TEXT)
                val goalDescription = intent.getStringExtra(Companion.EXTRA_GOAL_DESCRIPTION)
                val goalEmoji = intent.getStringExtra(Companion.EXTRA_GOAL_EMOJI) ?: "🎯"
                val extraInfo = intent.getStringExtra(Companion.EXTRA_INFO)

                val ringtoneSettings = settingsRepository.getRingtoneSettings()
                val ringtoneUri = resolveRingtoneUri(ringtoneSettings)
                val channelSuffix = ringtoneSettings.currentType.storageKey
                val vibrationEnabled = settingsRepository.isReminderVibrationEnabled()

                if (reminderId == null || goalId == null || goalText == null) {
                    Log.e(Companion.tag, "Received broadcast with missing data. reminderId=$reminderId, goalId=$goalId, goalText=$goalText")
                    return@launch
                }

                Log.d(Companion.tag, "Starting reminder for goal: $goalText")

                // Перевіряємо, чи активність вже запущена
                if (ReminderLockScreenActivity.isActive) {
                    Log.d(Companion.tag, "Lock screen activity already active, skipping duplicate")
                    return@launch
                }

                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isScreenOn = powerManager.isInteractive
                Log.d(Companion.tag, "Reminder data ok. screenOn=$isScreenOn reminderId=$reminderId goalId=$goalId")

                val vibrationTag = if (vibrationEnabled) "vib" else "novib"
                val channelId =
                    if (isScreenOn) {
                        "${Companion.CHANNEL_ID_SCREEN_ON}_${channelSuffix}_$vibrationTag"
                    } else {
                        "${Companion.CHANNEL_ID_SCREEN_OFF}_${channelSuffix}_$vibrationTag"
                    }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createNotificationChannel(notificationManager, channelId, ringtoneUri, vibrationEnabled)

                val notificationText = buildString {
                    if (!goalDescription.isNullOrBlank()) append(goalDescription)
                    if (!extraInfo.isNullOrBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(extraInfo)
                    }
                }

                showNotification(
                    context = context,
                    notificationManager = notificationManager,
                    reminderId = reminderId,
                    goalId = goalId,
                    goalText = goalText,
                    goalDescription = notificationText,
                    goalEmoji = goalEmoji,
                    extraInfo = extraInfo,
                    isScreenOn = isScreenOn,
                    channelId = channelId,
                    ringtoneUri = ringtoneUri,
                    vibrationEnabled = vibrationEnabled,
                )
            } finally {
                pendingResult.finish()
            }
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

    private fun createNotificationChannel(notificationManager: NotificationManager, channelId: String, soundUri: Uri?, vibrationEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Goal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical goal reminders that appear on lock screen"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(vibrationEnabled)
                vibrationPattern = if (vibrationEnabled) longArrayOf(0, 200, 150, 200) else longArrayOf(0L)
                setSound(soundUri, audioAttributes)
                setBypassDnd(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(Companion.tag, "Notification channel created/updated: ${channel.id}, importance=${channel.importance}")
        }
    }

    private fun resolveRingtoneUri(settings: com.romankozak.forwardappmobile.data.repository.RingtoneSettings): Uri? {
        val stored = settings.uris[settings.currentType].orEmpty()
        return stored.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: defaultUriFor(settings.currentType)
    }

    private fun defaultUriFor(type: com.romankozak.forwardappmobile.domain.reminders.RingtoneType): Uri? =
        when (type) {
            com.romankozak.forwardappmobile.domain.reminders.RingtoneType.Energetic -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            com.romankozak.forwardappmobile.domain.reminders.RingtoneType.Moderate -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            com.romankozak.forwardappmobile.domain.reminders.RingtoneType.Quiet -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
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
        isScreenOn: Boolean,
        channelId: String,
        ringtoneUri: Uri?,
        vibrationEnabled: Boolean,
    ) {
        val completeIntent = createActionIntent(context, Companion.ACTION_COMPLETE, reminderId)
        val snoozeIntent = createActionIntent(context, Companion.ACTION_SNOOZE, reminderId)
        val dismissIntent = createActionIntent(context, Companion.ACTION_DISMISS, reminderId)

        val channelImportance =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.getNotificationChannel(channelId)?.importance
                    ?: NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_HIGH
            }
        val canUseFullScreenIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                notificationManager.canUseFullScreenIntent()
            } else {
                true
            }
        val shouldUseFullScreenIntent =
            !isScreenOn && canUseFullScreenIntent &&
                channelImportance >= NotificationManager.IMPORTANCE_HIGH

        Log.d(
            Companion.tag,
            "FSI decision: screenOn=$isScreenOn channelImportance=$channelImportance canUseFSI=$canUseFullScreenIntent useFSI=$shouldUseFullScreenIntent",
        )

        val baseIntent = Intent(context, ReminderLockScreenActivity::class.java).apply {
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
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            getNotificationId(reminderId),
            baseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenPendingIntent =
            if (shouldUseFullScreenIntent) {
                PendingIntent.getActivity(
                    context,
                    getFullScreenId(reminderId),
                    baseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                null
            }

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(System.currentTimeMillis())
        val primaryDescription = goalDescription?.takeIf { it.isNotBlank() } ?: "Time to focus on this goal"
        val extra = extraInfo?.takeIf { !it.isNullOrBlank() } ?: ""
        val vibrationPattern = longArrayOf(0, 200, 150, 200)

        val collapsedView = RemoteViews(context.packageName, com.romankozak.forwardappmobile.R.layout.notification_custom_collapsed).apply {
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_title, goalText)
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_description, primaryDescription)
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_emoji, goalEmoji)
        }

        val expandedView = RemoteViews(context.packageName, com.romankozak.forwardappmobile.R.layout.notification_custom).apply {
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_title, goalText)
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_description, primaryDescription)
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_extra, extra)
            setViewVisibility(com.romankozak.forwardappmobile.R.id.notification_extra, if (extra.isNotEmpty()) View.VISIBLE else View.GONE)
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_chip, "REMINDER")
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_time, timeText)
            setTextViewText(com.romankozak.forwardappmobile.R.id.notification_emoji, goalEmoji)
        }


        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(if (vibrationEnabled) NotificationCompat.DEFAULT_ALL else NotificationCompat.DEFAULT_SOUND)
            .setSound(ringtoneUri)
            .setColor(0xFF6366F1.toInt())
            .setLights(Color.BLUE, 1000, 500)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_notification_done, "Готово", completeIntent)
            .addAction(R.drawable.ic_notification_snooze, "Відкласти", snoozeIntent)
            .addAction(R.drawable.ic_notification_close, "Пропустити", dismissIntent)

        if (vibrationEnabled) {
            builder.setVibrate(vibrationPattern)
        } else {
            builder.setVibrate(longArrayOf(0L))
        }

        if (isScreenOn) {
            val bigText = buildString {
                append(primaryDescription)
                if (extra.isNotEmpty()) {
                    append("\n")
                    append(extra)
                }
            }
            builder
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentTitle("$goalEmoji $goalText")
                .setContentText(primaryDescription)
                .setSubText("REMINDER • $timeText")
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setColorized(true)
                .setContentIntent(contentPendingIntent)
        } else {
            builder
                .setAutoCancel(false)
                .setOngoing(true)
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(contentPendingIntent)

            fullScreenPendingIntent?.let {
                builder.setFullScreenIntent(it, true)
            }
        }

        val notification = builder.build()

        // Додаємо флаги для показу на екрані блокування
        notification.flags = notification.flags or
                NotificationCompat.FLAG_INSISTENT or
                NotificationCompat.FLAG_NO_CLEAR

        try {
            notificationManager.notify(Companion.getNotificationId(reminderId), notification)
            Log.d(Companion.tag, "Notification posted. screenOn=$isScreenOn goal=$goalId id=${Companion.getNotificationId(reminderId)}")

            // Якщо система не дає full-screen інтерфейс, явно стартуємо activity для заблокованого екрану
            if (!isScreenOn && fullScreenPendingIntent == null && !ReminderLockScreenActivity.isActive) {
                Log.d(Companion.tag, "FSI unavailable, starting lock screen activity manually")
                startLockScreenActivity(
                    context = context,
                    reminderId = reminderId,
                    goalId = goalId,
                    goalText = goalText,
                    goalDescription = goalDescription,
                    goalEmoji = goalEmoji,
                    extraInfo = extraInfo,
                )
            }
        } catch (e: Exception) {
            Log.e(Companion.tag, "Failed to show reminder notification", e)
        }
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


    private suspend fun handleCompleteAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID) ?: return
        Log.d(Companion.tag, "Goal completed via notification: $reminderId")

        reminderRepository.markAsCompleted(reminderId)

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

    private suspend fun handleSnoozeAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID) ?: return
        Log.d(Companion.tag, "Goal snoozed via notification: $reminderId")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Companion.getNotificationId(reminderId))

        reminderRepository.snoozeReminder(reminderId)

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

    private suspend fun handleDismissAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(Companion.EXTRA_REMINDER_ID) ?: return
        Log.d(Companion.tag, "Goal dismissed via notification: $reminderId")

        reminderRepository.dismissReminder(reminderId)

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
