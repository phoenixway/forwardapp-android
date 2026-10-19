package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.EmbodyingWorkspaceRequest
import com.romankozak.forwardappmobile.data.workspace.SemanticEmbodyingWorkspaceCoordinator
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal val SemanticEntityEditorModel.canCreateEmbodyingWorkspace: Boolean get() =
    !isSaving && !isRelationMutationInFlight && workspaceBindings.none { it.bindingType == WorkspaceBindingType.EMBODIES }

data class EmbodyingWorkspaceState(
    val request: EmbodyingWorkspaceRequest? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val createdId: String? = null,
)

@HiltViewModel
class EmbodyingWorkspaceViewModel @Inject constructor(private val coordinator: SemanticEmbodyingWorkspaceCoordinator) : ViewModel() {
    // No persisted command: cancellation/recreation cannot replay Workspace creation.
    private val mutableState = MutableStateFlow(EmbodyingWorkspaceState())
    val state = mutableState.asStateFlow()

    fun open(model: SemanticEntityEditorModel) {
        if (state.value.busy || state.value.createdId != null || !model.canCreateEmbodyingWorkspace) return
        mutableState.value = EmbodyingWorkspaceState(request = EmbodyingWorkspaceRequest(model.id,
            when (model.entityClass) {
                SemanticEntityClass.ORIENTATION -> ManagedSubjectType.ORIENTATION
                SemanticEntityClass.ASPECT -> ManagedSubjectType.ASPECT
            }, model.version, model.persistedName, model.description))
    }

    fun setName(value: String) { if (!state.value.busy) mutableState.value = state.value.copy(request = state.value.request?.copy(name = value), error = null) }
    fun setDescription(value: String) { if (!state.value.busy) mutableState.value = state.value.copy(request = state.value.request?.copy(description = value), error = null) }
    fun cancel() { if (!state.value.busy) mutableState.value = EmbodyingWorkspaceState() }
    fun consumeCreatedId(): String? = state.value.createdId.also { mutableState.value = state.value.copy(createdId = null) }

    fun confirm() {
        val request = state.value.request ?: return
        if (state.value.busy || request.name.isBlank()) return
        mutableState.value = state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                mutableState.value = EmbodyingWorkspaceState(createdId = coordinator.create(request))
            } catch (cancelled: CancellationException) {
                mutableState.value = EmbodyingWorkspaceState()
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = EmbodyingWorkspaceState(request = request, error = failure.message ?: "Не вдалося створити Workspace")
            }
        }
    }
}
