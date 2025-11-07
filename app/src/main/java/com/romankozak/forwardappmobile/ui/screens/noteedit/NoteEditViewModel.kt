package com.romankozak.forwardappmobile.ui.screens.noteedit

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class NoteEditEvent {
    data class NavigateBack(val message: String? = null) : NoteEditEvent()
}

data class NoteEditUiState(
    val title: TextFieldValue = TextFieldValue(""),
    val content: TextFieldValue = TextFieldValue(""),
    val isReady: Boolean = false,
    val isNewNote: Boolean = true,
    val error: String? = null,
    val isSaveButtonEnabled: Boolean = false,
)

@HiltViewModel
class NoteEditViewModel
    @Inject
    constructor(
        private val noteRepository: com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NoteEditUiState())
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<NoteEditEvent>()
        val events = _events.receiveAsFlow()

        private var noteId: String? = null
        private var projectId: String? = null
        private var initialTitle: String = ""
        private var initialContent: String = ""

        init {
            noteId = savedStateHandle.get<String>("noteId")

            viewModelScope.launch {
                if (noteId != null) {
                    noteRepository.getNoteById(noteId!!)?.let { note ->
                        projectId = note.projectId
                        initialTitle = note.title
                        initialContent = note.content
                        _uiState.update {
                            it.copy(
                                title = TextFieldValue(note.title),
                                content = TextFieldValue(note.content),
                                isNewNote = false,
                                isReady = true,
                            )
                        }
                    } ?: run {
                        _uiState.update { it.copy(isReady = true, error = "Note not found") }
                    }
                } else {
                    projectId = savedStateHandle.get<String>("projectId")
                    if (projectId != null) {
                        _uiState.update { it.copy(isReady = true, isNewNote = true) }
                    } else {
                        _uiState.update { it.copy(isReady = true, error = "Project ID is required for a new note") }
                    }
                }
            }
        }

        fun onTitleChange(newValue: TextFieldValue) {
            _uiState.update {
                it.copy(
                    title = newValue,
                    isSaveButtonEnabled = newValue.text != initialTitle || it.content.text != initialContent,
                )
            }
        }

        fun onContentChange(newValue: TextFieldValue) {
            _uiState.update {
                it.copy(
                    content = newValue,
                    isSaveButtonEnabled = it.title.text != initialTitle || newValue.text != initialContent,
                )
            }
        }

        fun onSave() {
            viewModelScope.launch {
                val title = _uiState.value.title.text
                val content = _uiState.value.content.text

                if (title.isBlank()) {
                    _uiState.update { it.copy(error = "Title cannot be empty") }
                    return@launch
                }

                if (projectId == null) {
                    _uiState.update { it.copy(error = "Cannot determine which project to save to.") }
                    return@launch
                }

                val noteToSave =
                    if (_uiState.value.isNewNote) {
                        LegacyNoteEntity(
                            id = UUID.randomUUID().toString(),
                            projectId = projectId!!,
                            title = title,
                            content = content,
                        )
                    } else {
                        
                        val originalNote = noteRepository.getNoteById(noteId!!)!!
                        originalNote.copy(
                            title = title,
                            content = content,
                            updatedAt = System.currentTimeMillis(),
                        )
                    }

                noteRepository.saveNote(noteToSave)
                _events.send(NoteEditEvent.NavigateBack("Note saved"))
            }
        }
    }
