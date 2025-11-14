package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model

data class DayPlan(
    val id: String,
    val date: Long,
    val name: String?,
    val status: DayStatus,
    val reflection: String?,
    val energyLevel: Int?,
    val mood: String?,
    val weatherConditions: String?,
    val totalPlannedMinutes: Long,
    val totalCompletedMinutes: Long,
    val completionPercentage: Float,
    val createdAt: Long,
    val updatedAt: Long?,
)
