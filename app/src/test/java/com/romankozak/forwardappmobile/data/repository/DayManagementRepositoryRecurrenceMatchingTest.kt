package com.romankozak.forwardappmobile.data.repository

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Calendar

class DayManagementRepositoryRecurrenceMatchingTest {
    @Test
    fun `daily interval is anchored to the series start day`() = runTest {
        val start = localDate(2026, 8, 18)

        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 8, 18),
            frequency = RecurrenceFrequency.DAILY,
            interval = 2,
            expectedEligible = true,
        )
        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 8, 19),
            frequency = RecurrenceFrequency.DAILY,
            interval = 2,
            expectedEligible = false,
        )
        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 8, 20),
            frequency = RecurrenceFrequency.DAILY,
            interval = 2,
            expectedEligible = true,
        )
    }

    @Test
    fun `weekly interval uses seven day blocks and falls back to the start weekday`() = runTest {
        val startMonday = localDate(2026, 8, 17)

        assertCalendarEligibility(
            startDate = startMonday,
            targetDate = localDate(2026, 8, 17),
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            expectedEligible = true,
        )
        assertCalendarEligibility(
            startDate = startMonday,
            targetDate = localDate(2026, 8, 18),
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            expectedEligible = false,
        )
        assertCalendarEligibility(
            startDate = startMonday,
            targetDate = localDate(2026, 8, 24),
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            expectedEligible = false,
        )
        assertCalendarEligibility(
            startDate = startMonday,
            targetDate = localDate(2026, 8, 31),
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 2,
            expectedEligible = true,
        )
    }

    @Test
    fun `monthly interval preserves the original day of month and skips missing dates`() = runTest {
        val start = localDate(2026, 1, 31)

        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 1, 31),
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 3,
            expectedEligible = true,
        )
        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 4, 30),
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 3,
            expectedEligible = false,
        )
        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 7, 31),
            frequency = RecurrenceFrequency.MONTHLY,
            interval = 3,
            expectedEligible = true,
        )
    }

    @Test
    fun `yearly interval uses month and day so leap day does not drift`() = runTest {
        val start = localDate(2024, 2, 29)

        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2025, 3, 1),
            frequency = RecurrenceFrequency.YEARLY,
            interval = 2,
            expectedEligible = false,
        )
        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 2, 28),
            frequency = RecurrenceFrequency.YEARLY,
            interval = 2,
            expectedEligible = false,
        )
        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2028, 2, 29),
            frequency = RecurrenceFrequency.YEARLY,
            interval = 2,
            expectedEligible = true,
        )
    }

    @Test
    fun `hourly remains legacy day eligible and does not reinterpret interval as calendar days`() = runTest {
        val start = localDate(2026, 8, 18)

        assertCalendarEligibility(
            startDate = start,
            targetDate = localDate(2026, 8, 19),
            frequency = RecurrenceFrequency.HOURLY,
            interval = 8,
            expectedEligible = true,
        )
    }

    private suspend fun assertCalendarEligibility(
        startDate: Long,
        targetDate: Long,
        frequency: RecurrenceFrequency,
        interval: Int,
        expectedEligible: Boolean,
    ) {
        val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
        val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)
        val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
        val dailyMetricDao = mockk<DailyMetricDao>(relaxed = true)
        val goalDao = mockk<GoalDao>(relaxed = true)
        val contextDao = mockk<ContextDao>(relaxed = true)
        val recurringTaskDao = mockk<RecurringTaskDao>(relaxed = true)
        val listItemDao = mockk<ListItemDao>(relaxed = true)
        val activityRepository = mockk<ActivityRepository>(relaxed = true)
        val taskExecutionTimingCalculator = mockk<TaskExecutionTimingCalculator>(relaxed = true)
        val taskExecutionAlarmCoordinator = mockk<TaskExecutionAlarmCoordinator>(relaxed = true)
        val aiEventRepository = mockk<AiEventRepository>(relaxed = true)

        val repository =
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

        val dayPlan = mockk<DayPlan>(relaxed = true)
        every { dayPlan.id } returns "target-plan"
        every { dayPlan.date } returns targetDate

        val recurringTask = mockk<RecurringTask>(relaxed = true)
        every { recurringTask.id } returns "series-1"
        every { recurringTask.startDate } returns startDate
        every { recurringTask.endDate } returns null
        every { recurringTask.recurrenceRule } returns
            RecurrenceRule(
                frequency = frequency,
                interval = interval,
                daysOfWeek = null,
            )

        val existingOccurrence = mockk<DayTask>(relaxed = true)

        coEvery { dayPlanDao.getPlanForDateSync(targetDate) } returns dayPlan
        coEvery { recurringTaskDao.getAll() } returns listOf(recurringTask)
        coEvery {
            dayTaskDao.findRecurringOccurrenceForDayIncludingDeleted(
                recurringTaskId = "series-1",
                dayPlanId = "target-plan",
            )
        } returns existingOccurrence
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(dayPlan)
        coEvery { dayTaskDao.getTasksForDaySync("target-plan") } returns emptyList()
        coEvery { dayTaskDao.getAllTasksSync() } returns emptyList()

        repository.generateRecurringTasksForDate(targetDate)

        coVerify(exactly = if (expectedEligible) 1 else 0) {
            dayTaskDao.findRecurringOccurrenceForDayIncludingDeleted(
                recurringTaskId = "series-1",
                dayPlanId = "target-plan",
            )
        }
        coVerify(exactly = 0) { dayTaskDao.insert(any()) }
    }

    private fun localDate(
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
