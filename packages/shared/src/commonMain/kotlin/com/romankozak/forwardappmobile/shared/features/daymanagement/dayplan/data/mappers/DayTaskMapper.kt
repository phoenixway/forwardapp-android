package com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.mappers

import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.DayTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.model.TaskStatus

fun DayTasks.toDomain(): DayTask =
    DayTask(
        id = id,
        dayPlanId = dayPlanId,
        title = title,
        description = description,
        goalId = goalId,
        projectId = projectId,
        activityRecordId = activityRecordId,
        recurringTaskId = recurringTaskId,
        taskType = taskType,
        entityId = entityId,
        order = order,
        priority = priority,
        status = status,
        completed = completed,
        scheduledTime = scheduledTime,
        estimatedDurationMinutes = estimatedDurationMinutes,
        actualDurationMinutes = actualDurationMinutes,
        dueTime = dueTime,
        valueImportance = valueImportance.toFloat(),
        valueImpact = valueImpact.toFloat(),
        effort = effort.toFloat(),
        cost = cost.toFloat(),
        risk = risk.toFloat(),
        location = location,
        tags = tags,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completedAt = completedAt,
        nextOccurrenceTime = nextOccurrenceTime,
        points = points,
    )
