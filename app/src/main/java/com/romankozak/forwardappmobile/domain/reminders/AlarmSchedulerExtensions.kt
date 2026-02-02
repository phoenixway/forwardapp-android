package com.romankozak.forwardappmobile.domain.reminders

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun AlarmScheduler.scheduleForActivityRecord(record: ActivityRecord) {
    // 1. Resolve Smart Cast & Nullability by "shadowing" or using .let
    // 2. Add clarifying parentheses for the boolean expression
    val reminderTime = record.reminderTime

    if ((reminderTime != null) && record.isOngoing) {
        val requestCode = record.id.hashCode()

        // Use 'this' explicitly or implicitly to fix the "Receiver parameter is never used" warning
        this.scheduleNotification(
            requestCode = requestCode,
            triggerTime = reminderTime, // Now safely a non-null Long
            title = "Нагадування про дедлайн",
            message = "Пора закінчувати з ${record.text}",
            extraInfo = "Розпочато: ${
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.startTime ?: 0))
            }"
        )
    }
}
fun AlarmScheduler.cancelForActivityRecord(record: ActivityRecord) {
    val requestCode = record.id.hashCode()
    cancelNotification(requestCode)
}

interface AlarmSchedulerInterface {
    fun scheduleNotification(
        requestCode: Int,
        triggerTime: Long,
        title: String,
        message: String,
        extraInfo: String? = null,
    )

    fun cancelNotification(requestCode: Int)
}
