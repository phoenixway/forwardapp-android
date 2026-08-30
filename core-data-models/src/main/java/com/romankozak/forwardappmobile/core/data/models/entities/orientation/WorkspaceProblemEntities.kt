package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical Workspace-owned KEY_PROBLEMS content.
 *
 * Foreign keys are intentionally deferred. Backup/restore and incremental sync
 * apply Workspace/capability/Problem dependencies in an explicit order, while
 * tombstoned targets must remain representable for historical relations.
 */
@Entity(
    tableName = "workspace_problems",
    indices = [
        Index("workspaceId"),
        Index("capabilityInstanceId"),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["capabilityInstanceId", "problemOrder"]),
    ],
)
data class WorkspaceProblemEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val title: String,
    val description: String,
    val status: String,
    val problemOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "workspace_problem_workspace_refs",
    indices = [
        Index("problemId"),
        Index("targetWorkspaceId"),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["problemId", "targetWorkspaceId"]),
    ],
)
data class WorkspaceProblemWorkspaceRefEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val targetWorkspaceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

@Entity(
    tableName = "workspace_problem_attachment_refs",
    indices = [
        Index("problemId"),
        Index("attachmentId"),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["problemId", "attachmentId"]),
    ],
)
data class WorkspaceProblemAttachmentRefEntity(
    @PrimaryKey val id: String,
    val problemId: String,
    val attachmentId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
