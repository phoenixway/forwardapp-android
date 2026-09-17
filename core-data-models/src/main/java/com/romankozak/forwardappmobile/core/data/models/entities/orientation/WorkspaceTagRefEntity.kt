package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Canonical tag membership for an operational Workspace.
 *
 * Membership is independently versioned from Workspace presentation metadata so
 * tag edits do not compete with rename/hierarchy/order edits for one Workspace
 * whole-row freshness winner.
 */
@Entity(
    tableName = "workspace_tag_refs",
    primaryKeys = ["workspaceId", "normalizedTag"],
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("normalizedTag"),
        Index("updatedAt"),
        Index("isDeleted"),
    ],
)
data class WorkspaceTagRefEntity(
    val workspaceId: String,
    val normalizedTag: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
