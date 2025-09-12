package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InboxHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val listener: ResultListener,
) {
    interface ResultListener {
        fun updateCurrentView(view: ProjectViewMode)

        fun showEditInboxRecordDialog(record: InboxRecord)
    }

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

    fun updateInboxRecordText(
        record: InboxRecord,
        newText: String,
    ) {
        scope.launch(Dispatchers.IO) {
            goalRepository.updateInboxRecord(record.copy(text = newText))
        }
    }

    fun onInboxRecordEditRequest(record: InboxRecord) {
        listener.showEditInboxRecordDialog(record)
    }
}
