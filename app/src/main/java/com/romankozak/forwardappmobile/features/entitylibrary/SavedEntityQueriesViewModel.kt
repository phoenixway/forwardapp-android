package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQuery
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQueries
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQueryRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedQueryDraft(val id: String?, val name: String, val query: CanonicalEntityQuery?)
data class SavedQueryEditing(val draft: SavedQueryDraft? = null, val busy: Boolean = false, val error: String? = null)

@HiltViewModel
class SavedEntityQueriesViewModel @Inject constructor(private val repository: SavedEntityQueryRepository) : ViewModel() {
    val queries = repository.observe().catch { emit(SavedEntityQueries(issue = "Не вдалося прочитати збережені запити")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedEntityQueries())
    private val mutableEditing = MutableStateFlow(SavedQueryEditing())
    val editing = mutableEditing.asStateFlow()

    fun beginSave(query: CanonicalEntityQuery) {
        if (!editing.value.busy) mutableEditing.value = SavedQueryEditing(SavedQueryDraft(null, "", query))
    }
    fun beginRename(item: SavedEntityQuery) {
        if (!editing.value.busy) mutableEditing.value = SavedQueryEditing(SavedQueryDraft(item.id, item.name, null))
    }
    fun changeName(name: String) {
        val state = editing.value
        if (!state.busy) mutableEditing.value = state.copy(draft = state.draft?.copy(name = name), error = null)
    }
    fun cancel() {
        if (!editing.value.busy) mutableEditing.value = SavedQueryEditing()
    }
    fun submit() {
        val state = editing.value
        val draft = state.draft ?: return
        if (state.busy) return
        mutate {
            if (draft.id == null) repository.save(draft.name, requireNotNull(draft.query))
            else repository.rename(draft.id, draft.name)
        }
    }
    fun delete(id: String) {
        if (!editing.value.busy) mutate { repository.delete(id) }
    }
    private fun mutate(command: suspend () -> Unit) {
        mutableEditing.value = editing.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                command()
                mutableEditing.value = SavedQueryEditing()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableEditing.value = editing.value.copy(busy = false, error = error.message ?: "Не вдалося зберегти запит")
            }
        }
    }
}
