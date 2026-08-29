package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Canonical operational identity. Context remains its compatibility source until cutover. */
@Entity(
    tableName = "workspaces",
    indices = [
        Index("parentWorkspaceId"),
        Index(value = ["parentWorkspaceId", "workspaceOrder"]),
        Index("updatedAt"),
        Index("isDeleted"),
        Index(value = ["sourceContextId"], unique = true),
    ],
)
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val nameOverride: String?,
    val descriptionOverride: String?,
    val parentWorkspaceId: String?,
    val roleCode: String?,
    val workspaceOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
    @ColumnInfo(defaultValue = "'CONTEXT_BACKED'")
    val provenance: String = "CONTEXT_BACKED",
    val sourceContextId: String? = if (provenance == "CANONICAL_ONLY") null else id,
)
