package com.romankozak.forwardappmobile.ui.navigation

import java.net.URLEncoder

object NavTargetRouter {

    fun routeOf(target: NavTarget): String =
        when (target) {

            NavTarget.ProjectHierarchy ->
                "goal_lists_screen"

            is NavTarget.ProjectDetail ->
                if (target.initialViewMode != null)
                    "goal_detail_screen/${target.projectId}?initialViewMode=${target.initialViewMode}"
                else
                    "goal_detail_screen/${target.projectId}"

            is NavTarget.NoteDocument ->
                "note_document_screen/${target.id}"

            is NavTarget.Checklist ->
                "checklist_screen?checklistId=${target.id}"

            is NavTarget.GlobalSearch ->
                "global_search_screen/${target.query}"

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

            NavTarget.AttachmentsLibrary ->
                "attachments_library_screen"

            NavTarget.ScriptsLibrary ->
                "scripts_library_screen"

            is NavTarget.ImportExport ->
                if (target.uri != null)
                    "selective_import_screen/${URLEncoder.encode(target.uri, "UTF-8")}"
                else
                    "selective_import_screen"
        }
}
