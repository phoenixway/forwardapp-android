package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.toBigPictureTarget
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityRefCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EntityLibraryPlacementState(
    val pendingRef: CanonicalEntityRef? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

/** Library placement intent only. H1 owns all structural validation and writes. */
@HiltViewModel
class EntityLibraryPlacementViewModel @Inject constructor(
    private val writer: H1StructuralMutationCoordinator,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val restored = runCatching {
        savedState.get<String>(PENDING_KEY)?.let(CanonicalEntityRefCodec::decode)
    }
    private val mutableState = MutableStateFlow(EntityLibraryPlacementState(
        pendingRef = restored.getOrNull(),
        message = if (restored.isFailure) "Вибір сутності втрачено. Оберіть її знову." else null,
    ))
    val state = mutableState.asStateFlow()

    fun begin(ref: CanonicalEntityRef) {
        if (state.value.busy) return
        savedState[PENDING_KEY] = CanonicalEntityRefCodec.encode(ref)
        mutableState.value = EntityLibraryPlacementState(pendingRef = ref)
    }

    fun cancel() {
        if (state.value.busy) return
        savedState[PENDING_KEY] = null
        mutableState.value = EntityLibraryPlacementState()
    }

    fun clearMessage() { mutableState.value = state.value.copy(message = null) }

    fun place(destination: H1StructuralParent) {
        if (state.value.busy) return
        val ref = state.value.pendingRef ?: return
        mutableState.value = state.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                // Same atomic PRIMARY/LINK decision as Big Picture Add.
                writer.addAppearance(ref.toBigPictureTarget(), destination)
                savedState[PENDING_KEY] = null
                mutableState.value = EntityLibraryPlacementState(message = "Додано до Big Picture")
            } catch (cancelled: CancellationException) {
                mutableState.value = state.value.copy(busy = false)
                throw cancelled
            } catch (error: Exception) {
                // Keep the selected identity so the user can choose another destination or cancel.
                mutableState.value = state.value.copy(busy = false,
                    error = error.message ?: "Не вдалося додати до Big Picture")
            }
        }
    }

    private companion object { const val PENDING_KEY = "libraryBigPicturePendingRef" }
}
