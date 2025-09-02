package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode

class InboxHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val listener: ResultListener
) {

    /**
     * Інтерфейс для передачі результатів роботи хендлера назад у ViewModel.
     */
    interface ResultListener {
        fun updateCurrentView(view: ProjectViewMode)
        fun showEditInboxRecordDialog(record: InboxRecord)
    }

    // --- Публічні методи, які буде викликати ViewModel ---

    fun onProjectViewChange(newView: ProjectViewMode) {
        listener.updateCurrentView(newView)
    }

    fun addQuickRecord(text: String) {
        scope.launch(Dispatchers.IO) {
            val listId = listIdFlow.value
            if (listId.isNotEmpty() && text.isNotBlank()) {
                goalRepository.addInboxRecord(text, listId)
            }
        }
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

    fun updateInboxRecordText(record: InboxRecord, newText: String) {
        scope.launch(Dispatchers.IO) {
            goalRepository.updateInboxRecord(record.copy(text = newText))
        }
    }

    // --- Методи для керування діалогом редагування ---

    fun onInboxRecordEditRequest(record: InboxRecord) {
        listener.showEditInboxRecordDialog(record)
    }
}