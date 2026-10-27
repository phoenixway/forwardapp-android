package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.*
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class OrientationKindState(
    val eligible: Boolean = false, val open: Boolean = false, val busy: Boolean = false,
    val plan: OrientationKindPlan? = null, val target: OrientationKind? = null,
    val error: String? = null, val completed: Boolean = false,
)

/** Only the route ID is restored; a pending transformation is never replayed. */
@HiltViewModel
class OrientationKindViewModel @Inject constructor(saved: SavedStateHandle, private val coordinator: CanonicalOrientationKindCoordinator) : ViewModel() {
    private val id = saved.get<String>(SemanticEntityEditorViewModel.SUBJECT_ID_ARGUMENT).orEmpty()
    private val mutable = MutableStateFlow(OrientationKindState())
    val state = mutable.asStateFlow()
    init { refreshEligibility() }
    fun refreshEligibility() {
        if (state.value.busy) return
        viewModelScope.launch {
            try { mutable.value = state.value.copy(eligible = coordinator.canOffer(id)) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value = state.value.copy(eligible = false) }
        }
    }
    fun open() {
        if (!state.value.eligible || state.value.busy) return
        mutable.value = OrientationKindState(eligible = true, open = true, busy = true)
        viewModelScope.launch {
            try { mutable.value = state.value.copy(plan = coordinator.preview(id), busy = false) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = state.value.copy(busy = false, error = e.message) }
        }
    }
    fun select(kind: OrientationKind) {
        if (state.value.open && !state.value.busy && kind in SUBJECT_CONVERSION_TARGET_KINDS && kind != state.value.plan?.kind)
            mutable.value = state.value.copy(target = kind)
    }
    fun cancel() { if (!state.value.busy) mutable.value = OrientationKindState(eligible = state.value.eligible) }
    fun confirm() {
        val request = state.value
        val plan = request.plan ?: return
        val target = request.target ?: return
        if (!request.open || request.busy) return
        mutable.value = request.copy(busy = true, error = null)
        viewModelScope.launch {
            try { coordinator.transform(plan, target); mutable.value = OrientationKindState(completed = true) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = request.copy(error = e.message ?: "Не вдалося змінити тип") }
        }
    }
    fun consumeCompleted() = state.value.completed.also { if (it) mutable.value = state.value.copy(completed = false) }
}
