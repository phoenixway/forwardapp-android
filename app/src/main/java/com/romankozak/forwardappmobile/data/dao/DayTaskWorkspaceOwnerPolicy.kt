package com.romankozak.forwardappmobile.data.dao

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

internal enum class DayTaskOwnerRejection {
    EMPTY_OWNER_ID,
    MISSING_WORKSPACE,
    DELETED_WORKSPACE,
    DIVERGENT_ID,
    INVALID_SYSTEM_OWNER,
    UNPROVEN_LIVE_CONTEXT_MAPPING,
    UNPROVEN_RETIRED_CONTEXT_MAPPING,
    UNPROVEN_CONTEXTLESS_OWNER,
}

internal class DayTaskWorkspaceOwnerRejected(
    val ownerId: String,
    val reason: DayTaskOwnerRejection,
) : IllegalArgumentException(
    "DayTask Workspace owner $ownerId rejected: $reason",
)

/**
 * Canonical Workspace admission for DayTask persistence.
 *
 * A legacy Context identity is ingress evidence, never persistence
 * authority. Same-id equality alone does not establish ownership.
 *
 * The same admission rules are reused by TacticalMission during
 * Context-FK.5. Physical ownership columns and transport boundaries
 * remain specific to each carrier.
 */
internal fun requireDayTaskWorkspaceOwner(
    ownerId: String,
    context: Context?,
    workspace: WorkspaceEntity?,
): WorkspaceEntity {
    fun reject(reason: DayTaskOwnerRejection): Nothing =
        throw DayTaskWorkspaceOwnerRejected(ownerId, reason)

    if (ownerId.isBlank()) {
        reject(DayTaskOwnerRejection.EMPTY_OWNER_ID)
    }

    if (context != null && context.id != ownerId) {
        reject(DayTaskOwnerRejection.DIVERGENT_ID)
    }

    val candidate =
        workspace ?: reject(DayTaskOwnerRejection.MISSING_WORKSPACE)

    if (candidate.id != ownerId) {
        reject(DayTaskOwnerRejection.DIVERGENT_ID)
    }

    if (candidate.isDeleted) {
        reject(DayTaskOwnerRejection.DELETED_WORKSPACE)
    }

    val canonicalOnly =
        candidate.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
            candidate.sourceContextId == null

    if (SystemContexts.isSystem(ContextId(ownerId))) {
        if (!canonicalOnly) {
            reject(DayTaskOwnerRejection.INVALID_SYSTEM_OWNER)
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
                reject(DayTaskOwnerRejection.UNPROVEN_LIVE_CONTEXT_MAPPING)
            }
        }

        true -> {
            if (!canonicalOnly) {
                reject(DayTaskOwnerRejection.UNPROVEN_RETIRED_CONTEXT_MAPPING)
            }
        }

        null -> {
            val standalone =
                candidate.provenance ==
                    WorkspaceProvenance.STANDALONE.name &&
                    candidate.sourceContextId == null

            val provenContextBacked =
                candidate.provenance ==
                    WorkspaceProvenance.CONTEXT_BACKED.name &&
                    candidate.sourceContextId == ownerId

            if (!canonicalOnly && !standalone && !provenContextBacked) {
                reject(DayTaskOwnerRejection.UNPROVEN_CONTEXTLESS_OWNER)
            }
        }
    }

    return candidate
}
