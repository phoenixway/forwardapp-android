package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.romankozak.forwardappmobile.features.planning.PlanningOrientationSourceSnapshot
import com.romankozak.forwardappmobile.features.planning.observePlanningOrientationSources
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DayFocusOrientationSourcesViewModel @Inject constructor(
    references: CanonicalPlanningReferenceRepository,
    catalog: CanonicalEntityCatalogRepository,
    orientationDao: OrientationDao,
    private val navigation: DefaultNavigationDispatcher,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {
    val snapshot = observePlanningOrientationSources(
        references, catalog, orientationDao, PlanningReferenceOwnerKind.DAY_FOCUS,
    ).flowOn(dispatcher)
        .catch { emit(PlanningOrientationSourceSnapshot()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningOrientationSourceSnapshot())

    fun open(itemId: String) {
        val orientationId = snapshot.value.sources[itemId]?.ref?.id ?: return
        snapshot.value.destination(orientationId)?.let(navigation::navigate)
    }
}

internal data class DayFocusOrientationSourcesUi(
    val byItem: Map<String, CanonicalEntitySummary>,
    val onOpen: (String) -> Unit,
)

@Composable
internal fun rememberDayFocusOrientationSources(): DayFocusOrientationSourcesUi {
    val model: DayFocusOrientationSourcesViewModel = hiltViewModel()
    val snapshot by model.snapshot.collectAsStateWithLifecycle()
    return remember(model, snapshot) {
        DayFocusOrientationSourcesUi(snapshot.sources, model::open)
    }
}
