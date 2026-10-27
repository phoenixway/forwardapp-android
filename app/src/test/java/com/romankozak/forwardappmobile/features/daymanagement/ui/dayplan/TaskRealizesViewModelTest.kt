package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.data.planning.day.DayTaskRealizationRepository
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRealizesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val task = MutableStateFlow<DayTask?>(DayTask(id = "task", dayPlanId = "future", title = "Task"))
    private val priorities = MutableStateFlow(listOf(DayFocusItem("focus", "future", "Focus")))
    private val document = MutableStateFlow(DayThemeDocument(listOf(DayTheme("theme", "future", "Theme")),
        listOf(DayThemeAssignment("future", "focus", setOf("theme")))))
    private val relations = mockk<DayTaskRealizationRepository> {
        every { observeTask("task") } returns task
        every { observePriorities("future") } returns priorities
    }
    private val themes = mockk<DayThemeRepository> { every { observe("future") } returns document }
    private val store = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { relations.toggle("task", "focus", any()) } coAnswers {
            val current = task.value!!
            task.value = current.copy(realizedDayFocusItemIds = if (current.realizedDayFocusItemIds.isEmpty()) listOf("focus") else emptyList())
        }
        coEvery { themes.update("future", any()) } coAnswers {
            document.value = secondArg<(DayThemeDocument) -> DayThemeDocument>()(document.value)
        }
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model() = TaskRealizesViewModel(relations, themes, dispatcher, dispatcher).also { store.put("model", it); it.select("task") }

    @Test fun `priority updates reactive task links and themes require explicit acceptance`() = runBlocking<Unit> {
        val vm = model()
        val collection = launch(dispatcher) { vm.state.collect() }
        try {
            withTimeout(5_000) { vm.state.first { it.task != null } }
            vm.togglePriority("focus")
            val linked = withTimeout(5_000) { vm.state.first { it.suggestedThemeIds.isNotEmpty() } }
            assertThat(linked.selectedThemeIds).isEmpty()
            coVerify(exactly = 0) { themes.update(any(), any()) }
            withTimeout(5_000) { vm.busy.first { !it } }
            vm.acceptSuggestedThemes()
            val accepted = withTimeout(5_000) { vm.state.first { it.selectedThemeIds.contains("theme") } }
            assertThat(accepted.suggestedThemeIds).isEmpty()
            withTimeout(5_000) { vm.busy.first { !it } }
            vm.togglePriority("focus")
            val removed = withTimeout(5_000) { vm.state.first { it.task?.realizedDayFocusItemIds?.isEmpty() == true } }
            assertThat(removed.selectedThemeIds).containsExactly("theme")
            withTimeout(5_000) { vm.busy.first { !it } }
            vm.toggleTheme("theme")
            withTimeout(5_000) { vm.state.first { it.selectedThemeIds.isEmpty() } }
        } finally { collection.cancel() }
    }

    @Test fun `failed relation command leaves assignments unchanged and exposes error`() = runBlocking<Unit> {
        coEvery { relations.toggle(any(), any(), any()) } throws IllegalStateException("Deleted priority")
        val vm = model()
        val collection = launch(dispatcher) { vm.state.collect() }
        try {
            withTimeout(5_000) { vm.state.first { it.task != null } }
            vm.togglePriority("focus")
            assertThat(withTimeout(5_000) { vm.error.first { it != null } }).isEqualTo("Deleted priority")
            assertThat(task.value!!.realizedDayFocusItemIds).isEmpty()
            coVerify(exactly = 0) { themes.update(any(), any()) }
        } finally { collection.cancel() }
    }
}
