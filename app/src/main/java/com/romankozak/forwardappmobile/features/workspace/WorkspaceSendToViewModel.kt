package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WorkspaceSendToState(
    val workspaceId: String = "",
    val title: String = "",
    val loading: Boolean = true,
    val busy: Boolean = false,
    val completed: WorkspaceSendTarget? = null,
    val error: String? = null,
) {
    val canSend: Boolean get() = workspaceId.isNotBlank() && !loading && !busy && completed == null
}

@HiltViewModel
class WorkspaceSendToViewModel @Inject constructor(private val actions: WorkspaceSendToActions) : ViewModel() {
    private val _state = MutableStateFlow(WorkspaceSendToState())
    val state = _state.asStateFlow()

    fun open(workspaceId: String) {
        if (_state.value.busy) return
        _state.value = WorkspaceSendToState(workspaceId = workspaceId)
        viewModelScope.launch {
            try {
                val source = actions.source(workspaceId)
                _state.value = WorkspaceSendToState(workspaceId, source.title, loading = false)
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { _state.value = WorkspaceSendToState(loading = false, error = "Workspace недоступний") }
        }
    }

    fun send(target: WorkspaceSendTarget) {
        val current = _state.value
        if (!current.canSend) return
        _state.value = current.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                actions.send(current.workspaceId, target)
                _state.value = current.copy(completed = target, error = null)
            } catch (cancel: CancellationException) { _state.value = current; throw cancel }
            catch (_: Exception) { _state.value = current.copy(error = "Не вдалося створити елемент. Спробуйте ще раз.") }
        }
    }
}
