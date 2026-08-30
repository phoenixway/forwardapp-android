@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.workspace

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

@JsExport
enum class WorkspaceProblemStatus {
    OPEN,
    IN_PROGRESS,
    BLOCKED,
    RESOLVED,
    CLOSED,
}

@JsExport
data class WorkspaceProblem(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val title: String,
    val description: String,
    val status: WorkspaceProblemStatus,
    val order: Long,
) : SyncEntityMeta

@JsExport
data class WorkspaceProblemWorkspaceRef(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val problemId: String,
    val targetWorkspaceId: String,
) : SyncEntityMeta

@JsExport
data class WorkspaceProblemAttachmentRef(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val problemId: String,
    val attachmentId: String,
) : SyncEntityMeta
