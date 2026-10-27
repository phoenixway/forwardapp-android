package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.data.recurrence.CanonicalTaskRecurrenceAuthoringAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.shared.core.models.recurrence.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*

@OptIn(ExperimentalCoroutinesApi::class)
class EditTaskNotesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<DayManagementRepository>(relaxed = true)
    private val recurrence = mockk<CanonicalTaskRecurrenceAuthoringAdapter>(relaxed = true)
    private val task = DayTask(id = "t", dayPlanId = "d", title = "Task", description = "Template description", notes = "Old notes")
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getTaskById(any()) } answers { task.copy(id = firstArg()) }
        coEvery { repository.getPlanById(any()) } returns null
        coEvery { recurrence.getSeriesForOccurrence(any()) } returns null
    }
    @After fun reset() { Dispatchers.resetMain() }
    private fun vm(handle: SavedStateHandle = SavedStateHandle(mapOf("taskId" to "t"))) =
        EditTaskViewModel(repository, recurrence, mockk(relaxed = true), handle, dispatcher)

    @Test fun `loads existing notes separately from description`() = runTest(dispatcher) {
        val vm = vm()
        assertThat(vm.uiState.value.notes).isEqualTo("Old notes")
        assertThat(vm.uiState.value.description).isEqualTo("Template description")
        assertThat(vm.hasUnsavedNotes).isFalse()
    }
    @Test fun `notes save uses occurrence-only writer and no recurrence mutation`() = runTest(dispatcher) {
        val vm = vm(); vm.onNotesChange("- [x] Done\nNew thought")
        assertThat(vm.hasUnsavedNotes).isTrue()
        val exit = async { vm.uiEvent.first() }; vm.saveTask(); exit.await()
        coVerify(exactly = 1) { repository.updateTaskNotes("t", "- [x] Done\nNew thought") }
        coVerify(exactly = 0) { repository.updateTask(any()) }
        coVerify(exactly = 1) { recurrence.getSeriesForOccurrence(any()) }
        confirmVerified(recurrence)
    }
    @Test fun `recurring occurrence notes never update or split template`() = runTest(dispatcher) {
        val series = mockk<RecurringTaskSeries>()
        every { series.rule } returns RecurrenceRule(frequency = RecurrenceFrequency.DAILY, interval = 1, daysOfWeek = null)
        coEvery { repository.getTaskById("t") } returns task.copy(recurrenceSeriesId = "series")
        coEvery { recurrence.getSeriesForOccurrence(any()) } returns series
        val vm = vm(); vm.onNotesChange("Only today")
        val exit = async { vm.uiEvent.first() }; vm.saveTask(); exit.await()
        coVerify(exactly = 1) { repository.updateTaskNotes("t", "Only today") }
        coVerify(exactly = 1) { recurrence.getSeriesForOccurrence(any()) }
        confirmVerified(recurrence)
    }
    @Test fun `draft survives recreation without replaying save`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("taskId" to "t"))
        vm(saved).onNotesChange("Draft")
        val restored = vm(saved)
        assertThat(restored.uiState.value.notes).isEqualTo("Draft")
        assertThat(restored.hasUnsavedNotes).isTrue()
        coVerify(exactly = 0) { repository.updateTaskNotes(any(), any()) }
    }
    @Test fun `discard clears draft only and cancellation makes no write`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("taskId" to "t"))
        val vm = vm(saved); vm.onNotesChange("Draft"); vm.reset()
        assertThat(vm(saved).uiState.value.notes).isEqualTo("Old notes")
        coVerify(exactly = 0) { repository.updateTaskNotes(any(), any()) }
    }
    @Test fun `save failure keeps draft and editor open`() = runTest(dispatcher) {
        coEvery { repository.updateTaskNotes(any(), any()) } throws IllegalStateException("Unavailable")
        val saved = SavedStateHandle(mapOf("taskId" to "t"))
        val vm = vm(saved); vm.onNotesChange("Keep me"); vm.saveTask()
        assertThat(vm.uiState.value.saveError).isEqualTo("Unavailable")
        assertThat(vm.uiState.value.isSaving).isFalse()
        assertThat(vm(saved).uiState.value.notes).isEqualTo("Keep me")
    }
    @Test fun `double save cannot dispatch twice`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { repository.updateTaskNotes(any(), any()) } coAnswers { gate.await() }
        val vm = vm(); vm.onNotesChange("Draft")
        val exit = async { vm.uiEvent.first() }; vm.saveTask(); vm.saveTask()
        assertThat(vm.uiState.value.isSaving).isTrue()
        coVerify(exactly = 1) { repository.updateTaskNotes(any(), any()) }
        gate.complete(Unit); exit.await()
    }
    @Test fun `mixed edit includes notes explicitly in existing update command`() = runTest(dispatcher) {
        val vm = vm(); vm.onTitleChange("Renamed"); vm.onNotesChange("")
        val exit = async { vm.uiEvent.first() }; vm.saveTask(); exit.await()
        coVerify { repository.updateTask(match { it.title == "Renamed" && it.notes == "" && it.updateNotes && it.description == task.description }) }
        coVerify(exactly = 0) { repository.updateTaskNotes(any(), any()) }
    }
    @Test fun `unrelated edit does not overwrite notes`() = runTest(dispatcher) {
        val vm = vm()
        vm.onTitleChange("Renamed")
        val nextExit = async { vm.uiEvent.first() }; vm.saveTask(); nextExit.await()
        coVerify { repository.updateTask(match { !it.updateNotes }) }
        coVerify(exactly = 0) { repository.updateTaskNotes(any(), any()) }
    }
    @Test fun `drafts are scoped to exact task identity`() = runTest(dispatcher) {
        val vm = vm(); vm.onNotesChange("Only t"); vm.loadTask("other")
        assertThat(vm.uiState.value.notes).isEqualTo("Old notes")
        vm.loadTask("t"); assertThat(vm.uiState.value.notes).isEqualTo("Only t")
    }
    @Test fun `Save completes without active UI collector and queued completion cannot dispatch duplicate save`() = runTest(dispatcher) {
        val vm = vm(); vm.onNotesChange("Save without listener")
        vm.saveTask()
        assertThat(vm.uiState.value.isSaving).isFalse()
        assertThat(vm.hasUnsavedNotes).isFalse()
        vm.saveTask()
        coVerify(exactly = 1) { repository.updateTaskNotes("t", "Save without listener") }
        assertThat((vm.uiEvent.first() as EditTaskUiEvent.NavigateUp).taskId).isEqualTo("t")
    }
    @Test fun `description and journal drafts remain independent in one Notes tab`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("taskId" to "t"))
        val vm = vm(saved); vm.onDescriptionChange("Instruction draft"); vm.onNotesChange("Journal draft")
        val restored = vm(saved)
        assertThat(restored.uiState.value.description).isEqualTo("Instruction draft")
        assertThat(restored.uiState.value.notes).isEqualTo("Journal draft")
        assertThat(restored.hasUnsavedNotes).isTrue()
        coVerify(exactly = 0) { repository.updateTask(any()) }
    }
    @Test fun `recurrence save uses latest task after immediate realization changes`() = runTest(dispatcher) {
        val recurring = task.copy(recurrenceSeriesId = "series")
        val series = mockk<RecurringTaskSeries>()
        every { series.rule } returns RecurrenceRule(frequency = RecurrenceFrequency.DAILY, interval = 1, daysOfWeek = null)
        coEvery { repository.getTaskById("t") } returns recurring
        coEvery { recurrence.getSeriesForOccurrence(any()) } returns series
        val vm = vm()
        val latest = recurring.copy(version = 2, realizedDayFocusItemIds = listOf("focus"))
        coEvery { repository.getTaskById("t") } returns latest
        coEvery { recurrence.detachCurrentOccurrence(latest) } returns latest.copy(recurrenceSeriesId = null)
        vm.onRecurringChange(false)
        vm.saveTask()
        coVerify(exactly = 1) { recurrence.detachCurrentOccurrence(latest) }
        assertThat(vm.uiState.value.saveError).isNull()
    }
}
