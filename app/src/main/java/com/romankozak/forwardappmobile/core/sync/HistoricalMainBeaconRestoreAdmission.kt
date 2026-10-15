package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyBackupGeneration
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.isDeprecatedLegacyCompatibility
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

/**
 * Validate historical MainBeacon ownership against source-bundle evidence,
 * BEFORE legacy Context identities are canonicalized as Workspaces.
 *
 * This contract does not apply to current canonical Workspace transport.
 */
internal fun requireHistoricalMainBeaconRestoreAdmission(
    source: SnapshotBundle,
    generation: HierarchyBackupGeneration,
) {
    if (
        !generation.isDeprecatedLegacyCompatibility ||
        source.mainBeaconContextCrossRefs.isEmpty()
    ) {
        return
    }

    val contexts = source.contexts.associateBy { it.id }
    require(contexts.size == source.contexts.size) {
        "Historical MainBeacon owner evidence has duplicate Context identities"
    }

    val workspaceRows = source.workspaces.orEmpty()
    val workspaces = workspaceRows.associateBy { it.id }
    require(workspaces.size == workspaceRows.size) {
        "Historical MainBeacon owner evidence has duplicate Workspace identities"
    }

    val seen = hashSetOf<Pair<String, String>>()

    source.mainBeaconContextCrossRefs.forEach { ref ->
        val key = ref.beaconId to ref.contextId
        require(ref.beaconId.isNotBlank() && ref.contextId.isNotBlank()) {
            "Historical MainBeacon owner has blank association identity"
        }
        require(seen.add(key)) {
            "Historical MainBeacon owner has duplicate association $key"
        }

        val historicalContext = contexts[ref.contextId]
        val candidate = workspaces[ref.contextId]
        val isSystem =
            SystemContexts.isSystem(ContextId(ref.contextId))

        val admitted =
            if (source.workspaces == null) {
                // Pure Context-era source. Its Workspace will be generated
                // from the original live Context in the same restore.
                historicalContext != null && !historicalContext.isDeleted
            } else if (isSystem) {
                candidate != null &&
                    !candidate.isDeleted &&
                    candidate.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                    candidate.sourceContextId == null
            } else {
                // Mixed historical source needs BOTH original Context evidence
                // and a matching Workspace lineage. ID equality alone is not proof.
                historicalContext != null &&
                    !historicalContext.isDeleted &&
                    candidate != null &&
                    !candidate.isDeleted &&
                    candidate.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                    candidate.sourceContextId == ref.contextId
            }

        require(admitted) {
            "Historical MainBeacon owner ${ref.contextId} has no proven live source mapping"
        }
    }
}
