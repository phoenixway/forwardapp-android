package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*

sealed interface BigPictureUiState {
    data object Loading : BigPictureUiState
    data class Ready(
        val nodes: List<BigPictureNode>,
        val activeNode: BigPictureNode?,
        val breadcrumbs: List<BigPictureNode>,
        val scopes: List<BigPictureContainer> = emptyList(),
        val activeScope: BigPictureContainer? = null,
        val entries: List<BigPictureReadEntry> = emptyList(),
        val availableScopes: List<BigPictureContainer> = scopes,
        val scopeBreadcrumbs: List<BigPictureContainer> = emptyList(),
        val scopeAnchorPlacementId: String? = null,
        val parentSectionId: String? = null,
        val directSectionId: String? = null,
        val structure: BigPictureStructuralIndex? = null,
        val navigationBreadcrumbs: List<BreadcrumbItem> = emptyList(),
    ) : BigPictureUiState
    data class Error(val message: String) : BigPictureUiState
}

@HiltViewModel
class BigPictureViewModel @Inject constructor(
    repository: BigPictureStructureRepository,
    catalog: CanonicalEntityCatalogRepository,
    private val savedStateHandle: SavedStateHandle,
    @DefaultDispatcher computationDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val focusedPlacementId = savedStateHandle.getStateFlow<String?>("bigPictureFocus", null)
    private val focusedScopeId = savedStateHandle.getStateFlow<String?>("bigPictureScope", null)
    private val revealWorkspaceId = savedStateHandle.get<String>("projectIdToReveal")
    private val revealPlacementId = savedStateHandle.get<String>("placementIdToReveal")
    private val revealRequested = savedStateHandle.getStateFlow("bigPictureRevealRequested", true)

    // Canonical H1 is structural authority. Presentation contributes only
    // display sections and exact occurrence membership.
    private val tree = combine(
        repository.observe(),
        catalog.observeCatalog(),
    ) { structure, entities ->
        val nodes = withOperationalEmbodiments(
            projectBigPicture(structure.placements, entities), structure.embodimentBindings, entities,
        )
        val read = BigPictureReadComposition(
            nodes = nodes,
            historicalScopes = emptyList(),
            persisted = structure.presentation,
        )
        read to BigPictureStructuralIndex(read)
    }

    val uiState: StateFlow<BigPictureUiState> =
        combine(tree, focusedPlacementId, revealRequested, focusedScopeId) {
                indexedRead, focusedId, shouldReveal, scopeId ->
            val (read, structuralIndex) = indexedRead
            val reveal = read.nodes.firstOrNull {
                shouldReveal &&
                    (revealPlacementId == null || it.placement.id.value == revealPlacementId) &&
                    it.placement.target.type ==
                        com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType.WORKSPACE &&
                    (revealWorkspaceId == null || it.placement.target.id == revealWorkspaceId) &&
                    (revealPlacementId != null || revealWorkspaceId != null)
            }
            val focus = focusBigPicture(
                read.nodes,
                if (shouldReveal && revealPlacementId != null) reveal?.placement?.id?.value
                else reveal?.placement?.id?.value ?: focusedId,
            )

            val scope = if (focus.activeNode != null) {
                // Containers, including materialized historical groups, are ordinary H1 parents.
                focus.breadcrumbs.asReversed().firstNotNullOfOrNull { node ->
                    read.containingGenericSection(node.placement.id.value)
                }
            } else {
                scopeId?.let(read::section)
            }

            val entries = when {
                focus.activeNode != null ->
                    read.occurrenceEntries(focus.activeNode.placement.id.value)
                scope?.origin == BigPictureContainerOrigin.PRESENTATION ->
                    read.sectionEntries(scope.id)
                else -> read.rootEntries()
            }

            val scopeBreadcrumbs = when (scope?.origin) {
                BigPictureContainerOrigin.PRESENTATION ->
                    read.genericAncestors(scope.id)
                null -> emptyList()
                else -> listOf(scope)
            }

            val genericRecord = scope
                ?.takeIf { it.origin == BigPictureContainerOrigin.PRESENTATION }
                ?.let { read.genericSectionRecord(it.id) }

            BigPictureUiState.Ready(
                structure = structuralIndex,
                nodes = entries.filterIsInstance<BigPictureReadEntry.Occurrence>()
                    .map { it.node },
                activeNode = focus.activeNode,
                breadcrumbs = focus.breadcrumbs,
                scopes = read.rootScopes,
                activeScope = scope,
                entries = entries,
                availableScopes = read.allScopes,
                scopeBreadcrumbs = scopeBreadcrumbs,
                scopeAnchorPlacementId = genericRecord?.anchorPlacementId,
                parentSectionId = scopeBreadcrumbs.dropLast(1).lastOrNull()?.id,
                directSectionId = focus.activeNode
                    ?.placement?.id?.value
                    ?.let(read::containingGenericSection)?.id,
                navigationBreadcrumbs = structuralIndex.breadcrumbs(
                    focus.activeNode?.let {
                        H1StructuralParent.Node(
                            H1TypedNode.Occurrence(it.placement.id.value),
                        )
                    } ?: scope?.let {
                        H1StructuralParent.Node(
                            structuralIndex.sections.getValue(it.id),
                        )
                    } ?: H1StructuralParent.Root,
                ),
            ) as BigPictureUiState
        }
            .catch {
                emit(
                    BigPictureUiState.Error(
                        it.message ?: "Не вдалося завантажити Navigation",
                    ),
                )
            }
            .flowOn(computationDispatcher)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                BigPictureUiState.Loading,
            )

    fun focusPlacement(placementId: String?) {
        val current = uiState.value as? BigPictureUiState.Ready ?: return
        if (placementId != null &&
            current.nodes.none { it.placement.id.value == placementId } &&
            current.navigationBreadcrumbs.none { it.placementId == placementId }
        ) return
        savedStateHandle["bigPictureFocus"] = placementId
        if (placementId == null) savedStateHandle["bigPictureScope"] = null
        savedStateHandle["bigPictureRevealRequested"] = false
        rememberPath(current, placementId?.let { H1StructuralParent.Node(H1TypedNode.Occurrence(it)) } ?: H1StructuralParent.Root)
    }

    fun focusScope(scopeId: String?) {
        val current = uiState.value as? BigPictureUiState.Ready ?: return
        if (scopeId != null && current.availableScopes.none { it.id == scopeId }) return
        savedStateHandle["bigPictureScope"] = scopeId
        savedStateHandle["bigPictureFocus"] = null
        savedStateHandle["bigPictureRevealRequested"] = false
        rememberPath(current, scopeId?.let { current.structure?.sections?.get(it)?.let(H1StructuralParent::Node) } ?: H1StructuralParent.Root)
    }

    /** Presentation-only recovery; never a source/structural mutation. */
    fun rememberCurrentLocation() {
        val current = uiState.value as? BigPictureUiState.Ready ?: return
        val location = current.structuralDestination() ?: H1StructuralParent.Root
        rememberPath(current, location)
        setLocation(location)
    }

    fun recoverMissingFocus() {
        val current = uiState.value as? BigPictureUiState.Ready ?: return
        val index = current.structure ?: return
        val requested = focusedPlacementId.value?.let { H1TypedNode.Occurrence(it) }
            ?: focusedScopeId.value?.let { index.sections[it] ?: H1TypedNode.Container(it.removePrefix("presentation:")) }
            ?: return
        if (index.items.any { it.ref == requested }) return
        val history = savedStateHandle.get<List<String>>("bigPictureFocusPath").orEmpty()
        val survivor = history.asReversed().mapNotNull { runCatching { decodeStructuralNode(it) }.getOrNull() }
            .firstOrNull { candidate -> index.items.any { it.ref == candidate } }
        val location = survivor?.let { H1StructuralParent.Node(it) } ?: H1StructuralParent.Root
        rememberPath(current, location)
        setLocation(location)
    }

    private fun rememberPath(current: BigPictureUiState.Ready, location: H1StructuralParent) {
        val index = current.structure ?: return
        savedStateHandle["bigPictureFocusPath"] = index.breadcrumbs(location).map { crumb ->
            encodeStructuralNode(crumb.placementId?.let { H1TypedNode.Occurrence(it) } ?: index.sections.getValue(crumb.id))
        }
    }

    private fun setLocation(location: H1StructuralParent) {
        val node = (location as? H1StructuralParent.Node)?.ref
        savedStateHandle["bigPictureFocus"] = (node as? H1TypedNode.Occurrence)?.id
        savedStateHandle["bigPictureScope"] = (node as? H1TypedNode.Container)?.id?.let { "presentation:$it" }
        savedStateHandle["bigPictureRevealRequested"] = false
    }

    fun goUp() {
        val current = uiState.value as? BigPictureUiState.Ready ?: return
        val active = current.activeNode

        if (active != null) {
            // A directly sectioned occurrence returns to its containing
            // section before walking the structural H1 parent chain.
            current.directSectionId?.let {
                focusScope(it)
                return
            }
            active.placement.parentPlacementId?.let {
                focusPlacement(it.value)
                return
            }
            focusScope(current.activeScope?.id)
            return
        }

        val scope = current.activeScope
        if (scope?.origin != BigPictureContainerOrigin.PRESENTATION) {
            focusScope(null)
            return
        }

        current.parentSectionId?.let {
            focusScope(it)
            return
        }

        current.scopeAnchorPlacementId?.let { anchor ->
            // The anchor is validated by the persisted read composition.
            // It need not be one of the section's visible member rows.
            savedStateHandle["bigPictureFocus"] = anchor
            savedStateHandle["bigPictureScope"] = null
            savedStateHandle["bigPictureRevealRequested"] = false
            return
        }

        focusScope(null)
    }
}
