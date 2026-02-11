package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.navigation

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import java.net.URLDecoder

class ContextRouteResolver(
    private val handleLinkClickRoute: String,
) {
    sealed class ResolveResult {
        data object Back : ResolveResult()

        data class GoalDetail(val contextId: String) : ResolveResult()

        data class HandleLinkClick(val rawTarget: String) : ResolveResult()

        data class Navigate(val target: NavTarget) : ResolveResult()

        data class Unknown(val route: String) : ResolveResult()
    }

    fun resolve(route: String): ResolveResult {
        if (route == "back") return ResolveResult.Back
        if (route.startsWith("goal_detail_screen/")) {
            return ResolveResult.GoalDetail(route.substringAfter("goal_detail_screen/"))
        }
        if (route.startsWith(handleLinkClickRoute)) {
            val rawTarget = route.substringAfter("$handleLinkClickRoute/")
            return ResolveResult.HandleLinkClick(rawTarget)
        }
        val navTarget = parseRouteToNavTarget(route) ?: return ResolveResult.Unknown(route)
        return ResolveResult.Navigate(navTarget)
    }

    private fun parseRouteToNavTarget(route: String): NavTarget? {
        return when {
            route.startsWith("global_search_screen/") -> {
                val query = URLDecoder.decode(route.substringAfter("global_search_screen/"), "UTF-8")
                NavTarget.GlobalSearch(query)
            }

            route.startsWith("goal_settings_screen/") -> {
                val goalId = route.substringAfter("goal_settings_screen/")
                NavTarget.GoalSettings(goalId)
            }

            route.startsWith("note_document_screen/") -> {
                val tail = route.substringAfter("note_document_screen/")
                val id = tail.substringBefore("?")
                val startEdit = tail.substringAfter("?", "").contains("startEdit=true")
                NavTarget.NoteDocument(id = id, startEdit = startEdit)
            }

            route.startsWith("note_document_edit_screen") -> {
                val paramMap = parseQueryParams(route.substringAfter("?", ""))
                NavTarget.NoteDocumentEdit(
                    contextId =
                        paramMap["projectId"]?.takeIf { it.isNotBlank() }
                            ?: paramMap["contextId"]?.takeIf { it.isNotBlank() },
                    documentId = paramMap["documentId"]?.takeIf { it.isNotBlank() },
                )
            }

            route.startsWith("checklist_screen") -> {
                val paramMap = parseQueryParams(route.substringAfter("?", ""))
                NavTarget.Checklist(
                    id = paramMap["checklistId"]?.takeIf { it.isNotBlank() },
                    contextId =
                        paramMap["projectId"]?.takeIf { it.isNotBlank() }
                            ?: paramMap["contextId"]?.takeIf { it.isNotBlank() },
                )
            }

            route.startsWith("list_chooser_screen/") -> {
                val titleEncoded = route.substringAfter("list_chooser_screen/").substringBefore("?")
                val paramMap = parseQueryParams(route.substringAfter("?", ""))
                NavTarget.ListChooser(
                    title = URLDecoder.decode(titleEncoded, "UTF-8"),
                    currentParentId = paramMap["currentParentId"]?.takeIf { it.isNotBlank() },
                    disabledIds = paramMap["disabledIds"]?.takeIf { it.isNotBlank() },
                )
            }

            route == "activity_tracker_screen" -> NavTarget.Tracker
            route == "reminders_screen" -> NavTarget.Reminders
            route == "settings_screen" -> NavTarget.Settings
            route == "ai_insights_screen" -> NavTarget.AiInsights
            route == "life_state_screen" -> NavTarget.LifeState
            route == "attachments_library_screen" -> NavTarget.AttachmentsLibrary
            route == "scripts_library_screen" -> NavTarget.ScriptsLibrary
            route == "tactical_management_screen" -> NavTarget.TacticalManagement
            else -> null
        }
    }

    private fun parseQueryParams(params: String): Map<String, String> =
        params
            .split("&")
            .mapNotNull {
                val parts = it.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
}
