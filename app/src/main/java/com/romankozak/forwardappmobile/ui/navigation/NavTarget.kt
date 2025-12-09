package com.romankozak.forwardappmobile.ui.navigation

sealed interface NavTarget {
    data object ProjectHierarchy : NavTarget
    data class ProjectDetail(val id: String) : NavTarget
    data class NoteDocument(val id: String) : NavTarget
    data class Checklist(val id: String) : NavTarget
    data class GlobalSearch(val query: String) : NavTarget

    data object Tracker : NavTarget
    data object Reminders : NavTarget
    data object Settings : NavTarget
    data object LifeState : NavTarget
    data object AiInsights : NavTarget
    data object AttachmentsLibrary : NavTarget
    data object ScriptsLibrary : NavTarget
}
