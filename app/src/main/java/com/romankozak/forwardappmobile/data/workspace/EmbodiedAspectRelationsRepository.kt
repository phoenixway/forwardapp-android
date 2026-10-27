package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectLinksRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.workspace.projectEmbodiedAspectRelations
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

/** Reuses canonical read authorities; one bounded expansion, no per-row reads or mutations. */
class EmbodiedAspectRelationsRepository @Inject constructor(
    private val aspects: CanonicalAspectRepository,
    private val associations: CanonicalAspectLinksRepository,
    private val bindings: CanonicalWorkspaceBindingRepository,
    private val planning: CanonicalPlanningReferenceRepository,
) {
    fun observe(workspaceId: String, aspectId: String, catalog: List<CanonicalEntitySummary>) = combine(
        aspects.observeLive(), associations.observeMembershipsForAspect(aspectId), bindings.observeBindings(aspectId),
        planning.observeAspectRelations(aspectId),
    ) { nodes, refs, links, planningRows ->
        projectEmbodiedAspectRelations(workspaceId, aspectId, catalog, nodes, refs, links, planningRows)
    }
}
