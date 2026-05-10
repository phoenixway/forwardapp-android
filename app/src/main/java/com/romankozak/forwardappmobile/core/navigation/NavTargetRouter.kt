package com.romankozak.forwardappmobile.core.navigation

import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes

object NavTargetRouter {
    fun routeOf(target: NavTarget): String =
        when (target) {
            is NavTarget.ContextHierarchy ->
                NavigationRoutes.goalLists(target.projectIdToReveal)

            is NavTarget.ContextDetail ->
                NavigationRoutes.contextDetail(
                    contextId = target.contextId,
                    goalId = target.goalId,
                    itemIdToHighlight = target.itemIdToHighlight,
                    inboxRecordIdToHighlight = target.inboxRecordIdToHighlight,
                    initialViewMode = target.initialViewMode,
                    initialTagQuery = target.initialTagQuery,
                    originContextId = target.originContextId,
                )

            is NavTarget.NoteDocument ->
                NavigationRoutes.noteDocument(id = target.id, startEdit = target.startEdit)

            is NavTarget.JournalDocument ->
                NavigationRoutes.journalDocument(id = target.id, startEdit = target.startEdit)

            is NavTarget.NoteDocumentEdit ->
                NavigationRoutes.noteDocumentEdit(
                    projectId = target.contextId,
                    documentId = target.documentId,
                )

            is NavTarget.Checklist ->
                NavigationRoutes.checklist(
                    projectId = target.contextId,
                    checklistId = target.id,
                )

            is NavTarget.MusicNote ->
                NavigationRoutes.musicNote(id = target.id, startEdit = target.startEdit)

            is NavTarget.GlobalSearch ->
                NavigationRoutes.globalSearch(target.query)

            is NavTarget.ListChooser ->
                NavigationRoutes.listChooser(
                    title = target.title,
                    currentParentId = target.currentParentId,
                    disabledIds = target.disabledIds,
                )

            NavTarget.Tracker ->
                NavigationRoutes.ACTIVITY_TRACKER

            NavTarget.Reminders ->
                NavigationRoutes.REMINDERS

            NavTarget.Settings ->
                NavigationRoutes.SETTINGS

            NavTarget.LifeState ->
                NavigationRoutes.LIFE_STATE

            NavTarget.AiInsights ->
                NavigationRoutes.AI_INSIGHTS
            NavTarget.Chat ->
                NavigationRoutes.CHAT

            NavTarget.GlobalSearchHome ->
                NavigationRoutes.GLOBAL_SEARCH_HOME

            NavTarget.CommandDeck ->
                NavigationRoutes.COMMAND_DECK

            NavTarget.Sync ->
                NavigationRoutes.SYNC

            NavTarget.ManageContextMarkers ->
                NavigationRoutes.MANAGE_CONTEXT_MARKERS

            NavTarget.ScriptChooser ->
                NavigationRoutes.SCRIPT_CHOOSER

            NavTarget.AttachmentsLibrary ->
                NavigationRoutes.ATTACHMENTS_LIBRARY

            NavTarget.ScriptsLibrary ->
                NavigationRoutes.SCRIPTS_LIBRARY

            NavTarget.TacticalManagement ->
                NavigationRoutes.TACTICAL_MANAGEMENT

            NavTarget.StrategicManagement ->
                NavigationRoutes.STRATEGIC_MANAGEMENT

            is NavTarget.GoalSettings ->
                NavigationRoutes.goalSettings(target.goalId)

            is NavTarget.ProjectSettings ->
                NavigationRoutes.projectSettings(
                    goalId = target.goalId,
                    projectId = target.projectId,
                )

            is NavTarget.DayPlan ->
                NavigationRoutes.dayPlan(
                    dayPlanId = target.dayPlanId,
                    startTab = target.startTab,
                )

            is NavTarget.DayManagement ->
                NavigationRoutes.dayManagement(
                    date = target.date,
                    startTab = target.startTab,
                )

            is NavTarget.EditTask ->
                NavigationRoutes.editTask(target.taskId)

            is NavTarget.ScriptEditor ->
                NavigationRoutes.scriptEditor(
                    projectId = target.contextId,
                    scriptId = target.scriptId,
                )

            is NavTarget.ImportExport ->
                NavigationRoutes.selectiveImport(target.uri)

            is NavTarget.ContextStructure ->
                NavigationRoutes.contextStructure(target.contextId)

            NavTarget.StructurePresets ->
                NavigationRoutes.STRUCTURE_PRESETS

            is NavTarget.StructurePresetEditor ->
                NavigationRoutes.structurePresetEditor(
                    presetId = target.presetId,
                    copyFromPresetId = target.copyFromPresetId,
                )
        }
}
