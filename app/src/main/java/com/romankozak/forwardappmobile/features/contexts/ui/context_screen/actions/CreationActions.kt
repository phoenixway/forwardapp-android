package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository

class CreationActions(
    private val noteDocumentRepository: NoteDocumentRepository,
) {
    sealed class CreateNoteDocumentResult {
        data class Navigate(val target: NavTarget.NoteDocument) : CreateNoteDocumentResult()

        data class Error(val message: String) : CreateNoteDocumentResult()
    }

    sealed class CreateChecklistResult {
        data class Navigate(val target: NavTarget.Checklist) : CreateChecklistResult()

        data class Error(val message: String) : CreateChecklistResult()
    }

    suspend fun createNoteDocument(contextId: String): CreateNoteDocumentResult {
        if (contextId.isBlank()) {
            return CreateNoteDocumentResult.Error("Не вдалося визначити проект для створення документа")
        }
        val documentId =
            noteDocumentRepository.createDocument(
                name = "Нова нотатка",
                contextId = contextId,
                content = "",
            )
        return CreateNoteDocumentResult.Navigate(
            NavTarget.NoteDocument(id = documentId, startEdit = true),
        )
    }

    fun createChecklist(contextId: String): CreateChecklistResult {
        if (contextId.isBlank()) {
            return CreateChecklistResult.Error("Не вдалося визначити проект для створення чекліста")
        }
        return CreateChecklistResult.Navigate(
            NavTarget.Checklist(contextId = contextId),
        )
    }
}
