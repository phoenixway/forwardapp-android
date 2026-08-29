package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workspace_bootstrap_state")
data class WorkspaceBootstrapStateEntity(
    @PrimaryKey val id: Int = 1,
    val version: Int,
    val status: String,
    val completedAt: Long?,
    val comparedAt: Long?,
)

@Entity(
    tableName = "workspace_bootstrap_issues",
    indices = [
        Index("contextId"),
        Index(value = ["contextId", "code"], unique = true),
    ],
)
data class WorkspaceBootstrapIssueEntity(
    @PrimaryKey val id: String,
    val contextId: String,
    val code: String,
    val detail: String,
    val createdAt: Long,
    val resolvedAt: Long?,
)
