package com.romankozak.forwardappmobile.shared.contracts.contexts

import kotlinx.serialization.Serializable

@Serializable
data class SharedContextSummary(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val status: SharedContextStatus,
    val defaultView: SharedContextView,
    val score: Int,
    val isCompleted: Boolean,
)

@Serializable
enum class SharedContextStatus(
    val title: String,
) {
    NoPlan("Без плану"),
    Planning("Планується"),
    InProgress("В реалізації"),
    Completed("Завершено"),
    OnHold("Відкладено"),
    Paused("На паузі"),
}

@Serializable
enum class SharedContextView(
    val title: String,
) {
    Backlog("Backlog"),
    Inbox("Inbox"),
    Connections("Connections"),
    Dashboard("Dashboard"),
    Direction("Direction"),
    Log("Log"),
    Artifact("Artifact"),
    KeyProblems("Key Problems"),
}
