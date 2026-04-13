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
        val filteredInbox =
            source.inbox.filter { record ->
                record.id in selection.selectedInboxRecordIds && record.contextId in validContextIds
            }
        val filteredLogs =
            source.logs.filter { log ->
                log.id in selection.selectedContextLogIds && log.contextId in validContextIds
            }
        val filteredScripts = source.scripts.filter { script -> script.id in selection.selectedScriptIds }
        val filteredAttachments = source.attachments.filter { attachment -> attachment.id in selection.selectedAttachmentIds }
        val validAttachmentIds = filteredAttachments.mapTo(linkedSetOf()) { attachment -> attachment.id }

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
            inbox = filteredInbox,
            logs = filteredLogs,
            scripts = filteredScripts,
            attachments = filteredAttachments,
            crossRefs =
                source.crossRefs.filter { crossRef ->
                    crossRef.contextId in validContextIds && crossRef.attachmentId in validAttachmentIds
                },
            activityRecords =
                source.activityRecords.filter { record ->
                    record.id in selection.selectedActivityRecordIds
                },
            notes = emptyList(),
            musicNotes = emptyList(),
            artifacts = emptyList(),
            systemApps = emptyList(),
            recentProjectEntries = emptyList(),
            dayPlans = emptyList(),
            dayTasks = emptyList(),
            dailyMetrics = emptyList(),
            conversations = emptyList(),
            chatMessages = emptyList(),
            conversationFolders = emptyList(),
            reminders = emptyList(),
            recurringTasks = emptyList(),
            tacticalMissions = emptyList(),
            tacticalMissionAttachments = emptyList(),
            aiEvents = emptyList(),
            aiInsights = emptyList(),
            lifeSystemStates = emptyList(),
            contextRoleProfiles = emptyList(),
            contextRoleProfileItems = emptyList(),
            contextConfigurations = emptyList(),
            projectStructureItems = emptyList(),
            contextInboxSortingRules = emptyList(),
            contextKeyProblems = emptyList(),
            focusContextIntervals = emptyList(),
            userStateIntervals = emptyList(),
            directionItems = emptyList(),
        )
    }
}
