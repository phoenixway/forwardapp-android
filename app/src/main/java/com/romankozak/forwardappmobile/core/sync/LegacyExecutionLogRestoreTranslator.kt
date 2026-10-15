package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.toWorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.isAdmittedLegacyContextIngressOwner

/** Restore-only bridge from the historical Context locator to Workspace ownership. */
internal fun SnapshotBundle.withCanonicalExecutionLogRestoreIngress(
    canonicalWorkspaces: List<WorkspaceSnapshot>,
    workspaceIdsEstablishedFromLegacyContexts: Set<String> = emptySet(),
    legacyContexts: List<ContextSnapshot> = contexts,
): SnapshotBundle {
    canonicalExecutionLogs?.let {
        // Presence is authoritative, including an explicitly empty collection.
        return copy(logs = emptyList())
    }
    if (logs.isEmpty()) return this

    val contextsById = legacyContexts.associateBy { it.id }
    require(contextsById.size == legacyContexts.size) {
        "Legacy EXECUTION_LOG restore evidence contains duplicate Context ids"
    }
    val workspacesById = canonicalWorkspaces.associateBy { it.id }
    require(workspacesById.size == canonicalWorkspaces.size) {
        "Legacy EXECUTION_LOG restore evidence contains duplicate Workspace ids"
    }
    require(logs.map { it.id }.toSet().size == logs.size) {
        "Legacy EXECUTION_LOG restore evidence contains duplicate log ids"
    }

    val translated =
        logs.map { log ->
            require(log.id.isNotBlank()) { "Legacy EXECUTION_LOG id must not be blank" }
            require(log.contextId.isNotBlank()) {
                "Legacy EXECUTION_LOG ${log.id} has a blank Context owner"
            }
            val context =
                requireNotNull(contextsById[log.contextId]) {
                    "Legacy EXECUTION_LOG ${log.id} references missing Context ${log.contextId}"
                }
            val workspaceSnapshot =
                requireNotNull(workspacesById[log.contextId]) {
                    "Legacy EXECUTION_LOG ${log.id} has no canonical Workspace owner for Context ${log.contextId}"
                }
            val workspace = workspaceSnapshot.toWorkspaceEntity()
            val ownershipWasEstablishedByThisRestore =
                workspace.id in workspaceIdsEstablishedFromLegacyContexts
            require(
                ownershipWasEstablishedByThisRestore ||
                    isAdmittedLegacyContextIngressOwner(
                        contextId = context.id,
                        contextIsDeleted = context.isDeleted,
                        workspace = workspace,
                    ),
            ) {
                "Legacy EXECUTION_LOG ${log.id} has no provenance-proven Workspace owner"
            }
            require(!workspace.isDeleted || log.isDeleted) {
                "Live legacy EXECUTION_LOG ${log.id} cannot belong to a deleted Workspace"
            }

            CanonicalExecutionLogSnapshot(
                id = log.id,
                workspaceId = workspace.id,
                timestamp = log.timestamp,
                type = log.type,
                description = log.description,
                details = log.details,
                updatedAt = log.updatedAt,
                version = log.version,
                isDeleted = log.isDeleted,
            )
        }

    return copy(
        logs = emptyList(),
        canonicalExecutionLogs = translated,
    )
}
