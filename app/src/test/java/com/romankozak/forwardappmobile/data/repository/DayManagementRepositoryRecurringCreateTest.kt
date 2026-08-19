package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
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
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DayManagementRepositoryRecurringCreateTest {
    private val dayPlanDao = mockk<DayPlanDao>(relaxed = true)
    private val dayFocusItemDao = mockk<DayFocusItemDao>(relaxed = true)
    private val dayTaskDao = mockk<DayTaskDao>(relaxed = true)
    private val dailyMetricDao = mockk<DailyMetricDao>(relaxed = true)
    private val goalDao = mockk<GoalDao>(relaxed = true)
    private val contextDao = mockk<ContextDao>(relaxed = true)
    private val recurringTaskDao = mockk<RecurringTaskDao>(relaxed = true)
    private val listItemDao = mockk<ListItemDao>(relaxed = true)
    private val activityRepository = mockk<ActivityRepository>(relaxed = true)
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
            taskExecutionTimingCalculator = TaskExecutionTimingCalculator(),
            taskExecutionAlarmCoordinator = taskExecutionAlarmCoordinator,
            aiEventRepository = aiEventRepository,
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `create recurring task preserves weekly rule and links first occurrence to master`() = runTest {
        val day = 1_700_000_000_000L
        val dayPlan = mockk<DayPlan>()
        every { dayPlan.id } returns "plan-create"
        every { dayPlan.date } returns day

        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 2,
                daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            )

        val masterSlot = slot<RecurringTask>()
        val insertedOccurrenceSlot = slot<DayTask>()
        val updatedOccurrenceSlot = slot<DayTask>()

        coEvery { dayPlanDao.getPlanById("plan-create") } returns dayPlan
        coEvery { dayTaskDao.getMaxOrderForDayPlan("plan-create") } returns 4L
        coEvery { dayTaskDao.getTasksForDaySync("plan-create") } returns emptyList()
        coEvery { recurringTaskDao.insert(capture(masterSlot)) } returns Unit
        coEvery { dayTaskDao.insert(capture(insertedOccurrenceSlot)) } returns Unit
        coEvery { dayTaskDao.update(capture(updatedOccurrenceSlot)) } returns Unit

        repository.addRecurringTask(
            DayManagementRepository.AddRecurringTaskParams(
                title = "Recurring candidate",
                description = "Create lifecycle",
                duration = 45L,
                priority = TaskPriority.HIGH,
                recurrenceRule = rule,
                dayPlanId = "plan-create",
                points = 17,
                linkedProjectIds = listOf("project-1"),
                linkedAttachmentIds = listOf("attachment-1"),
            ),
        )

        assertThat(masterSlot.isCaptured).isTrue()
        val master = masterSlot.captured
        assertThat(master.title).isEqualTo("Recurring candidate")
        assertThat(master.description).isEqualTo("Create lifecycle")
        assertThat(master.priority).isEqualTo(TaskPriority.HIGH)
        assertThat(master.duration).isEqualTo(45)
        assertThat(master.points).isEqualTo(17)
        assertThat(master.startDate).isEqualTo(day)
        assertThat(master.endDate).isNull()
        assertThat(master.recurrenceRule).isEqualTo(rule)
        assertThat(master.recurrenceRule.frequency).isEqualTo(RecurrenceFrequency.WEEKLY)
        assertThat(master.recurrenceRule.interval).isEqualTo(2)
        assertThat(master.recurrenceRule.daysOfWeek)
            .containsExactly(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
            .inOrder()

        assertThat(insertedOccurrenceSlot.isCaptured).isTrue()
        assertThat(updatedOccurrenceSlot.isCaptured).isTrue()

        val insertedOccurrence = insertedOccurrenceSlot.captured
        val linkedOccurrence = updatedOccurrenceSlot.captured

        assertThat(linkedOccurrence.id).isEqualTo(insertedOccurrence.id)
        assertThat(linkedOccurrence.id)
            .isEqualTo("recurring-task-instance-plan-create-${master.id}")
        assertThat(linkedOccurrence.dayPlanId).isEqualTo("plan-create")
        assertThat(linkedOccurrence.title).isEqualTo("Recurring candidate")
        assertThat(linkedOccurrence.priority).isEqualTo(TaskPriority.HIGH)
        assertThat(linkedOccurrence.points).isEqualTo(17)
        assertThat(linkedOccurrence.recurringTaskId).isEqualTo(master.id)
        assertThat(linkedOccurrence.nextOccurrenceTime).isNull()
        assertThat(linkedOccurrence.isDeleted).isFalse()

        coVerify(exactly = 1) { recurringTaskDao.insert(any()) }
        coVerify(exactly = 1) { dayTaskDao.insert(any()) }
        coVerify(exactly = 1) { dayTaskDao.update(any()) }
    }

    @Test
    fun `detach legacy random-id occurrence creates exclusion tombstone and sync-visible independent task`() = runTest {
        val day = 1_700_000_000_000L
        val dayPlan = mockk<DayPlan>()
        every { dayPlan.id } returns "plan-detach"
        every { dayPlan.date } returns day

        val rule =
            RecurrenceRule(
                frequency = RecurrenceFrequency.WEEKLY,
                interval = 2,
                daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            )

        val masters = mutableListOf<RecurringTask>()
        val insertedTasks = mutableListOf<DayTask>()
        val updatedTasks = mutableListOf<DayTask>()

        coEvery { dayPlanDao.getPlanById("plan-detach") } returns dayPlan
        coEvery { dayTaskDao.getMaxOrderForDayPlan("plan-detach") } returns 0L
        coEvery { dayTaskDao.getTasksForDaySync("plan-detach") } returns emptyList()
        coEvery { recurringTaskDao.insert(capture(masters)) } returns Unit
        coEvery { dayTaskDao.insert(capture(insertedTasks)) } returns Unit
        coEvery { dayTaskDao.update(capture(updatedTasks)) } returns Unit

        repository.addRecurringTask(
            DayManagementRepository.AddRecurringTaskParams(
                title = "Detached occurrence",
                description = null,
                duration = 30L,
                priority = TaskPriority.MEDIUM,
                recurrenceRule = rule,
                dayPlanId = "plan-detach",
                points = 5,
            ),
        )

        val master = masters.single()
        val generatedOccurrence = updatedTasks.last()
        val legacyOccurrence =
            generatedOccurrence.copy(
                id = "legacy-random-occurrence-id",
                recurringTaskId = master.id,
                nextOccurrenceTime = 123_456L,
                syncedAt = 999L,
                version = 7,
            )
        val markerId = "recurring-task-instance-plan-detach-${master.id}"

        insertedTasks.clear()
        updatedTasks.clear()

        coEvery { dayTaskDao.getTaskById("legacy-random-occurrence-id") } returns legacyOccurrence
        coEvery { dayTaskDao.getTaskById(markerId) } returns null

        repository.detachFromRecurrence("legacy-random-occurrence-id")

        assertThat(insertedTasks).hasSize(1)
        val marker = insertedTasks.single()
        assertThat(marker.id).isEqualTo(markerId)
        assertThat(marker.dayPlanId).isEqualTo("plan-detach")
        assertThat(marker.recurringTaskId).isEqualTo(master.id)
        assertThat(marker.isDeleted).isTrue()
        assertThat(marker.nextOccurrenceTime).isNull()
        assertThat(marker.syncedAt).isNull()
        assertThat(marker.version).isEqualTo(8)

        assertThat(updatedTasks).hasSize(1)
        val detached = updatedTasks.single()
        assertThat(detached.id).isEqualTo("legacy-random-occurrence-id")
        assertThat(detached.dayPlanId).isEqualTo("plan-detach")
        assertThat(detached.recurringTaskId).isNull()
        assertThat(detached.nextOccurrenceTime).isNull()
        assertThat(detached.isDeleted).isFalse()
        assertThat(detached.updatedAt).isNotNull()
        assertThat(detached.syncedAt).isNull()
        assertThat(detached.version).isEqualTo(8)

        coVerify(exactly = 0) { dayTaskDao.detachFromRecurrence(any()) }
    }
}
