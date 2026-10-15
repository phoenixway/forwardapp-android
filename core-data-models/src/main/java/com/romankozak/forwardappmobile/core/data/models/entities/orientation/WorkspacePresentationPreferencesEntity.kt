package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Canonical durable presentation preferences for one operational Workspace.
 *
 * These values are deliberately independent from Workspace identity/presentation
 * metadata so UI preference edits do not compete with Workspace rename/lifecycle
 * freshness. Legacy Context scalar fields are compatibility evidence only.
 */
@Entity(
    tableName = "workspace_presentation_preferences",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("updatedAt"),
    ],
)
data class WorkspacePresentationPreferencesEntity(
    @PrimaryKey val workspaceId: String,
    val defaultViewModeName: String?,
    @ColumnInfo(defaultValue = "0")
    val isAttachmentsExpanded: Boolean,
    @ColumnInfo(defaultValue = "0")
    val showCheckboxes: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val version: Long,
)
