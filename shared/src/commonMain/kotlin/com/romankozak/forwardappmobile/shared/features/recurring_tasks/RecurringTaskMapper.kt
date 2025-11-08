package com.romankozak.forwardappmobile.shared.features.recurring_tasks

import com.romankozak.forwardappmobile.shared.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.data.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.shared.data.database.models.RecurringTask
import com.romankozak.forwardappmobile.shared.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.shared.database.Recurring_tasks

fun Recurring_tasks.toDomain(): RecurringTask {
    return RecurringTask(
        id = this.id,
        title = this.title,
        description = this.description,
        goalId = this.goal_id,
        duration = this.duration?.toInt(),
        priority = TaskPriority.valueOf(this.priority),
        points = this.points.toInt(),
        recurrenceRule = RecurrenceRule(
            frequency = RecurrenceFrequency.valueOf(this.frequency),
            interval = this.interval.toInt(),
            daysOfWeek = this.days_of_week?.split(",")
        ),
        startDate = this.start_date,
        endDate = this.end_date
    )
}
