package com.romankozak.forwardappmobile.shared.features.reminders.domain

import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder

actual interface AlarmScheduler {
    actual suspend fun schedule(reminder: Reminder)
    actual fun cancel(reminder: Reminder)
    actual fun scheduleNotification(
        requestCode: Int,
        triggerTime: Long,
        title: String,
        message: String,
        extraInfo: String? = null,
    )
    actual fun cancelNotification(requestCode: Int)
}
