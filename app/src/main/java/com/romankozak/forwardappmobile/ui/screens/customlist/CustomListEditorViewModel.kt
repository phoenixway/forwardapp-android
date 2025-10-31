package com.romankozak.forwardappmobile.ui.screens.customlist

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CustomListEditorViewModel
@Inject
constructor(
  private val projectRepository: ProjectRepository,
  private val noteDocumentRepository: NoteDocumentRepository,
  private val application: Application,
  private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val universalEditorViewModel = UniversalEditorViewModel(application)
  private var listId: String? = null

  fun loadDocument(id: String) {
    listId = id
    viewModelScope.launch {
      noteDocumentRepository.getDocumentById(id)?.let { document ->
        android.util.Log.d("CursorDebug", "Loaded document with lastCursorPosition: ${document.lastCursorPosition}")
        // Встановлюємо projectId для "Show Location"
        universalEditorViewModel.setProjectId(document.projectId)
        universalEditorViewModel.setInitialContent(
            document.content ?: "",
            document.lastCursorPosition
        )
      }
    }
  }

  fun saveDocument(content: String, cursorPosition: Int) {
    android.util.Log.d("CursorDebug", "saveDocument called with cursorPosition: $cursorPosition")
    listId?.let {
      viewModelScope.launch {
        noteDocumentRepository.getDocumentById(it)?.let { document ->
          val name = content.lines().firstOrNull()?.take(100) ?: "Unnamed Note"
          val updatedDocument = document.copy(
              name = name,
              content = content,
              updatedAt = System.currentTimeMillis(),
              lastCursorPosition = cursorPosition
          )
          noteDocumentRepository.updateDocument(updatedDocument)
        }
      }
    }
  }
}
