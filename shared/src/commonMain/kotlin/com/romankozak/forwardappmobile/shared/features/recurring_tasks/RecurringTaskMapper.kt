package com.romankozak.forwardappmobile.shared.features.recurring_tasks

import com.romankozak.forwardappmobile.shared.data.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.shared.data.database.models.RecurringTask
import com.romankozak.forwardappmobile.shared.database.RecurringTasks

fun RecurringTasks.toDomain(): RecurringTask {
    return RecurringTask(
        id = this.id,
        title = this.title,
        description = this.description,
        goalId = this.goalId,
        duration = this.duration?.toInt(),
        priority = this.priority,
        points = this.points.toInt(),
        recurrenceRule = RecurrenceRule(
            frequency = this.frequency,
            interval = this.interval.toInt(),
            daysOfWeek = this.daysOfWeek
        ),
        startDate = this.startDate,
        endDate = this.endDate
    )
}