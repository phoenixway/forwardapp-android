package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management

data class DayThemeDocumentSnapshot(
    val dayPlanId: String,
    val contentJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val version: Long = 0,
)
