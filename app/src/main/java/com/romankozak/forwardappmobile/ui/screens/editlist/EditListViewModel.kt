// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/editlist/EditListViewModel.kt

package com.romankozak.forwardappmobile.ui.screens.editlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditListViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val listId: String = checkNotNull(savedStateHandle["listId"])

    val list: StateFlow<GoalList?> = goalRepository.getGoalListByIdFlow(listId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )

    fun onSave(newName: String, newTags: List<String>) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val currentList = list.first() ?: return@launch
            val updatedList = currentList.copy(
                name = newName,
                tags = newTags.filter { it.isNotBlank() }.map { it.trim() },
                updatedAt = System.currentTimeMillis(),
            )
            goalRepository.updateGoalList(updatedList)
        }
    }
}