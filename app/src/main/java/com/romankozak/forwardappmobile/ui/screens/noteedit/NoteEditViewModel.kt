package com.romankozak.forwardappmobile.ui.screens.noteedit

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NoteEditEvent {
    data class NavigateBack(
        val message: String? = null,
    ) : NoteEditEvent()
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
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<NoteEditEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _uiState.value =
                NoteEditUiState(
                    isReady = true,
                    error = "Функціонал нотаток видалено.",
                )
        }
    }

    fun onTitleChange(newValue: TextFieldValue) {
    }

    fun onContentChange(newValue: TextFieldValue) {
    }

    fun onSave() {
        viewModelScope.launch {
            _events.send(NoteEditEvent.NavigateBack())
        }
    }
}