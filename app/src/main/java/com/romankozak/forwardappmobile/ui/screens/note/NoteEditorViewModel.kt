package com.romankozak.forwardappmobile.ui.screens.note

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorViewModel
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val application: Application,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val universalEditorViewModel = UniversalEditorViewModel(application)
    private var noteId: String? = null

    fun loadNote(id: String) {
        noteId = id
        viewModelScope.launch {
            projectRepository.getNoteById(id)?.let { note ->
                // Встановлюємо projectId для "Show Location"
                universalEditorViewModel.setProjectId(note.projectId)
                universalEditorViewModel.setInitialContent(note.content ?: "")
            }
        }
    }

    fun saveNote(content: String) {
        noteId?.let {
            viewModelScope.launch {
                projectRepository.getNoteById(it)?.let { note ->
                    val title = content.lines().firstOrNull() ?: ""
                    val updatedNote = note.copy(title = title, content = content)
                    projectRepository.saveNote(updatedNote)
                }
            }
        }
    }
}
