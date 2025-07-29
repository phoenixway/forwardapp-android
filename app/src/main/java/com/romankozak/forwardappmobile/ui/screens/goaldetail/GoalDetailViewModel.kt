package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import com.romankozak.forwardappmobile.data.database.models.toGoalInstance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.iterator

// --- ДОПОМІЖНІ КЛАСИ ТА СТАНИ ---
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ResetSwipeState(val instanceId: String) : UiEvent()
    data class ScrollTo(val index: Int) : UiEvent()
}
sealed class GoalActionDialogState {
    object Hidden : GoalActionDialogState()
    data class AwaitingActionChoice(val goalWithInstance: GoalWithInstanceInfo) : GoalActionDialogState()
    data class AwaitingListChoice(val goalWithInstance: GoalWithInstanceInfo, val actionType: GoalActionType) : GoalActionDialogState()
}
enum class GoalActionType { CreateInstance, MoveInstance, CopyGoal, MoveToTop }
enum class InputMode { AddGoal, SearchInList, SearchGlobal }

// ✨ ЗМІНА: Додано поле для відстеження ID нової цілі
data class UiState(
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val newlyAddedGoalInstanceId: String? = null
)
data class ListHierarchy(
    val topLevelLists: List<GoalList>,
    val childMap: Map<String, List<GoalList>>
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("goalListId", "")

    init {
        viewModelScope.launch {
            listIdFlow.collect { id ->
                Log.d("DEBUG_VM", "Отримано новий listId зі StateHandle: '$id'")
            }
        }
    }


    private val _uiState = MutableStateFlow(UiState(
        goalToHighlight = savedStateHandle.get<String>("goalToHighlight")
    ))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEventFlow = Channel<UiEvent>()
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    private var recentlyDeletedGoal: GoalWithInstanceInfo? = null

    val goalList: StateFlow<GoalList?> = listIdFlow.flatMapLatest { id ->
        if (id.isNotEmpty()) goalRepository.getGoalListByIdFlow(id) else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val filteredGoals: StateFlow<List<GoalWithInstanceInfo>> =
        combine(listIdFlow, _uiState) { id, state -> Pair(id, state) }
            .flatMapLatest { (id, state) ->
                if (id.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    goalRepository.getGoalsForListStream(id).map { goals ->
                        if (state.inputMode == InputMode.SearchInList && state.localSearchQuery.isNotBlank()) {
                            goals.filter { it.goal.text.lowercase().contains(state.localSearchQuery.lowercase()) }
                        } else {
                            goals
                        }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val associatedListsMap: StateFlow<Map<String, List<GoalList>>> = filteredGoals.flatMapLatest { goals ->
        val goalIds = goals.map { it.goal.id }.distinct()
        goalRepository.getAssociatedListsForGoals(goalIds)
            .map { fullMap ->
                val currentListId = listIdFlow.value
                val filteredMap = mutableMapOf<String, List<GoalList>>()
                for ((goalId, lists) in fullMap) {
                    filteredMap[goalId] = lists.filter { it.id != currentListId }
                }
                filteredMap
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    private val _goalActionDialogState = MutableStateFlow<GoalActionDialogState>(GoalActionDialogState.Hidden)
    val goalActionDialogState: StateFlow<GoalActionDialogState> = _goalActionDialogState.asStateFlow()

    private val _showInputModeDialog = MutableStateFlow(false)
    val showInputModeDialog: StateFlow<Boolean> = _showInputModeDialog.asStateFlow()

    val obsidianVaultName: StateFlow<String> = settingsRepository.obsidianVaultNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val listHierarchyForChooser: StateFlow<ListHierarchy> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevelLists = allLists.filter { it.parentId == null }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchy(topLevelLists, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchy(emptyList(), emptyMap()))

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(localSearchQuery = text) }
    }

    fun submitInput(text: String) {
        when (_uiState.value.inputMode) {
            InputMode.AddGoal -> addGoal(text)
            InputMode.SearchInList -> { /* Пошук вже виконується через combine */ }
            InputMode.SearchGlobal -> {
                viewModelScope.launch { _uiEventFlow.send(UiEvent.Navigate("global_search_screen/$text")) }
            }
        }
    }

    // ✨ ЗМІНА: Тепер метод зберігає ID нової цілі в UiState
    private fun addGoal(title: String) {
        val listId = listIdFlow.value
        if (listId.isEmpty()) return
        viewModelScope.launch {
            val newInstanceId = goalRepository.createGoal(title, listId)
            // Оновлюємо стан, щоб UI знав про нову ціль
            _uiState.update { it.copy(newlyAddedGoalInstanceId = newInstanceId) }
        }
    }

    // ✨ ЗМІНА: Додано новий метод, щоб UI міг повідомити, що скрол виконано
    fun onScrolledToNewGoal() {
        _uiState.update { it.copy(newlyAddedGoalInstanceId = null) }
    }

    fun deleteGoal(goal: GoalWithInstanceInfo) {
        viewModelScope.launch {
            goalRepository.deleteGoalInstance(goal.instanceId)
            recentlyDeletedGoal = goal
            _uiEventFlow.send(UiEvent.ShowSnackbar(message = "Ціль видалено", action = "Скасувати"))
        }
    }

    fun undoDelete() {
        recentlyDeletedGoal?.let {
            viewModelScope.launch {
                goalRepository.insertInstance(it.toGoalInstance())
                resetSwipe(it.instanceId)
                recentlyDeletedGoal = null
            }
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(completed = !goal.completed))
        }
    }

   fun moveGoal(from: Int, to: Int, needsScroll: Boolean) {
        val currentGoals = filteredGoals.value.toMutableList()
        if (from in currentGoals.indices && to in currentGoals.indices) {
            val item = currentGoals.removeAt(from)
            currentGoals.add(to, item)
            viewModelScope.launch {
                val updatedInstances = currentGoals.mapIndexed { index, goalWithInstanceInfo ->
                    goalWithInstanceInfo.copy(order = index.toLong())
                }
                updatedInstances.forEach { goalRepository.insertInstance(it.toGoalInstance()) }

                // ✨ ЗМІНА №2: Виконуємо скрол тільки якщо needsScroll == true
                if (needsScroll) {
                    _uiEventFlow.send(UiEvent.ScrollTo(to))
                }
            }
        }
    }

    fun onEditGoal(goal: GoalWithInstanceInfo) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("goal_edit_screen/${goal.listId}/${goal.goal.id}"))
        }
    }

    fun onModeChangeRequest() {
        _showInputModeDialog.value = true
    }

    fun onTagClicked(tag: String) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("global_search_screen/%23$tag"))
        }
    }

    fun onAssociatedListClicked(listId: String) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("goal_detail_screen/$listId"))
        }
    }

    fun onHighlightShown() {
        _uiState.update { it.copy(goalToHighlight = null) }
    }

    fun onGoalActionInitiated(goal: GoalWithInstanceInfo) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingActionChoice(goal)
    }

    fun onGoalActionSelected(actionType: GoalActionType) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingActionChoice) {
            val goalToActOn = currentState.goalWithInstance

            when (actionType) {
                GoalActionType.MoveToTop -> {
                    val fromIndex = filteredGoals.value.indexOfFirst { it.instanceId == goalToActOn.instanceId }
                    if (fromIndex > 0) {
                        // ✨ ВИПРАВЛЕННЯ: Додаємо needsScroll = true
                        moveGoal(fromIndex, 0, needsScroll = true)
                    }
                    onDismissGoalActionDialogs()
                }
                else -> {
                    _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(goalToActOn, actionType)
                }
            }
        }
    }

    fun onDismissGoalActionDialogs() {
        val currentState = _goalActionDialogState.value
        val goalInstanceId = when (currentState) {
            is GoalActionDialogState.AwaitingActionChoice -> currentState.goalWithInstance.instanceId
            is GoalActionDialogState.AwaitingListChoice -> currentState.goalWithInstance.instanceId
            else -> null
        }
        _goalActionDialogState.value = GoalActionDialogState.Hidden
        goalInstanceId?.let { resetSwipe(it) }
    }

    fun confirmGoalAction(targetListId: String) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingListChoice) {
            val goal = currentState.goalWithInstance
            val actionType = currentState.actionType
            viewModelScope.launch {
                when (actionType) {
                    GoalActionType.CreateInstance -> goalRepository.createGoalInstance(goal.goal.id, targetListId)
                    GoalActionType.MoveInstance -> goalRepository.moveGoalInstance(goal.instanceId, targetListId)
                    GoalActionType.CopyGoal -> goalRepository.copyGoal(goal.goal, targetListId)
                    GoalActionType.MoveToTop -> { /* Handled in onGoalActionSelected */ }
                }
                _goalActionDialogState.value = GoalActionDialogState.Hidden
                resetSwipe(goal.instanceId)
            }
        }
    }

    fun resetSwipe(instanceId: String) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.ResetSwipeState(instanceId))
        }
    }

    fun onInputModeChangeRequest() {
        _showInputModeDialog.value = true
    }

    fun onDismissInputModeDialog() {
        _showInputModeDialog.value = false
    }

    fun onInputModeSelected(mode: InputMode) {
        _uiState.update { it.copy(inputMode = mode, localSearchQuery = "") }
        _showInputModeDialog.value = false
    }
}