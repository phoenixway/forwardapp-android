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

@org.junit.Ignore("Legacy Android recurrence deletion and split semantics are quarantined; canonical recurrence is owned by recurrence-v2")
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
            canonicalRecurrenceMaterializationAdapter = mockk<com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceMaterializationAdapter>(relaxed = true),
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

        val stopSeries =
            RecurringTask(
                id = "series-stop",
                title = "Stopped recurring task",
                description = null,
                goalId = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                duration = null,
                priority = com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority.MEDIUM,
                points = 0,
                recurrenceRule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 1,
                        daysOfWeek = null,
                    ),
                startDate = date,
                endDate = null,
            )
        val expectedEndDate =
            java.util.Calendar.getInstance().apply {
                timeInMillis = date
                add(java.util.Calendar.DAY_OF_YEAR, -1)
            }.timeInMillis
        val futurePlanIds = listOf("plan-1", "plan-2")
        var stoppedMaster: RecurringTask? = null

        coEvery { recurringTaskDao.getById("series-stop") } returns stopSeries
        coEvery { dayPlanDao.getPlanById("plan-1") } returns dayPlan
        coEvery { dayPlanDao.getFutureDayPlanIds(date) } returns futurePlanIds
        coEvery { recurringTaskDao.update(any()) } answers {
            stoppedMaster = firstArg()
        }

        repository.deleteAllFutureInstancesOfRecurringTask(
            recurringTaskId = "series-stop",
            dayPlanId = "plan-1",
        )

        assertThat(stoppedMaster).isNotNull()
        assertThat(stoppedMaster!!.id).isEqualTo("series-stop")
        assertThat(stoppedMaster!!.endDate).isEqualTo(expectedEndDate)
        assertThat(stoppedMaster!!.endDate).isLessThan(date)

        coVerify(exactly = 1) { dayPlanDao.getFutureDayPlanIds(date) }
        coVerify(exactly = 1) {
            dayTaskDao.softDeleteTasksForDayPlanIds(
                recurringTaskId = "series-stop",
                dayPlanIds = futurePlanIds,
                updatedAt = any(),
            )
        }
    }

    @Test
    fun `split recurring task tombstones old series and materializes replacement on split day`() = runTest {
        val date = 1_700_086_400_000L
        val dayPlan = mockk<DayPlan>()
        every { dayPlan.id } returns "plan-split"
        every { dayPlan.date } returns date

        val originalOccurrence = mockk<DayTask>()
        every { originalOccurrence.recurringTaskId } returns "series-old"
        every { originalOccurrence.dayPlanId } returns "plan-split"

        val oldMaster =
            RecurringTask(
                id = "series-old",
                title = "Old title",
                description = "Old description",
                goalId = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                duration = 20,
                priority = com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority.MEDIUM,
                points = 3,
                recurrenceRule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 1,
                        daysOfWeek = null,
                    ),
                startDate = date,
                endDate = null,
            )

        val futurePlanIds = listOf("plan-split", "plan-future")
        var stoppedMaster: RecurringTask? = null
        var createdMaster: RecurringTask? = null
        var replacementOccurrence: DayTask? = null

        coEvery { recurringTaskDao.getById("series-old") } returns oldMaster
        coEvery { dayPlanDao.getPlanById("plan-split") } returns dayPlan
        coEvery { dayPlanDao.getFutureDayPlanIds(date) } returns futurePlanIds

        coEvery { recurringTaskDao.update(any()) } answers {
            stoppedMaster = firstArg()
        }
        coEvery { recurringTaskDao.insert(any()) } answers {
            createdMaster = firstArg()
        }

        coEvery { dayPlanDao.getPlanForDateSync(date) } returns dayPlan
        coEvery { recurringTaskDao.getAll() } answers {
            listOfNotNull(stoppedMaster, createdMaster)
        }
        coEvery { dayPlanDao.getAllPlansSync() } returns listOf(dayPlan)
        coEvery { dayTaskDao.getTasksForDaySync("plan-split") } returns emptyList()
        coEvery { dayTaskDao.getAllTasksSync() } returns emptyList()
        coEvery { dayTaskDao.getMinOrderForDayPlan("plan-split") } returns 0L
        coEvery {
            dayTaskDao.findRecurringOccurrenceForDayIncludingDeleted(
                recurringTaskId = any(),
                dayPlanId = "plan-split",
            )
        } returns null
        coEvery { dayTaskDao.findTemplateForRecurringTask(any()) } returns null
        coEvery { dayTaskDao.getTaskById(any()) } returns null

        coEvery { dayTaskDao.update(any()) } answers {
            val updated = firstArg<DayTask>()
            if (updated.recurringTaskId != null) {
                replacementOccurrence = updated
            }
        }

        repository.splitRecurringTask(
            DayManagementRepository.SplitRecurringTaskParams(
                originalTask = originalOccurrence,
                newTitle = "New title",
                newDescription = "New description",
                newPriority = com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority.HIGH,
                newDuration = 45L,
                points = 17,
            ),
        )

        assertThat(stoppedMaster).isNotNull()
        assertThat(stoppedMaster!!.id).isEqualTo("series-old")
        assertThat(stoppedMaster!!.endDate).isNotNull()
        assertThat(stoppedMaster!!.endDate!!).isLessThan(date)

        assertThat(createdMaster).isNotNull()
        val newMaster = createdMaster!!
        assertThat(newMaster.id).isNotEqualTo("series-old")
        assertThat(newMaster.startDate).isEqualTo(date)
        assertThat(newMaster.endDate).isNull()
        assertThat(newMaster.title).isEqualTo("New title")
        assertThat(newMaster.points).isEqualTo(17)
        assertThat(newMaster.recurrenceRule).isEqualTo(oldMaster.recurrenceRule)

        coVerify(exactly = 1) {
            dayTaskDao.softDeleteTasksForDayPlanIds(
                recurringTaskId = "series-old",
                dayPlanIds = futurePlanIds,
                updatedAt = any(),
            )
        }

        assertThat(replacementOccurrence).isNotNull()
        assertThat(replacementOccurrence!!.dayPlanId).isEqualTo("plan-split")
        assertThat(replacementOccurrence!!.recurringTaskId).isEqualTo(newMaster.id)
        assertThat(replacementOccurrence!!.id)
            .isEqualTo("recurring-task-instance-plan-split-${newMaster.id}")
        assertThat(replacementOccurrence!!.title).isEqualTo("New title")
        assertThat(replacementOccurrence!!.points).isEqualTo(17)
        assertThat(replacementOccurrence!!.isDeleted).isFalse()
    }
}
