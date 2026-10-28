package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.DefaultNavigationDispatcher
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.product.CanonicalGoalSemanticProjection
import com.romankozak.forwardappmobile.data.product.CanonicalGoalSemanticReadRepository
import com.romankozak.forwardappmobile.data.workspace.EmbodyingWorkspaceRequest
import com.romankozak.forwardappmobile.data.workspace.SemanticEmbodyingWorkspaceCoordinator
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.features.semanticeditor.EmbodyingWorkspaceState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal data class BacklogGoalSemanticLink(
    val subjectId: String,
    val version: Long,
    val title: String,
    val description: String?,
    val workspace: CanonicalEntitySummary?,
    val canCreate: Boolean,
)

/** Goal locators resolve only through the existing CUT_OVER projection, never raw ID equality. */
internal fun projectBacklogGoalLinks(
    goals: List<CanonicalGoalSemanticProjection>,
    subjects: List<ManagedSubjectEntity>,
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
): Map<String, BacklogGoalSemanticLink> {
    val byId = subjects.filter { !it.isDeleted && it.subjectType == ManagedSubjectType.ORIENTATION.name }.associateBy { it.id }
    val embodiments = resolveOperationalEmbodiments(bindings, catalog)
    val entities = catalog.associateBy { it.ref }
    return goals.mapNotNull { goal ->
        val subject = byId[goal.orientationId] ?: return@mapNotNull null
        val ref = CanonicalEntityRef.Orientation(subject.id)
        if (ref !in entities) return@mapNotNull null
        goal.goalId to BacklogGoalSemanticLink(subject.id, subject.version, subject.title, subject.description,
            embodiments[ref]?.let(entities::get), bindings.none {
                !it.isDeleted && it.subjectId == subject.id && it.bindingType == WorkspaceBindingType.EMBODIES.name
            })
    }.toMap()
}

@HiltViewModel
class BacklogGoalWorkspaceViewModel @Inject constructor(
    goals: CanonicalGoalSemanticReadRepository,
    dao: OrientationDao,
    catalog: CanonicalEntityCatalogRepository,
    private val coordinator: SemanticEmbodyingWorkspaceCoordinator,
    private val navigation: DefaultNavigationDispatcher,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {
    internal val links = combine(goals.observeAll(), dao.observeManagedSubjects(), dao.observeWorkspaceBindings(), catalog.observeCatalog(),
        ::projectBacklogGoalLinks).flowOn(dispatcher).catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    private val _creation = MutableStateFlow(EmbodyingWorkspaceState())
    val creation = _creation.asStateFlow()

    fun open(goalId: String) {
        val link = links.value[goalId] ?: return
        if (_creation.value.busy || !link.canCreate) return
        _creation.value = EmbodyingWorkspaceState(request = EmbodyingWorkspaceRequest(
            link.subjectId, ManagedSubjectType.ORIENTATION, link.version, link.title, link.description))
    }
    fun setName(value: String) { if (!_creation.value.busy) _creation.value = _creation.value.copy(request = _creation.value.request?.copy(name = value), error = null) }
    fun setDescription(value: String) { if (!_creation.value.busy) _creation.value = _creation.value.copy(request = _creation.value.request?.copy(description = value), error = null) }
    fun cancel() { if (!_creation.value.busy) _creation.value = EmbodyingWorkspaceState() }
    fun confirm() {
        val request = _creation.value.request ?: return
        if (_creation.value.busy || request.name.isBlank()) return
        _creation.value = _creation.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try { _creation.value = EmbodyingWorkspaceState(createdId = coordinator.create(request)) }
            catch (cancel: CancellationException) { _creation.value = EmbodyingWorkspaceState(); throw cancel }
            catch (failure: Exception) { _creation.value = EmbodyingWorkspaceState(request = request, error = failure.message ?: "Не вдалося створити Workspace") }
        }
    }
    fun openSemantic(goalId: String) { links.value[goalId]?.let { navigation.navigate(NavTarget.SemanticEntityEditor(it.subjectId)) } }
    fun openPlanning(goalId: String) { links.value[goalId]?.let { navigation.navigate(NavTarget.OrientationPlanning(it.subjectId)) } }
    fun openWorkspace(goalId: String) { links.value[goalId]?.workspace?.let { navigation.navigate(NavTarget.Workspace(it.ref.id)) } }
}
