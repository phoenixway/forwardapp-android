package com.romankozak.forwardappmobile.shared.features.reminders.data.mappers

import com.romankozak.forwardappmobile.shared.features.reminders.Reminders
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder

fun Reminders.toDomain(): Reminder =
    Reminder(
        id = id,
        entityId = entityId,
        entityType = entityType,
        reminderTime = reminderTime,
        status = status,
        creationTime = creationTime,
        snoozeUntil = snoozeUntil,
    )
