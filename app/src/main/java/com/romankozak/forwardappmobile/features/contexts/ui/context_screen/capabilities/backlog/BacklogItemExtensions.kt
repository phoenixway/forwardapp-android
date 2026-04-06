package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues

fun BacklogItemContent.isCompleted(): Boolean =
    when (this) {
        is BacklogItemContent.GoalItem -> goal.completed
        is BacklogItemContent.ContextLinkItem -> project.isCompleted
        else -> false
    }

fun BacklogItemContent.isGroupedAtEnd(): Boolean =
    when (this) {
        is BacklogItemContent.GoalItem -> GoalStatusValues.isGroupedAtEnd(goal.goalStatus)
        is BacklogItemContent.ContextLinkItem -> project.isCompleted
        else -> false
    }

fun List<BacklogItemContent>.withCompletedAtEnd(): List<BacklogItemContent> {
    val (grouped, active) = partition { it.isGroupedAtEnd() }
    return active + grouped
}
