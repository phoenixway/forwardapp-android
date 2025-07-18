package com.romankozak.forwardappmobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ResetSwipeState(val instanceId: String): UiEvent()
}

enum class InputMode {
    AddGoal, SearchInList, SearchGlobal
}

data class GoalDetailUiState(
    val isLoading: Boolean = true,
    val goalList: GoalList? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null
)

sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val goalWithInstance: GoalWithInstanceInfo) : GoalActionDialogState()
    data class AwaitingListChoice(val goalWithInstance: GoalWithInstanceInfo, val actionType: GoalActionType) : GoalActionDialogState()
}
enum class GoalActionType {
    CreateInstance, MoveInstance, CopyGoal
}

class GoalDetailViewModel(
    private val goalDao: GoalDao,
    private val goalListDao: GoalListDao,
    settingsRepo: SettingsRepository,
    private val listId: String,
    private val highlightedGoalId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalDetailUiState(goalToHighlight = highlightedGoalId))
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEvent.asSharedFlow()

    private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
    val goalActionDialogState = _goalActionDialogState.asStateFlow()

    private val _showInputModeDialog = MutableStateFlow(false)
    val showInputModeDialog = _showInputModeDialog.asStateFlow()

    private var recentlyDeletedGoal: GoalWithInstanceInfo? = null

    private val allGoalsForList = goalDao.getGoalsForListStream(listId)

    val filteredGoals: StateFlow<List<GoalWithInstanceInfo>> = combine(
        allGoalsForList,
        _uiState
    ) { goals, state ->
        if (state.localSearchQuery.isBlank()) {
            goals
        } else {
            goals.filter { it.goal.text.contains(state.localSearchQuery, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val associatedListsMap: StateFlow<Map<String, List<GoalList>>> = allGoalsForList.flatMapLatest { goals ->
        val goalIds = goals.map { it.goal.id }
        if (goalIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyMap())
        } else {
            goalDao.getAssociatedListsForGoals(goalIds)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- ВИПРАВЛЕНО: Використовуємо новий, єдиний клас ListHierarchyData ---
    val listHierarchyForChooser: StateFlow<ListHierarchyData> = goalListDao.getAllLists()
        .combine(goalListDao.getGoalListByIdStream(listId)) { allLists, currentList ->
            val allButCurrent = allLists.filter { it.id != currentList?.id }
            val topLevel = allButCurrent.filter { it.parentId == null }
            val childMap = allButCurrent
                .filter { it.parentId != null }
                .groupBy { it.parentId!! }
            ListHierarchyData(allLists, topLevel, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val obsidianVaultName: StateFlow<String> = settingsRepo.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        viewModelScope.launch {
            val goalList = goalListDao.getGoalListById(listId)
            _uiState.update { it.copy(isLoading = false, goalList = goalList) }
        }
    }
    fun onInputTextChanged(text: String) {
        if (_uiState.value.inputMode == InputMode.SearchInList) {
            _uiState.update { it.copy(localSearchQuery = text) }
        }
    }

    fun onInputModeChangeRequest() {
        _showInputModeDialog.value = true
    }

    fun onInputModeSelected(mode: InputMode) {
        _showInputModeDialog.value = false
        _uiState.update { it.copy(inputMode = mode, localSearchQuery = "") }
    }

    fun onDismissInputModeDialog() {
        _showInputModeDialog.value = false
    }

    fun submitInput(text: String) {
        val currentInputMode = uiState.value.inputMode
        viewModelScope.launch {
            when (currentInputMode) {
                InputMode.AddGoal -> {
                    val minOrder = goalDao.getMinOrderForList(listId)
                    val newOrder = (minOrder ?: 0L) - 1

                    val newGoal = Goal(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        completed = false,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    val newInstance = GoalInstance(
                        instanceId = UUID.randomUUID().toString(),
                        goalId = newGoal.id,
                        listId = listId,
                        order = newOrder
                    )

                    goalDao.insertGoalWithInstance(newGoal, newInstance)
                }

                InputMode.SearchInList -> {
                    _uiState.update { it.copy(localSearchQuery = text) }
                }

                InputMode.SearchGlobal -> {
                    val encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
                    _uiEvent.emit(UiEvent.Navigate("global_search_screen/$encodedText"))
                }
            }
        }
    }

    fun onEditGoal(goalWithInstance: GoalWithInstanceInfo) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Navigate("goal_edit_screen?listId=$listId&goalId=${goalWithInstance.goal.id}"))
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            goalDao.updateGoal(
                goal.copy(
                    completed = !goal.completed,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun moveGoal(from: Int, to: Int) {
        viewModelScope.launch {
            val currentList = filteredGoals.value.toMutableList()
            if (from < 0 || from >= currentList.size || to < 0 || to >= currentList.size) return@launch

            val item = currentList.removeAt(from)
            currentList.add(to, item)

            val newOrderList = currentList.mapIndexed { index, goalWithInstance ->
                goalWithInstance.copy(order = index.toLong())
            }

            goalDao.updateInstancesOrder(newOrderList)
        }
    }

    fun deleteGoal(goalWithInstance: GoalWithInstanceInfo) {
        viewModelScope.launch {
            recentlyDeletedGoal = goalWithInstance
            goalDao.deleteInstanceById(goalWithInstance.instanceId)
            _uiEvent.emit(UiEvent.ShowSnackbar(message = "Ціль видалено", action = "Скасувати"))
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            recentlyDeletedGoal?.let {
                goalDao.restoreInstance(it.toGoalInstance())
                recentlyDeletedGoal = null
            }
        }
    }

    fun onHighlightShown() {
        _uiState.update { it.copy(goalToHighlight = null) }
    }

    fun resetSwipe(instanceId: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ResetSwipeState(instanceId))
        }
    }

    fun onTagClicked(tag: String) {
        viewModelScope.launch {
            if (_uiState.value.inputMode != InputMode.SearchGlobal) {
                onInputModeSelected(InputMode.SearchInList)
            }
            _uiState.update { it.copy(localSearchQuery = tag) }
        }
    }

    fun onGoalActionInitiated(goalWithInstance: GoalWithInstanceInfo) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingActionChoice(goalWithInstance)
    }

    fun onGoalActionSelected(actionType: GoalActionType) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingActionChoice) {
            _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(currentState.goalWithInstance, actionType)
        }
    }

    fun onDismissGoalActionDialogs() {
        _goalActionDialogState.value = GoalActionDialogState.Hidden
    }

    fun confirmGoalAction(targetListId: String) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingListChoice) {
            when(currentState.actionType) {
                GoalActionType.CreateInstance -> createInstanceInList(currentState.goalWithInstance.goal, targetListId)
                GoalActionType.MoveInstance -> moveInstanceToList(currentState.goalWithInstance, targetListId)
                GoalActionType.CopyGoal -> copyGoalToList(currentState.goalWithInstance.goal, targetListId)
            }
        }
        onDismissGoalActionDialogs()
    }

    private fun createInstanceInList(goal: Goal, targetListId: String) {
        viewModelScope.launch {
            val newInstance = GoalInstance(
                instanceId = UUID.randomUUID().toString(),
                goalId = goal.id,
                listId = targetListId,
                order = System.currentTimeMillis()
            )
            goalDao.insertInstance(newInstance)
            _uiEvent.emit(UiEvent.ShowSnackbar("Екземпляр цілі створено"))
        }
    }

    private fun moveInstanceToList(goalWithInstance: GoalWithInstanceInfo, targetListId: String) {
        viewModelScope.launch {
            goalDao.updateInstanceListId(goalWithInstance.instanceId, targetListId)
            _uiEvent.emit(UiEvent.ShowSnackbar("Екземпляр переміщено"))
        }
    }

    private fun copyGoalToList(goal: Goal, targetListId: String) {
        viewModelScope.launch {
            val newGoal = goal.copy(
                id = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newInstance = GoalInstance(
                instanceId = UUID.randomUUID().toString(),
                goalId = newGoal.id,
                listId = targetListId,
                order = System.currentTimeMillis()
            )
            goalDao.insertGoalWithInstance(newGoal, newInstance)
            _uiEvent.emit(UiEvent.ShowSnackbar("Ціль скопійовано"))
        }
    }

    fun onAssociatedListClicked(listId: String) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Navigate("goal_detail_screen/$listId"))
        }
    }
}