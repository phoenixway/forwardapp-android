package com.romankozak.forwardappmobile.ui.screens.customlist

import android.app.Application
import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.CustomListEntity
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CustomListEditUiState(
    val title: String = "",
    val content: TextFieldValue = TextFieldValue(""),
    val isNewList: Boolean = true,
    val isReady: Boolean = false,
    val error: String? = null,
    val isSaveButtonEnabled: Boolean = false
)

sealed class CustomListEditEvent {
    data class NavigateBack(val message: String? = null) : CustomListEditEvent()
}

@HiltViewModel
class CustomListEditViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomListEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CustomListEditEvent>()
    val events = _events.receiveAsFlow()

    private val listId: String? = savedStateHandle["listId"]
    private val projectId: String? = savedStateHandle["projectId"]

    init {
        viewModelScope.launch {
            if (listId != null) {
                val list = projectRepository.getCustomListById(listId)
                if (list != null) {
                    _uiState.value = CustomListEditUiState(
                        title = list.name,
                        content = TextFieldValue(list.content ?: ""),
                        isNewList = false,
                        isReady = true,
                        isSaveButtonEnabled = true
                    )
                } else {
                    _events.send(CustomListEditEvent.NavigateBack("Custom list not found"))
                }
            } else if (projectId != null) {
                _uiState.value = CustomListEditUiState(isNewList = true, isReady = true)
            } else {
                _events.send(CustomListEditEvent.NavigateBack("Project ID not provided"))
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            title = newTitle,
            isSaveButtonEnabled = newTitle.isNotBlank()
        )
    }

    fun onContentChange(newContent: TextFieldValue) {
        _uiState.value = _uiState.value.copy(content = newContent)
    }

    private val TAG = "CUSTOM_LIST_DEBUG"

    fun onSave() {
        val currentState = _uiState.value
        Log.d(TAG, "onSave called with state: $currentState")
        if (currentState.title.isBlank()) {
            _uiState.value = currentState.copy(error = "Title can't be empty")
            return
        }

        val job = viewModelScope.launch {
            Log.d(TAG, "onSave coroutine started")
            if (listId != null) {
                val list = projectRepository.getCustomListById(listId)
                if (list != null) {
                    val updatedList = list.copy(
                        name = currentState.title,
                        content = currentState.content.text
                    )
                    Log.d(TAG, "Updating existing list: $updatedList")
                    projectRepository.updateCustomList(updatedList)
                } else {
                    Log.d(TAG, "onSave: list with id $listId not found, cannot update")
                }
            } else if (projectId != null) {
                Log.d(TAG, "Creating new list with title: ${currentState.title}, projectId: $projectId, content: ${currentState.content.text}")
                projectRepository.createCustomList(currentState.title, projectId, currentState.content.text)
            }
        }

        job.invokeOnCompletion { throwable ->
            viewModelScope.launch {
                if (throwable == null) {
                    Log.d(TAG, "Save job completed successfully")
                    _events.send(CustomListEditEvent.NavigateBack("Custom list action completed"))
                } else {
                    Log.e(TAG, "Save job failed", throwable)
                    _uiState.value = _uiState.value.copy(error = "Error: ${throwable.message}")
                }
            }
        }
    }
}
