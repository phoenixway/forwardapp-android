package com.romankozak.forwardappmobile.shared.features.daily_metrics

import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.shared.database.DailyMetrics
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun DailyMetrics.toDomain(): DailyMetric {
    return DailyMetric(
        id = id,
        dayPlanId = dayPlanId,
        date = date,
        tasksPlanned = tasksPlanned.toInt(),
        tasksCompleted = tasksCompleted.toInt(),
        completionRate = completionRate.toFloat(),
        totalPlannedTime = totalPlannedTime,
        totalActiveTime = totalActiveTime,
        completedPoints = completedPoints.toInt(),
        totalBreakTime = totalBreakTime,
        morningEnergyLevel = morningEnergyLevel?.toInt(),
        eveningEnergyLevel = eveningEnergyLevel?.toInt(),
        overallMood = overallMood,
        stressLevel = stressLevel?.toInt(),
        customMetrics = customMetrics?.let { Json.decodeFromString(it) },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}