package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus
import com.romankozak.forwardappmobile.shared.features.daymanagement.domain.DayTaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class DayTaskRepositoryImpl(
    private val db: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : DayTaskRepository {

    override fun getDayTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>> {
        return db.dayTaskQueries.selectAllByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks -> dayTasks.map { it.toDomain() } }
    }

    override fun getDayTaskById(id: String): Flow<DayTask?> {
        return db.dayTaskQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
            .map { it?.toDomain() }
    }

    override suspend fun getMaxOrderForDayPlan(dayPlanId: String): Long? {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.getMaxOrderForDayPlan(dayPlanId).executeAsOneOrNull()?.MAX?.toLong()
        }
    }

    override fun getTasksForDayPlan(dayPlanId: String): Flow<List<DayTask>> {
        return db.dayTaskQueries.selectAllByDayPlanId(dayPlanId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks -> dayTasks.map { it.toDomain() } }
    }

    override fun getTasksForGoal(goalId: String): Flow<List<DayTask>> {
        return db.dayTaskQueries.selectTasksForGoal(goalId)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dayTasks -> dayTasks.map { it.toDomain() } }
    }

    override suspend fun getTasksForDayPlanOnce(dayPlanId: String): List<DayTask> {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.selectAllByDayPlanId(dayPlanId).executeAsList().map { it.toDomain() }
        }
    }

    // ВИПРАВЛЕНО: Правильний порядок параметрів
    override suspend fun updateTaskOrder(taskId: String, newOrder: Long, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateTaskOrder(
                taskId = taskId,
                newOrder = newOrder,
                updatedAt = updatedAt
            )
        }
    }

    // ВИПРАВЛЕНО: status передається як enum, не String
    override suspend fun updateTaskCompletion(
        taskId: String,
        completed: Boolean,
        status: TaskStatus,
        completedAt: Long?,
        updatedAt: Long
    ) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateTaskCompletion(
                taskId = taskId,
                completed = if (completed) 1L else 0L,
                status = status, // Передаємо enum напряму
                completedAt = completedAt,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun deleteTasksForDayPlanIds(recurringTaskId: String, dayPlanIds: List<String>) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.deleteTasksForDayPlanIds(
                recurringTaskId = recurringTaskId,
                dayPlanIds = dayPlanIds
            )
        }
    }

    override suspend fun linkTaskWithActivity(taskId: String, activityRecordId: String, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.linkTaskWithActivity(
                taskId = taskId,
                activityRecordId = activityRecordId,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun updateTaskDuration(taskId: String, durationMinutes: Long, updatedAt: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateTaskDuration(
                taskId = taskId,
                durationMinutes = durationMinutes,
                updatedAt = updatedAt
            )
        }
    }

    override suspend fun findByRecurringIdAndDayPlanId(recurringTaskId: String, dayPlanId: String): DayTask? {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.selectByRecurringIdAndDayPlanId(recurringTaskId, dayPlanId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun findTemplateForRecurringTask(recurringTaskId: String): DayTask? {
        return withContext(ioDispatcher) {
            db.dayTaskQueries.selectTemplateForRecurringTask(recurringTaskId).executeAsOneOrNull()?.toDomain()
        }
    }

    override suspend fun detachFromRecurrence(taskId: String) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.detachFromRecurrence(
                taskId = taskId,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override suspend fun updateNextOccurrenceTime(taskId: String, nextOccurrenceTime: Long) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.updateNextOccurrenceTime(
                taskId = taskId,
                nextOccurrenceTime = nextOccurrenceTime,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override suspend fun insertDayTask(dayTask: DayTask) {
        withContext(ioDispatcher) {
            val task = dayTask.toSqlDelight()
            db.dayTaskQueries.insert(
                id = task.id,
                dayPlanId = task.dayPlanId,
                title = task.title,
                description = task.description,
                goalId = task.goalId,
                projectId = task.projectId,
                activityRecordId = task.activityRecordId,
                recurringTaskId = task.recurringTaskId,
                taskType = task.taskType,
                entityId = task.entityId,
                order = task.order,
                priority = task.priority,
                status = task.status,
                completed = task.completed,
                scheduledTime = task.scheduledTime,
                estimatedDurationMinutes = task.estimatedDurationMinutes,
                actualDurationMinutes = task.actualDurationMinutes,
                dueTime = task.dueTime,
                valueImportance = task.valueImportance,
                valueImpact = task.valueImpact,
                effort = task.effort,
                cost = task.cost,
                risk = task.risk,
                location = task.location,
                tags = task.tags,
                notes = task.notes,
                createdAt = task.createdAt,
                updatedAt = task.updatedAt,
                completedAt = task.completedAt,
                nextOccurrenceTime = task.nextOccurrenceTime,
                points = task.points
            )
        }
    }

    override suspend fun updateDayTask(dayTask: DayTask) {
        withContext(ioDispatcher) {
            val task = dayTask.toSqlDelight()
            db.dayTaskQueries.update(
                id = task.id,
                dayPlanId = task.dayPlanId,
                title = task.title,
                description = task.description,
                goalId = task.goalId,
                projectId = task.projectId,
                activityRecordId = task.activityRecordId,
                recurringTaskId = task.recurringTaskId,
                taskType = task.taskType,
                entityId = task.entityId,
                order = task.order,
                priority = task.priority,
                status = task.status,
                completed = task.completed,
                scheduledTime = task.scheduledTime,
                estimatedDurationMinutes = task.estimatedDurationMinutes,
                actualDurationMinutes = task.actualDurationMinutes,
                dueTime = task.dueTime,
                valueImportance = task.valueImportance,
                valueImpact = task.valueImpact,
                effort = task.effort,
                cost = task.cost,
                risk = task.risk,
                location = task.location,
                tags = task.tags,
                notes = task.notes,
                updatedAt = task.updatedAt,
                completedAt = task.completedAt,
                nextOccurrenceTime = task.nextOccurrenceTime,
                points = task.points
            )
        }
    }

    override suspend fun deleteDayTask(id: String) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.deleteById(id)
        }
    }

    override suspend fun deleteAllDayTasksForDayPlan(dayPlanId: String) {
        withContext(ioDispatcher) {
            db.dayTaskQueries.deleteAllByDayPlanId(dayPlanId)
        }
    }
}