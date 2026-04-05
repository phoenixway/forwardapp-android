package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.DirectionRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.ClipboardEntityRef
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.ClipboardOperation
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.EntityClipboardPayload
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.EntityClipboardService
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
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
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<String> = emptySet(),
)

@HiltViewModel
class ChecklistViewModel
    @Inject
    constructor(
        private val checklistRepository: ChecklistRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val contextRepository: ContextRepository,
        private val goalRepository: GoalRepository,
        private val inboxRepository: InboxRepository,
        private val listItemRepository: ListItemRepository,
        private val directionRepository: DirectionRepository,
        private val dayManagementRepository: DayManagementRepository,
        private val missionRepository: MissionRepository,
        private val recentItemsRepository: RecentItemsRepository,
        private val entityClipboardService: EntityClipboardService,
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
                (
                    docs.map { doc -> "doc:${doc.id}|${doc.name.ifBlank { "Untitled" }}" } +
                        musicNotes.map { note -> "music:${note.id}|${note.name.ifBlank { "Untitled" }}" } +
                        checklists.map { checklist -> "checklist:${checklist.id}|${checklist.name.ifBlank { "Untitled" }}" } +
                        contexts.map { ctx -> "ctx:${ctx.id}|${ctx.name.ifBlank { "Untitled" }}" }
                )
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
                        val uiItems =
                            items.map { entity ->
                                ChecklistItemUiModel(
                                    id = entity.id,
                                    content = entity.content,
                                    isChecked = entity.isChecked,
                                    order = entity.itemOrder,
                                )
                            }
                        val validIds = uiItems.map { it.id }.toSet()
                        val normalizedSelected = state.selectedItemIds.filterTo(mutableSetOf()) { it in validIds }
                        state.copy(
                            isLoading = false,
                            checklistId = checklistId,
                            title = checklist?.name ?: state.title,
                            items = uiItems,
                            errorMessage = if (checklist == null) state.errorMessage else null,
                            selectedItemIds = normalizedSelected,
                            isSelectionMode = state.isSelectionMode && normalizedSelected.isNotEmpty(),
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

        fun setSelectionMode(enabled: Boolean) {
            _uiState.update {
                it.copy(
                    isSelectionMode = enabled,
                    selectedItemIds = if (enabled) it.selectedItemIds else emptySet(),
                )
            }
        }

        fun toggleSelectionMode() = setSelectionMode(!_uiState.value.isSelectionMode)

        fun onToggleItemSelected(itemId: String) {
            _uiState.update { state ->
                val updated =
                    state.selectedItemIds.toMutableSet().apply {
                        if (!add(itemId)) remove(itemId)
                    }
                state.copy(
                    isSelectionMode = updated.isNotEmpty() || state.isSelectionMode,
                    selectedItemIds = updated,
                )
            }
        }

        fun onItemLongPressed(itemId: String) {
            _uiState.update { state ->
                state.copy(
                    isSelectionMode = true,
                    selectedItemIds = state.selectedItemIds + itemId,
                )
            }
        }

        fun onClearSelection() {
            _uiState.update { it.copy(isSelectionMode = false, selectedItemIds = emptySet()) }
        }

        fun onSelectAllForSelectionMode() {
            _uiState.update { state ->
                state.copy(
                    isSelectionMode = true,
                    selectedItemIds = state.items.map { it.id }.toSet(),
                )
            }
        }

        fun copySelectedToEntityClipboard(): Int {
            val selectedIds = selectedIdsInUiOrder()
            if (selectedIds.isEmpty()) return 0
            entityClipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = currentChecklist.value?.contextId.orEmpty(),
                    operation = ClipboardOperation.COPY,
                    entities = selectedIds.map { ClipboardEntityRef.ChecklistItem(checklistItemId = it) },
                ),
            )
            return selectedIds.size
        }

        fun cutSelectedToEntityClipboard(): Int {
            val selectedIds = selectedIdsInUiOrder()
            if (selectedIds.isEmpty()) return 0
            entityClipboardService.set(
                EntityClipboardPayload(
                    sourceContextId = currentChecklist.value?.contextId.orEmpty(),
                    operation = ClipboardOperation.CUT,
                    entities = selectedIds.map { ClipboardEntityRef.ChecklistItem(checklistItemId = it) },
                ),
            )
            return selectedIds.size
        }

        fun canPasteChecklistItemsFromEntityClipboard(): Boolean {
            val payload = entityClipboardService.payload.value ?: return false
            return payload.entities.any {
                it is ClipboardEntityRef.ChecklistItem ||
                    it is ClipboardEntityRef.BacklogGoal ||
                    it is ClipboardEntityRef.BacklogItem ||
                    it is ClipboardEntityRef.DirectionItem ||
                    it is ClipboardEntityRef.BacklogContextLink ||
                    it is ClipboardEntityRef.DayTask ||
                    it is ClipboardEntityRef.TacticalMission
            }
        }

        fun pasteChecklistItemsFromEntityClipboard(onResult: (String) -> Unit) {
            val checklistId = checklistIdState.value
            if (checklistId.isNullOrBlank()) {
                onResult("Чекліст не відкрито")
                return
            }
            val payload = entityClipboardService.payload.value
            if (payload == null) {
                onResult("Буфер порожній")
                return
            }
            if (!canPasteChecklistItemsFromEntityClipboard()) {
                onResult("У буфері немає підтримуваних елементів")
                return
            }
            viewModelScope.launch {
                val resolved = resolveChecklistPastePayload(payload, checklistId)
                if (resolved.itemsToInsert.isEmpty() && resolved.sameChecklistItemsToMove.isEmpty()) {
                    onResult("Немає валідних елементів для вставки")
                    return@launch
                }

                val now = System.currentTimeMillis()
                val maxOrder = _uiState.value.items.maxOfOrNull { it.order } ?: -1L
                var nextOrder = maxOrder + 1L

                when (payload.operation) {
                    ClipboardOperation.COPY -> {
                        val newItems =
                            resolved.itemsToInsert.map { source ->
                                ChecklistItemEntity(
                                    checklistId = checklistId,
                                    content = source.content,
                                    isChecked = false,
                                    itemOrder = nextOrder++,
                                    updatedAt = now,
                                    syncedAt = null,
                                    version = 1,
                                )
                            }
                        checklistRepository.addItems(newItems)
                        onResult("Скопійовано елементів: ${newItems.size}")
                    }

                    ClipboardOperation.CUT -> {
                        var movedCount = 0

                        if (resolved.sameChecklistItemsToMove.isNotEmpty()) {
                            val movedItems =
                                resolved.sameChecklistItemsToMove.map { item ->
                                    item.copy(itemOrder = nextOrder++)
                                }
                            checklistRepository.updateItems(movedItems)
                            movedCount += movedItems.size
                        }

                        if (resolved.itemsToInsert.isNotEmpty()) {
                            val newItems =
                                resolved.itemsToInsert.map { source ->
                                    ChecklistItemEntity(
                                        checklistId = checklistId,
                                        content = source.content,
                                        isChecked = false,
                                        itemOrder = nextOrder++,
                                        updatedAt = now,
                                        syncedAt = null,
                                        version = 1,
                                    )
                                }
                            checklistRepository.addItems(newItems)
                            movedCount += newItems.size
                        }

                        if (resolved.checklistItemIdsToDelete.isNotEmpty()) {
                            checklistRepository.deleteItems(resolved.checklistItemIdsToDelete)
                        }
                        if (resolved.backlogItemIdsToDelete.isNotEmpty()) {
                            listItemRepository.deleteListItems(resolved.backlogItemIdsToDelete)
                        }
                        if (resolved.directionItemIdsToDelete.isNotEmpty()) {
                            directionRepository.deleteDirectionItems(resolved.directionItemIdsToDelete)
                        }
                        if (resolved.dayTaskIdsToDelete.isNotEmpty()) {
                            resolved.dayTaskIdsToDelete.forEach { taskId ->
                                dayManagementRepository.deleteTask(taskId)
                            }
                        }
                        if (resolved.tacticalMissionIdsToDelete.isNotEmpty()) {
                            resolved.tacticalMissionIdsToDelete.forEach { missionId ->
                                missionRepository.deleteMissionById(missionId)
                            }
                        }
                        if (resolved.inboxRecordIdsToDelete.isNotEmpty()) {
                            inboxRepository.deleteInboxRecordsByIds(resolved.inboxRecordIdsToDelete)
                        }
                        entityClipboardService.clear()
                        onResult("Переміщено елементів: $movedCount")
                    }
                }
            }
        }

        private data class ChecklistPasteSourceItem(
            val content: String,
            val checklistItemIdToDelete: String? = null,
            val backlogItemIdToDelete: String? = null,
            val directionItemIdToDelete: String? = null,
            val dayTaskIdToDelete: String? = null,
            val tacticalMissionIdToDelete: Long? = null,
            val inboxRecordIdToDelete: String? = null,
        )

        private data class ResolvedChecklistPastePayload(
            val itemsToInsert: List<ChecklistPasteSourceItem>,
            val sameChecklistItemsToMove: List<ChecklistItemEntity>,
            val checklistItemIdsToDelete: List<String>,
            val backlogItemIdsToDelete: List<String>,
            val directionItemIdsToDelete: List<String>,
            val dayTaskIdsToDelete: List<String>,
            val tacticalMissionIdsToDelete: List<Long>,
            val inboxRecordIdsToDelete: List<String>,
        )

        private suspend fun resolveChecklistPastePayload(
            payload: EntityClipboardPayload,
            targetChecklistId: String,
        ): ResolvedChecklistPastePayload {
            val checklistIds = payload.entities.filterIsInstance<ClipboardEntityRef.ChecklistItem>().map { it.checklistItemId }
            val backlogGoalIds = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogGoal>().map { it.goalId }
            val backlogItemIds = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogItem>().map { it.listItemId }
            val directionIds = payload.entities.filterIsInstance<ClipboardEntityRef.DirectionItem>().map { it.directionItemId }
            val contextIds = payload.entities.filterIsInstance<ClipboardEntityRef.BacklogContextLink>().map { it.contextId }
            val dayTaskIds = payload.entities.filterIsInstance<ClipboardEntityRef.DayTask>().map { it.taskId }
            val tacticalMissionIds = payload.entities.filterIsInstance<ClipboardEntityRef.TacticalMission>().map { it.missionId }

            val checklistById = checklistRepository.getItemsByIds(checklistIds).associateBy { it.id }
            val backlogItemsById = listItemRepository.getItemsByIds(backlogItemIds).associateBy { it.id }
            val directionById = directionRepository.getDirectionItemsByIds(directionIds).associateBy { it.id }
            val dayTasksById = dayTaskIds.associateWith { id -> contextRepository; null }

            val goalsFromRefs = backlogGoalIds.associateWith { id -> goalRepository.getGoalById(id) }
            val goalIdsFromBacklog = backlogItemsById.values.filter { it.itemType == BacklogItemTypeValues.GOAL }.map { it.entityId }.distinct()
            val goalsFromBacklog = goalIdsFromBacklog.associateWith { id -> goalRepository.getGoalById(id) }
            val contextNames = contextIds.distinct().associateWith { id -> contextRepository.getContextById(id)?.name?.trim().orEmpty() }
            val contextNamesFromBacklog =
                backlogItemsById.values
                    .filter { it.itemType == BacklogItemTypeValues.SUBLIST }
                    .map { it.entityId }
                    .distinct()
                    .associateWith { id -> contextRepository.getContextById(id)?.name?.trim().orEmpty() }

            val sameChecklistItemsToMove = mutableListOf<ChecklistItemEntity>()
            val itemsToInsert = mutableListOf<ChecklistPasteSourceItem>()

            payload.entities.forEach { ref ->
                when (ref) {
                    is ClipboardEntityRef.ChecklistItem -> {
                        val item = checklistById[ref.checklistItemId] ?: return@forEach
                        if (payload.operation == ClipboardOperation.CUT && item.checklistId == targetChecklistId) {
                            sameChecklistItemsToMove += item
                        } else {
                            itemsToInsert +=
                                ChecklistPasteSourceItem(
                                    content = item.content,
                                    checklistItemIdToDelete = if (payload.operation == ClipboardOperation.CUT) item.id else null,
                                )
                        }
                    }

                    is ClipboardEntityRef.BacklogGoal -> {
                        val goalText = goalsFromRefs[ref.goalId]?.text?.trim().orEmpty()
                        if (goalText.isNotBlank()) {
                            itemsToInsert += ChecklistPasteSourceItem(content = goalText)
                        }
                    }

                    is ClipboardEntityRef.BacklogItem -> {
                        val item = backlogItemsById[ref.listItemId] ?: return@forEach
                        when (item.itemType) {
                            BacklogItemTypeValues.GOAL -> {
                                val goalText = goalsFromBacklog[item.entityId]?.text?.trim().orEmpty()
                                if (goalText.isNotBlank()) {
                                    itemsToInsert +=
                                        ChecklistPasteSourceItem(
                                            content = goalText,
                                            backlogItemIdToDelete = if (payload.operation == ClipboardOperation.CUT) item.id else null,
                                        )
                                }
                            }

                            BacklogItemTypeValues.SUBLIST -> {
                                val name = contextNamesFromBacklog[item.entityId].orEmpty().ifBlank { "Контекст" }
                                itemsToInsert +=
                                    ChecklistPasteSourceItem(
                                        content = name,
                                        backlogItemIdToDelete = if (payload.operation == ClipboardOperation.CUT) item.id else null,
                                    )
                            }

                            else -> Unit
                        }
                    }

                    is ClipboardEntityRef.DirectionItem -> {
                        val directionItem = directionById[ref.directionItemId] ?: return@forEach
                        val text = directionItem.text.trim()
                        if (text.isNotBlank()) {
                            itemsToInsert +=
                                ChecklistPasteSourceItem(
                                    content = text,
                                    directionItemIdToDelete = if (payload.operation == ClipboardOperation.CUT) directionItem.id else null,
                                )
                        }
                    }

                    is ClipboardEntityRef.BacklogContextLink -> {
                        val name = contextNames[ref.contextId].orEmpty().ifBlank { "Контекст" }
                        itemsToInsert += ChecklistPasteSourceItem(content = name)
                    }

                    is ClipboardEntityRef.DayTask -> {
                        val task = dayManagementRepository.getTaskById(ref.taskId) ?: return@forEach
                        val text = task.title.trim().ifBlank { task.description?.trim().orEmpty() }
                        if (text.isNotBlank()) {
                            itemsToInsert +=
                                ChecklistPasteSourceItem(
                                    content = text,
                                    dayTaskIdToDelete = if (payload.operation == ClipboardOperation.CUT) task.id else null,
                                )
                        }
                    }

                    is ClipboardEntityRef.TacticalMission -> {
                        val mission = missionRepository.getMissionById(ref.missionId) ?: return@forEach
                        val text = mission.title.trim().ifBlank { mission.description?.trim().orEmpty() }
                        if (text.isNotBlank()) {
                            itemsToInsert +=
                                ChecklistPasteSourceItem(
                                    content = text,
                                    tacticalMissionIdToDelete = if (payload.operation == ClipboardOperation.CUT) mission.id else null,
                                )
                        }
                    }

                    is ClipboardEntityRef.InboxRecord -> {
                        val record = inboxRepository.getInboxRecordById(ref.recordId) ?: return@forEach
                        val text = record.text.trim()
                        if (text.isNotBlank()) {
                            itemsToInsert +=
                                ChecklistPasteSourceItem(
                                    content = text,
                                    inboxRecordIdToDelete = if (payload.operation == ClipboardOperation.CUT) record.id else null,
                                )
                        }
                    }

                    is ClipboardEntityRef.BacklogAttachment -> Unit
                }
            }

            return ResolvedChecklistPastePayload(
                itemsToInsert = itemsToInsert,
                sameChecklistItemsToMove = sameChecklistItemsToMove,
                checklistItemIdsToDelete = itemsToInsert.mapNotNull { it.checklistItemIdToDelete }.distinct(),
                backlogItemIdsToDelete = itemsToInsert.mapNotNull { it.backlogItemIdToDelete }.distinct(),
                directionItemIdsToDelete = itemsToInsert.mapNotNull { it.directionItemIdToDelete }.distinct(),
                dayTaskIdsToDelete = itemsToInsert.mapNotNull { it.dayTaskIdToDelete }.distinct(),
                tacticalMissionIdsToDelete = itemsToInsert.mapNotNull { it.tacticalMissionIdToDelete }.distinct(),
                inboxRecordIdsToDelete = itemsToInsert.mapNotNull { it.inboxRecordIdToDelete }.distinct(),
            )
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

        fun onRequestItemFocus(itemId: String) {
            _uiState.update { it.copy(pendingFocusItemId = itemId) }
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

        private fun selectedIdsInUiOrder(): List<String> {
            val selected = _uiState.value.selectedItemIds
            if (selected.isEmpty()) return emptyList()
            return _uiState.value.items.map { it.id }.filter { it in selected }
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
