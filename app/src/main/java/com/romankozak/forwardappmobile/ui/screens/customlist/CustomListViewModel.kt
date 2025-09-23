package com.romankozak.forwardappmobile.ui.screens.customlist

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.CustomListEntity
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class UnifiedCustomListUiState(
  val title: String = "",
  val content: TextFieldValue = TextFieldValue(""),
  val isExistingList: Boolean = false,
  val isLoading: Boolean = false,
  val error: String? = null,
  val isSaveEnabled: Boolean = false,
  val list: CustomListEntity? = null,
)

sealed class UnifiedCustomListEvent {
  data class NavigateBack(val message: String? = null) : UnifiedCustomListEvent()

  data class ShowError(val message: String) : UnifiedCustomListEvent()
}

@HiltViewModel
class UnifiedCustomListViewModel
@Inject
constructor(private val projectRepository: ProjectRepository, savedStateHandle: SavedStateHandle) :
  ViewModel() {

  private val _uiState = MutableStateFlow(UnifiedCustomListUiState())
  val uiState = _uiState.asStateFlow()

  private val _events = Channel<UnifiedCustomListEvent>()
  val events = _events.receiveAsFlow()

  private var currentListId: String? = null
  private var currentProjectId: String? = null

  private val TAG = "UNIFIED_CUSTOM_LIST_DEBUG"

  fun initialize(listId: String?, projectId: String?) {
    Log.d(TAG, "Initialize called with listId: $listId, projectId: $projectId")

    currentListId = listId
    currentProjectId = projectId

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      try {
        if (listId != null) {
          // Завантажуємо існуючий список
          val list = projectRepository.getCustomListById(listId)
          if (list != null) {
            Log.d(TAG, "Loaded existing list: $list")
            _uiState.value =
              UnifiedCustomListUiState(
                title = list.name,
                content = TextFieldValue(list.content ?: ""),
                isExistingList = true,
                isLoading = false,
                isSaveEnabled = true,
                list = list,
              )
          } else {
            Log.e(TAG, "List with id $listId not found")
            _events.send(UnifiedCustomListEvent.NavigateBack("Custom list not found"))
          }
        } else if (projectId != null) {
          // Створюємо новий список
          Log.d(TAG, "Creating new list for project: $projectId")
          _uiState.value =
            UnifiedCustomListUiState(
              isExistingList = false,
              isLoading = false,
              content = TextFieldValue("• "), // Починаємо з bullet point
            )
        } else {
          Log.e(TAG, "Neither listId nor projectId provided")
          _events.send(UnifiedCustomListEvent.NavigateBack("Invalid parameters"))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error during initialization", e)
        _events.send(UnifiedCustomListEvent.ShowError("Failed to load: ${e.message}"))
      }
    }
  }

  fun onTitleChange(newTitle: String) {
    Log.d(TAG, "Title changed to: $newTitle")
    _uiState.value =
      _uiState.value.copy(title = newTitle, error = null, isSaveEnabled = newTitle.isNotBlank())
  }

  fun onContentChange(newContent: TextFieldValue) {
    Log.d(TAG, "Content changed, length: ${newContent.text.length}")
    _uiState.value = _uiState.value.copy(content = newContent)
  }

  fun onSave() {
    val currentState = _uiState.value
    Log.d(
      TAG,
      "Save called with state: isExistingList=${currentState.isExistingList}, title='${currentState.title}', contentLength=${currentState.content.text.length}",
    )

    // Валідація для нових списків
    if (!currentState.isExistingList && currentState.title.isBlank()) {
      _uiState.value = currentState.copy(error = "Title can't be empty")
      return
    }

    viewModelScope.launch {
      try {
        if (currentState.isExistingList && currentListId != null) {
          // Оновлюємо існуючий список
          val list = currentState.list
          if (list != null) {
            val updatedList =
              list.copy(
                name = if (currentState.title.isNotBlank()) currentState.title else list.name,
                content = currentState.content.text,
              )
            Log.d(TAG, "Updating existing list: $updatedList")
            projectRepository.updateCustomList(updatedList)

            // Оновлюємо стан після збереження
            _uiState.value = currentState.copy(list = updatedList, title = updatedList.name)

            Log.d(TAG, "List updated successfully")
          } else {
            Log.e(TAG, "Cannot update: list is null")
            _events.send(UnifiedCustomListEvent.ShowError("Failed to update list"))
          }
        } else if (!currentState.isExistingList) {
          val projectId = currentProjectId
          if (projectId != null) {
            // Створюємо новий список
            Log.d(
              TAG,
              "Creating new list with title: '${currentState.title}', projectId: $projectId",
            )
            val newList =
              projectRepository.createCustomList(
                name = currentState.title,
                projectId = projectId,
                content = currentState.content.text,
              )
            Log.d(TAG, "New list created successfully: $newList")
            _events.send(UnifiedCustomListEvent.NavigateBack("List created successfully"))
          } else {
            Log.e(TAG, "Cannot create list: projectId is null")
            _events.send(
              UnifiedCustomListEvent.ShowError("Cannot create list: project not specified")
            )
          }
        } else {
          Log.e(TAG, "Invalid save state: listId=$currentListId, projectId=$currentProjectId")
          _events.send(UnifiedCustomListEvent.ShowError("Cannot save list"))
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error saving list", e)
        _uiState.value = currentState.copy(error = "Error saving: ${e.message}")
      }
    }
  }

  fun onSaveContent(content: String) {
    Log.d(TAG, "SaveContent called with content length: ${content.length}")
    viewModelScope.launch {
      val currentState = _uiState.value
      val list = currentState.list
      if (list != null) {
        try {
          val updatedList = list.copy(content = content)
          projectRepository.updateCustomList(updatedList)
          _uiState.value = currentState.copy(content = TextFieldValue(content), list = updatedList)
          Log.d(TAG, "Content saved successfully")
        } catch (e: Exception) {
          Log.e(TAG, "Error saving content", e)
          _events.send(UnifiedCustomListEvent.ShowError("Failed to save content"))
        }
      } else {
        Log.e(TAG, "Cannot save content: list is null")
      }
    }
  }
}
