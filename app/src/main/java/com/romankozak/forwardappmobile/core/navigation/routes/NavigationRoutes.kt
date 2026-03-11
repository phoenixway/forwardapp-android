package com.romankozak.forwardappmobile.core.navigation.routes

import java.net.URLEncoder

object NavigationRoutes {
    const val MAIN_GRAPH = "main_graph"
    const val COMMAND_DECK = "command_deck_screen"
    const val CHARACTER = "character_screen"
    const val GOAL_LISTS = "goal_lists_screen"
    const val AI_INSIGHTS = "ai_insights_screen"
    const val LIFE_STATE = "life_state_screen"
    const val KANBAN = "kanban_screen"
    const val VET_CASE_SUMMARY = "vet_case_summary_screen"
    const val VET_CASE_HISTORY = "vet_case_history_screen"
    const val CHAT = "chat_screen"
    const val TACTICAL_MANAGEMENT = "tactical_management_screen"
    const val STRATEGIC_MANAGEMENT = "strategic_management_screen"

    const val CONTEXT_DETAIL = "goal_detail_screen"
    const val GLOBAL_SEARCH = "global_search_screen"
    const val GLOBAL_SEARCH_HOME = "global_search"
    const val PROJECT_STRUCTURE = "project_structure_screen"
    const val STRUCTURE_PRESETS = "structure_presets_screen"
    const val STRUCTURE_PRESET_EDITOR = "structure_preset_editor_screen"
    const val ATTACHMENTS_LIBRARY = "attachments_library_screen"
    const val SCRIPTS_LIBRARY = "scripts_library_screen"
    const val SCRIPT_CHOOSER = "script_chooser_screen"
    const val SETTINGS = "settings_screen"
    const val MANAGE_CONTEXTS = "manage_contexts_screen"
    const val ACTIVITY_TRACKER = "activity_tracker_screen"
    const val PROJECT_SETTINGS = "project_settings_screen"
    const val GOAL_SETTINGS = "goal_settings_screen"
    const val SYNC = "sync_screen"
    const val NOTE_DOCUMENT = "note_document_screen"
    const val NOTE_DOCUMENT_CREATE = "note_document_create_screen"
    const val NOTE_DOCUMENT_EDIT = "note_document_edit_screen"
    const val SCRIPT_EDITOR = "script_editor_screen"
    const val CHECKLIST = "checklist_screen"
    const val MUSIC_NOTE = "music_note_screen"
    const val LIST_CHOOSER = "list_chooser_screen"
    const val REMINDERS = "reminders_screen"
    const val EDIT_TASK = "edit_task_screen"
    const val INBOX_EDITOR = "inbox_editor_screen"
    const val DAY_MANAGEMENT = "day_management"
    const val DAY_PLAN = "day_plan_screen"
    const val SELECTIVE_IMPORT = "selective_import_screen"
    const val PLACEHOLDER = "placeholder_screen"

    const val ARG_DAY_PLAN_DATE = "dayPlanDate"
    const val ARG_DAY_PLAN_ID = "dayPlanId"
    const val ARG_START_TAB = "startTab"
    const val ARG_FILE_URI = "fileUri"

    const val SELECTIVE_IMPORT_PATTERN = "$SELECTIVE_IMPORT?$ARG_FILE_URI={$ARG_FILE_URI}"
    const val PLACEHOLDER_PATTERN = "$PLACEHOLDER/{viewId}/{screenId}"
    const val CONTEXT_DETAIL_PATTERN =
        "$CONTEXT_DETAIL/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}" +
            "&inboxRecordIdToHighlight={inboxRecordIdToHighlight}&initialViewMode={initialViewMode}" +
            "&originContextId={originContextId}"
    const val SCRIPT_EDITOR_PATTERN = "$SCRIPT_EDITOR?projectId={projectId}&scriptId={scriptId}"
    const val NOTE_DOCUMENT_EDIT_PATTERN = "$NOTE_DOCUMENT_EDIT?projectId={projectId}&documentId={documentId}"
    const val CHECKLIST_PATTERN = "$CHECKLIST?projectId={projectId}&checklistId={checklistId}"
    const val MUSIC_NOTE_PATTERN = "$MUSIC_NOTE/{musicNoteId}?startEdit={startEdit}"
    const val PROJECT_SETTINGS_PATTERN = "$PROJECT_SETTINGS?goalId={goalId}&projectId={projectId}"
    const val STRUCTURE_PRESET_EDITOR_PATTERN =
        "$STRUCTURE_PRESET_EDITOR?presetId={presetId}&copyFromPresetId={copyFromPresetId}"

