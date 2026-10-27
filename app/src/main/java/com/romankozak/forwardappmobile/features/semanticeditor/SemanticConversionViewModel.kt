package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.*
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SemanticConversionState(
    val eligible: Boolean = false, val open: Boolean = false, val busy: Boolean = false,
    val plan: AspectConversionPlan? = null, val kind: OrientationKind? = null,
    val error: String? = null, val completed: Boolean = false,
)

/** No saved command state: recreation loads the identity, never repeats a conversion. */
@HiltViewModel
class SemanticConversionViewModel @Inject constructor(saved: SavedStateHandle, private val coordinator: CanonicalAspectConversionCoordinator) : ViewModel() {
    private val id = saved.get<String>(SemanticEntityEditorViewModel.SUBJECT_ID_ARGUMENT).orEmpty()
    private val mutable = MutableStateFlow(SemanticConversionState())
    val state: StateFlow<SemanticConversionState> = mutable.asStateFlow()
    init { viewModelScope.launch {
        try { mutable.value = mutable.value.copy(eligible = coordinator.canOffer(id)) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutable.value = mutable.value.copy(eligible = false) }
    } }

    fun open() {
        if (!state.value.eligible || state.value.busy) return
        mutable.value = state.value.copy(open = true, busy = true, kind = null, error = null)
        viewModelScope.launch {
            try { mutable.value = state.value.copy(plan = coordinator.preview(id), busy = false) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = state.value.copy(plan = null, busy = false, error = e.message) }
        }
    }
    fun select(kind: OrientationKind) {
        if (state.value.open && !state.value.busy && kind in SUBJECT_CONVERSION_TARGET_KINDS) mutable.value = state.value.copy(kind = kind)
    }
    fun cancel() { if (!state.value.busy) mutable.value = SemanticConversionState(eligible = state.value.eligible) }
    fun confirm() {
        val request = state.value
        val plan = request.plan ?: return
        val kind = request.kind ?: return
        if (!request.open || request.busy) return
        mutable.value = request.copy(busy = true, error = null)
        viewModelScope.launch {
            try { coordinator.convert(plan, kind); mutable.value = SemanticConversionState(completed = true) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.value = request.copy(error = e.message ?: "Не вдалося перетворити сутність") }
        }
    }
    fun consumeCompleted(): Boolean = state.value.completed.also { if (it) mutable.value = state.value.copy(completed = false) }
}
