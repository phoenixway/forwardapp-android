package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import com.romankozak.forwardappmobile.shared.database.DayPlans
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayStatus

fun DayPlans.toDomain(): DayPlan {
    return DayPlan(
        id = this.id,
        date = this.date,
        name = this.name,
        status = this.status,
        reflection = this.reflection,
        energyLevel = this.energyLevel?.toInt(),
        mood = this.mood,
        weatherConditions = this.weatherConditions,
        totalPlannedMinutes = this.totalPlannedMinutes.toInt(),
        totalCompletedMinutes = this.totalCompletedMinutes.toInt(),
        completionPercentage = this.completionPercentage.toFloat(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}
