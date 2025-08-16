package com.romankozak.forwardappmobile.ui.screens.goaldetail

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.GoalWithInstanceInfo
import com.romankozak.forwardappmobile.data.database.models.toGoalInstance
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.romankozak.forwardappmobile.ui.utils.HierarchyFilter // Імпорт нашого хелпера
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData // Оновлений імпорт


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
    data class AwaitingListChoice(
        val sourceInstanceIds: Set<String>,
        val actionType: GoalActionType
    ) : GoalActionDialogState()
}

enum class GoalActionType { CreateInstance, MoveInstance, CopyGoal, MoveToTop }
enum class InputMode { AddGoal, SearchInList, SearchGlobal }

data class UiState(
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val newlyAddedGoalInstanceId: String? = null,
    val selectedInstanceIds: Set<String> = emptySet(),
    val inputValue: TextFieldValue = TextFieldValue(""),
    val resetTriggers: Map<String, Int> = emptyMap(),
    val swipedInstanceId: String? = null
)
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val contextHandler: ContextHandler,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listIdFlow: StateFlow<String> = savedStateHandle.getStateFlow("listId", "")

    private val _uiState = MutableStateFlow(UiState(
        goalToHighlight = savedStateHandle.get<String>("goalToHighlight")
    ))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEventFlow = Channel<UiEvent>()
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    private var recentlyDeletedGoal: GoalWithInstanceInfo? = null
    private var recentlyDeletedInstances: List<GoalWithInstanceInfo>? = null

    val allContextNames: StateFlow<List<String>> = contextHandler.contextNamesFlow

    init {
        viewModelScope.launch {
            contextHandler.initialize()
        }
    }

    val isSelectionModeActive: StateFlow<Boolean> = _uiState
        .map { it.selectedInstanceIds.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    private val _listChooserFilterText = MutableStateFlow("")
    val listChooserFilterText = _listChooserFilterText.asStateFlow()

    // ✨ КРОК 1: Перейменовуємо стан, щоб він зберігав лише вибір користувача
    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    val listChooserUserExpandedIds = _listChooserUserExpandedIds.asStateFlow()

    private val fullListHierarchy: StateFlow<ListHierarchyData> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevelLists = allLists.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            // і використовуйте новий клас тут
            ListHierarchyData(allLists, topLevelLists, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData(emptyList(), emptyList(), emptyMap()))


    val fullHierarchyForDialog: StateFlow<ListHierarchyData> = fullListHierarchy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val filteredListHierarchyForDialog: StateFlow<ListHierarchyData> =
        combine(listChooserFilterText, fullHierarchyForDialog) { filter, originalHierarchy ->
            HierarchyFilter.filter(originalHierarchy, filter)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

     // Подальший код залишається без змін
    // ✨ КРОК 2: Створюємо новий фінальний стан для UI, який комбінує вибір користувача та примусово розгорнуті елементи
    val listChooserFinalExpandedIds: StateFlow<Set<String>> = combine(
        listChooserFilterText,
        filteredListHierarchyForDialog,
        listChooserUserExpandedIds
    ) { filter, filteredHierarchy, userExpandedIds ->
        if (filter.isBlank()) {
            userExpandedIds // Якщо фільтр вимкнено, повертаємо стан користувача
        } else {
            // Якщо фільтр активний, беремо всіх видимих батьків і додаємо до вибору користувача
            val allVisibleIds = filteredHierarchy.topLevelLists.map { it.id }.toSet() +
                    filteredHierarchy.childMap.keys
            userExpandedIds + allVisibleIds
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())


    fun onListChooserFilterChanged(text: String) {
        _listChooserFilterText.value = text
    }

    // ✨ КРОК 3: Функція тепер оновлює тільки стан користувача
    fun onListChooserToggleExpanded(listId: String) {
        _listChooserUserExpandedIds.value = _listChooserUserExpandedIds.value.toMutableSet().apply {
            if (listId in this) remove(listId) else add(listId)
        }
    }

    fun onSwipeStart(instanceId: String) {
        if (_uiState.value.swipedInstanceId != instanceId) {
            _uiState.update { it.copy(swipedInstanceId = instanceId) }
        }
    }

    fun onCreateInstanceRequest(goal: GoalWithInstanceInfo) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
            sourceInstanceIds = setOf(goal.instanceId),
            actionType = GoalActionType.CreateInstance
        )
    }

    fun onMoveInstanceRequest(goal: GoalWithInstanceInfo) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
            sourceInstanceIds = setOf(goal.instanceId),
            actionType = GoalActionType.MoveInstance
        )
    }

    fun onCopyGoalRequest(goal: GoalWithInstanceInfo) {
        _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
            sourceInstanceIds = setOf(goal.instanceId),
            actionType = GoalActionType.CopyGoal
        )
    }

    fun onInputTextChanged(newValue: TextFieldValue) {
        _uiState.update {
            it.copy(
                inputValue = newValue,
                localSearchQuery = if (it.inputMode == InputMode.SearchInList) newValue.text else ""
            )
        }
    }

    fun submitInput() {
        val textToSubmit = _uiState.value.inputValue.text
        if (textToSubmit.isBlank()) return

        when (_uiState.value.inputMode) {
            InputMode.AddGoal -> addGoal(textToSubmit)
            InputMode.SearchInList -> { /* Search is handled reactively */ }
            InputMode.SearchGlobal -> {
                viewModelScope.launch { _uiEventFlow.send(UiEvent.Navigate("global_search_screen/$textToSubmit")) }
            }
        }
        _uiState.update { it.copy(inputValue = TextFieldValue("")) }
    }

    private fun addGoal(title: String) {
        val listId = listIdFlow.value
        if (title.isBlank() || listId.isEmpty()) return

        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val newGoal = Goal(
                id = UUID.randomUUID().toString(),
                text = title,
                completed = false,
                createdAt = currentTime,
                updatedAt = currentTime,
                associatedListIds = listOf(listId)
            )
            goalRepository.insertGoal(newGoal)
            val order = goalRepository.getGoalCountInList(listId).toLong()
            val newInstance = GoalInstance(
                instanceId = UUID.randomUUID().toString(),
                goalId = newGoal.id,
                listId = listId,
                order = order
            )
            goalRepository.insertInstance(newInstance)
            contextHandler.handleContextsOnCreate(newGoal)
            _uiState.update { it.copy(newlyAddedGoalInstanceId = newInstance.instanceId) }
        }
    }

    fun onScrolledToNewGoal() {
        _uiState.update { it.copy(newlyAddedGoalInstanceId = null) }
    }

    fun deleteGoal(goal: GoalWithInstanceInfo) {
        viewModelScope.launch {
            recentlyDeletedInstances = null
            recentlyDeletedGoal = goal
            goalRepository.deleteGoalInstance(goal.instanceId)
            _uiEventFlow.send(UiEvent.ShowSnackbar(message = "Ціль видалено", action = "Скасувати"))
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            recentlyDeletedGoal?.let {
                goalRepository.insertInstance(it.toGoalInstance())
                resetSwipe(it.instanceId)
                recentlyDeletedGoal = null
            }

            recentlyDeletedInstances?.let {
                if (it.isNotEmpty()) {
                    val instancesToRestore = it.map { info -> info.toGoalInstance() }
                    goalRepository.insertGoalInstances(instancesToRestore)
                    recentlyDeletedInstances = null
                }
            }
        }
    }

    fun toggleGoalCompleted(goal: Goal) {
        viewModelScope.launch {
            goalRepository.updateGoal(goal.copy(completed = !goal.completed))
        }
    }

    fun moveGoal(fromIndex: Int, toIndex: Int, needsScroll: Boolean) {
        val currentGoals = filteredGoals.value.toMutableList()
        if (fromIndex in currentGoals.indices && toIndex in currentGoals.indices) {
            val item = currentGoals.removeAt(fromIndex)
            currentGoals.add(toIndex, item)
            viewModelScope.launch {
                val updatedInstances = currentGoals.mapIndexed { index, goalWithInstanceInfo ->
                    goalWithInstanceInfo.copy(order = index.toLong())
                }
                updatedInstances.forEach { goalRepository.insertInstance(it.toGoalInstance()) }
                if (needsScroll) {
                    _uiEventFlow.send(UiEvent.ScrollTo(toIndex))
                }
            }
        }
    }

    fun onEditGoal(goal: GoalWithInstanceInfo) {
        viewModelScope.launch {
            _uiEventFlow.send(UiEvent.Navigate("goal_edit_screen/${goal.listId}/${goal.goal.id}"))
        }
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

    fun onGoalLongClick(instanceId: String) {
        _uiState.update {
            it.copy(selectedInstanceIds = it.selectedInstanceIds + instanceId)
        }
    }

    fun onGoalClick(instance: GoalWithInstanceInfo) {
        if (_uiState.value.selectedInstanceIds.isNotEmpty()) {
            _uiState.update {
                val currentSelection = it.selectedInstanceIds
                if (instance.instanceId in currentSelection) {
                    it.copy(selectedInstanceIds = currentSelection - instance.instanceId)
                } else {
                    it.copy(selectedInstanceIds = currentSelection + instance.instanceId)
                }
            }
        } else {
            onEditGoal(instance)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedInstanceIds = emptySet()) }
    }

    fun deleteSelectedGoals() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedInstanceIds
            if (selectedIds.isEmpty()) return@launch

            val instancesToDelete = filteredGoals.value.filter { it.instanceId in selectedIds }

            recentlyDeletedGoal = null
            recentlyDeletedInstances = instancesToDelete

            goalRepository.deleteGoalInstances(instancesToDelete.map { it.instanceId })
            _uiEventFlow.send(UiEvent.ShowSnackbar(message = "Видалено цілей: ${instancesToDelete.size}", action = "Скасувати"))
            clearSelection()
        }
    }

    fun toggleCompletionForSelectedGoals() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedInstanceIds
            if (selectedIds.isEmpty()) return@launch

            val goalsToUpdate = filteredGoals.value
                .filter { it.instanceId in selectedIds }
                .map { it.goal.copy(completed = !it.goal.completed) }
                .distinctBy { it.id }

            goalRepository.updateGoals(goalsToUpdate)
            clearSelection()
        }
    }

    fun onBulkActionRequest(actionType: GoalActionType) {
        val selectedIds = _uiState.value.selectedInstanceIds
        if (selectedIds.isNotEmpty()) {
            _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
                sourceInstanceIds = selectedIds,
                actionType = actionType
            )
        }
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
                        moveGoal(fromIndex, 0, needsScroll = true)
                    }
                    onDismissGoalActionDialogs()
                }
                else -> {
                    _goalActionDialogState.value = GoalActionDialogState.AwaitingListChoice(
                        sourceInstanceIds = setOf(goalToActOn.instanceId),
                        actionType = actionType
                    )
                }
            }
        }
    }

    fun onDismissGoalActionDialogs() {
        val currentState = _goalActionDialogState.value
        val goalInstanceId = when (currentState) {
            is GoalActionDialogState.AwaitingActionChoice -> currentState.goalWithInstance.instanceId
            is GoalActionDialogState.AwaitingListChoice -> if (currentState.sourceInstanceIds.size == 1) currentState.sourceInstanceIds.first() else null
            else -> null
        }
        _goalActionDialogState.value = GoalActionDialogState.Hidden
        goalInstanceId?.let { resetSwipe(it) }

        _uiState.update { it.copy(swipedInstanceId = null) }
        _listChooserFilterText.value = ""
        // ✨ КРОК 4: При закритті діалогу також скидаємо стан користувача
        _listChooserUserExpandedIds.value = emptySet()
    }

    fun confirmGoalAction(targetListId: String) {
        val currentState = _goalActionDialogState.value
        if (currentState is GoalActionDialogState.AwaitingListChoice) {
            val ids = currentState.sourceInstanceIds
            val actionType = currentState.actionType

            viewModelScope.launch(Dispatchers.IO) {
                val goalsToProcess = filteredGoals.value
                    .filter { it.instanceId in ids }
                val goalIds = goalsToProcess.map { it.goal.id }.distinct()

                when (actionType) {
                    GoalActionType.CreateInstance -> goalRepository.createGoalInstances(goalIds, targetListId)
                    GoalActionType.MoveInstance -> goalRepository.moveGoalInstances(ids.toList(), targetListId)
                    GoalActionType.CopyGoal -> goalRepository.copyGoals(goalIds, targetListId)
                    GoalActionType.MoveToTop -> { /* Not applicable here */ }
                }

                launch(Dispatchers.Main) {
                    _goalActionDialogState.value = GoalActionDialogState.Hidden
                    if (ids.size == 1) {
                        resetSwipe(ids.first())
                    } else {
                        clearSelection()
                    }
                }
            }
        }
    }

    fun resetSwipe(instanceId: String) {
        viewModelScope.launch {
            val currentTriggers = _uiState.value.resetTriggers
            val newTriggerValue = currentTriggers.getOrDefault(instanceId, 0) + 1
            _uiState.update { it.copy(resetTriggers = currentTriggers + (instanceId to newTriggerValue)) }
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

    fun selectAllGoals() {
        val allInstanceIds = filteredGoals.value.map { it.instanceId }.toSet()
        _uiState.update { it.copy(selectedInstanceIds = allInstanceIds) }
    }
}