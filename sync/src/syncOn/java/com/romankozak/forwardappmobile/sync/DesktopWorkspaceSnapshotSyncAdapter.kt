package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.GoalStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogOrderSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.GoalSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

class DesktopWorkspaceSnapshotSyncAdapter {
    fun toSnapshotBundle(
        snapshot: DesktopWorkspaceSnapshot,
        exportedAt: Long = System.currentTimeMillis(),
    ): SnapshotBundle {
        val validContextIds = snapshot.contexts.mapTo(linkedSetOf()) { it.id }
        val goals =
            snapshot.backlogItems
                .filter { backlogItem -> backlogItem.contextId in validContextIds }
                .map { item ->
                    GoalSnapshot(
                        id = goalId(item.id),
                        text = item.title,
                        description = item.details,
                        isCompleted = item.isDone,
                        goalStatus = if (item.isDone) GoalStatusValues.DONE else GoalStatusValues.ACTIVE,
                        createdAt = exportedAt,
                        updatedAt = exportedAt,
                        version = 1,
                        isDeleted = false,
                        tags = emptyList(),
                        scoringStatus = ScoringStatusValues.NOT_ASSESSED,
                        valueImportance = 0,
                        valueImpact = 0,
                        effort = 0,
                        cost = 0,
                        risk = 0,
                        weightEffort = 1f,
                        weightCost = 1f,
                        weightRisk = 1f,
                        rawScore = 0.0,
                        displayScore = 0.0,
                        relativeSize = 0,
                        parentValueImportance = null,
                        impactOnParentGoal = null,
                        timeCost = null,
                        financialCost = null,
                    )
                }

        val backlogItems =
            snapshot.backlogItems
                .filter { backlogItem -> backlogItem.contextId in validContextIds }
                .mapIndexed { index, item ->
                    BacklogItemSnapshot(
                        id = backlogItemId(item.id),
                        contextId = item.contextId,
                        itemType = BacklogItemTypeValues.GOAL,
                        entityId = goalId(item.id),
                        order = index.toLong(),
                        updatedAt = exportedAt,
                        version = 1,
                        isDeleted = false,
                    )
                }

        val backlogOrders =
            backlogItems
                .groupBy { it.contextId }
                .values
                .flatMap { itemsForContext ->
                    itemsForContext.mapIndexed { index, item ->
                        BacklogOrderSnapshot(
                            id = "desktop-order-${item.id}",
                            listId = item.contextId,
                            itemId = item.id,
                            order = index.toLong(),
                            orderVersion = 1,
                            updatedAt = exportedAt,
                            isDeleted = false,
                        )
                    }
                }

        return SnapshotBundle(
            version = 2,
            exportedAt = exportedAt,
            contexts =
                snapshot.contexts.mapIndexed { index, context ->
                    ContextSnapshot(
                        id = context.id,
                        name = context.name,
                        parentId = context.parentId.takeIf { parentId -> parentId in validContextIds },
                        description = context.description,
                        createdAt = exportedAt,
                        updatedAt = exportedAt,
                        isExpanded = true,
                        isDeleted = false,
                        version = 1,
                        tags = emptyList(),
                        relatedLinks = emptyList(),
                        order = index,
                        isAttachmentsExpanded = false,
                        defaultViewModeName = context.defaultView.toAndroidViewModeName(),
                        isCompleted = context.isCompleted,
                        isContextManagementEnabled = true,
                        contextStatus = context.status.toAndroidContextStatus(),
                        contextStatusText = null,
                        contextLogLevel = "NORMAL",
                        totalTimeSpentMinutes = 0,
                        valueImportance = 0,
                        valueImpact = 0,
                        effort = 0,
                        cost = 0,
                        risk = 0,
                        weightEffort = 1f,
                        weightCost = 1f,
                        weightRisk = 1f,
                        rawScore = 0.0,
                        displayScore = context.score.toDouble(),
                        scoringStatus = ScoringStatusValues.NOT_ASSESSED,
                        showCheckboxes = false,
                        roleCode = null,
                    )
                },
            goals = goals,
            backlogItems = backlogItems,
            backlogOrders = backlogOrders,
        )
    }

    private fun goalId(backlogItemId: String): String = "desktop-goal-$backlogItemId"

    private fun backlogItemId(backlogItemId: String): String = "desktop-backlog-$backlogItemId"
}

private fun SharedContextStatus.toAndroidContextStatus(): String =
    when (this) {
        SharedContextStatus.NoPlan -> ContextStatusValues.NO_PLAN
        SharedContextStatus.Planning -> ContextStatusValues.PLANNING
        SharedContextStatus.InProgress -> ContextStatusValues.IN_PROGRESS
        SharedContextStatus.Completed -> ContextStatusValues.COMPLETED
        SharedContextStatus.OnHold -> ContextStatusValues.ON_HOLD
        SharedContextStatus.Paused -> ContextStatusValues.PAUSED
    }

private fun SharedContextView.toAndroidViewModeName(): String =
    when (this) {
        SharedContextView.Backlog -> "BACKLOG"
        SharedContextView.Inbox -> "INBOX"
        SharedContextView.Connections -> "CONNECTIONS"
        SharedContextView.Dashboard -> "DASHBOARD"
        SharedContextView.Direction -> "DIRECTION"
        SharedContextView.Log -> "LOG"
        SharedContextView.JournalLog -> "JOURNAL_LOG"
        SharedContextView.Artifact -> "ARTIFACT"
        SharedContextView.KeyProblems -> "KEY_PROBLEMS"
    }
