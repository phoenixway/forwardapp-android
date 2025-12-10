package com.romankozak.forwardappmobile.ui.navigation

sealed interface NavTarget {

    /* -------- HIGH LEVEL -------- */

    data object ProjectHierarchy : NavTarget

    data class ProjectDetail(
        val projectId: String,
        val initialViewMode: String? = null,
    ) : NavTarget


    /* -------- CONTENT -------- */

    data class NoteDocument(
        val id: String,
    ) : NavTarget

    data class Checklist(
        val id: String,
    ) : NavTarget

    data class GlobalSearch(
        val query: String,
    ) : NavTarget


    /* -------- SCREENS -------- */

    data object Settings : NavTarget
    data object Reminders : NavTarget
    data object Tracker : NavTarget
    data object AiInsights : NavTarget
    data object LifeState : NavTarget
    data object AttachmentsLibrary : NavTarget
    data object ScriptsLibrary : NavTarget


    /* -------- IMPORT / EXPORT -------- */

    data class ImportExport(
        val uri: String? = null,
    ) : NavTarget
}
