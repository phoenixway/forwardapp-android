package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workspace_inbox_records",
    indices = [
        Index("workspaceId"),
        Index("capabilityInstanceId"),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["capabilityInstanceId", "recordOrder"]),
    ],
)
data class WorkspaceInboxRecordEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val text: String,
    val recordOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)
