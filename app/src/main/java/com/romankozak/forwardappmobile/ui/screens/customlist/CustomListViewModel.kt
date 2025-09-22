package com.romankozak.forwardappmobile.ui.screens.customlist

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.CustomListEntity
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomListUiState(
    val list: CustomListEntity? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CustomListViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(CustomListUiState())
    val uiState = _uiState.asStateFlow()

    private val TAG = "CUSTOM_LIST_DEBUG"

    init {
        Log.d(TAG, "CustomListViewModel init for listId: $listId")
        viewModelScope.launch {
            val list = projectRepository.getCustomListById(listId)
            Log.d(TAG, "CustomListViewModel loaded list: $list")
            _uiState.value = CustomListUiState(list = list, isLoading = false)
        }
    }

    fun onSaveContent(content: String) {
        Log.d(TAG, "CustomListViewModel onSaveContent called with content: $content")
        viewModelScope.launch {
            val list = _uiState.value.list
            if (list != null) {
                Log.d(TAG, "CustomListViewModel updating list with new content")
                projectRepository.updateCustomList(list.copy(content = content))
                Log.d(TAG, "CustomListViewModel update finished")
            } else {
                Log.d(TAG, "CustomListViewModel onSaveContent: list is null, cannot save")
            }
        }
    }
}