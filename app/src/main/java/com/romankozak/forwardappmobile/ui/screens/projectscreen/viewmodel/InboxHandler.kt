package com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

interface InboxHandlerResultListener {
    fun requestNavigation(route: String)

    fun showSnackbar(
        message: String,
        action: String?,
    )

    fun scrollToListEnd()

    fun updateInputState(inputValue: TextFieldValue)
}

class InboxHandler(
    private val projectRepository: ProjectRepository,
    private val inboxRepository: com.romankozak.forwardappmobile.data.repository.InboxRepository,
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

    init {
        scope.launch {
            projectIdFlow
                .filter { it.isNotEmpty() }
                .flatMapLatest { id -> inboxRepository.getInboxRecordsStream(id) }
                .collect { records ->
                    _inboxRecords.value = records.sortedBy { it.createdAt }
                }
        }
    }

    fun addQuickRecord(text: String) {
        scope.launch(Dispatchers.IO) {
            val projectId = projectIdFlow.value
            if (projectId.isNotEmpty() && text.isNotBlank()) {
                inboxRepository.addInboxRecord(text, projectId)
            }
        }
        listener.updateInputState(TextFieldValue(""))
        listener.scrollToListEnd()
    }

    fun deleteInboxRecord(recordId: String) {
        scope.launch(Dispatchers.IO) {
            inboxRepository.deleteInboxRecordById(recordId)
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
}
