package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class SharedBacklogItem(
    val id: String,
    val contextId: String,
    val title: String,
    val details: String?,
    val kind: SharedBacklogItemKind,
    val priority: SharedBacklogPriority,
    val isDone: Boolean = false,
)

@Serializable
enum class SharedBacklogItemKind(
    val title: String,
) {
    Feature("Feature"),
    Architecture("Architecture"),
    Task("Task"),
    Research("Research"),
    Goal("Goal"),
    Checklist("Checklist"),
    Note("Note"),
    Link("Link"),
}

@Serializable
enum class SharedBacklogPriority(
    val title: String,
    val weight: Int,
) {
    Critical("Critical", 4),
    High("High", 3),
    Medium("Medium", 2),
    Low("Low", 1),
}
