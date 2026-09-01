@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.workspace

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

/**
 * Canonical ordered appearance of one reusable Attachment inside one
 * CONNECTIONS capability instance.
 *
 * The Attachment remains the global content/reference identity. This row owns
 * only Workspace placement, order, and placement lifecycle.
 */
@JsExport
data class WorkspaceConnection(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val attachmentId: String,
    val order: Long,
) : SyncEntityMeta
