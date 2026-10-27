package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType

/** Native capability: admit the view without inventing a legacy capability mapping. */
internal fun ContextScreenDataSnapshot.canonicalRelationsViewOverride(workspaceId: String): Map<CapabilityId, Boolean> {
    if (!hasCanonicalWorkspaceOwner || presentation?.id != workspaceId) return emptyMap()
    val coreInstances = workspaceCapabilities.filter {
        it.workspaceId == workspaceId && it.instanceKey == "default" &&
            it.capabilityType in setOf(WorkspaceCapabilityType.DASHBOARD.name, WorkspaceCapabilityType.RELATIONS.name)
    }.groupBy { it.capabilityType }
    require(coreInstances.values.all { it.size <= 1 }) { "Multiple core view default instances violate logical identity" }
    return com.romankozak.forwardappmobile.core.context.WorkspaceCoreViews.overrides()
}
