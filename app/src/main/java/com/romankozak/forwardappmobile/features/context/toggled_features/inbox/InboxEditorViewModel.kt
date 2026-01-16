package com.romankozak.forwardappmobile.features.context.toggled_features.inbox

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class InboxEditorViewModel
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val inboxRepository: InboxRepository,
  private val application: Application,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val universalEditorViewModel = UniversalEditorViewModel(application)
  private var inboxId: String? = null

  fun loadInboxItem(id: String) {
    inboxId = id
    viewModelScope.launch {
      inboxRepository.getInboxRecordById(id)?.let { record ->
        // Встановлюємо projectId для "Show Location"
        universalEditorViewModel.setProjectId(record.projectId)
        universalEditorViewModel.setInitialContent(record.text)
      }
    }
  }

  fun saveInboxItem(content: String) {
    inboxId?.let {
      viewModelScope.launch {
        inboxRepository.getInboxRecordById(it)?.let { record ->
          val updatedRecord = record.copy(text = content)
          inboxRepository.updateInboxRecord(updatedRecord)
        }
      }
    }
  }
}
