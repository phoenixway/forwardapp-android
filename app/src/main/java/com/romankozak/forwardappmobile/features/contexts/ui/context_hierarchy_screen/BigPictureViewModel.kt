package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
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
    private val revealRequested = savedStateHandle.getStateFlow("bigPictureRevealRequested", true)

    // Canonical H1 is structural authority. Presentation contributes only
    // display sections and exact occurrence membership.
    private val tree = combine(
        repository.observe(),
        catalog.observeCatalog(),
    ) { structure, entities ->
        val nodes = projectBigPicture(structure.placements, entities)
        val read = BigPictureReadComposition(
            nodes = nodes,
            historicalScopes = bigPictureScopes(structure, nodes, entities),
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
                    it.placement.target.type ==
                        com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType.WORKSPACE &&
                    it.placement.target.id == revealWorkspaceId
            }
            val focus = focusBigPicture(
                read.nodes,
                reveal?.placement?.id?.value ?: focusedId,
            )

            val scope = if (focus.activeNode != null) {
                // Nearest presentation membership takes precedence.
                // Otherwise preserve the historical root Group/NoBeacon scope.
                focus.breadcrumbs.asReversed().firstNotNullOfOrNull { node ->
                    read.containingGenericSection(node.placement.id.value)
                } ?: focus.breadcrumbs.firstOrNull()?.placement?.id
                    ?.let(read::historicalScopeForRoot)
            } else {
                scopeId?.let(read::section)
            }

            val entries = when {
                focus.activeNode != null ->
                    read.occurrenceEntries(focus.activeNode.placement.id.value)
                scope?.origin == BigPictureContainerOrigin.PRESENTATION ->
                    read.sectionEntries(scope.id)
                scope != null -> read.historicalEntries(scope.id)
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
            ) as BigPictureUiState
        }
            .catch {
                emit(
                    BigPictureUiState.Error(
                        it.message ?: "Не вдалося завантажити Big Picture",
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
        if (placementId != null && (current.nodes + current.breadcrumbs).none { it.placement.id.value == placementId }) return
        savedStateHandle["bigPictureFocus"] = placementId
        if (placementId == null) savedStateHandle["bigPictureScope"] = null
        savedStateHandle["bigPictureRevealRequested"] = false
    }

    fun focusScope(scopeId: String?) {
        val current = uiState.value as? BigPictureUiState.Ready ?: return
        if (scopeId != null && current.availableScopes.none { it.id == scopeId }) return
        savedStateHandle["bigPictureScope"] = scopeId
        savedStateHandle["bigPictureFocus"] = null
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
