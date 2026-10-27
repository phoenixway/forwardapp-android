package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.planning.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*

/** SOURCE is not the Mission's own Orientation participation or a RELATED reference. */
internal fun projectTacticalOrientationSources(
    rows: List<PlanningReferenceEntity>,
    catalog: List<CanonicalEntitySummary>,
): Map<Long, CanonicalEntitySummary> =
    projectPlanningOrientationSources(rows, catalog, PlanningReferenceOwnerKind.TACTICAL_MISSION).mapKeys { it.key.toLong() }

@HiltViewModel
class TacticalMissionOrientationSourcesViewModel @Inject constructor(
    references: CanonicalPlanningReferenceRepository,
    catalog: CanonicalEntityCatalogRepository,
    orientationDao: OrientationDao,
    private val navigation: DefaultNavigationDispatcher,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val snapshot = observePlanningOrientationSources(references, catalog, orientationDao, PlanningReferenceOwnerKind.TACTICAL_MISSION)
        .flowOn(dispatcher).catch { emit(PlanningOrientationSourceSnapshot()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningOrientationSourceSnapshot())

    val sources = snapshot.map { state -> state.sources.mapKeys { it.key.toLong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val related = snapshot.map { state -> state.related.mapKeys { it.key.toLong() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun open(id: String) {
        snapshot.value.destination(id)?.let(navigation::navigate)
    }
}

internal data class TacticalOrientationSourcesUi(
    val byMission: Map<Long, CanonicalEntitySummary>,
    val relatedByMission: Map<Long, List<CanonicalEntitySummary>>,
    val onOpen: (String) -> Unit,
)

/** One shared observable read per list, never a DAO/ViewModel lookup per card. */
@Composable
internal fun rememberTacticalOrientationSources(): TacticalOrientationSourcesUi {
    val model: TacticalMissionOrientationSourcesViewModel = hiltViewModel()
    val sources by model.sources.collectAsStateWithLifecycle()
    val related by model.related.collectAsStateWithLifecycle()
    return remember(model, sources, related) { TacticalOrientationSourcesUi(sources, related, model::open) }
}

@Composable
internal fun TacticalMissionOrientationBadge(source: CanonicalEntitySummary, onOpen: () -> Unit) {
    PlanningOrientationSourceBadge(source, onOpen = onOpen)
}
