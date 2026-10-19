package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticEntityLifecycleCoordinator
import com.romankozak.forwardappmobile.data.orientation.SemanticEntityDeletionTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SemanticEntityDeleteState(
    val target: SemanticEntityDeletionTarget? = null,
    val confirming: Boolean = false,
    val busy: Boolean = false,
    val deleted: Boolean = false,
    val error: String? = null,
)

/** Consumer of canonical lifecycle. No saved destructive command, structural writes or compensation. */
@HiltViewModel
class SemanticEntityDeleteViewModel @Inject constructor(
    private val lifecycle: CanonicalSemanticEntityLifecycleCoordinator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SemanticEntityDeleteState())
    val state = mutableState.asStateFlow()
    private var capabilityJob: Job? = null

    fun prepare(subjectId: String?) {
        if (state.value.busy || state.value.deleted) return
        capabilityJob?.cancel()
        mutableState.value = SemanticEntityDeleteState()
        if (subjectId == null) return
        capabilityJob = viewModelScope.launch {
            try {
                mutableState.value = SemanticEntityDeleteState(target = lifecycle.deletionTarget(subjectId))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Fail closed: no delete action without an authoritative capability.
                mutableState.value = SemanticEntityDeleteState()
            }
        }
    }

    fun request() {
        if (state.value.target == null || state.value.busy || state.value.deleted) return
        mutableState.value = state.value.copy(confirming = true, error = null)
    }

    fun cancel() {
        if (!state.value.busy) mutableState.value = state.value.copy(confirming = false, error = null)
    }

    fun confirm() {
        val captured = state.value
        val target = captured.target ?: return
        if (!captured.confirming || captured.busy || captured.deleted) return
        mutableState.value = captured.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                lifecycle.tombstone(target.subjectId, target.subjectType, target.version)
                mutableState.value = SemanticEntityDeleteState(deleted = true)
            } catch (cancelled: CancellationException) {
                mutableState.value = captured.copy(confirming = false)
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = captured.copy(error = failure.message ?: "Не вдалося видалити сутність")
            }
        }
    }

    /** Consume once before popping the editor; recomposition cannot replay navigation. */
    fun consumeDeleted(): Boolean = state.value.deleted.also {
        if (it) mutableState.value = SemanticEntityDeleteState()
    }
}
