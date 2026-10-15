package com.romankozak.forwardappmobile.features.mainscreen.core

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

internal enum class MainBeaconOwnerRejection {
    EMPTY_OWNER_ID,
    MISSING_WORKSPACE,
    DELETED_WORKSPACE,
    DIVERGENT_ID,
    INVALID_SYSTEM_OWNER,
    UNPROVEN_LIVE_CONTEXT_MAPPING,
    UNPROVEN_RETIRED_CONTEXT_MAPPING,
    UNPROVEN_CONTEXTLESS_OWNER,
}

internal class MainBeaconWorkspaceOwnerRejected(
    val ownerId: String,
    val reason: MainBeaconOwnerRejection,
) : IllegalArgumentException(
    "Main Beacon Workspace owner $ownerId rejected: $reason",
)

/**
 * Validates the physical Workspace owner of one Main Beacon association.
 *
 * Historical Context identity is ingress evidence, not permission to
 * insert a Workspace FK merely because the ids happen to match.
 *
 * This policy does not transfer Context authority for other capabilities.
 */
internal fun requireMainBeaconWorkspaceOwner(
    ownerId: String,
    context: Context?,
    workspace: WorkspaceEntity?,
): WorkspaceEntity {
    fun reject(reason: MainBeaconOwnerRejection): Nothing =
        throw MainBeaconWorkspaceOwnerRejected(ownerId, reason)

    if (ownerId.isBlank()) {
        reject(MainBeaconOwnerRejection.EMPTY_OWNER_ID)
    }

    if (context != null && context.id != ownerId) {
        reject(MainBeaconOwnerRejection.DIVERGENT_ID)
    }

    val candidate =
        workspace ?: reject(MainBeaconOwnerRejection.MISSING_WORKSPACE)

    if (candidate.id != ownerId) {
        reject(MainBeaconOwnerRejection.DIVERGENT_ID)
    }

    if (candidate.isDeleted) {
        reject(MainBeaconOwnerRejection.DELETED_WORKSPACE)
    }

    val canonicalOnly =
        candidate.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
            candidate.sourceContextId == null

    if (SystemContexts.isSystem(ContextId(ownerId))) {
        if (!canonicalOnly) {
            reject(MainBeaconOwnerRejection.INVALID_SYSTEM_OWNER)
        }
        return candidate
    }

    when (context?.isDeleted) {
        false -> {
            val provenContextBacked =
                candidate.provenance ==
                    WorkspaceProvenance.CONTEXT_BACKED.name &&
                    candidate.sourceContextId == ownerId

            if (!provenContextBacked) {
                reject(
                    MainBeaconOwnerRejection.UNPROVEN_LIVE_CONTEXT_MAPPING,
                )
            }
        }

        true -> {
            if (!canonicalOnly) {
                reject(
                    MainBeaconOwnerRejection.UNPROVEN_RETIRED_CONTEXT_MAPPING,
                )
            }
        }

        null -> {
            val standalone =
                candidate.provenance ==
                    WorkspaceProvenance.STANDALONE.name &&
                    candidate.sourceContextId == null

            // Wi-Fi ingress can omit ordinary Context rows.
            // A CONTEXT_BACKED Workspace still needs an exact,
            // explicit sourceContextId mapping.
            val provenContextBacked =
                candidate.provenance ==
                    WorkspaceProvenance.CONTEXT_BACKED.name &&
                    candidate.sourceContextId == ownerId

            if (!canonicalOnly && !standalone && !provenContextBacked) {
                reject(
                    MainBeaconOwnerRejection.UNPROVEN_CONTEXTLESS_OWNER,
                )
            }
        }
    }

    return candidate
}

/**
 * One logical owner association per (Beacon, Workspace) identity.
 *
 * Equivalent physical rows collapse. Divergent orders remain an
 * explicit integrity failure, never silently selected by SQL ordering.
 * Workspace provenance must be checked separately by the caller.
 */
internal fun coalesceMainBeaconLogicalOwnerRefs(
    rows: List<
        com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
    >,
): List<
    com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
> {
    val owners = linkedMapOf<Pair<String, String>,
        com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef>()

    rows.forEach { row ->
        require(row.beaconId.isNotBlank() && row.contextId.isNotBlank()) {
            "Main Beacon logical owner identity must not be blank"
        }

        val key = row.beaconId to row.contextId
        val previous = owners[key]

        require(previous == null || previous.order == row.order) {
            "Main Beacon logical owner $key has divergent physical ref orders"
        }

        if (previous == null) {
            owners[key] = row
        }
    }

    return owners.values.sortedWith(
        compareBy<
            com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
        > { it.beaconId }
            .thenBy { it.order }
            .thenBy { it.contextId },
    )
}
