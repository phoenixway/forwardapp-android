package com.romankozak.forwardappmobile.ui.screens.reminders

import com.romankozak.forwardappmobile.data.database.models.Goal

enum class ReminderStatus {
    ACTIVE,
    SNOOZED,
    COMPLETED
}

data class Reminder(
    val goal: Goal,
    val status: ReminderStatus
)
