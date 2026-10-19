package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority as AndroidTaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyDayOfWeek
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.localDayKeyOf
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority as CanonicalTaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.*


internal fun DayTask.matchesTemplate(template: RecurringTaskTemplate): Boolean =
    title == template.title &&
        description?.trim()?.takeIf { it.isNotEmpty() } ==
        template.description?.trim()?.takeIf { it.isNotEmpty() } &&
        goalId == template.goalId &&
        logicalProjectId == template.projectId &&
        linkedProjectIds.orEmpty() == template.linkedProjectIds &&
        linkedAttachmentIds.orEmpty() == template.linkedAttachmentIds &&
        taskType == effectiveTaskType(template) &&
        priority.name == template.priority.name &&
        estimatedDurationMinutes == template.estimatedDurationMinutes &&
        executionStrictness == effectiveExecutionStrictness(template) &&
        points == template.points

internal fun effectiveTaskType(template: RecurringTaskTemplate): String? =
    template.taskType ?: if (template.goalId != null) "GOAL" else null

internal fun effectiveExecutionStrictness(
    template: RecurringTaskTemplate,
): TaskExecutionStrictness =
    TaskExecutionStrictness.valueOf(template.executionStrictness ?: "NORMAL")

internal fun buildTemplate(
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
): RecurringTaskTemplate =
    RecurringTaskTemplate(
        title = title.trim(),
        description = description?.trim()?.takeIf { it.isNotEmpty() },
        goalId = goalId,
        linkedProjectIds = linkedProjectIds.filter { it.isNotBlank() }.distinct(),
        linkedAttachmentIds = linkedAttachmentIds.filter { it.isNotBlank() }.distinct(),
        priority = CanonicalTaskPriority.valueOf(priority.name),
        estimatedDurationMinutes = estimatedDurationMinutes,
        points = points,
        projectId = projectId,
        taskType = taskType,
        executionStrictness = executionStrictness.name,
    )

internal fun RecurrenceRule.normalizedForSeriesStart(startDayKey: String): RecurrenceRule {
    require(interval >= 1) { "Invalid recurrence interval: $interval" }

    return when (frequency) {
        RecurrenceFrequency.WEEKLY -> {
            val selectedDays = daysOfWeek.orEmpty()
            if (selectedDays.isEmpty()) {
                copy(daysOfWeek = null)
            } else {
                val startDay = localDayKeyDayOfWeek(startDayKey)
                copy(
                    daysOfWeek =
                        (selectedDays + startDay)
                            .distinct()
                            .sortedBy { it.ordinal },
                )
            }
        }

        else -> copy(daysOfWeek = null)
    }
}

internal fun canonicalLocalDayKey(timestamp: Long): String {
    val calendar =
        java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
    return localDayKeyOf(
        year = calendar.get(java.util.Calendar.YEAR),
        month = calendar.get(java.util.Calendar.MONTH) + 1,
        day = calendar.get(java.util.Calendar.DAY_OF_MONTH),
    )
}
