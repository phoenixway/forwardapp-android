package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

/** Bounded pre-canonical backup evidence. This is not a runtime owner classifier. */
internal fun isAdmittedLegacyContextIngressOwner(
    contextId: String,
    contextIsDeleted: Boolean?,
    workspace: WorkspaceEntity?,
): Boolean {
    val candidate = workspace ?: return false
    if (candidate.id != contextId) return false

    if (SystemContexts.isSystem(ContextId(contextId))) {
        return !candidate.isDeleted &&
            candidate.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
            candidate.sourceContextId == null
    }

    return when (contextIsDeleted) {
        // A live historical Context may already have a deleted CONTEXT_BACKED
        // projection. It remains valid bounded migration ownership so legacy
        // placements can be accounted for as canonical tombstones.
        false ->
            candidate.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                candidate.sourceContextId == contextId

        // Retirement evidence requires a surviving canonical owner.
        true ->
            !candidate.isDeleted &&
                candidate.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                candidate.sourceContextId == null

        null -> false
    }
}
