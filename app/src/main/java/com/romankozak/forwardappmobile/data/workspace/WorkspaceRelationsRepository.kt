package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalWorkspaceBindingRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.workspace.WorkspaceRelationsProjection
import com.romankozak.forwardappmobile.features.workspace.projectWorkspaceRelations
import com.romankozak.forwardappmobile.features.workspace.embodiedSemanticRef
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** Derived read only. No H1, Context, tags, or operational routing inputs. */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceRelationsRepository @Inject constructor(
    private val bindings: CanonicalWorkspaceBindingRepository,
    private val catalog: CanonicalEntityCatalogRepository,
    private val planning: CanonicalPlanningReferenceRepository,
    private val aspectRelations: EmbodiedAspectRelationsRepository,
    private val orientationRelations: EmbodiedOrientationRelationsRepository,
) {
    fun observe(workspaceId: String) = combine(bindings.observeBindingsForWorkspace(workspaceId),
        catalog.observeCatalog(), planning.observeWorkspaceRelations(workspaceId)) { links, entities, references ->
        Triple(links, entities, references)
    }.flatMapLatest { (links, entities, references) ->
        val direct = if (entities.none { it.ref == CanonicalEntityRef.Workspace(workspaceId) }) WorkspaceRelationsProjection(workspaceId, emptyList())
            else projectWorkspaceRelations(workspaceId, links, entities, references)
        when (val source = embodiedSemanticRef(workspaceId, links, entities)) {
            is CanonicalEntityRef.Aspect -> aspectRelations.observe(workspaceId, source.id, entities).map { direct.copy(embodiedAspect = it) }
            is CanonicalEntityRef.Orientation -> orientationRelations.observe(source.id, entities).map { direct.copy(embodiedOrientation = it) }
            else -> flowOf(direct)
        }
    }.distinctUntilChanged().flowOn(Dispatchers.Default)
}
