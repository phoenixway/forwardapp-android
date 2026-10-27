package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectLinksRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.workspace.projectEmbodiedOrientationRelations
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

/** Scoped observable relations plus the existing bulk catalog; no per-card or per-edge reads. */
class EmbodiedOrientationRelationsRepository @Inject constructor(
    private val memberships: CanonicalAspectLinksRepository,
    private val graph: CanonicalOrientationGraphRepository,
) {
    fun observe(id: String, catalog: List<CanonicalEntitySummary>) =
        combine(memberships.observeMemberships(id), graph.observeRelations(id)) { refs, edges ->
            projectEmbodiedOrientationRelations(id, catalog, refs, edges)
        }
}
