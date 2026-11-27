package com.romankozak.forwardappmobile.data.recurrence

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.data.daythemes.rekeyTaskThemeAssignments
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import java.util.UUID

internal suspend fun AppDatabase.convertTaskToRecurringSeries(
    requested: DayTask, rule: RecurrenceRule, planningSources: RecurrencePlanningSourceBridge,
    priority: TaskPriority, executionStrictness: TaskExecutionStrictness, template: RecurringTaskTemplate,
): DayTask = withTransaction {
    val task = requireNotNull(dayTaskDao().getTaskById(requested.id))
    check(task.version == requested.version) { "Task changed during recurrence conversion" }
    require(task.recurrenceSeriesId == null) {
        "Cannot convert already-canonical recurring task ${task.id}"
    }
    require(!task.isDeleted) {
        "Cannot convert deleted task ${task.id}"
    }
    require(template.title.isNotBlank()) {
        "Canonical recurring task title must not be blank"
    }

    val dayPlan =
        checkNotNull(dayPlanDao().getPlanById(task.dayPlanId)) {
            "Cannot convert task to canonical recurrence: DayPlan not found: ${task.dayPlanId}"
        }

    val now = System.currentTimeMillis()
    val seriesId = UUID.randomUUID().toString()
    val startDayKey = canonicalLocalDayKey(dayPlan.date)
    val canonicalRule = rule.normalizedForSeriesStart(startDayKey)
    val series =
        RecurringTaskSeries(
            id = seriesId,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1,
            rule = canonicalRule,
            startDayKey = startDayKey,
            endDayKey = null,
            template = template.copy(planningSource = planningSources.source(task.id), realizesDefaults = conversionDefaults(task)),
        )

    val occurrence =
        task.copy(
            id =
                recurrenceOccurrenceId(
                    kind = RecurringSeriesKind.TASK,
                    seriesId = seriesId,
                    dayKey = startDayKey,
                ),
            title = template.title,
            description = template.description,
            goalId = template.goalId,
            projectId = template.projectId,
            projectWorkspaceId = null,
            linkedProjectIds = template.linkedProjectIds,
            linkedAttachmentIds = template.linkedAttachmentIds,
            recurrenceSeriesId = seriesId,
            recurrenceOccurrenceDayKey = startDayKey,
            recurrenceSourceSeriesVersion = series.version,
            taskType = template.taskType ?: if (template.goalId != null) "GOAL" else null,
            priority = priority,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            executionStrictness = executionStrictness,
            points = template.points,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1,
        )

    canonicalRecurringSeriesDao().convertTaskOneOffToCanonicalSeries(
        series = series.toAndroidEntity(),
        occurrence = occurrence,
        sourceTaskId = task.id,
        sourceExpectedVersion = task.version,
        updatedAt = now,
    )

    planningSources.replace(task, occurrence, now)
    rekeyTaskThemeAssignments(listOf(task to occurrence), now)
    return@withTransaction checkNotNull(
        dayTaskDao().getByIdForCanonicalRecurrenceSync(occurrence.id),
    ) {
        "Canonical recurring task occurrence was not persisted: ${occurrence.id}"
    }
}
