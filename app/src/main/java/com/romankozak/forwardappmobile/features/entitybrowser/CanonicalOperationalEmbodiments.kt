package com.romankozak.forwardappmobile.features.entitybrowser

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

/** Derived navigation only. Both endpoints must be live in the canonical catalog. */
internal fun resolveOperationalEmbodiments(
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
): Map<CanonicalEntityRef, CanonicalEntityRef.Workspace> {
    val subjects = catalog.filter { it.ref is CanonicalEntityRef.Orientation || it.ref is CanonicalEntityRef.Aspect }
        .groupBy { it.ref.id }
    val workspaces = catalog.filter { it.ref is CanonicalEntityRef.Workspace }.groupBy { it.ref.id }
    val embodiments = bindings.filter { !it.isDeleted && it.bindingType == WorkspaceBindingType.EMBODIES.name }
    val bySubject = embodiments.groupBy { it.subjectId }
    val byWorkspace = embodiments.groupBy { it.workspaceId }
    return bySubject.mapNotNull { (subjectId, candidates) ->
        // Canonical commands enforce uniqueness. Corrupt/ambiguous frames must not redirect.
        val binding = candidates.singleOrNull() ?: return@mapNotNull null
        val subject = subjects[subjectId]?.singleOrNull() ?: return@mapNotNull null
        val workspace = workspaces[binding.workspaceId]?.singleOrNull() ?: return@mapNotNull null
        if (byWorkspace[binding.workspaceId]?.size != 1) return@mapNotNull null
        subject.ref to (workspace.ref as CanonicalEntityRef.Workspace)
    }.toMap()
}
