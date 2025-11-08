package com.romankozak.forwardappmobile.shared.features.recurring_tasks

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.data.database.models.RecurringTask
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecurringTaskRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : RecurringTaskRepository {

    override fun getRecurringTasks(): Flow<List<RecurringTask>> {
        return db.recurringTaskQueries.selectAll()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { tasks -> tasks.map { it.toDomain() } }
    }

    override fun getRecurringTask(id: String): Flow<RecurringTask?> {
        return db.recurringTaskQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun addRecurringTask(task: RecurringTask) {
        withContext(ioDispatcher) {
            db.recurringTaskQueries.insert(
                id = task.id,
                title = task.title,
                description = task.description,
                goal_id = task.goalId,
                duration = task.duration?.toLong(),
                priority = task.priority.name,
                points = task.points.toLong(),
                frequency = task.recurrenceRule.frequency.name,
                interval = task.recurrenceRule.interval.toLong(),
                days_of_week = task.recurrenceRule.daysOfWeek?.joinToString(","),
                start_date = task.startDate,
                end_date = task.endDate
            )
        }
    }

    override suspend fun deleteRecurringTask(id: String) {
        withContext(ioDispatcher) {
            db.recurringTaskQueries.deleteById(id)
        }
    }
}
