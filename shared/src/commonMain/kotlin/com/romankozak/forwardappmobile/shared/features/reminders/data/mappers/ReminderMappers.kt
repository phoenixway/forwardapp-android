package com.romankozak.forwardappmobile.shared.features.reminders.data.mappers

import com.romankozak.forwardappmobile.shared.database.Reminders
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder

fun Reminders.toKmpReminder(): Reminder {
    return Reminder(
        id = this.id,
        entityId = this.entityId,
        entityType = this.entityType,
        reminderTime = this.reminderTime,
        status = this.status,
        creationTime = this.creationTime,
        snoozeUntil = this.snoozeUntil
    )
}
