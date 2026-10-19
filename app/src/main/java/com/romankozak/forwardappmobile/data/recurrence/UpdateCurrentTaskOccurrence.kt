package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority as AndroidTaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness

internal suspend fun updateCurrentTaskOccurrence(
    appDatabase: com.romankozak.forwardappmobile.database.AppDatabase,
    task: DayTask,
    title: String,
    description: String?,
    goalId: String?,
    projectId: String?,
    taskType: String?,
    linkedProjectIds: List<String>,
    linkedAttachmentIds: List<String>,
    priority: AndroidTaskPriority,
    estimatedDurationMinutes: Long?,
    points: Int,
    executionStrictness: TaskExecutionStrictness,
): DayTask {
    require(task.recurrenceSeriesId != null) {
        "Cannot edit canonical occurrence for non-recurring task ${task.id}"
    }
    require(task.recurrenceOccurrenceDayKey != null) {
        "Canonical task has no occurrence day key: ${task.id}"
    }
    require(!task.isDeleted) {
        "Cannot edit deleted canonical task occurrence ${task.id}"
    }

    val template =
        buildTemplate(
            title = title,
            description = description,
            goalId = goalId,
            projectId = projectId,
            taskType = taskType,
            linkedProjectIds = linkedProjectIds,
            linkedAttachmentIds = linkedAttachmentIds,
            priority = priority,
            estimatedDurationMinutes = estimatedDurationMinutes,
            points = points,
            executionStrictness = executionStrictness,
        )
    val now = System.currentTimeMillis()
    val updated =
        task.copy(
            title = template.title,
            description = template.description,
            goalId = template.goalId,
            projectId = template.projectId,
            projectWorkspaceId = null,
            linkedProjectIds = template.linkedProjectIds,
            linkedAttachmentIds = template.linkedAttachmentIds,
            taskType = effectiveTaskType(template),
            priority = priority,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            executionStrictness = effectiveExecutionStrictness(template),
            points = template.points,
            updatedAt = now,
            syncedAt = null,
            version = task.version + 1,
        )

    appDatabase.dayTaskDao().update(updated)
    return checkNotNull(
        appDatabase.dayTaskDao().getByIdForCanonicalRecurrenceSync(updated.id),
    ) {
        "Updated canonical task occurrence was not persisted: ${updated.id}"
    }
}
