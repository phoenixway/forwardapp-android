package com.romankozak.forwardappmobile.ui.features.backlog

import com.romankozak.forwardappmobile.data.database.models.ListItemContent

fun ListItemContent.isCompleted(): Boolean =
    when (this) {
        is ListItemContent.GoalItem -> goal.completed
        is ListItemContent.SublistItem -> project.isCompleted
        else -> false
    }

fun List<ListItemContent>.withCompletedAtEnd(): List<ListItemContent> {
    val (completed, active) = partition { it.isCompleted() }
    return active + completed
}
