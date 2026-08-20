package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority as AndroidTaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus as AndroidTaskStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem as AndroidDayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType as AndroidDayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan as AndroidDayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask as AndroidDayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusItem as CanonicalDayFocusItem
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusType as CanonicalDayFocusType
import com.romankozak.forwardappmobile.shared.core.models.day.DayPlan as CanonicalDayPlan
import com.romankozak.forwardappmobile.shared.core.models.day.DayStatus as CanonicalDayStatus
import com.romankozak.forwardappmobile.shared.core.models.day.DayTask as CanonicalDayTask
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority as CanonicalTaskPriority
import com.romankozak.forwardappmobile.shared.core.models.day.TaskStatus as CanonicalTaskStatus

fun AndroidDayPlan.toCanonicalDayPlan(dayKey: String): CanonicalDayPlan =
    CanonicalDayPlan(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt ?: createdAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        dayKey = dayKey,
        name = name,
        linkedProjectIds = linkedProjectIds.orEmpty(),
        linkedAttachmentIds = linkedAttachmentIds.orEmpty(),
        status = CanonicalDayStatus.valueOf(status.name),
        reflection = reflection,
        energyLevel = energyLevel,
        mood = mood,
        weatherConditions = weatherConditions,
        predictedDurationMinutes = predictedDurationMinutes,
        totalPlannedMinutes = totalPlannedMinutes,
        totalCompletedMinutes = totalCompletedMinutes,
        completionPercentage = completionPercentage,
    )

fun AndroidDayTask.toCanonicalDayTask(): CanonicalDayTask =
    CanonicalDayTask(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt ?: createdAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        dayPlanId = dayPlanId,
        recurrence = toCanonicalRecurrenceOrigin(),
        title = title,
        description = description,
        goalId = goalId,
        projectId = projectId,
        linkedProjectIds = linkedProjectIds.orEmpty(),
        linkedAttachmentIds = linkedAttachmentIds.orEmpty(),
        activityRecordId = activityRecordId,
        taskType = taskType,
        entityId = entityId,
        order = order,
        priority = CanonicalTaskPriority.valueOf(priority.name),
        status = CanonicalTaskStatus.valueOf(status.name),
        completed = completed,
        scheduledTime = scheduledTime,
        estimatedDurationMinutes = estimatedDurationMinutes,
        actualDurationMinutes = actualDurationMinutes,
        dueTime = dueTime,
        executionStrictness = executionStrictness.name,
        valueImportance = valueImportance,
        valueImpact = valueImpact,
        effort = effort,
        cost = cost,
        risk = risk,
        location = location,
        tags = tags.orEmpty(),
        notes = notes,
        completedAt = completedAt,
        points = points,
    )

fun AndroidDayFocusItem.toCanonicalDayFocusItem(): CanonicalDayFocusItem =
    CanonicalDayFocusItem(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt ?: createdAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        dayPlanId = dayPlanId,
        recurrence = toCanonicalRecurrenceOrigin(),
        title = title,
        notes = notes,
        relatedLinks = relatedLinks.toCanonicalRelatedLinks(),
        type = CanonicalDayFocusType.valueOf(type.name),
        budgetPercent = budgetPercent,
        order = order,
    )

fun CanonicalDayTask.toAndroidDayTask(): AndroidDayTask =
    AndroidDayTask(
        id = id,
        dayPlanId = dayPlanId,
        title = title,
        description = description,
        goalId = goalId,
        projectId = projectId,
        linkedProjectIds = linkedProjectIds,
        linkedAttachmentIds = linkedAttachmentIds,
        activityRecordId = activityRecordId,
        recurringTaskId = null,
        recurrenceSeriesId = recurrence?.seriesId,
        recurrenceOccurrenceDayKey = recurrence?.occurrenceDayKey,
        recurrenceSourceSeriesVersion = recurrence?.sourceSeriesVersion,
        taskType = taskType,
        entityId = entityId,
        order = order,
        priority = AndroidTaskPriority.valueOf(priority.name),
        status = AndroidTaskStatus.valueOf(status.name),
        completed = completed,
        scheduledTime = scheduledTime,
        estimatedDurationMinutes = estimatedDurationMinutes,
        actualDurationMinutes = actualDurationMinutes,
        dueTime = dueTime,
        executionStrictness = TaskExecutionStrictness.valueOf(executionStrictness ?: "NORMAL"),
        valueImportance = valueImportance,
        valueImpact = valueImpact,
        effort = effort,
        cost = cost,
        risk = risk,
        location = location,
        tags = tags,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        completedAt = completedAt,
        nextOccurrenceTime = null,
        points = points,
    )

fun CanonicalDayFocusItem.toAndroidDayFocusItem(): AndroidDayFocusItem =
    AndroidDayFocusItem(
        id = id,
        dayPlanId = dayPlanId,
        title = title,
        notes = notes,
        relatedLinks = relatedLinks.toAndroidRelatedLinks(),
        type = AndroidDayFocusType.valueOf(type.name),
        isEveryday = false,
        recurringKey = null,
        recurrenceSeriesId = recurrence?.seriesId,
        recurrenceOccurrenceDayKey = recurrence?.occurrenceDayKey,
        recurrenceSourceSeriesVersion = recurrence?.sourceSeriesVersion,
        budgetPercent = budgetPercent,
        order = order,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )
