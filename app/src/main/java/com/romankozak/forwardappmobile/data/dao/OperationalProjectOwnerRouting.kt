package com.romankozak.forwardappmobile.data.dao

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

/** Physical persistence branch for an operational project's single logical id. */
internal enum class OperationalProjectOwnerStorage {
    CONTEXT,
    WORKSPACE,
}

/**
 * Resolves the only supported physical owner for a DayTask or TacticalMission.
 *
 * A live ordinary Context remains authoritative over a same-id Workspace. A
 * CANONICAL_ONLY non-System Workspace is an operational owner only when the
 * deleted Context row proves that the ordinary owner was retired.
 */
internal fun classifyOperationalProjectOwner(
    logicalProjectId: String,
    context: Context?,
    workspace: WorkspaceEntity?,
): OperationalProjectOwnerStorage {
    if (SystemContexts.isSystem(ContextId(logicalProjectId))) {
        require(workspace.isLiveCanonicalOnlyWithoutContextSource()) {
            "Reserved operational project $logicalProjectId is not a live same-id CANONICAL_ONLY Workspace"
        }
        return OperationalProjectOwnerStorage.WORKSPACE
    }

    if (context?.isDeleted == false) {
        return OperationalProjectOwnerStorage.CONTEXT
    }

    requireNotNull(workspace) {
        "Operational project $logicalProjectId has neither a live Context nor a Workspace owner"
    }
    require(!workspace.isDeleted) {
        "Operational project $logicalProjectId has a deleted Workspace owner"
    }

    return when {
        workspace.provenance == WorkspaceProvenance.STANDALONE.name &&
            workspace.sourceContextId == null -> OperationalProjectOwnerStorage.WORKSPACE

        context?.isDeleted == true && workspace.isLiveCanonicalOnlyWithoutContextSource() ->
            OperationalProjectOwnerStorage.WORKSPACE

        else ->
            throw IllegalArgumentException(
                "Operational project $logicalProjectId is neither a live Context owner nor an admitted Workspace owner",
            )
    }
}

private fun WorkspaceEntity?.isLiveCanonicalOnlyWithoutContextSource(): Boolean =
    this != null &&
        !isDeleted &&
        provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
        sourceContextId == null
