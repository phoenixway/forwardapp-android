package com.romankozak.forwardappmobile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class GoalEditEvent {
    data class NavigateBack(val message: String? = null) : GoalEditEvent()
}

data class GoalEditUiState(
    val goalText: String = "",
    val goalDescription: String = "",
    val associatedLists: List<GoalList> = emptyList(),
    val isReady: Boolean = false,
    val isNewGoal: Boolean = true,
    val showListChooser: Boolean = false
)

@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String? = savedStateHandle["goalId"]
    private val initialListId: String? = savedStateHandle["listId"]

    private val _uiState = MutableStateFlow(GoalEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GoalEditEvent>()
    val events = _events.receiveAsFlow()

    private var currentGoal: Goal? = null

    val listHierarchy: StateFlow<ListHierarchyData> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevel = allLists.filter { it.parentId == null }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchyData(allLists, topLevel, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    init {
        viewModelScope.launch {
            if (goalId != null) {
                // Редагування існуючої цілі
                val goal = goalRepository.getGoalById(goalId)
                if (goal != null) {
                    currentGoal = goal
                    val associatedIds = goal.associatedListIds ?: emptyList()
                    val lists = if (associatedIds.isNotEmpty()) goalRepository.getListsByIds(associatedIds) else emptyList()

                    _uiState.update {
                        it.copy(
                            goalText = goal.text,
                            goalDescription = goal.description ?: "",
                            associatedLists = lists,
                            isReady = true,
                            isNewGoal = false
                        )
                    }
                } else {
                    _events.send(GoalEditEvent.NavigateBack("Ціль не знайдено"))
                }
            } else {
                // Створення нової цілі
                _uiState.update {
                    it.copy(
                        goalText = savedStateHandle.get<String>("text") ?: "",
                        associatedLists = emptyList(),
                        isReady = true,
                        isNewGoal = true
                    )
                }
            }
        }
    }

    fun onTextChange(newText: String) {
        _uiState.update { it.copy(goalText = newText) }
    }

    fun onDescriptionChange(newDescription: String) {
        _uiState.update { it.copy(goalDescription = newDescription) }
    }

    fun onAddListAssociation(listId: String) {
        viewModelScope.launch {
            if (_uiState.value.associatedLists.any { it.id == listId }) return@launch
            val listToAdd = listHierarchy.value.allLists.find { it.id == listId }
            if (listToAdd != null) {
                _uiState.update {
                    it.copy(associatedLists = it.associatedLists + listToAdd)
                }
            }
        }
        onDismissListChooser()
    }

    fun onRemoveListAssociation(listId: String) {
        _uiState.update {
            it.copy(associatedLists = it.associatedLists.filterNot { it.id == listId })
        }
    }

    fun onShowListChooser() {
        _uiState.update { it.copy(showListChooser = true) }
    }

    fun onDismissListChooser() {
        _uiState.update { it.copy(showListChooser = false) }
    }

    fun onSave() {
        viewModelScope.launch {
            if (_uiState.value.goalText.isBlank()) {
                _events.send(GoalEditEvent.NavigateBack("Назва цілі не може бути пустою"))
                return@launch
            }

            val associatedIds = _uiState.value.associatedLists.map { it.id }

            if (currentGoal != null) {
                val updatedGoal = currentGoal!!.copy(
                    text = _uiState.value.goalText,
                    description = _uiState.value.goalDescription.ifBlank { null },
                    updatedAt = System.currentTimeMillis(),
                    associatedListIds = associatedIds
                )
                goalRepository.updateGoal(updatedGoal)
            } else {
                val listIdForNewGoal = initialListId
                if (listIdForNewGoal == null) {
                    _events.send(GoalEditEvent.NavigateBack("Не вдалося створити ціль: невідомий список."))
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val newGoal = Goal(
                    id = UUID.randomUUID().toString(),
                    text = _uiState.value.goalText,
                    description = _uiState.value.goalDescription.ifBlank { null },
                    completed = false,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    tags = null,
                    associatedListIds = associatedIds.ifEmpty { listOf(listIdForNewGoal) }
                )
                goalRepository.insertGoal(newGoal)

                val order = goalRepository.getGoalCountInList(listIdForNewGoal).toLong()
                val newInstance = GoalInstance(
                    instanceId = UUID.randomUUID().toString(),
                    goalId = newGoal.id,
                    listId = listIdForNewGoal,
                    order = order
                )
                goalRepository.insertInstance(newInstance)
            }

            _events.send(GoalEditEvent.NavigateBack("Збережено"))
        }
    }
}