package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository

import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayPlan
import kotlinx.coroutines.flow.Flow

interface DayPlanRepository {
    fun observeDayPlan(planId: String): Flow<DayPlan?>

    fun observeDayPlansInRange(startDate: Long, endDate: Long): Flow<List<DayPlan>>

    suspend fun getDayPlanByDate(date: Long): DayPlan?

    suspend fun upsertDayPlan(plan: DayPlan)

    suspend fun deleteDayPlan(planId: String)

    suspend fun clearDayPlans()
}
