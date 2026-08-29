package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Diagnostics owned only by the DIRECTION entry compatibility materializer.
 *
 * Separate ownership prevents Orientation/Workspace bootstrap runs from
 * resolving unrelated Direction-entry failures.
 */
@Entity(
    tableName = "workspace_direction_entry_issues",
    indices = [
        Index("sourceDirectionItemId"),
        Index(
            value = ["sourceDirectionItemId", "code"],
            unique = true,
        ),
    ],
)
data class WorkspaceDirectionEntryIssueEntity(
    @PrimaryKey val id: String,
    val sourceDirectionItemId: String,
    val code: String,
    val detail: String,
    val createdAt: Long,
    val resolvedAt: Long?,
)
