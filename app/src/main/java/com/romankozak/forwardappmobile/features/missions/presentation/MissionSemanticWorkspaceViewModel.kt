package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.PlanningCompositionReadRepository
import com.romankozak.forwardappmobile.data.workspace.SemanticEmbodyingWorkspaceCoordinator
import com.romankozak.forwardappmobile.features.semanticeditor.EmbodyingWorkspaceState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class MissionSemanticWorkspaceViewModel @Inject constructor(
    planning: PlanningCompositionReadRepository,
    dao: OrientationDao,
    catalog: CanonicalEntityCatalogRepository,
    private val coordinator: SemanticEmbodyingWorkspaceCoordinator,
    private val navigation: DefaultNavigationDispatcher,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val snapshot = combine(planning.projection, dao.observeManagedSubjects(), dao.observeWorkspaceBindings(), catalog.observeCatalog()) { frame, subjects, bindings, entities ->
        val actions = projectMissionSemanticWorkspaces(frame, subjects, bindings, entities)
        MissionSemanticWorkspaceSnapshot(actions, projectMissionPrimaryTargets(frame, entities, actions))
    }.flowOn(dispatcher).catch { emit(MissionSemanticWorkspaceSnapshot()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MissionSemanticWorkspaceSnapshot())
    internal val actions = snapshot.map { it.actions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    internal val targets = snapshot.map { it.targets }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val mutableCreation = MutableStateFlow(EmbodyingWorkspaceState())
    val creation = mutableCreation.asStateFlow()

    fun perform(missionId: Long) {
        if (creation.value.busy || creation.value.request != null || creation.value.createdId != null) return
        when (val action = actions.value[missionId]) {
            is MissionSemanticWorkspaceAction.Open -> openWorkspace(action.workspaceId)
            is MissionSemanticWorkspaceAction.Create -> mutableCreation.value = EmbodyingWorkspaceState(request = action.request)
            null -> Unit
        }
    }
    fun setName(value: String) { if (!creation.value.busy) mutableCreation.value = creation.value.copy(request = creation.value.request?.copy(name = value), error = null) }
    fun setDescription(value: String) { if (!creation.value.busy) mutableCreation.value = creation.value.copy(request = creation.value.request?.copy(description = value), error = null) }
    fun cancel() { if (!creation.value.busy) mutableCreation.value = EmbodyingWorkspaceState() }
    fun consumeCreated(): String? = creation.value.createdId.also { mutableCreation.value = creation.value.copy(createdId = null) }
    fun openWorkspace(id: String) { navigation.navigate(NavTarget.Workspace(id)) }
    fun openPrimary(missionId: Long) { snapshot.value.targets[missionId]?.destination?.let(navigation::navigate) }

    fun confirm() {
        val request = creation.value.request ?: return
        if (creation.value.busy || request.name.isBlank()) return
        mutableCreation.value = creation.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try { mutableCreation.value = EmbodyingWorkspaceState(createdId = coordinator.create(request)) }
            catch (cancel: CancellationException) { mutableCreation.value = EmbodyingWorkspaceState(); throw cancel }
            catch (failure: Exception) { mutableCreation.value = EmbodyingWorkspaceState(request = request, error = failure.message ?: "Не вдалося створити Workspace") }
        }
    }
}
