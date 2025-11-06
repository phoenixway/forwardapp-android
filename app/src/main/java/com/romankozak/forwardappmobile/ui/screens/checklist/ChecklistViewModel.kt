package com.romankozak.forwardappmobile.ui.screens.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChecklistItemUiModel(
    val id: String,
    val content: String,
    val isChecked: Boolean,
    val order: Long,
)

data class ChecklistUiState(
    val isLoading: Boolean = true,
    val checklistId: String? = null,
    val title: String = "",
    val items: List<ChecklistItemUiModel> = emptyList(),
    val showCheckboxes: Boolean = true,
    val pendingFocusItemId: String? = null,
    val errorMessage: String? = null,
    val showUndoSnackbar: Boolean = false,
    val lastDeletedItem: ChecklistItemEntity? = null,
)

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val recentItemsRepository: RecentItemsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    companion object {
        private const val DEFAULT_CHECKLIST_NAME = "Новий чекліст"
    }

    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState.asStateFlow()

    private val itemsById = MutableStateFlow<Map<String, ChecklistItemEntity>>(emptyMap())
    private val currentChecklist = MutableStateFlow<ChecklistEntity?>(null)
    private val checklistIdState = MutableStateFlow(savedStateHandle.get<String>("checklistId"))
    private val projectId: String? = savedStateHandle.get<String>("projectId")

    private var hasLoggedAccess = false

    init {
        viewModelScope.launch {
            val resolvedId =
                when (val existingId = checklistIdState.value) {
                    null, "" -> {
                        val project = projectId
                        if (project.isNullOrBlank()) {
                            _uiState.update { it.copy(isLoading = false, errorMessage = "Не вдалося відкрити чекліст") }
                            return@launch
                        }
                        val createdId = checklistRepository.createChecklist(DEFAULT_CHECKLIST_NAME, project)
                        val firstItemId = checklistRepository.addItem(createdId, order = 0)
                        checklistIdState.value = createdId
                        savedStateHandle["checklistId"] = createdId
                        _uiState.update {
                            it.copy(
                                title = DEFAULT_CHECKLIST_NAME,
                                pendingFocusItemId = firstItemId,
                            )
                        }
                        createdId
                    }
                    else -> existingId
                }

            startObservingChecklist(resolvedId)
        }
    }

    private fun startObservingChecklist(checklistId: String) {
        viewModelScope.launch {
            combine(
                checklistRepository.observeChecklistById(checklistId),
                checklistRepository.getItemsForChecklist(checklistId),
            ) { checklist, items ->
                checklist to items.sortedBy { it.itemOrder }
            }.collect { (checklist, items) ->
                if (checklist != null && !hasLoggedAccess) {
                    recentItemsRepository.logChecklistAccess(checklist.id, checklist.name)
                    hasLoggedAccess = true
                }

                currentChecklist.value = checklist
                itemsById.value = items.associateBy { it.id }

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        checklistId = checklistId,
                        title = checklist?.name ?: state.title,
                        items =
                            items.map { entity ->
                                ChecklistItemUiModel(
                                    id = entity.id,
                                    content = entity.content,
                                    isChecked = entity.isChecked,
                                    order = entity.itemOrder,
                                )
                            },
                        errorMessage = if (checklist == null) state.errorMessage else null,
                    )
                }
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
        val checklist = currentChecklist.value ?: return
        if (checklist.name == newTitle) return

        viewModelScope.launch {
            checklistRepository.updateChecklist(checklist.copy(name = newTitle))
        }
    }

    fun onToggleCheckboxVisibility() {
        _uiState.update { it.copy(showCheckboxes = !it.showCheckboxes) }
    }

    fun onItemContentChange(itemId: String, newContent: String) {
        _uiState.update { state ->
            state.copy(
                items =
                    state.items.map { item ->
                        if (item.id == itemId) item.copy(content = newContent) else item
                    },
            )
        }

        val entity = itemsById.value[itemId] ?: return
        if (entity.content == newContent) return

        val updatedEntity = entity.copy(content = newContent)
        itemsById.value = itemsById.value + (itemId to updatedEntity)
        viewModelScope.launch {
            checklistRepository.updateItem(updatedEntity)
        }
    }

    fun onToggleItemChecked(itemId: String, isChecked: Boolean) {
        _uiState.update { state ->
            state.copy(
                items =
                    state.items.map { item ->
                        if (item.id == itemId) item.copy(isChecked = isChecked) else item
                    },
            )
        }

        val entity = itemsById.value[itemId] ?: return
        if (entity.isChecked == isChecked) return

        val updatedEntity = entity.copy(isChecked = isChecked)
        itemsById.value = itemsById.value + (itemId to updatedEntity)
        viewModelScope.launch {
            checklistRepository.updateItem(updatedEntity)
        }
    }

    fun onAddItem(afterItemId: String?) {
        val checklistId = checklistIdState.value ?: return
        val currentItems = _uiState.value.items
        val insertIndex =
            when {
                afterItemId == null -> currentItems.size
                else -> currentItems.indexOfFirst { it.id == afterItemId }.let { index -> if (index == -1) currentItems.size else index + 1 }
            }

        val itemsToShift =
            if (currentItems.isEmpty() || insertIndex >= currentItems.size) {
                emptyList()
            } else {
                currentItems
                    .subList(insertIndex, currentItems.size)
                    .mapNotNull { uiItem ->
                        itemsById.value[uiItem.id]?.copy(itemOrder = uiItem.order + 1)
                    }
            }

        viewModelScope.launch {
            if (itemsToShift.isNotEmpty()) {
                checklistRepository.updateItems(itemsToShift)
            }
            val newItemOrder = insertIndex.toLong()
            val newItemId = checklistRepository.addItem(checklistId, order = newItemOrder)
            _uiState.update { it.copy(pendingFocusItemId = newItemId) }
        }
    }

    fun onMoveItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return

        val reordered =
            _uiState.value.items.toMutableList().apply {
                val moved = removeAt(fromIndex)
                add(toIndex, moved)
            }

        _uiState.update { it.copy(items = reordered) }
        normalizeOrder(reordered)
    }

    fun onDeleteItem(itemId: String) {
        val itemToDelete = itemsById.value[itemId] ?: return
        _uiState.update { it.copy(lastDeletedItem = itemToDelete, showUndoSnackbar = true) }
    }

    fun onUndoDelete() {
        _uiState.update { it.copy(lastDeletedItem = null, showUndoSnackbar = false) }
    }

    fun onConfirmDelete() {
        val itemToDelete = _uiState.value.lastDeletedItem ?: return
        viewModelScope.launch {
            checklistRepository.deleteItem(itemToDelete.id)
        }
        _uiState.update { it.copy(lastDeletedItem = null, showUndoSnackbar = false) }
    }

    fun onClearCompleted() {
        val completedIds = itemsById.value.filterValues { it.isChecked }.keys
        if (completedIds.isEmpty()) return

        viewModelScope.launch {
            completedIds.forEach { id -> checklistRepository.deleteItem(id) }
        }
    }

    fun onPendingFocusConsumed() {
        _uiState.update { it.copy(pendingFocusItemId = null) }
    }

    private fun normalizeOrder(items: List<ChecklistItemUiModel>) {
        viewModelScope.launch {
            val updatedEntities =
                items.mapIndexedNotNull { index, item ->
                    itemsById.value[item.id]?.copy(itemOrder = index.toLong())
                }

            if (updatedEntities.isNotEmpty()) {
                checklistRepository.updateItems(updatedEntities)
                itemsById.value = itemsById.value.toMutableMap().apply {
                    updatedEntities.forEach { put(it.id, it) }
                }
            }
        }
    }
}
