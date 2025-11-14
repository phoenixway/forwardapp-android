package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.mappers

import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayPlans
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus

fun DayPlans.toDomain(): DayPlan =
    DayPlan(
        id = id,
        date = date,
        name = name,
        status = status,
        reflection = reflection,
        energyLevel = energyLevel,
        mood = mood,
        weatherConditions = weatherConditions,
        totalPlannedMinutes = totalPlannedMinutes,
        totalCompletedMinutes = totalCompletedMinutes,
        completionPercentage = completionPercentage.toFloat(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
