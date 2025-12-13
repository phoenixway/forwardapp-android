package com.romankozak.forwardappmobile.ui.screens.notedocument

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.ui.common.editor.NoteTitleExtractor
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class NoteDocumentEditorViewModel
@Inject
  constructor(
    private val noteDocumentRepository: NoteDocumentRepository,
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
  ) : ViewModel() {

  val universalEditorViewModel = UniversalEditorViewModel(application)
  private var listId: String? = null

  val linkSuggestions =
    noteDocumentRepository.getAllDocumentsAsFlow()
      .map { docs -> docs.map { it.name.ifBlank { "Untitled" } } }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
          val name = NoteTitleExtractor.extract(content).take(100)
          Log.d("NoteTitleExtractor", "saveDocument extracted title='$name'")
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

  suspend fun findDocumentIdByName(name: String): String? =
    noteDocumentRepository.findDocumentByName(name)?.id
}
