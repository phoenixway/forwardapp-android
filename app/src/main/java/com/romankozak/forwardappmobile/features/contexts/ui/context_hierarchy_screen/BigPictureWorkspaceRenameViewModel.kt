package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BigPictureNode
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BigPictureWorkspaceRenameTarget(val ref: CanonicalEntityRef.Workspace, val title: String)

/** Target identity, not the optional operational open destination of a semantic occurrence. */
internal fun workspaceRenameTarget(node: BigPictureNode): BigPictureWorkspaceRenameTarget? {
    val entity = node.entity ?: return null
    val ref = entity.ref as? CanonicalEntityRef.Workspace ?: return null
    return BigPictureWorkspaceRenameTarget(ref, entity.title)
}

data class BigPictureWorkspaceRenameState(
    val target: BigPictureWorkspaceRenameTarget? = null,
    val name: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class BigPictureWorkspaceRenameViewModel @Inject constructor(private val repository: CanonicalWorkspaceRepository) : ViewModel() {
    private val mutable = MutableStateFlow(BigPictureWorkspaceRenameState())
    val state = mutable.asStateFlow()

    fun request(target: BigPictureWorkspaceRenameTarget) {
        if (!state.value.busy) mutable.value = BigPictureWorkspaceRenameState(target, target.title)
    }
    fun edit(name: String) { if (!state.value.busy) mutable.value = state.value.copy(name = name, error = null) }
    fun cancel() { if (!state.value.busy) mutable.value = BigPictureWorkspaceRenameState() }
    fun confirm() {
        val request = state.value
        val target = request.target ?: return
        if (request.busy || request.name.isBlank()) return
        val title = request.name.trim()
        if (title == target.title) { cancel(); return }
        mutable.value = request.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                repository.rename(target.ref.id, title)
                mutable.value = BigPictureWorkspaceRenameState()
            } catch (cancelled: CancellationException) {
                mutable.value = request
                throw cancelled
            } catch (failure: Exception) {
                mutable.value = request.copy(error = failure.message ?: "Не вдалося перейменувати Workspace")
            }
        }
    }
}
