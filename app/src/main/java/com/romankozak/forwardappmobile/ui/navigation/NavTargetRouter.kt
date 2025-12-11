package com.romankozak.forwardappmobile.ui.navigation

import java.net.URLEncoder

object NavTargetRouter {

    private fun buildQuery(params: List<Pair<String, String?>>): String {
        val nonNullParams = params.filter { it.second != null }.map { it.first to it.second!! }
        return if (nonNullParams.isEmpty()) {
            ""
        } else {
            "?" + nonNullParams.joinToString("&") { (key, value) -> "$key=$value" }
        }
    }

    fun routeOf(target: NavTarget): String =
        when (target) {

            NavTarget.ProjectHierarchy ->
                "goal_lists_screen"

            is NavTarget.ProjectDetail ->
                "goal_detail_screen/${target.projectId}" +
                    buildQuery(
                        listOf(
                            "goalId" to target.goalId,
                            "itemIdToHighlight" to target.itemIdToHighlight,
                            "inboxRecordIdToHighlight" to target.inboxRecordIdToHighlight,
                            "initialViewMode" to target.initialViewMode,
                        )
                    )

            is NavTarget.NoteDocument ->
                "note_document_screen/${target.id}" +
                    if (target.startEdit) "?startEdit=true" else ""

            is NavTarget.NoteDocumentEdit ->
                "note_document_edit_screen" +
                    buildQuery(
                        listOf(
                            "projectId" to target.projectId,
                            "documentId" to target.documentId,
                        )
                    )

            is NavTarget.Checklist ->
                "checklist_screen" +
                    buildQuery(
                        listOf(
                            "projectId" to target.projectId,
                            "checklistId" to target.id,
                        )
                    )

            is NavTarget.GlobalSearch ->
                "global_search_screen/${target.query}"

            is NavTarget.ListChooser ->
                "list_chooser_screen/${URLEncoder.encode(target.title, "UTF-8")}" +
                    buildQuery(
                        listOf(
                            "currentParentId" to target.currentParentId,
                            "disabledIds" to target.disabledIds,
                        )
                    )

            NavTarget.Tracker ->
                "activity_tracker_screen"

            NavTarget.Reminders ->
                "reminders_screen"

            NavTarget.Settings ->
                "settings_screen"

            NavTarget.LifeState ->
                "life_state_screen"

            NavTarget.AiInsights ->
                "ai_insights_screen"
            NavTarget.Chat ->
                "chat_screen"

            NavTarget.AttachmentsLibrary ->
                "attachments_library_screen"

            NavTarget.ScriptsLibrary ->
                "scripts_library_screen"

            NavTarget.TacticalManagement ->
                "tactical_management_screen"

            is NavTarget.ProjectDetail ->
                "project_screen?projectId=${target.projectId}" +
                    (target.goalId?.let { "&goalId=$it" } ?: "")

            is NavTarget.ScriptEditor ->
                "script_editor_screen" +
                    buildQuery(
                        listOf(
                            "projectId" to target.projectId,
                            "scriptId" to target.scriptId,
                        )
                    )

            is NavTarget.ImportExport ->
                if (target.uri != null)
                    "selective_import_screen/${URLEncoder.encode(target.uri, "UTF-8")}"
                else
                    "selective_import_screen"
        }
}
