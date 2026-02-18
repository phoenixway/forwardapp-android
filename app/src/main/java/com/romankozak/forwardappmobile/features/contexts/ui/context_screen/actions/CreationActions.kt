package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository

class CreationActions(
    private val noteDocumentRepository: NoteDocumentRepository,
    private val musicNoteRepository: MusicNoteRepository,
) {
    sealed class CreateNoteDocumentResult {
        data class Navigate(val target: NavTarget.NoteDocument) : CreateNoteDocumentResult()

        data class Error(val message: String) : CreateNoteDocumentResult()
    }

    sealed class CreateChecklistResult {
        data class Navigate(val target: NavTarget.Checklist) : CreateChecklistResult()

        data class Error(val message: String) : CreateChecklistResult()
    }

    sealed class CreateMusicNoteResult {
        data class Navigate(val target: NavTarget.MusicNote) : CreateMusicNoteResult()

        data class Error(val message: String) : CreateMusicNoteResult()
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

    suspend fun createMusicNote(contextId: String): CreateMusicNoteResult {
        if (contextId.isBlank()) {
            return CreateMusicNoteResult.Error("Не вдалося визначити контекст для створення нот")
        }
        val musicNoteId =
            musicNoteRepository.create(
                name = "Нові ноти",
                contextId = contextId,
                content = "",
            )
        return CreateMusicNoteResult.Navigate(
            NavTarget.MusicNote(id = musicNoteId, startEdit = true),
        )
    }
}
