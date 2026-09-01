package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextLogSnapshot

private const val CONTEXT_BACKED_WORKSPACE_PROVENANCE = "CONTEXT_BACKED"

/**
 * Compatibility projection for the existing Context-oriented selective-import UI.
 *
 * Canonical EXECUTION_LOG remains Workspace-owned. Context identity is derived
 * only from a live, proven CONTEXT_BACKED Workspace. There is intentionally no
 * fallback to legacy SnapshotBundle.logs.
 */
internal fun SnapshotBundle.contextBackedExecutionLogOwnerContexts(): Map<String, String> =
    workspaces
        .orEmpty()
        .asSequence()
        .filter { workspace ->
            !workspace.isDeleted &&
                workspace.provenance == CONTEXT_BACKED_WORKSPACE_PROVENANCE &&
                workspace.sourceContextId != null &&
                workspace.sourceContextId == workspace.id
        }
        .associate { workspace ->
            workspace.id to requireNotNull(workspace.sourceContextId)
        }

internal fun SnapshotBundle.projectCanonicalExecutionLogsForSelectiveImportPreview(): List<ContextLogSnapshot> {
    val canonicalLogs = canonicalExecutionLogs ?: return emptyList()
    val ownerContexts = contextBackedExecutionLogOwnerContexts()

    return canonicalLogs.mapNotNull { log ->
        val contextId = ownerContexts[log.workspaceId] ?: return@mapNotNull null
        ContextLogSnapshot(
            id = log.id,
            contextId = contextId,
            timestamp = log.timestamp,
            type = log.type,
            description = log.description,
            details = log.details,
            updatedAt = log.updatedAt,
            version = log.version,
            isDeleted = log.isDeleted,
        )
    }
}
