package com.romankozak.forwardappmobile.shared.features.daymanagement.domain

import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayPlan
import kotlinx.coroutines.flow.Flow

interface DayPlanRepository {
    fun getAllDayPlans(): Flow<List<DayPlan>>
    fun getDayPlanById(id: String): Flow<DayPlan?>
    fun getDayPlanForDate(date: Long): Flow<DayPlan?>
    suspend fun insertDayPlan(dayPlan: DayPlan)
    suspend fun updateDayPlan(dayPlan: DayPlan)
    suspend fun deleteDayPlan(id: String)
    suspend fun deleteAllDayPlans()
    fun getFutureDayPlanIds(date: Long): Flow<List<String>>
    fun getPlansForDateRange(startDate: Long, endDate: Long): Flow<List<DayPlan>>
    suspend fun updatePlanProgress(planId: String, minutes: Long, percentage: Float, updatedAt: Long)
}
