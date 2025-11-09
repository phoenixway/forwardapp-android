package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import com.romankozak.forwardappmobile.shared.database.DayPlans
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayPlan

fun DayPlans.toDomain(): DayPlan {
    return DayPlan(
        id = this.id,
        date = this.date,
        name = this.name,
        status = this.status,
        reflection = this.reflection,
        energyLevel = this.energyLevel,
        mood = this.mood,
        weatherConditions = this.weatherConditions,
        totalPlannedMinutes = this.totalPlannedMinutes,
        totalCompletedMinutes = this.totalCompletedMinutes,
        completionPercentage = this.completionPercentage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}

fun DayPlan.toSqlDelight(): DayPlans {
    return DayPlans(
        id = this.id,
        date = this.date,
        name = this.name,
        status = this.status,
        reflection = this.reflection,
        energyLevel = this.energyLevel,
        mood = this.mood,
        weatherConditions = this.weatherConditions,
        totalPlannedMinutes = this.totalPlannedMinutes,
        totalCompletedMinutes = this.totalCompletedMinutes,
        completionPercentage = this.completionPercentage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}