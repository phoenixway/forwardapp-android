package com.romankozak.forwardappmobile.shared.data.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DailyMetric(
    val id: String,
    val dayPlanId: String,
    val date: Long,
    val tasksPlanned: Int = 0,
    val tasksCompleted: Int = 0,
    val completionRate: Float = 0f,
    val totalPlannedTime: Long = 0,
    val totalActiveTime: Long = 0,
    val completedPoints: Int = 0,
    val totalBreakTime: Long = 0,
    val morningEnergyLevel: Int? = null,
    val eveningEnergyLevel: Int? = null,
    val overallMood: String? = null,
    val stressLevel: Int? = null,
    val customMetrics: Map<String, Float>? = null,
    val createdAt: Long,
    val updatedAt: Long? = null
)
