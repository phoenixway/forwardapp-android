package com.romankozak.forwardappmobile.ui.screens.customlist

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CustomListEditorViewModel
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val customListRepository: com.romankozak.forwardappmobile.data.repository.CustomListRepository,
  private val application: Application,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val universalEditorViewModel = UniversalEditorViewModel(application)
  private var listId: String? = null

  fun loadCustomList(id: String) {
    listId = id
    viewModelScope.launch {
      customListRepository.getCustomListById(id)?.let { customList ->
        android.util.Log.d("CursorDebug", "Loaded customList with lastCursorPosition: ${customList.lastCursorPosition}")
        // Встановлюємо projectId для "Show Location"
        universalEditorViewModel.setProjectId(customList.projectId)
        universalEditorViewModel.setInitialContent(
            customList.content ?: "",
            customList.lastCursorPosition
        )
      }
    }
  }

  fun saveCustomList(content: String, cursorPosition: Int) {
    android.util.Log.d("CursorDebug", "saveCustomList called with cursorPosition: $cursorPosition")
    listId?.let {
      viewModelScope.launch {
        customListRepository.getCustomListById(it)?.let { customList ->
          val name = content.lines().firstOrNull()?.take(100) ?: "Unnamed List"
          val updatedList = customList.copy(
              name = name,
              content = content,
              updatedAt = System.currentTimeMillis(),
              lastCursorPosition = cursorPosition
          )
          customListRepository.updateCustomList(updatedList)
        }
      }
    }
  }
}
