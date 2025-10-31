package com.romankozak.forwardappmobile.ui.screens.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChecklistItemUi(
    val id: String?,
    val content: String,
    val isChecked: Boolean,
    val order: Long,
    val localId: String = id ?: UUID.randomUUID().toString(),
)

data class ChecklistUiState(
    val checklistId: String? = null,
    val projectId: String? = null,
    val title: String = "",
    val items: List<ChecklistItemUi> = emptyList(),
    val isNewChecklist: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed class ChecklistEvent {
    object NavigateBack : ChecklistEvent()
    data class ShowError(val message: String) : ChecklistEvent()
}

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChecklistEvent>()
    val events = _events.receiveAsFlow()

    private var originalItems: List<ChecklistItemEntity> = emptyList()
    private var originalChecklist: ChecklistEntity? = null

    init {
        val checklistId: String? = savedStateHandle["checklistId"] ?: savedStateHandle["listId"]
        val projectId: String? = savedStateHandle["projectId"]

        if (checklistId == null) {
            _uiState.value =
                ChecklistUiState(
                    checklistId = null,
                    projectId = projectId,
                    title = "",
                    items = emptyList(),
                    isNewChecklist = true,
                    isLoading = false,
                )
            onAddItem()
        } else {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                val checklist = checklistRepository.getChecklistById(checklistId)
                if (checklist == null) {
                    _events.send(ChecklistEvent.NavigateBack)
                    return@launch
                }
                originalChecklist = checklist
                originalItems = checklistRepository.getChecklistItems(checklistId).first()
                _uiState.value =
                    ChecklistUiState(
                        checklistId = checklist.id,
                        projectId = checklist.projectId,
                        title = checklist.name,
                        items = originalItems.mapToUi(),
                        isNewChecklist = false,
                        isLoading = false,
                    )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun onItemContentChange(index: Int, content: String) {
        _uiState.update {
            val updated =
                it.items.toMutableList().apply {
                    if (index in indices) {
                        this[index] = this[index].copy(content = content)
                    }
                }
            it.copy(items = updated)
        }
    }

    fun onItemToggle(index: Int, isChecked: Boolean) {
        _uiState.update {
            val updated =
                it.items.toMutableList().apply {
                    if (index in indices) {
                        this[index] = this[index].copy(isChecked = isChecked)
                    }
                }
            it.copy(items = updated)
        }
    }

    fun onAddItem(afterIndex: Int? = null) {
        _uiState.update {
            val newItem = ChecklistItemUi(id = null, content = "", isChecked = false, order = it.items.size.toLong())
            val updated =
                it.items.toMutableList().apply {
                    if (afterIndex == null || afterIndex !in indices) {
                        add(newItem)
                    } else {
                        add(afterIndex + 1, newItem)
                    }
                }
            it.copy(items = updated.reindexed())
        }
    }

    fun onRemoveItem(index: Int) {
        _uiState.update {
            val updated = it.items.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            it.copy(items = updated.reindexed())
        }
    }

    fun onMoveItem(from: Int, to: Int) {
        if (from == to) return
        _uiState.update {
            val mutable = it.items.toMutableList()
            val item = mutable.removeAt(from)
            val clampedTo = to.coerceIn(0, mutable.size)
            mutable.add(clampedTo, item)
            it.copy(items = mutable.reindexed())
        }
    }

    fun onSave() {
        val currentState = _uiState.value
        viewModelScope.launch {
            if (currentState.isSaving) return@launch
            if (currentState.projectId == null && currentState.isNewChecklist) {
                _events.send(ChecklistEvent.ShowError("Project ID is missing"))
                return@launch
            }
            _uiState.update { it.copy(isSaving = true) }
            try {
                val trimmedTitle = currentState.title.ifBlank { "Новий чекліст" }
                if (currentState.isNewChecklist) {
                    val projectId = currentState.projectId!!
                    val checklistId = checklistRepository.createChecklist(trimmedTitle, projectId)
                    val filteredItems = currentState.items.filter { it.content.isNotBlank() }
                    filteredItems.forEachIndexed { index, item ->
                        checklistRepository.addChecklistItem(
                            checklistId = checklistId,
                            content = item.content,
                            order = index.toLong(),
                        )
                    }
                } else {
                    val checklistId = currentState.checklistId ?: return@launch
                    val projectId = currentState.projectId ?: originalChecklist?.projectId ?: return@launch
                    checklistRepository.updateChecklist(
                        ChecklistEntity(
                            id = checklistId,
                            projectId = projectId,
                            name = trimmedTitle,
                        ),
                    )

                    val filteredItems = currentState.items.filter { it.content.isNotBlank() }
                    val filteredIds = filteredItems.mapNotNull { it.id }.toSet()
                    val originalIds = originalItems.map { it.id }.toSet()

                    // Delete removed items
                    originalIds.minus(filteredIds).forEach { checklistRepository.deleteChecklistItem(it) }

                    // Update existing
                    filteredItems.forEachIndexed { index, item ->
                        if (item.id != null) {
                            checklistRepository.updateChecklistItem(
                                ChecklistItemEntity(
                                    id = item.id,
                                    checklistId = checklistId,
                                    content = item.content,
                                    isChecked = item.isChecked,
                                    itemOrder = index.toLong(),
                                ),
                            )
                        } else {
                            checklistRepository.addChecklistItem(
                                checklistId = checklistId,
                                content = item.content,
                                order = index.toLong(),
                            )
                        }
                    }
                }
                _events.send(ChecklistEvent.NavigateBack)
            } catch (ex: Exception) {
                _events.send(ChecklistEvent.ShowError(ex.message ?: "Не вдалося зберегти чекліст"))
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun List<ChecklistItemUi>.reindexed(): List<ChecklistItemUi> =
        mapIndexed { index, item -> item.copy(order = index.toLong()) }

    private fun List<ChecklistItemEntity>.mapToUi(): List<ChecklistItemUi> =
        mapIndexed { index, entity ->
            ChecklistItemUi(
                id = entity.id,
                content = entity.content,
                isChecked = entity.isChecked,
                order = entity.itemOrder.takeIf { it >= 0 } ?: index.toLong(),
                localId = entity.id,
            )
        }

}
