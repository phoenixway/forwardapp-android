package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.planning.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*

@HiltViewModel
class DayTaskOrientationSourcesViewModel @Inject constructor(
    references: CanonicalPlanningReferenceRepository,
    catalog: CanonicalEntityCatalogRepository,
    orientationDao: OrientationDao,
    private val navigation: DefaultNavigationDispatcher,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val snapshot = observePlanningOrientationSources(references, catalog, orientationDao, PlanningReferenceOwnerKind.DAY_TASK)
        .flowOn(dispatcher).catch { emit(PlanningOrientationSourceSnapshot()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningOrientationSourceSnapshot())
    val sources = snapshot.map { it.sources }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val related = snapshot.map { it.related }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun open(id: String) {
        snapshot.value.destination(id)?.let(navigation::navigate)
    }
}

internal data class DayTaskOrientationSourcesUi(
    val byTask: Map<String, CanonicalEntitySummary>,
    val relatedByTask: Map<String, List<CanonicalEntitySummary>>,
    val onOpen: (String) -> Unit,
)

@Composable
internal fun rememberDayTaskOrientationSources(): DayTaskOrientationSourcesUi {
    val model: DayTaskOrientationSourcesViewModel = hiltViewModel()
    val sources by model.sources.collectAsStateWithLifecycle()
    val related by model.related.collectAsStateWithLifecycle()
    return remember(model, sources, related) { DayTaskOrientationSourcesUi(sources, related, model::open) }
}
