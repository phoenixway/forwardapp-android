package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
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
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val task = mockk<DayTask>()
            val recurringTask = mockk<RecurringTask>()

            every { task.id } returns "task-1"
            every { task.recurringTaskId } returns "series-1"
            every { task.linkedProjectIds } returns emptyList()
            every { task.title } returns "Recurring task"
            every { task.description } returns "Before edit"
            every { task.priority } returns TaskPriority.MEDIUM
            every { task.estimatedDurationMinutes } returns 30L
            every { task.scheduledTime } returns null
            every { task.dueTime } returns null
            every { task.executionStrictness } returns TaskExecutionStrictness.NORMAL
            every { task.points } returns 4
            every { task.dayPlanId } returns "plan-1"
            every { task.projectId } returns null

            every { recurringTask.recurrenceRule } returns
                RecurrenceRule(
                    frequency = RecurrenceFrequency.DAILY,
                    interval = 1,
                    daysOfWeek = null,
                )

            coEvery { repository.getTaskById("task-1") } returns task
            coEvery { repository.getRecurringTask("series-1") } returns recurringTask
            coEvery { repository.getPlanById("plan-1") } returns null

            val viewModel =
                EditTaskViewModel(
                    dayManagementRepository = repository,
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
                repository.updateRecurringTaskTemplate(
                    recurringTaskId = "series-1",
                    title = "Recurring task",
                    description = "Before edit",
                    priority = TaskPriority.MEDIUM,
                    duration = 30L,
                    recurrenceRule =
                        RecurrenceRule(
                            frequency = RecurrenceFrequency.MONTHLY,
                            interval = 3,
                            daysOfWeek = null,
                        ),
                    points = 17,
                    linkedProjectIds = emptyList(),
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

            coVerify(exactly = 0) { repository.addRecurringTask(any()) }
            coVerify(exactly = 0) { repository.deleteTask(any()) }
        }
}
