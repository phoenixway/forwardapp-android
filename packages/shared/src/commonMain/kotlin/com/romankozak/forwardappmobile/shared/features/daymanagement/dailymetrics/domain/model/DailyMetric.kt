package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.model

import com.romankozak.forwardappmobile.shared.data.database.models.StringDoubleMap

data class DailyMetric(
    val id: String,
    val dayPlanId: String,
    val date: Long,
    val tasksPlanned: Long,
    val tasksCompleted: Long,
    val completionRate: Double,
    val totalPlannedTime: Long,
    val totalActiveTime: Long,
    val completedPoints: Long,
    val totalBreakTime: Long,
    val morningEnergyLevel: Long?,
    val eveningEnergyLevel: Long?,
    val overallMood: String?,
    val stressLevel: Long?,
    val customMetrics: StringDoubleMap?,
    val createdAt: Long,
    val updatedAt: Long?,
)
