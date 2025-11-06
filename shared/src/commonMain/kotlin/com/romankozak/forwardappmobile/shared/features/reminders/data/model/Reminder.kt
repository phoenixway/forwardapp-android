package com.romankozak.forwardappmobile.shared.features.reminders.data.model

data class Reminder(
    val id: String,
    val entityId: String,
    val entityType: String,
    val reminderTime: Long,
    val status: String,
    val creationTime: Long,
    val snoozeUntil: Long? = null
)
