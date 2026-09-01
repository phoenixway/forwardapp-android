package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity

@Entity(
    tableName = "workspace_connections",
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
        ForeignKey(
            entity = AttachmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["attachmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("workspaceId"),
        Index("capabilityInstanceId"),
        Index("attachmentId"),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["capabilityInstanceId", "connectionOrder"]),
        Index(value = ["capabilityInstanceId", "attachmentId"], unique = true),
    ],
)
data class WorkspaceConnectionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val attachmentId: String,
    val connectionOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
