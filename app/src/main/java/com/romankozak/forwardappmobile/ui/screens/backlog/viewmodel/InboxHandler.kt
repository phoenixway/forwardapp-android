package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

/**
 * Оновлений інтерфейс для комунікації з ViewModel.
 */
interface InboxHandlerResultListener {
    fun requestNavigation(route: String)
    /**
     * ВИПРАВЛЕНО: Прибрано значення за замовчуванням "= null", щоб уникнути конфлікту реалізацій.
     */
    fun showSnackbar(message: String, action: String?)
    fun scrollToListEnd()
    fun updateInputState(inputValue: TextFieldValue)
}

/**
 * Клас, що інкапсулює всю бізнес-логіку та стан для функціоналу "Інбокс".
 */
class InboxHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val listener: InboxHandlerResultListener,
) {

    // --- Стан (State) ---

    private val _inboxRecords = MutableStateFlow<List<InboxRecord>>(emptyList())
    val inboxRecords: StateFlow<List<InboxRecord>> = _inboxRecords.asStateFlow()

    private val _recordToEdit = MutableStateFlow<InboxRecord?>(null)
    val recordToEdit: StateFlow<InboxRecord?> = _recordToEdit.asStateFlow()

    private val _recordForPromotion = MutableStateFlow<InboxRecord?>(null)
    val recordForPromotion: StateFlow<InboxRecord?> = _recordForPromotion.asStateFlow()

    init {
        scope.launch {
            listIdFlow
                .filter { it.isNotEmpty() }
                .flatMapLatest { id -> goalRepository.getInboxRecordsStream(id) }
                .collect { records ->
                    _inboxRecords.value = records.sortedBy { it.createdAt }
                }
        }
    }

    // --- Публічні методи для UI ---

    fun addQuickRecord(text: String) {
        scope.launch(Dispatchers.IO) {
            val listId = listIdFlow.value
            if (listId.isNotEmpty() && text.isNotBlank()) {
                goalRepository.addInboxRecord(text, listId)
            }
        }
        listener.updateInputState(TextFieldValue(""))
        listener.scrollToListEnd()
    }

    fun deleteInboxRecord(recordId: String) {
        scope.launch(Dispatchers.IO) {
            goalRepository.deleteInboxRecordById(recordId)
        }
    }

    fun promoteInboxRecordToGoal(record: InboxRecord) {
        scope.launch(Dispatchers.IO) {
            goalRepository.promoteInboxRecordToGoal(record)
        }
    }

    private fun promoteInboxRecordToGoal(record: InboxRecord, targetListId: String) {
        scope.launch(Dispatchers.IO) {
            goalRepository.promoteInboxRecordToGoal(record, targetListId)
        }
    }

    private fun updateInboxRecordText(record: InboxRecord, newText: String) {
        scope.launch(Dispatchers.IO) {
            goalRepository.updateInboxRecord(record.copy(text = newText))
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
            val disabledIds = listIdFlow.value
            listener.requestNavigation("list_chooser_screen/$encodedTitle?disabledIds=$disabledIds")
        }
    }

    fun onListSelectedForInboxPromotion(targetListId: String) {
        val recordToPromote = _recordForPromotion.value
        if (recordToPromote != null) {
            promoteInboxRecordToGoal(recordToPromote, targetListId)
            listener.showSnackbar("Запис переміщено до цілей", null)
        }
        _recordForPromotion.value = null
    }

    fun onInboxPromotionCancelled() {
        _recordForPromotion.value = null
    }
}