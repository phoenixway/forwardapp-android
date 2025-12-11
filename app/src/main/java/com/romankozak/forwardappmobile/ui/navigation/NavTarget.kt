package com.romankozak.forwardappmobile.ui.navigation

sealed interface NavTarget {

    /* -------- HIGH LEVEL -------- */

    data object ProjectHierarchy : NavTarget

    data class ProjectDetail(
        val projectId: String,
        val goalId: String? = null,
        val itemIdToHighlight: String? = null,
        val inboxRecordIdToHighlight: String? = null,
        val initialViewMode: String? = null,
    ) : NavTarget


    /* -------- CONTENT -------- */

    data class NoteDocument(
        val id: String,
        val startEdit: Boolean = false,
    ) : NavTarget

    data class NoteDocumentEdit(
        val projectId: String? = null,
        val documentId: String? = null,
    ) : NavTarget

    data class Checklist(
        val id: String? = null,
        val projectId: String? = null,
    ) : NavTarget

    data class GlobalSearch(
        val query: String,
    ) : NavTarget

    data class ListChooser(
        val title: String,
        val currentParentId: String? = null,
        val disabledIds: String? = null,
    ) : NavTarget


    /* -------- SCREENS -------- */

    data object Settings : NavTarget
    data object Reminders : NavTarget
    data object Tracker : NavTarget
    data object AiInsights : NavTarget
    data object LifeState : NavTarget
    data object Chat : NavTarget
    data object AttachmentsLibrary : NavTarget
    data object ScriptsLibrary : NavTarget
    data object TacticalManagement : NavTarget

    data class ScriptEditor(
        val projectId: String? = null,
        val scriptId: String? = null,
    ) : NavTarget


    /* -------- IMPORT / EXPORT -------- */

    data class ImportExport(
        val uri: String? = null,
    ) : NavTarget
}
