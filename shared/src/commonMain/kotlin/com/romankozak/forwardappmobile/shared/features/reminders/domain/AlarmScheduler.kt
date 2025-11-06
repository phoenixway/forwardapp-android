package com.romankozak.forwardappmobile.shared.features.reminders.domain

import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder

expect interface AlarmScheduler {
    suspend fun schedule(reminder: Reminder)
    fun cancel(reminder: Reminder)

    fun scheduleNotification(
        requestCode: Int,
        triggerTime: Long,
        title: String,
        message: String,
        extraInfo: String? = null,
    )

    fun cancelNotification(requestCode: Int)
}
