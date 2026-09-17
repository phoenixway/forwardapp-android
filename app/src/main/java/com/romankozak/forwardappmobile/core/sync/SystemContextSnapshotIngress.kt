package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceLegacyContextEvidence

/**
 * Snapshot ingress boundary for roadmap Step 11.
 *
 * Exact current reserved System Context snapshots are compatibility evidence,
 * never Context persistence input. Historical non-reserved ids, including ids
 * that merely start with "sys_", remain ordinary Context snapshots.
 */
internal data class SystemContextSnapshotIngress(
    val ordinarySnapshots: List<ContextSnapshot>,
    val systemEvidence: List<SystemWorkspaceLegacyContextEvidence>,
)

internal fun partitionSystemContextSnapshotIngress(
    snapshots: List<ContextSnapshot>,
): SystemContextSnapshotIngress {
    val ordinary = ArrayList<ContextSnapshot>(snapshots.size)
    val evidence = ArrayList<SystemWorkspaceLegacyContextEvidence>()

    snapshots.forEach { snapshot ->
        if (isReservedSystemContextId(snapshot.id)) {
            evidence +=
                SystemWorkspaceLegacyContextEvidence(
                    id = snapshot.id,
                    name = snapshot.name,
                    description = snapshot.description,
                    parentId = snapshot.parentId,
                    roleCode = snapshot.roleCode,
                    order = snapshot.order.toLong(),
                    createdAt = snapshot.createdAt,
                    updatedAt = snapshot.updatedAt,
                    isDeleted = snapshot.isDeleted,
                    version = snapshot.version,
                )
        } else {
            ordinary += snapshot
        }
    }

    return SystemContextSnapshotIngress(
        ordinarySnapshots = ordinary,
        systemEvidence = evidence,
    )
}

internal fun isReservedSystemContextId(id: String): Boolean =
    SystemContexts.isSystem(ContextId(id))
