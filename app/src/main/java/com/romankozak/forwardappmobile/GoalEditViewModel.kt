package com.romankozak.forwardappmobile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

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

// --- КЛАС `ListHierarchyForChooser` ВИДАЛЕНО ЗВІДСИ ---

class GoalEditViewModel(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val goalId: String? = savedStateHandle["goalId"]
    private val initialListId: String = savedStateHandle["listId"]!!

    private val _uiState = MutableStateFlow(GoalEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GoalEditEvent>()
    val events = _events.receiveAsFlow()

    private var currentGoal: Goal? = null

    // --- ВИПРАВЛЕНО: Використовуємо новий, єдиний клас ListHierarchyData ---
    val listHierarchy: MutableStateFlow<ListHierarchyData> = MutableStateFlow(ListHierarchyData())

    init {
        viewModelScope.launch {
            if (goalId != null) {
                val goal = goalDao.getGoalById(goalId)
                if (goal != null) {
                    currentGoal = goal
                    val associatedIds = goal.associatedListIds ?: emptyList()
                    val lists = if (associatedIds.isNotEmpty()) goalListDao.getListsByIds(associatedIds) else emptyList()

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
                _uiState.update {
                    it.copy(
                        goalText = "",
                        associatedLists = emptyList(),
                        isReady = true,
                        isNewGoal = true
                    )
                }
            }

            goalListDao.getAllLists().collect { allLists ->
                val topLevel = allLists.filter { it.parentId == null }
                val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
                listHierarchy.value = ListHierarchyData(allLists, topLevel, childMap)
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
                    description = _uiState.value.goalDescription,
                    updatedAt = System.currentTimeMillis(),
                    associatedListIds = associatedIds
                )
                goalDao.updateGoal(updatedGoal)
            } else {
                val currentTime = System.currentTimeMillis()
                val newGoal = Goal(
                    id = UUID.randomUUID().toString(),
                    text = _uiState.value.goalText,
                    description = _uiState.value.goalDescription,
                    completed = false,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    tags = null,
                    associatedListIds = associatedIds.ifEmpty { listOf(initialListId) }
                )
                goalDao.insertGoal(newGoal)

                val order = goalDao.getGoalCountInList(initialListId).toLong()
                val newInstance = GoalInstance(
                    instanceId = UUID.randomUUID().toString(),
                    goalId = newGoal.id,
                    listId = initialListId,
                    order = order
                )
                goalDao.insertInstance(newInstance)
            }

            _events.send(GoalEditEvent.NavigateBack("Збережено"))
        }
    }
}