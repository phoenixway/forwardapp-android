// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/noteedit/NoteEditViewModel.kt
package com.romankozak.forwardappmobile.ui.screens.noteedit

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Цей клас є заглушкою. Функціонал нотаток було видалено з проєкту.
 * Цей файл слід видалити разом з відповідним UI-екраном.
 */

// Події, які ViewModel може надсилати до UI
sealed class NoteEditEvent {
    data class NavigateBack(val message: String? = null) : NoteEditEvent()
}

// Стан UI для екрану редагування нотатки
data class NoteEditUiState(
    val title: TextFieldValue = TextFieldValue(""),
    val content: TextFieldValue = TextFieldValue(""),
    val isReady: Boolean = false,
    val isNewNote: Boolean = true,
    val error: String? = null,
    val isSaveButtonEnabled: Boolean = false
)

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<NoteEditEvent>()
    val events = _events.receiveAsFlow()

    init {
        // Оскільки нотаток більше немає, просто повідомляємо, що екран готовий.
        viewModelScope.launch {
            _uiState.value = NoteEditUiState(
                isReady = true,
                error = "Функціонал нотаток видалено."
            )
        }
    }

    fun onTitleChange(newValue: TextFieldValue) {
        // Дії не потрібні
    }

    fun onContentChange(newValue: TextFieldValue) {
        // Дії не потрібні
    }

    fun onSave() {
        // Дії не потрібні
        viewModelScope.launch {
            _events.send(NoteEditEvent.NavigateBack())
        }
    }
}