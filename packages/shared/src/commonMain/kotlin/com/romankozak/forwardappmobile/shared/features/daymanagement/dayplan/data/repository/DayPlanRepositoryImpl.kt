package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayPlan
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DayPlanRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : DayPlanRepository {

    override fun observeDayPlan(planId: String): Flow<DayPlan?> =
        database.dayPlansQueries.getDayPlanById(planId)
            .asFlow()
            .mapToOneOrNull(dispatcher)
            .map { row -> row?.toDomain() }

    override fun observeDayPlansInRange(startDate: Long, endDate: Long): Flow<List<DayPlan>> =
        database.dayPlansQueries.getDayPlansInRange(startDate, endDate)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getDayPlanByDate(date: Long): DayPlan? = withContext(dispatcher) {
        database.dayPlansQueries.getDayPlanByDate(date).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertDayPlan(plan: DayPlan) = withContext(dispatcher) {
        database.dayPlansQueries.insertDayPlan(
            id = plan.id,
            date = plan.date,
            name = plan.name,
            status = plan.status,
            reflection = plan.reflection,
            energyLevel = plan.energyLevel,
            mood = plan.mood,
            weatherConditions = plan.weatherConditions,
            totalPlannedMinutes = plan.totalPlannedMinutes,
            totalCompletedMinutes = plan.totalCompletedMinutes,
            completionPercentage = plan.completionPercentage.toDouble(),
            createdAt = plan.createdAt,
            updatedAt = plan.updatedAt,
        )
    }

    override suspend fun deleteDayPlan(planId: String) = withContext(dispatcher) {
        database.dayPlansQueries.deleteDayPlan(planId)
    }

    override suspend fun clearDayPlans() = withContext(dispatcher) {
        database.dayPlansQueries.deleteAllDayPlans()
    }
}
