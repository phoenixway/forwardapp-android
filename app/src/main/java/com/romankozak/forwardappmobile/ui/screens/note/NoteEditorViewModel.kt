package com.romankozak.forwardappmobile.ui.screens.note

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val noteRepository: com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository,
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val universalEditorViewModel = UniversalEditorViewModel(application)
    private var noteId: String? = null

    fun loadNote(id: String) {
        noteId = id
        viewModelScope.launch {
            noteRepository.getNoteById(id)?.let { note ->
                // Встановлюємо projectId для "Show Location"
                universalEditorViewModel.setProjectId(note.projectId)
                universalEditorViewModel.setInitialContent(note.content)
            }
        }
    }

    fun saveNote(content: String) {
        noteId?.let {
            viewModelScope.launch {
                noteRepository.getNoteById(it)?.let { note ->
                    val title = content.lines().firstOrNull() ?: ""
                    val updatedNote = note.copy(title = title, content = content)
                    noteRepository.saveNote(updatedNote)
                }
            }
        }
    }
}
