package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.CanonicalDayDatabase
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusItem
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusType
import com.romankozak.forwardappmobile.shared.core.models.day.DayPlan
import com.romankozak.forwardappmobile.shared.core.models.day.DayStatus
import com.romankozak.forwardappmobile.shared.core.models.day.DayTask
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.day.TaskStatus
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecurrenceMaterializerTest {
    private val dayKey = "2026-08-17"
    private val now = 1_776_000_000_000L

    private fun dayPlan(
        id: String = "day-plan:$dayKey",
        isDeleted: Boolean = false,
    ): DayPlan =
        DayPlan(
            id = id,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 1L,
            dayKey = dayKey,
            name = null,
            linkedProjectIds = emptyList(),
            linkedAttachmentIds = emptyList(),
            status = DayStatus.ACTIVE,
            reflection = null,
            energyLevel = null,
            mood = null,
            weatherConditions = null,
            predictedDurationMinutes = null,
            totalPlannedMinutes = 0L,
            totalCompletedMinutes = 0L,
            completionPercentage = 0f,
        )

    private fun taskSeries(
        id: String = "series:task:daily",
        title: String = "Daily task",
        version: Long = 3L,
    ): RecurringTaskSeries =
        RecurringTaskSeries(
            id = id,
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = version,
            rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null),
            startDayKey = dayKey,
            endDayKey = null,
            template =
                RecurringTaskTemplate(
                    title = title,
                    description = null,
                    goalId = null,
                    linkedProjectIds = emptyList(),
                    linkedAttachmentIds = emptyList(),
                    priority = TaskPriority.MEDIUM,
                    estimatedDurationMinutes = 30L,
                    points = 5,
                ),
        )

    private fun focusSeries(): RecurringFocusSeries =
        RecurringFocusSeries(
            id = "series:focus:daily",
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 2L,
            rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null),
            startDayKey = dayKey,
            endDayKey = null,
            template = RecurringFocusTemplate("Daily focus", null, emptyList(), 25),
        )

    private fun responsibilitySeries(): RecurringResponsibilitySeries =
        RecurringResponsibilitySeries(
            id = "series:responsibility:daily",
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 4L,
            rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null),
            startDayKey = dayKey,
            endDayKey = null,
            template = RecurringFocusTemplate("Daily responsibility", null, emptyList(), null),
        )

    private fun database(
        recurringSeries: List<RecurringSeries> = listOf(taskSeries()),
        plans: List<DayPlan> = listOf(dayPlan()),
        tasks: List<DayTask> = emptyList(),
        focusItems: List<DayFocusItem> = emptyList(),
    ): CanonicalDayDatabase =
        CanonicalDayDatabase(
            dayPlans = plans,
            dayTasks = tasks,
            dayFocusItems = focusItems,
            recurringSeries = recurringSeries,
            dayManagementRuntimeState = null,
        )

    @Test
    fun `materializes recurrence-owned task defaults from canonical template`() {
        val baseSeries = taskSeries()
        val series =
            baseSeries.copy(
                template =
                    baseSeries.template.copy(
                        projectId = "project:canonical",
                        taskType = "SUBLIST",
                        executionStrictness = "STRICT",
                    ),
            )

        val result =
            planRecurringSeriesForDay(
                database = database(recurringSeries = listOf(series)),
                dayKey = dayKey,
                now = now,
            )

        val task = result.tasksToCreate.single()
        assertEquals("project:canonical", task.projectId)
        assertEquals("SUBLIST", task.taskType)
        assertEquals("STRICT", task.executionStrictness)
        assertEquals(null, task.scheduledTime)
        assertEquals(null, task.dueTime)
    }

    @Test
    fun materializesCanonicalTaskWithProvenance() {
        val result = planRecurringSeriesForDay(database(), dayKey, now)

        assertEquals(RecurrenceMaterializationStatus.MATERIALIZED, result.status)
        assertEquals(1, result.tasksToCreate.size)
        assertEquals(emptyList(), result.focusItemsToCreate)

        val task = result.tasksToCreate.single()
        assertEquals("recurrence:TASK:series:task:daily:$dayKey", task.id)
        assertEquals(dayPlan().id, task.dayPlanId)
        assertEquals("Daily task", task.title)
        assertEquals(
            RecurrenceOrigin("series:task:daily", dayKey, 3L),
            task.recurrence,
        )
    }

    @Test
    fun existingLiveOccurrenceIsIdempotent() {
        val first = planRecurringSeriesForDay(database(), dayKey, now)
        val existing = first.tasksToCreate.single()

        val second =
            planRecurringSeriesForDay(
                database(tasks = listOf(existing)),
                dayKey,
                now + 1L,
            )

        assertEquals(emptyList(), second.tasksToCreate)
        assertEquals(
            listOf("series:task:daily@$dayKey"),
            second.skippedExistingOccurrenceKeys,
        )
    }

    @Test
    fun logicalIdentityDoesNotDependOnPhysicalEntityId() {
        val first = planRecurringSeriesForDay(database(), dayKey, now)
        val imported = first.tasksToCreate.single().copy(id = "legacy-or-imported-physical-id")

        val second =
            planRecurringSeriesForDay(
                database(tasks = listOf(imported)),
                dayKey,
                now + 1L,
            )

        assertEquals(emptyList(), second.tasksToCreate)
        assertEquals(
            listOf("series:task:daily@$dayKey"),
            second.skippedExistingOccurrenceKeys,
        )
    }

    @Test
    fun softDeletedOccurrenceBlocksResurrection() {
        val first = planRecurringSeriesForDay(database(), dayKey, now)
        val tombstone =
            first.tasksToCreate.single().copy(
                isDeleted = true,
                updatedAt = now + 10L,
                version = 2L,
            )

        val second =
            planRecurringSeriesForDay(
                database(tasks = listOf(tombstone)),
                dayKey,
                now + 20L,
            )

        assertEquals(emptyList(), second.tasksToCreate)
        assertEquals(
            listOf("series:task:daily@$dayKey"),
            second.skippedExistingOccurrenceKeys,
        )
    }

    @Test
    fun materializesFocusAndResponsibilityWithCorrectTypes() {
        val result =
            planRecurringSeriesForDay(
                database(recurringSeries = listOf(focusSeries(), responsibilitySeries())),
                dayKey,
                now,
            )

        assertEquals(emptyList(), result.tasksToCreate)
        assertEquals(2, result.focusItemsToCreate.size)

        val focus = result.focusItemsToCreate.single { it.type == DayFocusType.FOCUS }
        val responsibility =
            result.focusItemsToCreate.single { it.type == DayFocusType.RESPONSIBILITY }

        assertEquals(
            RecurrenceOrigin("series:focus:daily", dayKey, 2L),
            focus.recurrence,
        )
        assertEquals(
            RecurrenceOrigin("series:responsibility:daily", dayKey, 4L),
            responsibility.recurrence,
        )
    }

    @Test
    fun missingPlanReturnsPlanNotFoundWithoutCreatingAnything() {
        val result =
            planRecurringSeriesForDay(
                database(plans = emptyList()),
                dayKey,
                now,
            )

        assertEquals(RecurrenceMaterializationStatus.PLAN_NOT_FOUND, result.status)
        assertEquals(null, result.dayPlanId)
        assertEquals(emptyList(), result.tasksToCreate)
        assertEquals(emptyList(), result.focusItemsToCreate)
        assertEquals(emptyList(), result.skippedExistingOccurrenceKeys)
    }

    @Test
    fun suppliedWorkingDayControlsIdentityNotWallClockNow() {
        val laterWallClockTime = now + 2L * 24L * 60L * 60L * 1000L

        val result = planRecurringSeriesForDay(database(), dayKey, laterWallClockTime)
        val task = result.tasksToCreate.single()

        assertEquals(dayKey, task.recurrence?.occurrenceDayKey)
        assertEquals(laterWallClockTime, task.createdAt)
    }

    @Test
    fun multipleActivePlansForSameDayFail() {
        val error =
            assertFailsWith<IllegalStateException> {
                planRecurringSeriesForDay(
                    database(
                        plans = listOf(
                            dayPlan("day-plan:first"),
                            dayPlan("day-plan:duplicate"),
                        ),
                    ),
                    dayKey,
                    now,
                )
            }

        assertTrue(error.message.orEmpty().contains("multiple active DayPlans"))
    }

    @Test
    fun unrelatedPhysicalIdCollisionFails() {
        val canonicalId = "recurrence:TASK:series:task:daily:$dayKey"
        val unrelatedTask =
            DayTask(
                id = canonicalId,
                createdAt = now,
                updatedAt = now,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                dayPlanId = dayPlan().id,
                recurrence = null,
                title = "Manual collision",
                description = null,
                goalId = null,
                projectId = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                activityRecordId = null,
                taskType = null,
                entityId = null,
                order = 0L,
                priority = TaskPriority.MEDIUM,
                status = TaskStatus.NOT_STARTED,
                completed = false,
                scheduledTime = null,
                estimatedDurationMinutes = null,
                actualDurationMinutes = null,
                dueTime = null,
                executionStrictness = "NORMAL",
                valueImportance = 0f,
                valueImpact = 0f,
                effort = 0f,
                cost = 0f,
                risk = 0f,
                location = null,
                tags = emptyList(),
                notes = null,
                completedAt = null,
                points = 0,
            )

        val error =
            assertFailsWith<IllegalStateException> {
                planRecurringSeriesForDay(
                    database(tasks = listOf(unrelatedTask)),
                    dayKey,
                    now,
                )
            }

        assertTrue(error.message.orEmpty().contains("occurrence id collision"))
    }

    @Test
    fun multipleTaskSeriesReceiveSequentialOrders() {
        val result =
            planRecurringSeriesForDay(
                database(
                    recurringSeries = listOf(
                        taskSeries(id = "series:first", title = "First"),
                        taskSeries(id = "series:second", title = "Second"),
                    ),
                ),
                dayKey,
                now,
            )

        assertEquals(listOf(0L, 1L), result.tasksToCreate.map { it.order })
        assertFalse(result.tasksToCreate.any { it.isDeleted })
    }
}
