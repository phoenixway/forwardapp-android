package com.romankozak.forwardappmobile.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.app.Application

@HiltViewModel
class InboxEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val application: Application,
) : ViewModel() {

    val universalEditorViewModel = UniversalEditorViewModel(application)

    private var inboxId: String? = null

    fun loadInboxItem(id: String) {
        inboxId = id
        viewModelScope.launch {
            projectRepository.getInboxRecordById(id)?.let {
                universalEditorViewModel.setInitialContent(it.text)
            }
        }
    }

    fun saveInboxItem(content: String) {
        inboxId?.let {
            viewModelScope.launch {
                projectRepository.getInboxRecordById(it)?.let { record ->
                    val updatedRecord = record.copy(text = content)
                    projectRepository.updateInboxRecord(updatedRecord)
                }
            }
        }
    }
}
