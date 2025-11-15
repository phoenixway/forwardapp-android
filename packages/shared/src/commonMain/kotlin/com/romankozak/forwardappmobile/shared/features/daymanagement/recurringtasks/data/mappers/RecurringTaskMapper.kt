package com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.mappers

import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.RecurringTasks
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurrenceRule
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.model.RecurringTask

fun RecurringTasks.toDomain(): RecurringTask =
    RecurringTask(
        id = id,
        title = title,
        description = description,
        goalId = goalId,
        durationMinutes = duration,
        priority = priority,
        points = points,
        recurrenceRule = RecurrenceRule(
            frequency = frequency,
            interval = interval,
            daysOfWeek = daysOfWeek,
        ),
        startDate = startDate,
        endDate = endDate,
    )

fun RecurringTask.toEntity() = RecurringTasks(
    id = id,
    title = title,
    description = description,
    goalId = goalId,
    duration = durationMinutes,
    priority = priority,
    points = points,
    frequency = recurrenceRule.frequency,
    interval = recurrenceRule.interval,
    daysOfWeek = recurrenceRule.daysOfWeek,
    startDate = startDate,
    endDate = endDate,
)
