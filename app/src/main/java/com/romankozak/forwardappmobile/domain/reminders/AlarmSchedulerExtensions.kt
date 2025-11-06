package com.romankozak.forwardappmobile.domain.reminders

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun AlarmScheduler.scheduleForActivityRecord(record: ActivityRecord) {
    if (record.reminderTime != null && record.isOngoing) {
        val requestCode = record.id.hashCode()
        scheduleNotification(
            requestCode = requestCode,
            triggerTime = record.reminderTime,
            title = "Нагадування про дедлайн",
            message = "Пора закінчувати з ${record.text}",
            extraInfo = "Розпочато: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.startTime ?: 0))}",
        )
    }
}

fun AlarmScheduler.cancelForActivityRecord(record: ActivityRecord) {
    val requestCode = record.id.hashCode()
    cancelNotification(requestCode)
}


