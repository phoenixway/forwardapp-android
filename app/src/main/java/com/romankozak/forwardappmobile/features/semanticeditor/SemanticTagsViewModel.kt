package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.orientation.CanonicalSemanticTagRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SemanticTagEditing(val input: String = "", val busy: Boolean = false, val error: String? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SemanticTagsViewModel @Inject constructor(private val repository: CanonicalSemanticTagRepository) : ViewModel() {
    private val owner = MutableStateFlow<ManagedSubjectRef?>(null)
    private val mutableEditing = MutableStateFlow(SemanticTagEditing())
    val editing = mutableEditing.asStateFlow()
    val tags = owner.flatMapLatest { ref -> if (ref == null) flowOf(emptySet()) else repository.observeTags(ref) }
        .catch { mutableEditing.value = editing.value.copy(error = "Не вдалося прочитати теги"); emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun show(subject: ManagedSubjectRef) {
        if (owner.value != subject) { owner.value = subject; mutableEditing.value = SemanticTagEditing() }
    }
    fun setInput(value: String) {
        if (!editing.value.busy) mutableEditing.value = editing.value.copy(input = value, error = null)
    }
    fun add() = mutate(add = true, tag = editing.value.input)
    fun remove(tag: String) = mutate(add = false, tag = tag)
    private fun mutate(add: Boolean, tag: String) {
        val subject = owner.value ?: return
        if (editing.value.busy) return
        mutableEditing.value = editing.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                if (add) repository.addTag(subject, tag) else repository.removeTag(subject, tag)
                if (owner.value == subject) mutableEditing.value = editing.value.copy(busy = false, input = if (add) "" else editing.value.input)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                if (owner.value == subject) mutableEditing.value = editing.value.copy(busy = false, error = error.message ?: "Не вдалося змінити тег")
            }
        }
    }
}
