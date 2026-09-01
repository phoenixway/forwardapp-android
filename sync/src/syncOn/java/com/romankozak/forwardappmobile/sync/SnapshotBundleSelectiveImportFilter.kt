package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection

class SnapshotBundleSelectiveImportFilter {
    fun filter(
        source: SnapshotBundle,
        selection: WorkspaceSelectiveImportSelection,
    ): SnapshotBundle {
        val filteredContexts = source.contexts.filter { context -> context.id in selection.selectedContextIds }
        val validContextIds = filteredContexts.mapTo(linkedSetOf()) { context -> context.id }
        val filteredGoals = source.goals.filter { goal -> goal.id in selection.selectedGoalIds }
        val validGoalIds = filteredGoals.mapTo(linkedSetOf()) { goal -> goal.id }
        val filteredBacklogItems =
            source.backlogItems.filter { item ->
                item.id in selection.selectedBacklogItemIds &&
                    item.contextId in validContextIds &&
                    (item.itemType != "GOAL" || item.entityId in validGoalIds)
            }
        val validBacklogItemIds = filteredBacklogItems.mapTo(linkedSetOf()) { item -> item.id }
        val filteredDocuments =
            source.documents.filter { document ->
                document.id in selection.selectedDocumentIds &&
                    (document.contextId == null || document.contextId in validContextIds)
            }
        val filteredChecklists =
            source.checklists.filter { checklist ->
                checklist.id in selection.selectedChecklistIds &&
                    (checklist.contextId == null || checklist.contextId in validContextIds)
            }
        val filteredLinks = source.linkItemEntities.filter { link -> link.id in selection.selectedLinkItemIds }
        val executionLogOwnerContexts = source.contextBackedExecutionLogOwnerContexts()
        val selectedExecutionLogWorkspaceIds =
            executionLogOwnerContexts
                .filterValues { contextId -> contextId in validContextIds }
                .keys
        val filteredCanonicalExecutionLogs =
            source.canonicalExecutionLogs?.filter { log ->
                log.id in selection.selectedContextLogIds &&
                    log.workspaceId in selectedExecutionLogWorkspaceIds
            }
        val filteredScripts = source.scripts.filter { script -> script.id in selection.selectedScriptIds }
        val filteredAttachments = source.attachments.filter { attachment -> attachment.id in selection.selectedAttachmentIds }
        val validAttachmentIds = filteredAttachments.mapTo(linkedSetOf()) { attachment -> attachment.id }
        val filteredDayPlans = source.dayPlans
        val validDayPlanIds = filteredDayPlans.mapTo(linkedSetOf()) { plan -> plan.id }
        val filteredMainBeacons = source.mainBeacons
        val validMainBeaconIds = filteredMainBeacons.mapTo(linkedSetOf()) { beacon -> beacon.id }

        return source.copy(
            contexts = filteredContexts,
            goals = filteredGoals,
            backlogItems = filteredBacklogItems,
            backlogOrders =
                source.backlogOrders.filter { order ->
                    order.listId in validContextIds && order.itemId in validBacklogItemIds
                },
            documents = filteredDocuments,
            checklists = filteredChecklists,
            checklistItems =
                source.checklistItems.filter { item ->
                    item.checklistId in selection.selectedChecklistIds
                },
            linkItemEntities = filteredLinks,
            inbox = emptyList(),
            // Legacy Context logs are not an authority after EXECUTION_LOG cutover.
            // selectedContextLogIds are presentation-level stable row ids only.
            logs = emptyList(),
            canonicalExecutionLogs = filteredCanonicalExecutionLogs,
            workspaceDirectionEntries = null,
            scripts = filteredScripts,
            attachments = filteredAttachments,
            crossRefs = emptyList(),
            dayPlans = filteredDayPlans,
            dayFocusItems =
                source.dayFocusItems.filter { item ->
                    item.dayPlanId in validDayPlanIds
                },
            dayTasks =
                source.dayTasks.filter { task ->
                    task.dayPlanId in validDayPlanIds
                },
            dailyMetrics =
                source.dailyMetrics.filter { metric ->
                    metric.dayPlanId in validDayPlanIds
                },
            activityRecords =
                source.activityRecords.filter { record ->
                    record.id in selection.selectedActivityRecordIds
                },
            mainBeacons = filteredMainBeacons,
            mainBeaconContextCrossRefs =
                source.mainBeaconContextCrossRefs.filter { crossRef ->
                    crossRef.beaconId in validMainBeaconIds && crossRef.contextId in validContextIds
                },
            mainBeaconAttachmentCrossRefs =
                source.mainBeaconAttachmentCrossRefs.filter { crossRef ->
                    crossRef.beaconId in validMainBeaconIds && crossRef.attachmentId in validAttachmentIds
                },
            mainBeaconLevelStatuses =
                source.mainBeaconLevelStatuses.filter { status ->
                    status.mainBeaconId in validMainBeaconIds
                },
            lifeManagementLevelStatuses = source.lifeManagementLevelStatuses,
            dayManagementRuntimeState = source.dayManagementRuntimeState,
            notes = emptyList(),
            musicNotes = emptyList(),
            artifacts = emptyList(),
            systemApps = emptyList(),
            recentProjectEntries = emptyList(),
            conversations = emptyList(),
            chatMessages = emptyList(),
            conversationFolders = emptyList(),
            reminders = emptyList(),
            recurringTasks = emptyList(),
            tacticalMissions = emptyList(),
            tacticalMissionAttachments = emptyList(),
            tacticalIterations = emptyList(),
            missionStreams = emptyList(),
            tacticalActivitySlots = emptyList(),
            arcQuests = emptyList(),
            aiEvents = emptyList(),
            aiInsights = emptyList(),
            lifeSystemStates = source.lifeSystemStates,
            contextRoleProfiles = emptyList(),
            contextRoleProfileItems = emptyList(),
            contextConfigurations = emptyList(),
            projectStructureItems = emptyList(),
            contextInboxSortingRules = emptyList(),
            workspaceProblems = null,
            workspaceProblemWorkspaceRefs = null,
            workspaceProblemAttachmentRefs = null,
            workspaceInboxRecords = null,
            workspaceConnections = null,
            // Canonical BACKLOG selection requires Workspace + typed-target closure.
            // Keep it absent until the selective-import contract can express that selection.
            workspaceBacklogEntries = null,
            focusContextIntervals = emptyList(),
            userStateIntervals = emptyList(),
        )
    }
}
