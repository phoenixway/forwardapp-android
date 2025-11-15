package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.core.platform.Platform
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.mappers.toEntity
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurringTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.repository.RecurringTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecurringTaskRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : RecurringTaskRepository {

    override fun observeRecurringTasks(goalId: String?): Flow<List<RecurringTask>> {
        val query = if (goalId == null) {
            database.recurringTasksQueries.getAllRecurringTasks()
        } else {
            database.recurringTasksQueries.getRecurringTasksByGoal(goalId)
        }

        return query
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun searchRecurringTasks(query: String): Flow<List<RecurringTask>> {
        val tasks = if (Platform.isAndroid) {
            database.recurringTasksQueries.searchRecurringTasksFts(query)
        } else {
            database.recurringTasksQueries.searchRecurringTasksFallback(query)
        }
        return tasks.asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getRecurringTaskById(id: String): RecurringTask? = withContext(dispatcher) {
        database.recurringTasksQueries.getRecurringTaskById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertRecurringTask(task: RecurringTask) = withContext(dispatcher) {
        val entity = task.toEntity()
        database.recurringTasksQueries.insertRecurringTask(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            goalId = entity.goalId,
            duration = entity.duration,
            priority = entity.priority,
            points = entity.points,
            frequency = entity.frequency,
            interval = entity.interval,
            daysOfWeek = entity.daysOfWeek,
            startDate = entity.startDate,
            endDate = entity.endDate,
        )
    }

    override suspend fun deleteRecurringTask(id: String) = withContext(dispatcher) {
        database.recurringTasksQueries.deleteRecurringTaskById(id)
    }

    override suspend fun deleteRecurringTasksByGoal(goalId: String) = withContext(dispatcher) {
        database.recurringTasksQueries.deleteRecurringTasksByGoal(goalId)
    }

    override suspend fun deleteAll() = withContext(dispatcher) {
        database.recurringTasksQueries.deleteAllRecurringTasks()
    }
}
