package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.domain.DayPlanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class DayPlanRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DayPlanRepository {

    override fun getAllDayPlans(): Flow<List<DayPlan>> {
        return db.dayPlanQueries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayPlans -> dayPlans.map { it.toDomain() } }
    }

    override fun getDayPlanById(id: String): Flow<DayPlan?> {
        return db.dayPlanQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override fun getDayPlanForDate(date: Long): Flow<DayPlan?> {
        return db.dayPlanQueries.selectByDate(date)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun insertDayPlan(dayPlan: DayPlan) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.insert(
                id = dayPlan.id,
                date = dayPlan.date,
                name = dayPlan.name,
                status = dayPlan.status,
                reflection = dayPlan.reflection,
                energyLevel = dayPlan.energyLevel?.toLong(),
                mood = dayPlan.mood,
                weatherConditions = dayPlan.weatherConditions,
                totalPlannedMinutes = dayPlan.totalPlannedMinutes,
                totalCompletedMinutes = dayPlan.totalCompletedMinutes,
                completionPercentage = dayPlan.completionPercentage.toDouble(),
                createdAt = dayPlan.createdAt,
                updatedAt = dayPlan.updatedAt,
            )
        }
    }

    override suspend fun updateDayPlan(dayPlan: DayPlan) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.update(
                id = dayPlan.id,
                date = dayPlan.date,
                name = dayPlan.name,
                status = dayPlan.status,
                reflection = dayPlan.reflection,
                energyLevel = dayPlan.energyLevel?.toLong(),
                mood = dayPlan.mood,
                weatherConditions = dayPlan.weatherConditions,
                totalPlannedMinutes = dayPlan.totalPlannedMinutes,
                totalCompletedMinutes = dayPlan.totalCompletedMinutes,
                completionPercentage = dayPlan.completionPercentage.toDouble(),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            )
        }
    }

    override suspend fun deleteDayPlan(id: String) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.deleteById(id)
        }
    }

    override suspend fun deleteAllDayPlans() {
        withContext(ioDispatcher) {
            db.dayPlanQueries.deleteAll()
        }
    }

    override fun getFutureDayPlanIds(date: Long): Flow<List<String>> {
        return db.dayPlanQueries.selectFutureDayPlanIds(date)
            .asFlow()
            .mapToList(ioDispatcher)
    }

    override fun getPlansForDateRange(startDate: Long, endDate: Long): Flow<List<DayPlan>> {
        return db.dayPlanQueries.selectPlansForDateRange(startDate, endDate)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayPlans -> dayPlans.map { it.toDomain() } }
    }

    override suspend fun updatePlanProgress(planId: String, minutes: Long, percentage: Float, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayPlanQueries.updatePlanProgress(
                planId = planId,
                minutes = minutes,
                percentage = percentage.toDouble(),
                updatedAt = updatedAt
            )
        }
    }
}