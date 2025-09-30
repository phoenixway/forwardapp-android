package com.romankozak.forwardappmobile.ui.screens.customlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomListEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val application: Application,
) : ViewModel() {

    val universalEditorViewModel = UniversalEditorViewModel(application)
    private var listId: String? = null

    fun loadCustomList(id: String) {
        listId = id
        viewModelScope.launch {
            projectRepository.getCustomListById(id)?.let {
                universalEditorViewModel.setInitialContent(it.content ?: "")
            }
        }
    }

    fun saveCustomList(content: String) {
        listId?.let {
            viewModelScope.launch {
                projectRepository.getCustomListById(it)?.let { list ->
                    val updatedList = list.copy(content = content)
                    projectRepository.updateCustomList(updatedList)
                }
            }
        }
    }
}
