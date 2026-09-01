@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.workspace

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

/**
 * Closed target vocabulary for a BACKLOG placement.
 *
 * BACKLOG owns only the appearance. The referenced domain continues to own
 * target content and target lifecycle.
 */
@JsExport
enum class WorkspaceBacklogTargetKind {
    ORIENTATION,
    WORKSPACE,
    LINK_ITEM,
    LEGACY_NOTE,
    NOTE_DOCUMENT,
    JOURNAL_DOCUMENT,
    CHECKLIST,
    MUSIC_NOTE,
}

@JsExport
data class WorkspaceBacklogTargetRef(
    val kind: WorkspaceBacklogTargetKind,
    val id: String,
)

/** Canonical ordered explicit appearance inside one BACKLOG capability. */
@JsExport
data class WorkspaceBacklogEntry(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val workspaceId: String,
    val capabilityInstanceId: String,
    val target: WorkspaceBacklogTargetRef,
    val order: Long,
) : SyncEntityMeta
