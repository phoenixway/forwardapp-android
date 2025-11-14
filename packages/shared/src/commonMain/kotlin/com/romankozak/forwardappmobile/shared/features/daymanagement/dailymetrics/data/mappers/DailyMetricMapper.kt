package com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.data.mappers

import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.DailyMetrics
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.model.DailyMetric

fun DailyMetrics.toDomain(): DailyMetric =
    DailyMetric(
        id = id,
        dayPlanId = dayPlanId,
        date = date,
        tasksPlanned = tasksPlanned,
        tasksCompleted = tasksCompleted,
        completionRate = completionRate,
        totalPlannedTime = totalPlannedTime,
        totalActiveTime = totalActiveTime,
        completedPoints = completedPoints,
        totalBreakTime = totalBreakTime,
        morningEnergyLevel = morningEnergyLevel,
        eveningEnergyLevel = eveningEnergyLevel,
        overallMood = overallMood,
        stressLevel = stressLevel,
        customMetrics = customMetrics,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
