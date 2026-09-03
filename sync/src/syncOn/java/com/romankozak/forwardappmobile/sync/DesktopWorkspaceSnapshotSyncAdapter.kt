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
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedSyncMetadata

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
                    val sync = item.sync.withExportFallback(exportedAt)
                    GoalSnapshot(
                        id = goalId(item),
                        text = item.title,
                        description = item.details,
                        isCompleted = item.isDone,
                        goalStatus = if (item.isDone) GoalStatusValues.DONE else GoalStatusValues.ACTIVE,
                        createdAt = sync.createdAt,
                        updatedAt = sync.updatedAt,
                        version = sync.version,
                        isDeleted = item.isDeleted,
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
                    val sync = item.sync.withExportFallback(exportedAt)
                    BacklogItemSnapshot(
                        id = backlogItemId(item.id),
                        contextId = item.contextId,
                        itemType = BacklogItemTypeValues.GOAL,
                        entityId = goalId(item),
                        order = index.toLong(),
                        updatedAt = sync.updatedAt,
                        version = sync.version,
                        isDeleted = item.isDeleted,
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
                            isDeleted = item.isDeleted,
                        )
                    }
                }

        return SnapshotBundle(
            version = 2,
            exportedAt = exportedAt,
            contexts =
                snapshot.contexts.mapIndexed { index, context ->
                    val sync = context.sync.withExportFallback(exportedAt)
                    ContextSnapshot(
                        id = context.id,
                        name = context.name,
                        parentId = context.parentId.takeIf { parentId -> parentId in validContextIds },
                        description = context.description,
                        createdAt = sync.createdAt,
                        updatedAt = sync.updatedAt,
                        isExpanded = true,
                        isDeleted = context.isDeleted,
                        version = sync.version,
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

    private fun goalId(backlogItem: com.romankozak.forwardappmobile.shared.contracts.contexts.SharedBacklogItem): String =
        backlogItem.sourceEntityId ?: "desktop-goal-${backlogItem.id}"

    private fun backlogItemId(backlogItemId: String): String = backlogItemId
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
        SharedContextView.KeyProblems -> "KEY_PROBLEMS"
    }

private fun SharedSyncMetadata.withExportFallback(exportedAt: Long): SharedSyncMetadata =
    SharedSyncMetadata(
        createdAt = createdAt.takeIf { it > 0L } ?: exportedAt,
        updatedAt = updatedAt.takeIf { it > 0L } ?: exportedAt,
        version = version.takeIf { it > 0L } ?: 1L,
    )
