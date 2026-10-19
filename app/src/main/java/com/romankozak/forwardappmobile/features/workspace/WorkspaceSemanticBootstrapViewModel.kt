package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.workspace.WorkspaceSemanticBootstrapCoordinator
import com.romankozak.forwardappmobile.data.workspace.WorkspaceSemanticBootstrapRequest
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.features.semantic.creatableOrientationKinds
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkspaceSemanticBootstrapState(
    val open: Boolean = false,
    val title: String = "",
    val description: String = "",
    val entityClass: SemanticEntityClass? = null,
    val kind: OrientationKind? = null,
    val bindingType: WorkspaceBindingType? = null,
    val saving: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
) {
    val canSubmit: Boolean get() = open && !saving && createdId == null && title.isNotBlank() &&
        bindingType != null && entityClass != null &&
        (entityClass == SemanticEntityClass.ASPECT || kind in creatableOrientationKinds)
}

@HiltViewModel
class WorkspaceSemanticBootstrapViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val coordinator: WorkspaceSemanticBootstrapCoordinator,
) : ViewModel() {
    private val workspaceId = savedState.get<String>("workspaceId").orEmpty()
    // Commands are never restored/replayed. If process death interrupted confirmation,
    // require explicit inspection of Properties before starting a new creation action.
    private val _state = MutableStateFlow(WorkspaceSemanticBootstrapState(
        createdId = savedState[CREATED],
        error = if (savedState.get<Boolean>(SUBMITTED) == true && savedState.get<String>(CREATED) == null)
            "Створення було перервано. Перевірте семантичні зв’язки у властивостях перед новою спробою." else null,
    ))
    val state = _state.asStateFlow()

    fun open(title: String, description: String?) {
        if (_state.value.saving || workspaceId.isBlank()) return
        val interrupted = _state.value.error
        savedState[CREATED] = null
        savedState[SUBMITTED] = false
        _state.value = WorkspaceSemanticBootstrapState(open = true, title = title, description = description.orEmpty(), error = interrupted)
    }

    fun edit(title: String = _state.value.title, description: String = _state.value.description) {
        if (!_state.value.saving) _state.value = _state.value.copy(title = title, description = description, error = null)
    }

    fun chooseClass(value: SemanticEntityClass) {
        if (!_state.value.saving) _state.value = _state.value.copy(entityClass = value, kind = null, error = null)
    }

    fun chooseKind(value: OrientationKind) {
        require(value in creatableOrientationKinds)
        if (!_state.value.saving) _state.value = _state.value.copy(kind = value, error = null)
    }

    fun chooseBinding(value: WorkspaceBindingType) {
        if (!_state.value.saving) _state.value = _state.value.copy(bindingType = value, error = null)
    }

    fun cancel() {
        if (!_state.value.saving) _state.value = WorkspaceSemanticBootstrapState()
    }

    fun submit() {
        val draft = _state.value
        if (!draft.canSubmit || workspaceId.isBlank()) return
        _state.value = draft.copy(saving = true, error = null)
        savedState[SUBMITTED] = true
        viewModelScope.launch {
            try {
                val id = coordinator.createAndBind(WorkspaceSemanticBootstrapRequest(
                    workspaceId, ManagedSubjectType.valueOf(requireNotNull(draft.entityClass).name),
                    draft.kind, draft.title, draft.description, requireNotNull(draft.bindingType),
                ))
                savedState[CREATED] = id
                _state.value = draft.copy(open = false, createdId = id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                savedState[SUBMITTED] = false
                _state.value = draft.copy(error = failure.message ?: "Не вдалося створити сутність і зв’язок")
            }
        }
    }

    /** Consume only navigation, never a mutation. Recollection cannot replay creation. */
    fun consumeCreatedDestination(): NavTarget.SemanticEntityEditor? {
        val id = _state.value.createdId ?: return null
        savedState[CREATED] = null
        savedState[SUBMITTED] = false
        _state.value = WorkspaceSemanticBootstrapState()
        return NavTarget.SemanticEntityEditor(id)
    }

    private companion object {
        const val SUBMITTED = "workspaceSemanticBootstrap.submitted"
        const val CREATED = "workspaceSemanticBootstrap.created"
    }
}
