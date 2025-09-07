// Файл: app/src/main/java/com/romankozak/forwardappmobile/reminders/AlarmSchedulerExtensions.kt

package com.romankozak.forwardappmobile.domain.reminders

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Розширення для AlarmScheduler для роботи з нагадуваннями ActivityRecord
 */
fun AlarmScheduler.scheduleForActivityRecord(record: ActivityRecord) {
    if (record.reminderTime != null && record.isOngoing) {
        // Використовуємо ID активності як унікальний ключ для нагадування
        val requestCode = record.id.hashCode()
        scheduleNotification(
            requestCode = requestCode,
            triggerTime = record.reminderTime,
            title = "Нагадування про дедлайн",
            message = "Пора закінчувати з ${record.text}",
            // Можна додати додаткову інформацію про час початку активності
            extraInfo = "Розпочато: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.startTime ?: 0))}"
        )
    }
}

fun AlarmScheduler.cancelForActivityRecord(record: ActivityRecord) {
    val requestCode = record.id.hashCode()
    cancelNotification(requestCode)
}

/**
 * Базові методи, які повинні бути реалізовані в AlarmScheduler
 */
interface AlarmSchedulerInterface {
    fun scheduleNotification(
        requestCode: Int,
        triggerTime: Long,
        title: String,
        message: String,
        extraInfo: String? = null
    )

    fun cancelNotification(requestCode: Int)
}