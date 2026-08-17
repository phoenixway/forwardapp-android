package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurringTask
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.DayFocusItemDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain.TaskExecutionTimingCalculator
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.platform.TaskExecutionAlarmCoordinator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DayManagementRepositoryRecurringDeletionTest {
    private val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
    private val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)
    private val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
    private val dailyMetricDao = mockk<DailyMetricDao>(relaxed = true)
    private val goalDao = mockk<GoalDao>(relaxed = true)
    private val contextDao = mockk<ContextDao>(relaxed = true)
    private val recurringTaskDao = mockk<RecurringTaskDao>(relaxed = true)
    private val listItemDao = mockk<ListItemDao>(relaxed = true)
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
    private val taskExecutionTimingCalculator = mockk<TaskExecutionTimingCalculator>(relaxed = true)
    private val taskExecutionAlarmCoordinator = mockk<TaskExecutionAlarmCoordinator>(relaxed = true)
    private val aiEventRepository = mockk<AiEventRepository>(relaxed = true)

    private val repository =
        DayManagementRepository(
            dayPlanDao = dayPlanDao,
            dayFocusItemDao = dayFocusItemDao,
            dayTaskDao = dayTaskDao,
            dailyMetricDao = dailyMetricDao,
            goalDao = goalDao,
            contextDao = contextDao,
            recurringTaskDao = recurringTaskDao,
            listItemDao = listItemDao,
            activityRepository = activityRepository,
            taskExecutionTimingCalculator = taskExecutionTimingCalculator,
            taskExecutionAlarmCoordinator = taskExecutionAlarmCoordinator,
            aiEventRepository = aiEventRepository,
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `deleteTask uses soft tombstone instead of hard delete`() = runTest {
        val task = mockk<DayTask>()
        every { task.id } returns "occurrence-1"
        every { task.dayPlanId } returns "plan-1"

        coEvery { dayTaskDao.getTaskById("occurrence-1") } returns task
        coEvery { dayTaskDao.getTasksForDaySync("plan-1") } returns emptyList()
        coEvery { dayPlanDao.getPlanById("plan-1") } returns null

        repository.deleteTask("occurrence-1")

        val updatedAt = slot<Long>()
        coVerify(exactly = 1) {
            dayTaskDao.softDelete(
                taskId = "occurrence-1",
                updatedAt = capture(updatedAt),
            )
        }
        coVerify(exactly = 0) { dayTaskDao.deleteById(any()) }
        verify(exactly = 1) { taskExecutionAlarmCoordinator.cancel("occurrence-1") }
        assertThat(updatedAt.captured).isGreaterThan(0L)
    }

    @Test
    fun `tombstoned recurring occurrence blocks regeneration for same series and day`() = runTest {
        val date = 1_700_000_000_000L
        val dayPlan = mockk<DayPlan>()
        every { dayPlan.id } returns "plan-1"
        every { dayPlan.date } returns date

        val recurringTask = mockk<RecurringTask>()
        every { recurringTask.id } returns "series-1"
        every { recurringTask.startDate } returns date
        every { recurringTask.endDate } returns null
        every { recurringTask.recurrenceRule } returns
            RecurrenceRule(
                frequency = RecurrenceFrequency.DAILY,
                interval = 1,
                daysOfWeek = null,
            )

        val tombstone = mockk<DayTask>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(date) } returns dayPlan
        coEvery { recurringTaskDao.getAll() } returns listOf(recurringTask)
        coEvery {
            dayTaskDao.findRecurringOccurrenceForDayIncludingDeleted(
                recurringTaskId = "series-1",
                dayPlanId = "plan-1",
            )
        } returns tombstone
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(dayPlan)
        coEvery { dayTaskDao.getTasksForDaySync("plan-1") } returns emptyList()
        coEvery { dayTaskDao.getAllTasksSync() } returns emptyList()

        repository.generateRecurringTasksForDate(date)

        coVerify(exactly = 1) {
            dayTaskDao.findRecurringOccurrenceForDayIncludingDeleted(
                recurringTaskId = "series-1",
                dayPlanId = "plan-1",
            )
        }
        coVerify(exactly = 0) { dayTaskDao.insert(any()) }
        coVerify(exactly = 0) { dayTaskDao.update(any()) }
    }
}
