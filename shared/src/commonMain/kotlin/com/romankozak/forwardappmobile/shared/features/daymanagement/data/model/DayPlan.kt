package com.romankozak.forwardappmobile.shared.features.daymanagement.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class DayStatus { PLANNED, IN_PROGRESS, COMPLETED, MISSED, ARCHIVED }

@Serializable
data class DayPlan(
    val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val name: String? = null,
    val status: DayStatus = DayStatus.PLANNED,
    val reflection: String? = null,
    val energyLevel: Int? = null,
    val mood: String? = null,
    val weatherConditions: String? = null,
    val totalPlannedMinutes: Long = 0,
    val totalCompletedMinutes: Long = 0,
    val completionPercentage: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
)
