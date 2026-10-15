package com.romankozak.forwardappmobile.features.attachments.specifictypes.note

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel
    @Inject
    constructor(
        private val noteRepository: LegacyNoteRepository,
        private val application: Application,
    ) : ViewModel() {
        val universalEditorViewModel = UniversalEditorViewModel(application)
        private var noteId: String? = null
        private val saveMutex = Mutex()

        fun loadNote(id: String) {
            noteId = id
            // Historical contextId is not a current Note location.
            universalEditorViewModel.setProjectId(null)
            viewModelScope.launch {
                try {
                    val note = noteRepository.getNoteById(id)
                    if (noteId != id) return@launch

                    if (note == null || note.isDeleted) {
                        universalEditorViewModel.setInitialContent("")
                        universalEditorViewModel.showError("Note is unavailable or deleted")
                        return@launch
                    }

                    universalEditorViewModel.setInitialContent(note.content)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    universalEditorViewModel.setInitialContent("")
                    universalEditorViewModel.showError(
                        error.message ?: "Cannot load Note",
                    )
                }
            }
        }

        /**
         * Complete the actual write before reporting success.
         * Auto-save and explicit save share one serialized write boundary.
         */
        fun saveNote(
            content: String,
            onSaved: (() -> Unit)? = null,
        ) {
            viewModelScope.launch {
                val saved =
                    saveMutex.withLock {
                        try {
                            val currentNoteId = noteId
                            val title = content.lines().firstOrNull().orEmpty()

                            if (currentNoteId.isNullOrBlank()) {
                                // Do not persist untouched empty editor drafts.
                                if (content.isBlank()) return@withLock false

                                val newNote =
                                    LegacyNoteEntity(
                                        id = UUID.randomUUID().toString(),
                                        contextId = null,
                                        title = title.ifBlank { "Нова нотатка" },
                                        content = content,
                                    )

                                noteRepository.saveNote(
                                    note = newNote,
                                    initialWorkspaceId = null,
                                )

                                // Assign only after successful persistence.
                                // Mutex ensures that auto-save cannot duplicate it.
                                noteId = newNote.id
                                true
                            } else {
                                val existingNote = noteRepository.getNoteById(currentNoteId)
                                if (existingNote == null || existingNote.isDeleted) {
                                    universalEditorViewModel.showError(
                                        "Note is unavailable or deleted",
                                    )
                                    return@withLock false
                                }

                                noteRepository.saveNote(
                                    existingNote.copy(
                                        title = title,
                                        content = content,
                                    ),
                                )
                                true
                            }
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            universalEditorViewModel.showError(
                                error.message ?: "Cannot save Note",
                            )
                            false
                        }
                    }

                if (saved) {
                    onSaved?.invoke()
                }
            }
        }
    }
