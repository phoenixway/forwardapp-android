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

    data class MusicNote(
        val id: String,
        val startEdit: Boolean = false,
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

    data object GlobalSearchHome : NavTarget

    data object CommandDeck : NavTarget

    data object Sync : NavTarget

    data object ManageContexts : NavTarget

    data object ScriptChooser : NavTarget

    data object AttachmentsLibrary : NavTarget

    data object ScriptsLibrary : NavTarget

    data object TacticalManagement : NavTarget

    data object StrategicManagement : NavTarget

    data class ProjectSettings(
        val goalId: String? = null,
        val projectId: String? = null,
    ) : NavTarget

    data class DayPlan(
        val dayPlanId: String,
        val startTab: String? = null,
    ) : NavTarget

    data class DayManagement(
        val date: Long,
        val startTab: String? = null,
    ) : NavTarget

    data class EditTask(
        val taskId: String,
    ) : NavTarget

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
