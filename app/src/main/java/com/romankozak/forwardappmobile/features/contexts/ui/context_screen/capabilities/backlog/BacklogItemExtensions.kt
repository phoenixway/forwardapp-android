package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent

fun BacklogItemContent.isCompleted(): Boolean =
    when (this) {
        is BacklogItemContent.GoalItem -> goal.completed
        is BacklogItemContent.ContextLinkItem -> project.isCompleted
        else -> false
    }

fun List<BacklogItemContent>.withCompletedAtEnd(): List<BacklogItemContent> {
    val (completed, active) = partition { it.isCompleted() }
    return active + completed
}
