package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.data.planning.day.DayTaskRealizationRepository
import com.romankozak.forwardappmobile.data.planning.day.liveTaskPriorities
import com.romankozak.forwardappmobile.core.di.DefaultDispatcher
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class TaskRealizesState(
    val task: DayTask? = null,
    val themes: List<DayTheme> = emptyList(),
    val priorities: List<DayFocusItem> = emptyList(),
    val selectedThemeIds: Set<String> = emptySet(),
    val suggestedThemeIds: Set<String> = emptySet(),
)

internal fun projectTaskRealizes(task: DayTask, priorities: List<DayFocusItem>, doc: DayThemeDocument): TaskRealizesState {
    val selectedThemes = doc.assignments.singleOrNull { it.entityId == task.id }?.themeIds.orEmpty()
    val selectedPriorities = liveTaskPriorities(task, priorities).mapTo(hashSetOf()) { it.id }
    val suggestions = doc.assignments.filter { it.entityId in selectedPriorities }.flatMap { it.themeIds }.toSet() - selectedThemes
    return TaskRealizesState(task, doc.themes.filter { it.isActive }, priorities.filter { !it.isDeleted && it.dayPlanId == task.dayPlanId },
        selectedThemes, suggestions.intersect(doc.themes.filter { it.isActive }.map { it.id }.toSet()))
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskRealizesViewModel @Inject constructor(
    private val relations: DayTaskRealizationRepository,
    private val themes: DayThemeRepository,
    @DefaultDispatcher private val readDispatcher: CoroutineDispatcher,
    @IoDispatcher private val writeDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val taskId = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val state = taskId.flatMapLatest { id ->
        if (id == null) flowOf(TaskRealizesState()) else relations.observeTask(id).flatMapLatest { task ->
            if (task == null) flowOf(TaskRealizesState()) else combine(relations.observePriorities(task.dayPlanId), themes.observe(task.dayPlanId)) { priorities, doc ->
                projectTaskRealizes(task, priorities, doc)
            }
        }
    }.flowOn(readDispatcher).catch { error.value = "Не вдалося завантажити зв’язки"; emit(TaskRealizesState()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskRealizesState())
    fun select(id: String) { taskId.value = id }
    fun togglePriority(id: String) {
        val task = state.value.task ?: return
        command { relations.toggle(task.id, id) }
    }
    fun toggleTheme(id: String) {
        val current = state.value; val task = current.task ?: return
        command {
        require(current.themes.any { it.id == id }) { "Тема недоступна для дня завдання" }
        themes.update(task.dayPlanId) { doc ->
            val ids = doc.assignments.singleOrNull { it.entityId == task.id }?.themeIds.orEmpty()
            withTaskThemes(doc, task, if (id in ids) ids - id else ids + id)
        }
        }
    }
    fun acceptSuggestedThemes() {
        val current = state.value; val task = current.task ?: return
        command {
        themes.update(task.dayPlanId) { doc ->
            withTaskThemes(doc, task, doc.assignments.singleOrNull { it.entityId == task.id }?.themeIds.orEmpty() + current.suggestedThemeIds)
        }
        }
    }
    private fun command(action: suspend () -> Unit) {
        if (busy.value || state.value.task == null) return
        busy.value = true; error.value = null
        viewModelScope.launch {
            try { withContext(writeDispatcher) { action() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error.value = failure.message ?: "Не вдалося змінити зв’язок" }
            finally { busy.value = false }
        }
    }
}

private fun withTaskThemes(doc: DayThemeDocument, task: DayTask, ids: Set<String>) = doc.copy(assignments =
    doc.assignments.filterNot { it.entityId == task.id } + if (ids.isEmpty()) emptyList() else listOf(DayThemeAssignment(task.dayPlanId, task.id, ids)))

@HiltViewModel
class DayTaskPriorityCatalogViewModel @Inject constructor(repository: DayTaskRealizationRepository) : ViewModel() {
    val priorities = repository.observePriorityCatalog().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
