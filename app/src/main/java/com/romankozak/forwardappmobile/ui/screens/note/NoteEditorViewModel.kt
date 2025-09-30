package com.romankozak.forwardappmobile.ui.screens.note

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val application: Application,
) : ViewModel() {

    val universalEditorViewModel = UniversalEditorViewModel(application)
    private var noteId: String? = null

    fun loadNote(id: String) {
        noteId = id
        viewModelScope.launch {
            projectRepository.getNoteById(id)?.let {
                universalEditorViewModel.setInitialContent(it.content ?: "")
            }
        }
    }

    fun saveNote(content: String) {
        noteId?.let {
            viewModelScope.launch {
                projectRepository.getNoteById(it)?.let { note ->
                    val updatedNote = note.copy(content = content)
                    projectRepository.saveNote(updatedNote)
                }
            }
        }
    }
}
