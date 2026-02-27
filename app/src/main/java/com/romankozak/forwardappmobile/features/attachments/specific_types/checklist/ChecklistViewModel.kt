package com.romankozak.forwardappmobile.features.attachments.specific_types.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
class ChecklistViewModel
    @Inject
    constructor(
        private val checklistRepository: ChecklistRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val contextRepository: ContextRepository,
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

        val linkSuggestions: StateFlow<List<String>> =
            combine(
                noteDocumentRepository.getAllDocumentsAsFlow(),
                musicNoteRepository.getAllMusicNotesAsFlow(),
                checklistRepository.getAllChecklistsAsFlow(),
                contextRepository.getAllContextsFlow(),
            ) { docs, musicNotes, checklists, contexts ->
                (docs.map { doc -> "doc:${doc.id}|${doc.name.ifBlank { "Untitled" }}" } +
                    musicNotes.map { note -> "music:${note.id}|${note.name.ifBlank { "Untitled" }}" } +
                    checklists.map { checklist -> "checklist:${checklist.id}|${checklist.name.ifBlank { "Untitled" }}" } +
                    contexts.map { ctx -> "ctx:${ctx.id}|${ctx.name.ifBlank { "Untitled" }}" })
                    .filter { token -> token.substringAfter('|', "").isNotBlank() }
                    .distinct()
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        val contextSuggestions: StateFlow<List<String>> =
            contextRepository.getAllContextsFlow()
                .map { contexts ->
                    contexts.map { it.name }.filter { it.isNotBlank() }.distinct()
                }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
                    val activeItems = items.filterNot { it.isDeleted }
                    checklist to activeItems.sortedBy { it.itemOrder }
                }.collect { (checklist, items) ->
                    if (checklist != null && !hasLoggedAccess) {
                        recentItemsRepository.logChecklistAccess(checklist)
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

        fun onItemContentChange(
            itemId: String,
            newContent: String,
        ) {
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

        fun onToggleItemChecked(
            itemId: String,
            isChecked: Boolean,
        ) {
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

            val existingBlankItem =
                currentItems.firstOrNull { it.content.isBlank() }
            if (existingBlankItem != null) {
                _uiState.update { it.copy(pendingFocusItemId = existingBlankItem.id) }
                return
            }

            val insertIndex =
                when {
                    afterItemId == null -> currentItems.size
                    else ->
                        currentItems.indexOfFirst { it.id == afterItemId }.let {
                                index ->
                            if (index == -1) currentItems.size else index + 1
                        }
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

        fun onMoveItem(
            fromIndex: Int,
            toIndex: Int,
        ) {
            if (fromIndex == toIndex) return

            val reordered =
                _uiState.value.items.toMutableList().apply {
                    val moved = removeAt(fromIndex)
                    add(toIndex, moved)
                }

            _uiState.update { it.copy(items = reordered) }
            normalizeOrder(reordered)
        }

        fun onSelectAllItems() {
            markAll(isChecked = true)
        }

        fun onMarkAllCompleted() {
            markAll(isChecked = true)
        }

        fun onMarkAllIncomplete() {
            markAll(isChecked = false)
        }

        private fun markAll(isChecked: Boolean) {
            val updatedEntities = itemsById.value.values.map { it.copy(isChecked = isChecked) }
            itemsById.value = updatedEntities.associateBy { it.id }
            _uiState.update { state ->
                state.copy(items = state.items.map { it.copy(isChecked = isChecked) })
            }
            viewModelScope.launch {
                checklistRepository.updateItems(updatedEntities)
            }
        }

        fun onDeleteItem(itemId: String) {
            val itemToDelete = itemsById.value[itemId] ?: return

            // If another delete is pending, we can't undo it anymore.
            if (_uiState.value.showUndoSnackbar) {
                _uiState.update { it.copy(lastDeletedItem = null, showUndoSnackbar = false) }
            }

            itemsById.value = itemsById.value - itemId
            _uiState.update { state ->
                state.copy(
                    items = state.items.filterNot { it.id == itemId },
                    lastDeletedItem = itemToDelete,
                    showUndoSnackbar = true,
                )
            }

            viewModelScope.launch {
                checklistRepository.deleteItem(itemId)
            }
        }

        fun onUndoDelete() {
            val itemToRestore = _uiState.value.lastDeletedItem ?: return

            val restoredUiItem =
                ChecklistItemUiModel(
                    id = itemToRestore.id,
                    content = itemToRestore.content,
                    isChecked = itemToRestore.isChecked,
                    order = itemToRestore.itemOrder,
                )

            itemsById.value = itemsById.value + (itemToRestore.id to itemToRestore)
            _uiState.update { state ->
                val updatedItems =
                    (state.items + restoredUiItem)
                        .sortedBy { it.order }
                state.copy(
                    items = updatedItems,
                    lastDeletedItem = null,
                    showUndoSnackbar = false,
                )
            }

            viewModelScope.launch {
                checklistRepository.addItems(listOf(itemToRestore))
            }
        }

        fun onConfirmDelete() {
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

        suspend fun findDocumentIdByName(name: String): String? = noteDocumentRepository.findDocumentByName(name)?.id

        fun insertLinkIntoItem(
            itemId: String,
            linkToken: String,
        ) {
            val entity = itemsById.value[itemId] ?: return
            val insertion = "[[$linkToken]]"
            val separator = if (entity.content.isBlank() || entity.content.endsWith(" ")) "" else " "
            val newContent = entity.content + separator + insertion
            onItemContentChange(itemId, newContent)
        }

        private fun normalizeOrder(items: List<ChecklistItemUiModel>) {
            viewModelScope.launch {
                val updatedEntities =
                    items.mapIndexedNotNull { index, item ->
                        itemsById.value[item.id]?.copy(itemOrder = index.toLong())
                    }

                if (updatedEntities.isNotEmpty()) {
                    checklistRepository.updateItems(updatedEntities)
                    itemsById.value =
                        itemsById.value.toMutableMap().apply {
                            updatedEntities.forEach { put(it.id, it) }
                        }
                }
            }
        }

        fun buildMarkdownExport(): String {
            val state = _uiState.value
            val titleLine = state.title.takeIf { it.isNotBlank() } ?: DEFAULT_CHECKLIST_NAME
            val builder = StringBuilder()
            builder.append("# ").append(titleLine).append("\n\n")
            if (state.items.isEmpty()) {
                builder.append("_(empty checklist)_\n")
            } else {
                state.items.forEach { item ->
                    val checkbox = if (item.isChecked) "[x]" else "[ ]"
                    val content = item.content.ifBlank { "(blank)" }
                    builder.append("- ").append(checkbox).append(" ").append(content).append("\n")
                }
            }
            return builder.toString()
        }

        fun importMarkdown(
            markdown: String,
            onResult: (Boolean) -> Unit = {},
        ) {
            val checklistId =
                checklistIdState.value ?: run {
                    onResult(false)
                    return
                }
            val parsedItems = parseMarkdown(markdown, checklistId)
            if (parsedItems.isEmpty()) {
                onResult(false)
                return
            }
            viewModelScope.launch {
                checklistRepository.deleteItemsByChecklist(checklistId)
                checklistRepository.addItems(parsedItems)
                onResult(true)
            }
        }

        private fun parseMarkdown(
            markdown: String,
            checklistId: String,
        ): List<ChecklistItemEntity> {
            val checkboxRegex = Regex("""^\s*[-*+] \[(x|X| )]\s*(.+)$""")
            val bulletRegex = Regex("""^\s*(?:[-*+]|[0-9]+\.)\s*(.+)$""")
            val result = mutableListOf<ChecklistItemEntity>()
            var order = 0L
            markdown.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach

                var isChecked = false
                var content: String? = null

                val checkboxMatch = checkboxRegex.find(line)
                if (checkboxMatch != null) {
                    isChecked = checkboxMatch.groupValues[1].equals("x", ignoreCase = true)
                    content = checkboxMatch.groupValues[2].trim()
                } else {
                    val bulletMatch = bulletRegex.find(line)
                    if (bulletMatch != null) {
                        content = bulletMatch.groupValues[1].trim()
                    }
                }

                if (!content.isNullOrBlank()) {
                    result +=
                        ChecklistItemEntity(
                            checklistId = checklistId,
                            content = content,
                            isChecked = isChecked,
                            itemOrder = order++,
                        )
                }
            }
            return result
        }
    }
