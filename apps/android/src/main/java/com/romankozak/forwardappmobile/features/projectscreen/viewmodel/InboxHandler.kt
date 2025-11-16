package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import com.romankozak.forwardappmobile.shared.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.shared.data.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.data.repository.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

interface InboxHandlerResultListener {
  fun scrollToListEnd()
}

@Inject
class InboxHandler(
  private val projectRepository: ProjectRepository,
  private val inboxRepository: InboxRepository,
  private val scope: CoroutineScope,
  private val projectIdFlow: StateFlow<String>,
  private val listener: InboxHandlerResultListener,
) {
  val inboxRecords =
    projectIdFlow
      .flatMapLatest {
        if (it.isNotBlank()) {
          inboxRepository.getRecordsForProject(it)
        } else {
          flowOf(emptyList())
        }
      }
      .stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recordToEdit = MutableStateFlow<InboxRecord?>(null)
  val recordForPromotion = MutableStateFlow<InboxRecord?>(null)

  fun addQuickRecord(text: String) {
    if (text.isBlank()) return
    scope.launch(Dispatchers.IO) {
      inboxRepository.addRecord(text, projectIdFlow.value)
      withContext(Dispatchers.Main) { listener.scrollToListEnd() }
    }
  }

  fun onPromoteToGoal(record: InboxRecord) {
    scope.launch(Dispatchers.IO) {
      projectRepository.addGoalToProject(record.text, projectIdFlow.value)
      inboxRepository.deleteRecord(record)
    }
  }

  fun onPromoteToTask(record: InboxRecord) {
    recordForPromotion.value = record
    // TODO: Navigate to list chooser
  }

  fun onListSelectedForInboxPromotion(targetProjectId: String) {
    val record = recordForPromotion.value ?: return
    scope.launch(Dispatchers.IO) {
      projectRepository.addGoalToProject(record.text, targetProjectId)
      inboxRepository.deleteRecord(record)
      withContext(Dispatchers.Main) { recordForPromotion.value = null }
    }
  }

  fun onInboxRecordDelete(record: InboxRecord) {
    scope.launch(Dispatchers.IO) { inboxRepository.deleteRecord(record) }
  }

  fun onInboxRecordEdit(record: InboxRecord) {
    recordToEdit.value = record
  }

  fun onInboxRecordUpdate(record: InboxRecord, newText: String) {
    scope.launch(Dispatchers.IO) {
      inboxRepository.updateRecord(record.copy(text = newText))
      withContext(Dispatchers.Main) { recordToEdit.value = null }
    }
  }

  fun onInboxRecordEditDismiss() {
    recordToEdit.value = null
  }
}
