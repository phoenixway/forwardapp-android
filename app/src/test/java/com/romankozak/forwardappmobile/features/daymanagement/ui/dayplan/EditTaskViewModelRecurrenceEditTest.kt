package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.data.recurrence.CanonicalTaskRecurrenceAuthoringAdapter
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency as CanonicalRecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule as CanonicalRecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditTaskViewModelRecurrenceEditTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `editing recurring task persists new calendar rule and points to existing master`() =
        runTest(dispatcher) {
            val repository = mockk<DayManagementRepository>(relaxed = true)
            val canonicalTaskRecurrenceAuthoringAdapter =
                mockk<CanonicalTaskRecurrenceAuthoringAdapter>(relaxed = true)
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val task = mockk<DayTask>()
            val recurringSeries = mockk<RecurringTaskSeries>()

            every { task.id } returns "task-1"
            every { task.recurrenceSeriesId } returns "series-1"
            every { task.linkedProjectIds } returns emptyList()
            every { task.linkedAttachmentIds } returns emptyList()
            every { task.title } returns "Recurring task"
            every { task.description } returns "Before edit"
            every { task.priority } returns TaskPriority.MEDIUM
            every { task.estimatedDurationMinutes } returns 30L
            every { task.scheduledTime } returns null
            every { task.dueTime } returns null
            every { task.executionStrictness } returns TaskExecutionStrictness.NORMAL
            every { task.points } returns 4
            every { task.dayPlanId } returns "plan-1"
            every { task.goalId } returns null
            every { task.projectId } returns null
            every { task.taskType } returns null

            every { recurringSeries.rule } returns
                CanonicalRecurrenceRule(
                    frequency = CanonicalRecurrenceFrequency.DAILY,
                    interval = 1,
                    daysOfWeek = null,
                )

            coEvery { repository.getTaskById("task-1") } returns task
            coEvery { repository.getPlanById("plan-1") } returns null
            coEvery {
                canonicalTaskRecurrenceAuthoringAdapter.getSeriesForOccurrence(task)
            } returns recurringSeries
            coEvery {
                canonicalTaskRecurrenceAuthoringAdapter.splitSeriesFromOccurrence(
                    task = task,
                    title = any(),
                    description = any(),
                    goalId = any(),
                    projectId = any(),
                    taskType = any(),
                    linkedProjectIds = any(),
                    linkedAttachmentIds = any(),
                    priority = any(),
                    estimatedDurationMinutes = any(),
                    points = any(),
                    executionStrictness = any(),
                    rule = any(),
                )
            } returns task

            val viewModel =
                EditTaskViewModel(
                    dayManagementRepository = repository,
                    canonicalTaskRecurrenceAuthoringAdapter = canonicalTaskRecurrenceAuthoringAdapter,
                    contextRepository = contextRepository,
                    savedStateHandle = SavedStateHandle(mapOf("taskId" to "task-1")),
                )

            assertThat(viewModel.uiState.value.isRecurring).isTrue()
            assertThat(viewModel.uiState.value.recurrenceFrequency)
                .isEqualTo(RecurrenceFrequency.DAILY)
            assertThat(viewModel.uiState.value.recurrenceInterval).isEqualTo(1)

            viewModel.onRecurrenceFrequencyChange(RecurrenceFrequency.MONTHLY)
            viewModel.onRecurrenceIntervalChange(3)
            viewModel.onPointsChange(17)

            val navigationEvent = async { viewModel.uiEvent.first() }
            viewModel.saveTask()
            navigationEvent.await()

            coVerify(exactly = 1) {
                canonicalTaskRecurrenceAuthoringAdapter.splitSeriesFromOccurrence(
                    task = task,
                    title = "Recurring task",
                    description = "Before edit",
                    goalId = null,
                    projectId = null,
                    taskType = null,
                    linkedProjectIds = emptyList(),
                    linkedAttachmentIds = emptyList(),
                    priority = TaskPriority.MEDIUM,
                    estimatedDurationMinutes = 30L,
                    points = 17,
                    executionStrictness = TaskExecutionStrictness.NORMAL,
                    rule =
                        CanonicalRecurrenceRule(
                            frequency = CanonicalRecurrenceFrequency.MONTHLY,
                            interval = 3,
                            daysOfWeek = null,
                        ),
                )
            }

            coVerify(exactly = 1) {
                repository.updateTask(
                    match { params ->
                        params.taskId == "task-1" &&
                            params.points == 17 &&
                            params.updateContextLinks
                    },
                )
            }

            coVerify(exactly = 0) { repository.deleteTask(any()) }
        }
}
