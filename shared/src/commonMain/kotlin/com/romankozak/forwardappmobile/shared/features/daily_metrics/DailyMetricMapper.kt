package com.romankozak.forwardappmobile.shared.features.daily_metrics

import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.shared.database.DailyMetrics

fun DailyMetrics.toDomain(): DailyMetric {
    return DailyMetric(
        id = id,
        dayPlanId = dayPlanId,
        date = date,
        tasksPlanned = tasksPlanned.toInt(),
        tasksCompleted = tasksCompleted.toInt(),
        completionRate = completionRate,
        totalPlannedTime = totalPlannedTime,
        totalActiveTime = totalActiveTime,
        completedPoints = completedPoints.toInt(),
        totalBreakTime = totalBreakTime,
        morningEnergyLevel = morningEnergyLevel?.toInt(),
        eveningEnergyLevel = eveningEnergyLevel?.toInt(),
        overallMood = overallMood,
        stressLevel = stressLevel?.toInt(),
        customMetrics = customMetrics?.mapValues { it.value.toFloat() },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
