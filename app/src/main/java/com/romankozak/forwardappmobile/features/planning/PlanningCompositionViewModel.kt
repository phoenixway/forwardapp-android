package com.romankozak.forwardappmobile.features.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class PlanningCompositionViewModel @Inject constructor(
    reads: PlanningCompositionReadRepository,
    private val relations: CanonicalPlanningCommitmentRelationRepository,
    private val coordinator: PlanningCompositionCoordinator,
    private val navigation: DefaultNavigationDispatcher,
    private val contextCommands: PlanningContextCoordinator,
    catalog: com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository,
    orientationDao: com.romankozak.forwardappmobile.data.orientation.OrientationDao,
) : ViewModel() {
    val error = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val projection = reads.projection.catch { error.value = it.message; emit(PlanningCommitmentCompositionProjection(emptyList(), emptyList())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningCommitmentCompositionProjection(emptyList(), emptyList()))
    val embodiments = combine(catalog.observeCatalog(), orientationDao.observeWorkspaceBindings()) { entities, bindings ->
        com.romankozak.forwardappmobile.features.entitybrowser.resolveOperationalEmbodiments(bindings, entities)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun open(item: PlanningParticipantSummary) = navigation.navigate(item.planningDestination())
    fun context(item: PlanningParticipantSummary) = navigation.navigate(NavTarget.PlanningParticipation(item.commitment.id.value))
    fun orientation(id: String) = navigation.navigate(NavTarget.OrientationPlanning(id))
    fun plan(id: String, horizon: PlanningScopeKind) = command { contextCommands.add(id, horizon) }
    fun promote(item: PlanningParticipantSummary, horizon: PlanningScopeKind) = command { contextCommands.promote(item.commitment.id, horizon) }
    fun removeParticipation(item: PlanningParticipantSummary) = command { contextCommands.remove(item.commitment.id, item.commitment.version) }
    fun setStatus(item: PlanningParticipantSummary, status: PlanningCommitmentStatus) = command { contextCommands.status(item.commitment.id, item.commitment.version, status) }
    fun toggleCompleted(item: PlanningParticipantSummary) = setStatus(item, completionToggleStatus(item.commitment.status))
    fun semantic(item: PlanningParticipantSummary) = navigation.navigate(NavTarget.SemanticEntityEditor(item.commitment.orientationId))
    fun workspace(item: PlanningParticipantSummary) {
        val ref = com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef.Orientation(item.commitment.orientationId)
        embodiments.value[ref]?.let { navigation.navigate(NavTarget.Workspace(it.id)) }
    }
    fun link(source: PlanningCommitmentId, target: PlanningCommitmentId) = command { relations.addRelation(source, target) }
    suspend fun addComponent(target: PlanningCommitmentId, orientationId: String) { coordinator.addStrategicComponent(target, orientationId) }
    fun remove(edge: PlanningCompositionEdge) = command { relations.removeRelation(edge.relation.id, edge.relation.version) }
    fun move(target: PlanningCommitmentId, edge: PlanningCompositionEdge, delta: Int) = command {
        val edges = projection.value.incoming(target).map { it.relation.id }.toMutableList()
        val index = edges.indexOf(edge.relation.id)
        require(index >= 0 && index + delta in edges.indices)
        edges.add(index + delta, edges.removeAt(index))
        relations.reorderComponents(target, edges)
    }
    private fun command(action: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try { action() } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { error.value = failure.message ?: "Не вдалося змінити композицію" }
            finally { busy.value = false }
        }
    }
}
