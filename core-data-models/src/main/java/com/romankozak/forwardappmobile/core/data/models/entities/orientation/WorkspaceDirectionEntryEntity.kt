package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical ordered placement owned by one DIRECTION capability instance.
 *
 * This row owns placement, ordering, and an optional local label.
 * Orientation and Workspace lifecycle remain owned by their targets.
 *
 * Foreign keys are intentionally deferred until the cross-stream restore and
 * transport order for this collection is accepted.
 */
@Entity(
    tableName = "workspace_direction_entries",
    indices = [
        Index("workspaceId"),
        Index("capabilityInstanceId"),
        Index("orientationId"),
        Index("targetWorkspaceId"),
        Index("provenance"),
        Index(value = ["workspaceId", "capabilityInstanceId", "entryOrder"]),
    ],
)
data class WorkspaceDirectionEntryEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val orientationId: String?,
    val targetWorkspaceId: String?,
    val labelOverride: String?,
    val entryOrder: Long,
    val provenance: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

enum class WorkspaceDirectionEntryProvenance {
    LEGACY_DIRECTION_ITEM,
    CANONICAL_ONLY,
}
