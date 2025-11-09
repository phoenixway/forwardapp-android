package com.romankozak.forwardappmobile.shared.features.daymanagement.data

import com.romankozak.forwardappmobile.shared.database.DayTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.DayTask
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskPriority
import com.romankozak.forwardappmobile.shared.features.daymanagement.data.model.TaskStatus

fun DayTasks.toDomain(): DayTask {
    return DayTask(
        id = this.id,
        dayPlanId = this.dayPlanId,
        title = this.title,
        description = this.description,
        goalId = this.goalId,
        projectId = this.projectId,
        activityRecordId = this.activityRecordId,
        recurringTaskId = this.recurringTaskId,
        taskType = this.taskType,
        entityId = this.entityId,
        order = this.order.toInt(),
        priority = this.priority,
        status = this.status,
        completed = this.completed != 0L,
        scheduledTime = this.scheduledTime,
        estimatedDurationMinutes = this.estimatedDurationMinutes,
        actualDurationMinutes = this.actualDurationMinutes,
        dueTime = this.dueTime,
        valueImportance = this.valueImportance.toFloat(),
        valueImpact = this.valueImpact.toFloat(),
        effort = this.effort.toFloat(),
        cost = this.cost.toFloat(),
        risk = this.risk.toFloat(),
        location = this.location,
        tags = this.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() },
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        nextOccurrenceTime = this.nextOccurrenceTime,
        points = this.points.toInt(),
    )
}

fun DayTask.toSqlDelight(): DayTasks {
    return DayTasks(
        id = this.id,
        dayPlanId = this.dayPlanId,
        title = this.title,
        description = this.description,
        goalId = this.goalId,
        projectId = this.projectId,
        activityRecordId = this.activityRecordId,
        recurringTaskId = this.recurringTaskId,
        taskType = this.taskType,
        entityId = this.entityId,
        order = this.order.toLong(),
        priority = this.priority,
        status = this.status,
        completed = if (this.completed) 1L else 0L,
        scheduledTime = this.scheduledTime,
        estimatedDurationMinutes = this.estimatedDurationMinutes,
        actualDurationMinutes = this.actualDurationMinutes,
        dueTime = this.dueTime,
        valueImportance = this.valueImportance.toDouble(),
        valueImpact = this.valueImpact.toDouble(),
        effort = this.effort.toDouble(),
        cost = this.cost.toDouble(),
        risk = this.risk.toDouble(),
        location = this.location,
        tags = this.tags?.joinToString(","),
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        nextOccurrenceTime = this.nextOccurrenceTime,
        points = this.points.toLong(),
    )
}