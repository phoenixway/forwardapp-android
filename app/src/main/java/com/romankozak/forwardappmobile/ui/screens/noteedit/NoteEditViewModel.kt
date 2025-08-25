// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/noteedit/NoteEditViewModel.kt
package com.romankozak.forwardappmobile.ui.screens.noteedit

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Note
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

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
    val error: String? = null, // Інтегровано з v1
    val isSaveButtonEnabled: Boolean = false // Інтегровано з v1
)

@HiltViewModel
class NoteEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val noteId: String? = savedStateHandle["noteId"]
    private val initialListId: String? = savedStateHandle["listId"]

    private val _uiState = MutableStateFlow(NoteEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<NoteEditEvent>()
    val events = _events.receiveAsFlow()

    private var currentNote: Note? = null

    init {
        viewModelScope.launch {
            if (noteId != null) {
                loadExistingNote(noteId)
            } else {
                _uiState.update { it.copy(isReady = true, isNewNote = true) }
                updateSaveButtonState() // Інтегровано з v1
            }
        }
    }

    private suspend fun loadExistingNote(id: String) {
        val note = goalRepository.getNoteById(id)
        if (note != null) {
            currentNote = note
            _uiState.update {
                it.copy(
                    title = TextFieldValue(note.title ?: ""),
                    content = TextFieldValue(note.content),
                    isReady = true,
                    isNewNote = false
                )
            }
            updateSaveButtonState() // Інтегровано з v1
        } else {
            _events.send(NoteEditEvent.NavigateBack("Нотатку не знайдено"))
        }
    }

    fun onTitleChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(title = newValue) }
        updateSaveButtonState() // Інтегровано з v1
    }

    fun onContentChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(content = newValue) }
        updateSaveButtonState() // Інтегровано з v1
    }

    private fun updateSaveButtonState() {
        val state = _uiState.value
        val hasChanges = currentNote?.let {
            it.title.orEmpty() != state.title.text ||
                    it.content != state.content.text
        } ?: true // Для нової нотатки зміни є завжди

        // Перевіряємо, чи є валідним або заголовок, або вміст
        val isNoteValid = state.title.text.isNotBlank() || state.content.text.isNotBlank()

        _uiState.update {
            it.copy(
                isSaveButtonEnabled = hasChanges && isNoteValid,
                // Оновлюємо повідомлення про помилку, якщо обидва поля порожні
                error = if (isNoteValid) null else "Заголовок або вміст мають бути заповнені"
            )
        }
    }

    fun onSave() {
        if (!_uiState.value.isSaveButtonEnabled) return // Додаткова перевірка

        viewModelScope.launch {
            val state = _uiState.value

            val noteToSave = currentNote?.copy(
                title = state.title.text.ifBlank { null },
                content = state.content.text,
                updatedAt = System.currentTimeMillis()
            ) ?: Note(
                id = UUID.randomUUID().toString(),
                title = state.title.text.ifBlank { null },
                content = state.content.text,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            if (state.isNewNote) {
                initialListId ?: return@launch
                goalRepository.addNoteToList(noteToSave, initialListId)
            } else {
                goalRepository.updateNote(noteToSave)
            }

            _events.send(NoteEditEvent.NavigateBack("Збережено"))
        }
    }
}