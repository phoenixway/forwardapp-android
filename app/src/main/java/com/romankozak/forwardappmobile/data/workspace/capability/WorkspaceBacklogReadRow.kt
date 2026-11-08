package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind

/** Read DTO only; explicit placements and derived hashtag appearances retain separate provenance. */
data class WorkspaceBacklogReadRow(
    val id: String,
    val workspaceId: String,
    val targetKind: String,
    val targetId: String,
    val itemOrder: Long,
    val updatedAt: Long?,
    val syncedAt: Long?,
    val version: Long,
    val mappedGoalId: String?,
    val workspaceDeleted: Boolean?,
    val derivedGoalId: String?,
    val associationOwnerContextId: String?,
    val associationTag: String?,
) {
    internal fun toCompatibilityItem(): BacklogItem {
        val target = if (derivedGoalId != null) BacklogItemTypeValues.GOAL to derivedGoalId else {
            when (WorkspaceBacklogTargetKind.valueOf(targetKind)) {
                WorkspaceBacklogTargetKind.ORIENTATION -> {
                    BacklogItemTypeValues.GOAL to requireNotNull(mappedGoalId) {
                        "Canonical BACKLOG Orientation $targetId has no live CUT_OVER GOAL mapping"
                    }
                }
                WorkspaceBacklogTargetKind.WORKSPACE -> {
                    require(workspaceDeleted == false) { "Canonical BACKLOG Workspace target $targetId is missing or deleted" }
                    BacklogItemTypeValues.SUBLIST to targetId
                }
                WorkspaceBacklogTargetKind.LINK_ITEM -> BacklogItemTypeValues.LINK_ITEM to targetId
                WorkspaceBacklogTargetKind.LEGACY_NOTE -> BacklogItemTypeValues.NOTE to targetId
                WorkspaceBacklogTargetKind.NOTE_DOCUMENT -> BacklogItemTypeValues.NOTE_DOCUMENT to targetId
                WorkspaceBacklogTargetKind.CHECKLIST -> BacklogItemTypeValues.CHECKLIST to targetId
                WorkspaceBacklogTargetKind.MUSIC_NOTE -> BacklogItemTypeValues.MUSIC_NOTE to targetId
            }
        }
        return BacklogItem(id = id, contextId = workspaceId, itemType = target.first, entityId = target.second,
            associationOwnerContextId = associationOwnerContextId, associationTag = associationTag,
            order = itemOrder, updatedAt = updatedAt, syncedAt = syncedAt, version = version)
    }
}
