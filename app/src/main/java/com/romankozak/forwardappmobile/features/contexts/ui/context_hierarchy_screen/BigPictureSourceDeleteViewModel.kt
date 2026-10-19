package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BigPictureSourceDeleteState(
    val pending: BigPictureSourceDeleteTarget? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class BigPictureSourceDeleteViewModel @Inject constructor(private val actions: BigPictureTargetActions) : ViewModel() {
    // Confirmation is transient presentation state, never a SavedState mutation intent.
    private val mutableState = MutableStateFlow(BigPictureSourceDeleteState())
    val state = mutableState.asStateFlow()
    val targets = actions.observeTargets().catch { emit(emptyMap()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap<CanonicalEntityRef, BigPictureSourceDeleteTarget>())

    fun request(target: BigPictureSourceDeleteTarget) {
        if (state.value.busy || targets.value[target.ref] != target) return
        mutableState.value = BigPictureSourceDeleteState(pending = target)
    }

    fun cancel() { if (!state.value.busy) mutableState.value = BigPictureSourceDeleteState() }
    fun clearMessage() { mutableState.value = state.value.copy(message = null) }

    fun confirm() {
        val target = state.value.pending ?: return
        if (state.value.busy) return
        mutableState.value = state.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                actions.deleteSource(target)
                mutableState.value = BigPictureSourceDeleteState(message = "Джерело видалено")
            } catch (cancelled: CancellationException) {
                mutableState.value = BigPictureSourceDeleteState()
                throw cancelled
            } catch (failure: Exception) {
                mutableState.value = BigPictureSourceDeleteState(pending = target,
                    error = "Не вдалося видалити джерело: ${failure.message}. Перевірте дочірні елементи, Planning та актуальність даних.")
            }
        }
    }
}
