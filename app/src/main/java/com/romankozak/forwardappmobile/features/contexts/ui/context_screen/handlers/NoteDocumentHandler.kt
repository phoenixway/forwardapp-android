package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.NoteRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NoteDocumentHandler(
        private val contextRepository: ContextRepository,
        private val noteRepository: NoteRepository,
        private val stateManager: ContextStateManager,
        private val scope: CoroutineScope
) {
  fun onShowCreateNoteDocumentDialog() {
    stateManager.updateState { it.copy(showNoteDocumentEditor = true, noteDocumentToEdit = null) }
  }

  fun onDismissNoteDocumentEditor() {
    stateManager.updateState { it.copy(showNoteDocumentEditor = false, noteDocumentToEdit = null) }
  }

  fun onSaveNoteDocument(name: String, content: String) {
    scope.launch {
      // Тут логіка зі старого VM: збереження в NoteRepository та прив'язка до контексту
      // Спрощено для прикладу, адаптуй під свої репозиторії
      onDismissNoteDocumentEditor()
    }
  }
}
