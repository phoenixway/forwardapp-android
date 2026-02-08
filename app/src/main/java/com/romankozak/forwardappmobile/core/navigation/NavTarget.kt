package com.romankozak.forwardappmobile.core.navigation

sealed interface NavTarget {
    // -------- HIGH LEVEL --------

    data object ContextHierarchy : NavTarget

    data class ContextDetail(
        val contextId: String,
        val goalId: String? = null,
        val itemIdToHighlight: String? = null,
        val inboxRecordIdToHighlight: String? = null,
        val initialViewMode: String? = null,
        val originContextId: String? = null,
    ) : NavTarget

    // -------- CONTENT --------

    data class NoteDocument(
        val id: String,
        val startEdit: Boolean = false,
    ) : NavTarget

    data class NoteDocumentEdit(
        val contextId: String? = null,
        val documentId: String? = null,
    ) : NavTarget

    data class Checklist(
        val id: String? = null,
        val contextId: String? = null,
    ) : NavTarget

    data class GlobalSearch(
        val query: String,
    ) : NavTarget

    data class ListChooser(
        val title: String,
        val currentParentId: String? = null,
        val disabledIds: String? = null,
    ) : NavTarget

    // -------- SCREENS --------

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
        val contextId: String? = null,
        val scriptId: String? = null,
    ) : NavTarget

    data class ContextStructure(
        val contextId: String,
    ) : NavTarget

    data object StructurePresets : NavTarget

    data class StructurePresetEditor(
        val presetId: String? = null,
        val copyFromPresetId: String? = null,
    ) : NavTarget

    // -------- IMPORT / EXPORT --------

    data class ImportExport(
        val uri: String? = null,
    ) : NavTarget

    data class GoalSettings(
        val goalId: String,
    ) : NavTarget
}
