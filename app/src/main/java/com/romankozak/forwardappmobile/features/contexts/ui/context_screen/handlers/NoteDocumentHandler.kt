package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers

import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.NoteRepository // припустимо, є такий
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class DocumentHandler @Inject constructor(
    private val contextRepository: ContextRepository,
    private val stateManager: ContextStateManager,
    private val scope: CoroutineScope
) {
    fun onShowCreateNoteDocumentDialog() {
        stateManager.updateState { it.copy(showNoteDocumentEditor = true) }
    }

    fun onDismissNoteDocumentEditor() {
        stateManager.updateState { it.copy(showNoteDocumentEditor = false) }
    }

    fun onCreateChecklist(projectId: String) {
        scope.launch {
            // Логіка створення чек-листа
            // contextRepository.createChecklist(projectId, "Новий список")
        }
    }
    
    // Додай onSaveNoteDocument сюди на основі свого NoteRepository
}
