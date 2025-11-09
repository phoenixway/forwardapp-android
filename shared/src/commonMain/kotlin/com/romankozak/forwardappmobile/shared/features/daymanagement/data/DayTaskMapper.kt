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
        order = this.order_, // SQLDelight uses order_ for "order" column
        priority = TaskPriority.valueOf(this.priority),
        status = TaskStatus.valueOf(this.status),
        completed = this.completed != 0L,
        scheduledTime = this.scheduledTime,
        estimatedDurationMinutes = this.estimatedDurationMinutes,
        actualDurationMinutes = this.actualDurationMinutes,
        dueTime = this.dueTime,
        valueImportance = this.valueImportance,
        valueImpact = this.valueImpact,
        effort = this.effort,
        cost = this.cost,
        risk = this.risk,
        location = this.location,
        tags = this.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() },
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        nextOccurrenceTime = this.nextOccurrenceTime,
        points = this.points,
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
        order_ = this.order,
        priority = this.priority.name,
        status = this.status.name,
        completed = if (this.completed) 1L else 0L,
        scheduledTime = this.scheduledTime,
        estimatedDurationMinutes = this.estimatedDurationMinutes,
        actualDurationMinutes = this.actualDurationMinutes,
        dueTime = this.dueTime,
        valueImportance = this.valueImportance,
        valueImpact = this.valueImpact,
        effort = this.effort,
        cost = this.cost,
        risk = this.risk,
        location = this.location,
        tags = this.tags?.joinToString(","),
        notes = this.notes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        completedAt = this.completedAt,
        nextOccurrenceTime = this.nextOccurrenceTime,
        points = this.points,
    )
}
