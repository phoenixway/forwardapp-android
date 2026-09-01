package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical explicit BACKLOG placement.
 *
 * The heterogeneous target remains owned by its typed domain, so targetKind
 * and targetId deliberately have no cross-domain Room foreign key.
 */
@Entity(
    tableName = "workspace_backlog_entries",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = WorkspaceCapabilityInstanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["capabilityInstanceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("workspaceId"),
        Index("capabilityInstanceId"),
        Index(value = ["targetKind", "targetId"]),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["capabilityInstanceId", "entryOrder"]),
        Index(value = ["capabilityInstanceId", "targetKind", "targetId"]),
    ],
)
data class WorkspaceBacklogEntryEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val targetKind: String,
    val targetId: String,
    val entryOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
