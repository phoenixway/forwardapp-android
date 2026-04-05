package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

interface InboxHandlerResultListener {
    fun requestNavigation(route: String)

    fun showSnackbar(
        message: String,
        action: String?,
    )

    fun scrollToListEnd()

    fun highlightInboxRecord(recordId: String)

    fun updateInputState(inputValue: TextFieldValue)
}

@OptIn(ExperimentalCoroutinesApi::class)
class InboxHandler(
    private val contextRepository: ContextRepository,
    private val inboxRepository: com.romankozak.forwardappmobile.data.repository.InboxRepository,
    private val backlogClipboardUseCase: BacklogClipboardUseCase,
    private val scope: CoroutineScope,
    private val projectIdFlow: StateFlow<String>,
    private val listener: InboxHandlerResultListener,
) {
    private val _inboxRecords = MutableStateFlow<List<InboxRecord>>(emptyList())
    val inboxRecords: StateFlow<List<InboxRecord>> = _inboxRecords.asStateFlow()

    private val _recordToEdit = MutableStateFlow<InboxRecord?>(null)
    val recordToEdit: StateFlow<InboxRecord?> = _recordToEdit.asStateFlow()

    private val _recordForPromotion = MutableStateFlow<InboxRecord?>(null)
    val recordForPromotion: StateFlow<InboxRecord?> = _recordForPromotion.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedRecordIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedRecordIds: StateFlow<Set<String>> = _selectedRecordIds.asStateFlow()

    init {
        scope.launch {
            projectIdFlow
                .filter { it.isNotEmpty() }
                .flatMapLatest { id -> inboxRepository.getInboxRecordsStream(id) }
                .collect { records ->
                    _inboxRecords.value = records.sortedBy { it.createdAt }
                    val validIds = _inboxRecords.value.map { it.id }.toSet()
                    val normalizedSelection = _selectedRecordIds.value.filterTo(mutableSetOf()) { it in validIds }
                    _selectedRecordIds.value = normalizedSelection
                    _isSelectionMode.value = _isSelectionMode.value && normalizedSelection.isNotEmpty()
                }
        }
    }

    fun addQuickRecord(text: String) {
        listener.updateInputState(TextFieldValue(""))
        scope.launch {
            val projectId = projectIdFlow.value
            if (projectId.isNotEmpty() && text.isNotBlank()) {
                val recordId =
                    withContext(Dispatchers.IO) {
                        inboxRepository.addInboxRecord(text, projectId)
                    }
                listener.highlightInboxRecord(recordId)
            }
        }
    }

    fun deleteInboxRecord(recordId: String) {
        scope.launch(Dispatchers.IO) {
            inboxRepository.deleteInboxRecordById(recordId)
        }
    }

    fun onSelectionLongPress(recordId: String) {
        _isSelectionMode.value = true
        _selectedRecordIds.value = _selectedRecordIds.value + recordId
    }

    fun onToggleRecordSelection(recordId: String) {
        val updated =
            _selectedRecordIds.value.toMutableSet().apply {
                if (!add(recordId)) remove(recordId)
            }
        _selectedRecordIds.value = updated
        _isSelectionMode.value = updated.isNotEmpty()
    }

    fun onClearSelection() {
        _selectedRecordIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun onSelectAllForSelectionMode() {
        val allIds = _inboxRecords.value.map { it.id }.toSet()
        _selectedRecordIds.value = allIds
        _isSelectionMode.value = allIds.isNotEmpty()
    }

    fun copySelectedToClipboard(): Int {
        val selectedIds = selectedIdsInUiOrder()
        if (selectedIds.isEmpty()) return 0
        backlogClipboardUseCase.copyInboxRecords(
            sourceContextId = projectIdFlow.value,
            recordIds = selectedIds,
        )
        return selectedIds.size
    }

    fun cutSelectedToClipboard(): Int {
        val selectedIds = selectedIdsInUiOrder()
        if (selectedIds.isEmpty()) return 0
        backlogClipboardUseCase.cutInboxRecords(
            sourceContextId = projectIdFlow.value,
            recordIds = selectedIds,
        )
        return selectedIds.size
    }

    fun canPasteFromClipboard(): Boolean = backlogClipboardUseCase.canPasteIntoInbox(projectIdFlow.value)

    fun pasteFromClipboard(onResult: (Int) -> Unit) {
        val projectId = projectIdFlow.value
        if (projectId.isBlank()) {
            onResult(0)
            return
        }
        scope.launch(Dispatchers.IO) {
            val insertedCount = backlogClipboardUseCase.pasteIntoInbox(projectId)
            withContext(Dispatchers.Main) {
                onResult(insertedCount)
            }
        }
    }

    fun promoteInboxRecordToGoal(record: InboxRecord) {
        scope.launch(Dispatchers.IO) {
            inboxRepository.promoteInboxRecordToGoal(record)
        }
    }

    private fun promoteInboxRecordToGoal(
        record: InboxRecord,
        targetProjectId: String,
    ) {
        scope.launch(Dispatchers.IO) {
            inboxRepository.promoteInboxRecordToGoal(record, targetProjectId)
        }
    }

    private fun updateInboxRecordText(
        record: InboxRecord,
        newText: String,
    ) {
        scope.launch(Dispatchers.IO) {
            inboxRepository.updateInboxRecord(record.copy(text = newText))
        }
    }

    fun onInboxRecordEditRequest(record: InboxRecord) {
        _recordToEdit.value = record
    }

    fun onInboxRecordEditDismiss() {
        _recordToEdit.value = null
    }

    fun onInboxRecordEditConfirm(newText: String) {
        recordToEdit.value?.let { record ->
            updateInboxRecordText(record, newText)
        }
        _recordToEdit.value = null
    }

    fun onPromoteToAnotherList(record: InboxRecord) {
        _recordForPromotion.value = record
        val title = "Перемістити запис до..."
        scope.launch {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val disabledIds = projectIdFlow.value
            listener.requestNavigation("list_chooser_screen/$encodedTitle?disabledIds=$disabledIds")
        }
    }

    fun onListSelectedForInboxPromotion(targetProjectId: String) {
        val recordToPromote = _recordForPromotion.value
        if (recordToPromote != null) {
            promoteInboxRecordToGoal(recordToPromote, targetProjectId)
            listener.showSnackbar("Запис переміщено до цілей", null)
        }
        _recordForPromotion.value = null
    }

    fun onInboxPromotionCancelled() {
        _recordForPromotion.value = null
    }

    private fun selectedIdsInUiOrder(): List<String> {
        val selected = _selectedRecordIds.value
        if (selected.isEmpty()) return emptyList()
        return _inboxRecords.value.mapNotNull { record -> record.id.takeIf { it in selected } }
    }
}
