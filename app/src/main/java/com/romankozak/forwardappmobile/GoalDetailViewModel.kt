package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class ResetSwipeState(val instanceId: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}

sealed class GoalActionType {
    object CreateInstance : GoalActionType()
    object MoveInstance : GoalActionType()
    object CopyGoal : GoalActionType()
}

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val goalWithInstance: GoalWithInstanceInfo) : GoalActionDialogState()
    data class AwaitingListChoice(val goalWithInstance: GoalWithInstanceInfo, val actionType: GoalActionType) : GoalActionDialogState()
}

enum class InputMode {
    AddGoal,
    SearchInList,
    SearchGlobal
}

data class GoalDetailUiState(
    val goalList: GoalList? = null,
    val isLoading: Boolean = true,
    val inputMode: InputMode = InputMode.AddGoal,
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null
)

data class ListHierarchy(
    val allLists: List<GoalList> = emptyList(),
    val topLevelLists: List<GoalList> = emptyList(),
    val childMap: Map<String, List<GoalList>> = emptyMap()
)

class GoalDetailViewModel(
    private val goalDao: GoalDao,
    private val listId: String,
    private val highlightedGoalId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    private val _uiEventChannel = Channel<UiEvent>()
    val uiEventFlow = _uiEventChannel.receiveAsFlow()

    private var lastDeletedGoal: Goal? = null
    private var lastDeletedInstance: GoalInstance? = null

    private val _goalsWithInstances = goalDao.getGoalsForList(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredGoals: StateFlow<List<GoalWithInstanceInfo>> =
        combine(_goalsWithInstances, uiState.map { it.localSearchQuery }.distinctUntilChanged()) { goals, query ->
            if (query.isBlank()) {
                goals
            } else {
                goals.filter {
                    it.goal.text.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    private val _showInputModeDialog = MutableStateFlow(false)
    val showInputModeDialog = _showInputModeDialog.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(
                goalList = goalDao.getList(listId).first(),
                isLoading = false,
                goalToHighlight = highlightedGoalId
            ) }
        }
    }

    fun onHighlightShown() {
        viewModelScope.launch {
            delay(2000)
            _uiState.update { it.copy(goalToHighlight = null) }
        }
    }

    // --- ДОДАНО: Функція для обробки кліку на тег ---
    fun onTagClicked(tag: String) {
        viewModelScope.launch {
            _uiEventChannel.send(UiEvent.Navigate("global_search_screen/$tag"))
        }
    }

    // --- КЕРУВАННЯ РЕЖИМАМИ ТА ВВОДОМ ---

    fun onInputModeChangeRequest() {
        _showInputModeDialog.value = true
    }

    fun onDismissInputModeDialog() {
        _showInputModeDialog.value = false
    }

    fun onInputModeSelected(mode: InputMode) {
        resetToAddMode()
        _uiState.update { it.copy(inputMode = mode) }
        _showInputModeDialog.value = false
    }

    fun onInputTextChanged(text: String) {
        val currentMode = _uiState.value.inputMode
        if (currentMode == InputMode.SearchInList) {
            if (text.isBlank()) {
                resetToAddMode()
            } else {
                _uiState.update { it.copy(localSearchQuery = text) }
            }
        }
    }

    fun submitInput(text: String) {
        viewModelScope.launch {
            when (_uiState.value.inputMode) {
                InputMode.AddGoal -> {
                    if (text.isNotBlank()) {
                        addGoal(text)
                    }
                }
                InputMode.SearchInList -> {
                    resetToAddMode()
                }
                InputMode.SearchGlobal -> {
                    if (text.isNotBlank()) {
                        _uiEventChannel.send(UiEvent.Navigate("global_search_screen/$text"))
                    }
                    resetToAddMode()
                }
            }
        }
    }

    fun resetToAddMode() {
        _uiState.update { it.copy(inputMode = InputMode.AddGoal, localSearchQuery = "") }
    }

    private fun addGoal(text: String) {
        viewModelScope.launch {
            val newOrderIndex = filteredGoals.value.size
            val currentTime = System.currentTimeMillis()
            val newGoal = Goal(
                id = UUID.randomUUID().toString(),
                text = text,
                completed = false,
                createdAt = currentTime,
                updatedAt = currentTime,
                description = "",
                tags = null
            )
            val newInstance = GoalInstance(
                id = UUID.randomUUID().toString(),
                goalId = newGoal.id,
                listId = listId,
                orderIndex = newOrderIndex
            )
            goalDao.insertGoal(newGoal)
            goalDao.insertGoalInstance(newInstance)
        }
    }

    // --- РЕШТА ФУНКЦІЙ ViewModel (без змін) ---
    fun deleteGoal(goalWithInstance: GoalWithInstanceInfo) {
        viewModelScope.launch {
            lastDeletedGoal = goalWithInstance.goal
            lastDeletedInstance = GoalInstance(
                id = goalWithInstance.instanceId,
                goalId = goalWithInstance.goal.id,
                listId = listId,
                orderIndex = goalWithInstance.orderIndex
            )
            goalDao.deleteGoalInstanceById(goalWithInstance.instanceId)
            _uiEventChannel.send(UiEvent.ShowSnackbar(message = "Ціль видалено", action = "Скасувати"))
        }
    }

    fun undoDelete() {
        val goalToRestore = lastDeletedGoal
        val instanceToRestore = lastDeletedInstance
        if (goalToRestore != null && instanceToRestore != null) {
            viewModelScope.launch {
                goalDao.insertGoal(goalToRestore)
                goalDao.insertGoalInstance(instanceToRestore)
            }
        }
        lastDeletedGoal = null
        lastDeletedInstance = null
    }

    fun updateGoalText(goal: Goal, newText: String) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(text = newText, updatedAt = System.currentTimeMillis())
            goalDao.updateGoal(updatedGoal)
            onDismissEditGoalDialog()
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(completed = !goal.completed, updatedAt = System.currentTimeMillis())
            goalDao.updateGoal(updatedGoal)
        }
    }

    fun moveGoal(from: Int, to: Int) {
        viewModelScope.launch {
            val currentGoals = filteredGoals.value.toMutableList()
            if (from in currentGoals.indices && to in currentGoals.indices) {
                val movedItem = currentGoals.removeAt(from)
                currentGoals.add(to, movedItem)
                val updatedInstances = currentGoals.mapIndexed { index, item ->
                    GoalInstance(item.instanceId, item.goal.id, listId, index)
                }
                goalDao.updateGoalInstances(updatedInstances)
            }
        }
    }

    val listHierarchy: StateFlow<ListHierarchy> = goalDao.getAllLists()
        .map { flatList ->
            val topLevel = flatList.filter { it.parentId == null }
            val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchy(allLists = flatList, topLevelLists = topLevel, childMap = childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchy())

    private val _goalToEdit = MutableStateFlow<Goal?>(null)
    val goalToEdit = _goalToEdit.asStateFlow()

    fun onEditGoal(goalWithInstance: GoalWithInstanceInfo) {
        _goalToEdit.value = goalWithInstance.goal
        viewModelScope.launch {
            _uiEventChannel.send(UiEvent.ResetSwipeState(goalWithInstance.instanceId))
        }
    }

    fun onDismissEditGoalDialog() {
        _goalToEdit.value = null
    }

    private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
    val goalActionDialogState: StateFlow<GoalActionDialogState> = _goalActionDialogState.asStateFlow()

    fun onGoalActionInitiated(goalWithInstance: GoalWithInstanceInfo) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingActionChoice(goalWithInstance)
        viewModelScope.launch {
            _uiEventChannel.send(UiEvent.ResetSwipeState(goalWithInstance.instanceId))
        }
    }

    fun onActionSelected(actionType: GoalActionType) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingActionChoice) {
            _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(currentState.goalWithInstance, actionType)
        }
    }

    fun onDismissGoalActionDialogs() {
        _goalActionDialogState.value = GoalActionDialogState.Hidden
    }

    fun confirmGoalAction(destinationListId: String) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingListChoice) {
            if (destinationListId == listId) {
                onDismissGoalActionDialogs()
                return
            }
            viewModelScope.launch {
                when (currentState.actionType) {
                    is GoalActionType.MoveInstance -> moveInstanceToList(currentState.goalWithInstance, destinationListId)
                    is GoalActionType.CreateInstance -> createInstanceInList(currentState.goalWithInstance.goal, destinationListId)
                    is GoalActionType.CopyGoal -> copyGoalToList(currentState.goalWithInstance.goal, destinationListId)
                }
            }
        }
        onDismissGoalActionDialogs()
    }

    private suspend fun moveInstanceToList(goalWithInstance: GoalWithInstanceInfo, destinationListId: String) {
        val newOrderIndex = goalDao.getGoalCountInList(destinationListId)
        goalDao.moveGoalToNewList(goalWithInstance.instanceId, destinationListId, newOrderIndex)
    }

    private suspend fun createInstanceInList(sourceGoal: Goal, destinationListId: String) {
        val newOrderIndex = goalDao.getGoalCountInList(destinationListId)
        val newInstance = GoalInstance(
            id = UUID.randomUUID().toString(),
            goalId = sourceGoal.id,
            listId = destinationListId,
            orderIndex = newOrderIndex
        )
        goalDao.insertGoalInstance(newInstance)
    }

    private suspend fun copyGoalToList(sourceGoal: Goal, destinationListId: String) {
        val currentTime = System.currentTimeMillis()
        val newGoal = sourceGoal.copy(
            id = UUID.randomUUID().toString(),
            createdAt = currentTime,
            updatedAt = currentTime
        )
        goalDao.insertGoal(newGoal)
        val newOrderIndex = goalDao.getGoalCountInList(destinationListId)
        val newInstance = GoalInstance(
            id = UUID.randomUUID().toString(),
            goalId = newGoal.id,
            listId = destinationListId,
            orderIndex = newOrderIndex
        )
        goalDao.insertGoalInstance(newInstance)
    }
}