    fun contextDetail(
        contextId: String,
        goalId: String? = null,
        itemIdToHighlight: String? = null,
        inboxRecordIdToHighlight: String? = null,
        initialViewMode: String? = null,
        originContextId: String? = null,
    ): String =
        "$CONTEXT_DETAIL/$contextId" +
            queryOf(
                listOf(
                    "goalId" to goalId,
                    "itemIdToHighlight" to itemIdToHighlight,
                    "inboxRecordIdToHighlight" to inboxRecordIdToHighlight,
                    "initialViewMode" to initialViewMode,
                    "originContextId" to originContextId,
                ),
            )

    fun noteDocument(
        id: String,
        startEdit: Boolean,
    ): String = "$NOTE_DOCUMENT/$id" + if (startEdit) "?startEdit=true" else ""

    fun noteDocumentEdit(
        projectId: String? = null,
        documentId: String? = null,
    ): String =
        NOTE_DOCUMENT_EDIT +
            queryOf(
                listOf(
                    "projectId" to projectId,
                    "documentId" to documentId,
                ),
            )

    fun checklist(
        projectId: String? = null,
        checklistId: String? = null,
    ): String =
        CHECKLIST +
            queryOf(
                listOf(
                    "projectId" to projectId,
                    "checklistId" to checklistId,
                ),
            )

    fun musicNote(
        id: String,
        startEdit: Boolean,
    ): String = "$MUSIC_NOTE/$id" + if (startEdit) "?startEdit=true" else ""

    fun globalSearch(query: String): String = "$GLOBAL_SEARCH/$query"

    fun listChooser(
        title: String,
        currentParentId: String? = null,
        disabledIds: String? = null,
    ): String =
        "$LIST_CHOOSER/${URLEncoder.encode(title, "UTF-8")}" +
            queryOf(
                listOf(
                    "currentParentId" to currentParentId,
                    "disabledIds" to disabledIds,
                ),
            )

    fun projectSettings(
        goalId: String? = null,
        projectId: String? = null,
    ): String =
        PROJECT_SETTINGS +
            queryOf(
                listOf(
                    "goalId" to goalId,
                    "projectId" to projectId,
                ),
            )

    fun goalSettings(goalId: String): String = "$GOAL_SETTINGS/$goalId"

    fun dayPlan(
        dayPlanId: String,
        startTab: String? = null,
    ): String = "$DAY_PLAN/$dayPlanId" + queryOf(listOf(ARG_START_TAB to startTab))

    fun dayManagement(
        date: Long,
        startTab: String? = null,
    ): String = "$DAY_MANAGEMENT/$date" + queryOf(listOf(ARG_START_TAB to startTab))

    fun editTask(taskId: String): String = "$EDIT_TASK/$taskId"

    fun scriptEditor(
        projectId: String? = null,
        scriptId: String? = null,
    ): String =
        SCRIPT_EDITOR +
            queryOf(
                listOf(
                    "projectId" to projectId,
                    "scriptId" to scriptId,
                ),
            )

    fun contextStructure(contextId: String): String = "$PROJECT_STRUCTURE/$contextId"

    fun structurePresetEditor(
        presetId: String? = null,
        copyFromPresetId: String? = null,
    ): String =
        STRUCTURE_PRESET_EDITOR +
            queryOf(
                listOf(
                    "presetId" to presetId,
                    "copyFromPresetId" to copyFromPresetId,
                ),
            )

    fun selectiveImport(fileUri: String? = null): String =
        if (fileUri == null) {
            SELECTIVE_IMPORT
        } else {
            "$SELECTIVE_IMPORT?$ARG_FILE_URI=${URLEncoder.encode(fileUri, "UTF-8")}"
        }

    private fun queryOf(params: List<Pair<String, String?>>): String {
        val nonNullParams = params.filter { it.second != null }.map { it.first to it.second!! }
        return if (nonNullParams.isEmpty()) {
            ""
        } else {
            "?" + nonNullParams.joinToString("&") { (key, value) -> "$key=$value" }
        }
    }
}
