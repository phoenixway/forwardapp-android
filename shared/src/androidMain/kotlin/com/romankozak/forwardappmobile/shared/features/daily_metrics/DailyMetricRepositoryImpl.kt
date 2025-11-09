package com.romankozak.forwardappmobile.shared.features.daily_metrics

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.DailyMetric
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DailyMetricRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DailyMetricRepository {

    override fun getDailyMetrics(): Flow<List<DailyMetric>> {
        return db.dailyMetricQueries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { metrics -> metrics.map { it.toDomain() } }
    }

    override fun getDailyMetric(id: String): Flow<DailyMetric?> {
        return db.dailyMetricQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override fun getDailyMetricsForDayPlan(dayPlanId: String): Flow<List<DailyMetric>> {
        return db.dailyMetricQueries.selectByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { metrics -> metrics.map { it.toDomain() } }
    }

    override suspend fun addDailyMetric(metric: DailyMetric) {
        withContext(ioDispatcher) {
            db.dailyMetricQueries.insert(
                id = metric.id,
                day_plan_id = metric.dayPlanId,
                date = metric.date,
                tasks_planned = metric.tasksPlanned.toLong(),
                tasks_completed = metric.tasksCompleted.toLong(),
                completion_rate = metric.completionRate.toDouble(),
                total_planned_time = metric.totalPlannedTime,
                total_active_time = metric.totalActiveTime,
                completed_points = metric.completedPoints.toLong(),
                total_break_time = metric.totalBreakTime,
                morning_energy_level = metric.morningEnergyLevel?.toLong(),
                evening_energy_level = metric.eveningEnergyLevel?.toLong(),
                overall_mood = metric.overallMood,
                stress_level = metric.stressLevel?.toLong(),
                custom_metrics = metric.customMetrics?.let { Json.encodeToString(it) },
                created_at = metric.createdAt,
                updated_at = metric.updatedAt
            )
        }
    }

    override suspend fun deleteDailyMetric(id: String) {
        withContext(ioDispatcher) {
            db.dailyMetricQueries.deleteById(id)
        }
    }
}
