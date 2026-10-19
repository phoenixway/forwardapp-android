package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

/** Semantic-first composition, action-first navigation. No structural or lifecycle writes. */
internal fun withOperationalEmbodiments(
    nodes: List<BigPictureNode>,
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
): List<BigPictureNode> {
    val subjects = catalog.filter { it.ref is CanonicalEntityRef.Orientation || it.ref is CanonicalEntityRef.Aspect }
        .groupBy { it.ref.id }
    val workspaces = catalog.filter { it.ref is CanonicalEntityRef.Workspace }.groupBy { it.ref.id }
    val embodiments = bindings.filter { !it.isDeleted && it.bindingType == WorkspaceBindingType.EMBODIES.name }
    val bySubject = embodiments.groupBy { it.subjectId }
    val byWorkspace = embodiments.groupBy { it.workspaceId }
    val resolved = bySubject.mapNotNull { (subjectId, candidates) ->
        // Canonical commands enforce uniqueness. Corrupt/ambiguous frames must not redirect.
        val binding = candidates.singleOrNull() ?: return@mapNotNull null
        val subject = subjects[subjectId]?.singleOrNull() ?: return@mapNotNull null
        val workspace = workspaces[binding.workspaceId]?.singleOrNull() ?: return@mapNotNull null
        if (byWorkspace[binding.workspaceId]?.size != 1) return@mapNotNull null
        subject.ref to (workspace.ref as CanonicalEntityRef.Workspace)
    }.toMap()
    return nodes.map { node -> node.copy(operationalEmbodiment = resolved[node.entity?.ref]) }
}
