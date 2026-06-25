package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel

internal fun submitTodayQuickTask(
    dayPlanViewModel: DayPlanViewModel,
    dayPlanId: String?,
    rawInput: String,
): Boolean {
    val taskDraft = buildTodayQuickTaskDraft(rawInput)
    return if (dayPlanId == null || taskDraft == null) {
        false
    } else {
        dayPlanViewModel.addTask(
            dayPlanId = dayPlanId,
            title = taskDraft.title,
            description = taskDraft.description,
            duration = null,
            scheduledTime = null,
            dueTime = null,
            priority = TaskPriority.MEDIUM,
            strictness = TaskExecutionStrictness.NORMAL,
            recurrenceRule = null,
            points = 0,
        )
        true
    }
}
