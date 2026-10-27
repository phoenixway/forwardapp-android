package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.data.recurrence.CanonicalTaskRecurrenceAuthoringAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class EditTaskLoadingViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<DayManagementRepository>(relaxed = true)
    private val recurrence = mockk<CanonicalTaskRecurrenceAuthoringAdapter>(relaxed = true)
    private val task = DayTask(id = "task", dayPlanId = "day", title = "Task", notes = "Journal", description = "Instruction")
    private val store = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getTaskById(any()) } answers { task.copy(id = firstArg()) }
        coEvery { repository.getPlanById(any()) } returns null
        coEvery { recurrence.getSeriesForOccurrence(any()) } returns null
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model() = EditTaskViewModel(repository, recurrence, mockk(relaxed = true),
        SavedStateHandle(mapOf("taskId" to "task")), dispatcher).also { store.put("model", it) }

    @Test fun `core text appears while independent metadata reads run concurrently and Save stays guarded`() = runTest(dispatcher) {
        val seriesGate = CompletableDeferred<RecurringTaskSeries?>()
        val dayGate = CompletableDeferred<DayPlan?>()
        var dayStarted = false
        coEvery { recurrence.getSeriesForOccurrence(any()) } coAnswers { seriesGate.await() }
        coEvery { repository.getPlanById(any()) } coAnswers { dayStarted = true; dayGate.await() }
        val vm = model()
        assertThat(vm.uiState.value.task).isEqualTo(task)
        assertThat(vm.uiState.value.notes).isEqualTo("Journal")
        assertThat(vm.uiState.value.isLoadingDetails).isTrue()
        assertThat(dayStarted).isTrue() // Not queued behind the series read.
        vm.onNotesChange("Draft"); vm.onTitleChange("Edited while loading"); vm.saveTask()
        coVerify(exactly = 0) { repository.updateTask(any()) }
        coVerify(exactly = 0) { repository.updateTaskNotes(any(), any()) }
        seriesGate.complete(null); dayGate.complete(DayPlan(id = "day", date = 123))
        assertThat(vm.uiState.value.isLoadingDetails).isFalse()
        assertThat(vm.uiState.value.title).isEqualTo("Edited while loading")
        assertThat(vm.uiState.value.notes).isEqualTo("Draft")
        assertThat(vm.uiState.value.dayAnchorTime).isEqualTo(123)
        assertThat(vm.hasUnsavedNotes).isTrue()
    }

    @Test fun `failed metadata load keeps core text and retry never replays a write or loses draft`() = runTest(dispatcher) {
        coEvery { repository.getPlanById(any()) } throws IllegalStateException("Unavailable")
        val vm = model()
        assertThat(vm.uiState.value.task).isEqualTo(task)
        assertThat(vm.uiState.value.loadError).isEqualTo("Unavailable")
        vm.onNotesChange("Keep draft"); vm.saveTask()
        coVerify(exactly = 0) { repository.updateTaskNotes(any(), any()) }
        coEvery { repository.getPlanById(any()) } returns DayPlan(id = "day", date = 12)
        vm.retryTaskLoad()
        assertThat(vm.uiState.value.loadError).isNull()
        assertThat(vm.uiState.value.isLoadingDetails).isFalse()
        assertThat(vm.uiState.value.notes).isEqualTo("Keep draft")
        coVerify(exactly = 1) { repository.getTaskById("task") }
        coVerify(exactly = 0) { repository.updateTask(any()) }
    }

    @Test fun `switching tasks cancels stale metadata and reset prevents late state resurrection`() = runTest(dispatcher) {
        val gate = CompletableDeferred<RecurringTaskSeries?>()
        coEvery { recurrence.getSeriesForOccurrence(match { it.id == "task" }) } coAnswers { gate.await() }
        val vm = model()
        vm.loadTask("other")
        assertThat(vm.uiState.value.task!!.id).isEqualTo("other")
        gate.complete(null)
        assertThat(vm.uiState.value.task!!.id).isEqualTo("other")
        vm.reset()
        assertThat(vm.uiState.value.task).isNull()
    }
}
