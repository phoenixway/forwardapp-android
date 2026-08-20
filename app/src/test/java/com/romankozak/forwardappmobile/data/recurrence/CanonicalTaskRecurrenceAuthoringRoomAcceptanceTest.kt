package com.romankozak.forwardappmobile.data.recurrence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as AppContext
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class CanonicalTaskRecurrenceAuthoringRoomAcceptanceTest {
    private lateinit var db: AppDatabase
    private lateinit var materializer: CanonicalRecurrenceMaterializationAdapter
    private lateinit var authoring: CanonicalTaskRecurrenceAuthoringAdapter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(
                context,
                AppDatabase::class.java,
            ).allowMainThreadQueries().build()

        materializer =
            CanonicalRecurrenceMaterializationAdapter(
                appDatabase = db,
                ioDispatcher = Dispatchers.Unconfined,
            )
        authoring =
            CanonicalTaskRecurrenceAuthoringAdapter(
                appDatabase = db,
                materializationAdapter = materializer,
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `create persists canonical task series and idempotent first occurrence`() = runTest {
        val dayStart = localDayStart(2026, 8, 20)
        val plan =
            DayPlan(
                id = "plan-create",
                date = dayStart,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                version = 1,
            )
        db.dayPlanDao().insert(plan)
        db.contextDao().insert(contextFixture("project:alpha"))

        val created =
            authoring.createSeriesForPlan(
                dayPlanId = plan.id,
                title = "  Canonical create  ",
                description = "  series description  ",
                goalId = null,
                projectId = "project:alpha",
                taskType = "SUBLIST",
                linkedProjectIds =
                    listOf(
                        "project:alpha",
                        "project:beta",
                        "project:alpha",
                    ),
                linkedAttachmentIds =
                    listOf(
                        "attachment:1",
                        "attachment:1",
                    ),
                priority = TaskPriority.HIGH,
                estimatedDurationMinutes = 45L,
                points = 8,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule = dailyRule(),
            )

        val seriesId = requireNotNull(created.recurrenceSeriesId)

        assertEquals("recurrence:TASK:$seriesId:2026-08-20", created.id)
        assertEquals("2026-08-20", created.recurrenceOccurrenceDayKey)
        assertEquals(1L, created.recurrenceSourceSeriesVersion)
        assertNull(created.recurringTaskId)
        assertNull(created.nextOccurrenceTime)
        assertEquals("Canonical create", created.title)
        assertEquals("series description", created.description)
        assertEquals("project:alpha", created.projectId)
        assertEquals("SUBLIST", created.taskType)
        assertEquals(listOf("project:alpha", "project:beta"), created.linkedProjectIds)
        assertEquals(listOf("attachment:1"), created.linkedAttachmentIds)
        assertEquals(TaskPriority.HIGH, created.priority)
        assertEquals(45L, created.estimatedDurationMinutes)
        assertEquals(8, created.points)
        assertEquals(TaskExecutionStrictness.NORMAL, created.executionStrictness)

        val storedSeriesEntity = db.canonicalRecurringSeriesDao().getAllSync().single()
        val storedSeries = storedSeriesEntity.toCanonicalSeries()
        assertTrue(storedSeries is RecurringTaskSeries)
        storedSeries as RecurringTaskSeries

        assertEquals(seriesId, storedSeries.id)
        assertEquals("2026-08-20", storedSeries.startDayKey)
        assertNull(storedSeries.endDayKey)
        assertEquals(RecurrenceFrequency.DAILY, storedSeries.rule.frequency)
        assertEquals(1, storedSeries.rule.interval)
        assertEquals("Canonical create", storedSeries.template.title)
        assertEquals("series description", storedSeries.template.description)
        assertEquals("project:alpha", storedSeries.template.projectId)
        assertEquals("SUBLIST", storedSeries.template.taskType)
        assertEquals("NORMAL", storedSeries.template.executionStrictness)
        assertEquals(
            listOf("project:alpha", "project:beta"),
            storedSeries.template.linkedProjectIds,
        )
        assertEquals(listOf("attachment:1"), storedSeries.template.linkedAttachmentIds)
        assertEquals("HIGH", storedSeries.template.priority.name)
        assertEquals(45L, storedSeries.template.estimatedDurationMinutes)
        assertEquals(8, storedSeries.template.points)

        val secondPass =
            materializer.materializeForDate(
                date = dayStart,
                now = 50_000L,
            )

        assertTrue(secondPass.tasksToCreate.isEmpty())
        assertEquals(
            1,
            db.dayTaskDao().getTasksForDayIncludingDeletedSync(plan.id).size,
        )
    }

    @Test
    fun `convert one off tombstones source and preserves occurrence only state`() = runTest {
        val dayStart = localDayStart(2026, 8, 20)
        val scheduledTime = dayStart + 9L * 60L * 60L * 1_000L
        val dueTime = dayStart + 17L * 60L * 60L * 1_000L
        val plan =
            DayPlan(
                id = "plan-convert",
                date = dayStart,
                createdAt = 2_000L,
                updatedAt = 2_000L,
                version = 1,
            )
        db.dayPlanDao().insert(plan)
        db.contextDao().insert(contextFixture("project:converted"))

        val source =
            DayTask(
                id = "one-off-task",
                dayPlanId = plan.id,
                title = "Original one-off",
                scheduledTime = scheduledTime,
                dueTime = dueTime,
                notes = "occurrence-only notes",
                createdAt = 3_000L,
                updatedAt = 3_000L,
                version = 7,
            )
        db.dayTaskDao().insert(source)

        val converted =
            authoring.convertOneOffToSeries(
                task = source,
                title = "  Converted series  ",
                description = " recurring description ",
                goalId = null,
                projectId = "project:converted",
                taskType = "SUBLIST",
                linkedProjectIds = listOf("project:converted"),
                linkedAttachmentIds = listOf("attachment:converted"),
                priority = TaskPriority.HIGH,
                estimatedDurationMinutes = 30L,
                points = 13,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule = dailyRule(),
            )

        val seriesId = requireNotNull(converted.recurrenceSeriesId)

        assertEquals("recurrence:TASK:$seriesId:2026-08-20", converted.id)
        assertEquals("2026-08-20", converted.recurrenceOccurrenceDayKey)
        assertEquals(1L, converted.recurrenceSourceSeriesVersion)
        assertNull(converted.recurringTaskId)
        assertNull(converted.nextOccurrenceTime)
        assertEquals("Converted series", converted.title)
        assertEquals("recurring description", converted.description)
        assertEquals("project:converted", converted.projectId)
        assertEquals("SUBLIST", converted.taskType)
        assertEquals(TaskPriority.HIGH, converted.priority)
        assertEquals(30L, converted.estimatedDurationMinutes)
        assertEquals(13, converted.points)

        assertEquals(scheduledTime, converted.scheduledTime)
        assertEquals(dueTime, converted.dueTime)
        assertNull(converted.activityRecordId)
        assertEquals("occurrence-only notes", converted.notes)

        val sourceAfter = requireNotNull(db.dayTaskDao().getTaskById(source.id))
        assertTrue(sourceAfter.isDeleted)
        assertEquals(8L, sourceAfter.version)
        assertNull(sourceAfter.recurrenceSeriesId)
        assertNull(sourceAfter.recurrenceOccurrenceDayKey)
        assertNull(sourceAfter.recurrenceSourceSeriesVersion)

        val storedSeries =
            db.canonicalRecurringSeriesDao()
                .getAllSync()
                .single()
                .toCanonicalSeries()
        assertTrue(storedSeries is RecurringTaskSeries)
        storedSeries as RecurringTaskSeries
        assertEquals(seriesId, storedSeries.id)
        assertEquals("Converted series", storedSeries.template.title)
        assertEquals("project:converted", storedSeries.template.projectId)
        assertEquals("SUBLIST", storedSeries.template.taskType)
        assertEquals("NORMAL", storedSeries.template.executionStrictness)

        val secondPass =
            materializer.materializeForDate(
                date = dayStart,
                now = 60_000L,
            )
        assertTrue(secondPass.tasksToCreate.isEmpty())

        val allTasks = db.dayTaskDao().getTasksForDayIncludingDeletedSync(plan.id)
        assertEquals(2, allTasks.size)
        assertEquals(1, allTasks.count { it.isDeleted })
        assertEquals(1, allTasks.count { !it.isDeleted })
    }

    @Test
    fun `stale one off conversion rolls back series and occurrence atomically`() = runTest {
        val dayStart = localDayStart(2026, 8, 20)
        val plan =
            DayPlan(
                id = "plan-stale",
                date = dayStart,
                createdAt = 4_000L,
                updatedAt = 4_000L,
                version = 1,
            )
        db.dayPlanDao().insert(plan)

        val staleSnapshot =
            DayTask(
                id = "stale-one-off",
                dayPlanId = plan.id,
                title = "Stale snapshot",
                createdAt = 5_000L,
                updatedAt = 5_000L,
                version = 3,
            )
        val newerStored =
            staleSnapshot.copy(
                title = "Newer stored state",
                updatedAt = 6_000L,
                version = 4,
            )
        db.dayTaskDao().insert(newerStored)

        val result =
            runCatching {
                authoring.convertOneOffToSeries(
                    task = staleSnapshot,
                    title = "Should roll back",
                    description = null,
                    goalId = null,
                    projectId = null,
                    taskType = null,
                    linkedProjectIds = emptyList(),
                    linkedAttachmentIds = emptyList(),
                    priority = TaskPriority.MEDIUM,
                    estimatedDurationMinutes = null,
                    points = 0,
                    executionStrictness = TaskExecutionStrictness.NORMAL,
                    rule = dailyRule(),
                )
            }

        assertTrue(result.isFailure)
        assertTrue(db.canonicalRecurringSeriesDao().getAllSync().isEmpty())

        val allTasks = db.dayTaskDao().getTasksForDayIncludingDeletedSync(plan.id)
        assertEquals(1, allTasks.size)

        val survivingSource = allTasks.single()
        assertEquals("stale-one-off", survivingSource.id)
        assertEquals("Newer stored state", survivingSource.title)
        assertEquals(4L, survivingSource.version)
        assertFalse(survivingSource.isDeleted)
        assertNull(survivingSource.recurrenceSeriesId)
        assertNull(survivingSource.recurrenceOccurrenceDayKey)
        assertNull(survivingSource.recurrenceSourceSeriesVersion)

        assertNotNull(result.exceptionOrNull())
    }

    @Test
    fun `current edit stays local while series edit updates selected and clean occurrences only`() = runTest {
        val day20 = localDayStart(2026, 8, 20)
        val day21 = localDayStart(2026, 8, 21)
        val day22 = localDayStart(2026, 8, 22)
        val day23 = localDayStart(2026, 8, 23)

        val plans =
            listOf(
                DayPlan(id = "plan-20", date = day20, createdAt = 1_000L, updatedAt = 1_000L, version = 1),
                DayPlan(id = "plan-21", date = day21, createdAt = 1_000L, updatedAt = 1_000L, version = 1),
                DayPlan(id = "plan-22", date = day22, createdAt = 1_000L, updatedAt = 1_000L, version = 1),
                DayPlan(id = "plan-23", date = day23, createdAt = 1_000L, updatedAt = 1_000L, version = 1),
            )
        plans.forEach { db.dayPlanDao().insert(it) }
        db.contextDao().insert(contextFixture("project:initial"))
        db.contextDao().insert(contextFixture("project:updated"))

        val selected =
            authoring.createSeriesForPlan(
                dayPlanId = "plan-20",
                title = "Original template",
                description = "original description",
                goalId = null,
                projectId = "project:initial",
                taskType = "SUBLIST",
                linkedProjectIds = listOf("project:initial"),
                linkedAttachmentIds = listOf("attachment:original"),
                priority = TaskPriority.MEDIUM,
                estimatedDurationMinutes = 30L,
                points = 5,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule = dailyRule(),
            )
        val seriesId = requireNotNull(selected.recurrenceSeriesId)

        materializer.materializeForDate(day21, 20_000L)
        materializer.materializeForDate(day22, 30_000L)
        materializer.materializeForDate(day23, 40_000L)

        val day21Task =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                    "recurrence:TASK:$seriesId:2026-08-21",
                ),
            )
        val customized21 =
            day21Task.copy(
                title = "Customized future occurrence",
                notes = "keep me",
                updatedAt = 41_000L,
                syncedAt = null,
                version = day21Task.version + 1,
            )
        db.dayTaskDao().update(customized21)

        val day23Task =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                    "recurrence:TASK:$seriesId:2026-08-23",
                ),
            )
        db.dayTaskDao().update(
            day23Task.copy(
                isDeleted = true,
                updatedAt = 42_000L,
                syncedAt = null,
                version = day23Task.version + 1,
            ),
        )

        val currentOnly =
            authoring.updateCurrentOccurrence(
                task = selected,
                title = "Current only",
                description = "current-only description",
                goalId = null,
                projectId = "project:initial",
                taskType = "SUBLIST",
                linkedProjectIds = listOf("project:initial"),
                linkedAttachmentIds = listOf("attachment:original"),
                priority = TaskPriority.HIGH,
                estimatedDurationMinutes = 35L,
                points = 6,
                executionStrictness = TaskExecutionStrictness.NORMAL,
            )

        assertEquals("Current only", currentOnly.title)
        assertEquals(seriesId, currentOnly.recurrenceSeriesId)
        assertEquals("2026-08-20", currentOnly.recurrenceOccurrenceDayKey)
        assertEquals(1L, currentOnly.recurrenceSourceSeriesVersion)
        assertNull(currentOnly.recurringTaskId)
        assertNull(currentOnly.nextOccurrenceTime)

        val seriesAfterCurrentEdit =
            db.canonicalRecurringSeriesDao()
                .getById(seriesId)!!
                .toCanonicalSeries() as RecurringTaskSeries
        assertEquals(1L, seriesAfterCurrentEdit.version)
        assertEquals("Original template", seriesAfterCurrentEdit.template.title)

        val clean22BeforeSeriesEdit =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                    "recurrence:TASK:$seriesId:2026-08-22",
                ),
            )
        assertEquals("Original template", clean22BeforeSeriesEdit.title)

        val selectedAfterSeriesEdit =
            authoring.updateSeriesTemplate(
                task = currentOnly,
                title = "Updated series",
                description = "updated description",
                goalId = null,
                projectId = "project:updated",
                taskType = null,
                linkedProjectIds = listOf("project:updated"),
                linkedAttachmentIds = listOf("attachment:updated"),
                priority = TaskPriority.HIGH,
                estimatedDurationMinutes = 50L,
                points = 9,
                executionStrictness = TaskExecutionStrictness.NORMAL,
            )

        assertEquals("Updated series", selectedAfterSeriesEdit.title)
        assertEquals("project:updated", selectedAfterSeriesEdit.projectId)
        assertNull(selectedAfterSeriesEdit.taskType)
        assertEquals(2L, selectedAfterSeriesEdit.recurrenceSourceSeriesVersion)

        val seriesAfter =
            db.canonicalRecurringSeriesDao()
                .getById(seriesId)!!
                .toCanonicalSeries() as RecurringTaskSeries
        assertEquals(2L, seriesAfter.version)
        assertEquals("Updated series", seriesAfter.template.title)
        assertEquals("project:updated", seriesAfter.template.projectId)
        assertNull(seriesAfter.template.taskType)
        assertEquals(9, seriesAfter.template.points)

        val customized21After =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(customized21.id),
            )
        assertEquals("Customized future occurrence", customized21After.title)
        assertEquals("keep me", customized21After.notes)
        assertEquals(1L, customized21After.recurrenceSourceSeriesVersion)
        assertEquals(customized21.version, customized21After.version)

        val clean22After =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(clean22BeforeSeriesEdit.id),
            )
        assertEquals("Updated series", clean22After.title)
        assertEquals("updated description", clean22After.description)
        assertEquals("project:updated", clean22After.projectId)
        assertNull(clean22After.taskType)
        assertEquals(TaskPriority.HIGH, clean22After.priority)
        assertEquals(50L, clean22After.estimatedDurationMinutes)
        assertEquals(9, clean22After.points)
        assertEquals(2L, clean22After.recurrenceSourceSeriesVersion)
        assertEquals(clean22BeforeSeriesEdit.version + 1, clean22After.version)

        val tombstone23After =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(day23Task.id),
            )
        assertTrue(tombstone23After.isDeleted)
        assertEquals("Original template", tombstone23After.title)
        assertEquals(1L, tombstone23After.recurrenceSourceSeriesVersion)
        assertEquals(day23Task.version + 1, tombstone23After.version)

        val rematerialized23 = materializer.materializeForDate(day23, 50_000L)
        assertTrue(rematerialized23.tasksToCreate.isEmpty())
        assertTrue(
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(day23Task.id),
            ).isDeleted,
        )
    }

    @Test
    fun `split preserves clean custom detached and tombstone task semantics`() = runTest {
        val days =
            (20..25).associateWith { day ->
                localDayStart(2026, 8, day)
            }

        days.forEach { (day, date) ->
            db.dayPlanDao().insert(
                DayPlan(
                    id = "split-plan-$day",
                    date = date,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    version = 1,
                ),
            )
        }

        val selected =
            authoring.createSeriesForPlan(
                dayPlanId = "split-plan-20",
                title = "Old template",
                description = "old description",
                goalId = null,
                projectId = null,
                taskType = "SUBLIST",
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = listOf("attachment:old"),
                priority = TaskPriority.MEDIUM,
                estimatedDurationMinutes = 30L,
                points = 5,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule = dailyRule(),
            )
        val oldSeriesId = requireNotNull(selected.recurrenceSeriesId)

        (21..25).forEach { day ->
            materializer.materializeForDate(
                date = requireNotNull(days[day]),
                now = 10_000L + day,
            )
        }

        suspend fun oldOccurrence(day: Int): DayTask =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                    "recurrence:TASK:$oldSeriesId:2026-08-${day.toString().padStart(2, '0')}",
                ),
            )

        val customized22Source = oldOccurrence(22)
        db.dayTaskDao().update(
            customized22Source.copy(
                title = "Customized matching 22",
                notes = "keep matching customization",
                updatedAt = 22_000L,
                syncedAt = null,
                version = customized22Source.version + 1,
            ),
        )

        val customized23Source = oldOccurrence(23)
        db.dayTaskDao().update(
            customized23Source.copy(
                title = "Customized excluded 23",
                notes = "detach me",
                scheduledTime = requireNotNull(days[23]) + 9L * 60L * 60L * 1_000L,
                updatedAt = 23_000L,
                syncedAt = null,
                version = customized23Source.version + 1,
            ),
        )

        val tombstone24Source = oldOccurrence(24)
        db.dayTaskDao().update(
            tombstone24Source.copy(
                isDeleted = true,
                updatedAt = 24_000L,
                syncedAt = null,
                version = tombstone24Source.version + 1,
            ),
        )

        val splitResult =
            authoring.splitSeriesFromOccurrence(
                task = selected,
                title = "New split template",
                description = "new split description",
                goalId = null,
                projectId = null,
                taskType = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = listOf("attachment:new"),
                priority = TaskPriority.HIGH,
                estimatedDurationMinutes = 45L,
                points = 11,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule =
                    RecurrenceRule(
                        frequency = RecurrenceFrequency.DAILY,
                        interval = 2,
                        daysOfWeek = null,
                    ),
            )

        val newSeriesId = requireNotNull(splitResult.recurrenceSeriesId)
        assertTrue(newSeriesId != oldSeriesId)
        assertEquals("2026-08-20", splitResult.recurrenceOccurrenceDayKey)
        assertEquals("New split template", splitResult.title)
        assertEquals("new split description", splitResult.description)
        assertNull(splitResult.taskType)
        assertEquals(TaskPriority.HIGH, splitResult.priority)
        assertEquals(45L, splitResult.estimatedDurationMinutes)
        assertEquals(11, splitResult.points)
        assertNull(splitResult.recurringTaskId)
        assertNull(splitResult.nextOccurrenceTime)

        val oldSeries =
            requireNotNull(db.canonicalRecurringSeriesDao().getById(oldSeriesId))
                .toCanonicalSeries() as RecurringTaskSeries
        val newSeries =
            requireNotNull(db.canonicalRecurringSeriesDao().getById(newSeriesId))
                .toCanonicalSeries() as RecurringTaskSeries

        assertEquals("2026-08-19", oldSeries.endDayKey)
        assertEquals(2L, oldSeries.version)
        assertEquals("2026-08-20", newSeries.startDayKey)
        assertNull(newSeries.endDayKey)
        assertEquals(RecurrenceFrequency.DAILY, newSeries.rule.frequency)
        assertEquals(2, newSeries.rule.interval)
        assertEquals("New split template", newSeries.template.title)
        assertEquals(11, newSeries.template.points)

        (20..25).forEach { day ->
            assertTrue(oldOccurrence(day).isDeleted)
        }

        val newOccurrences =
            db.canonicalRecurringSeriesDao()
                .getTaskOccurrencesForSeries(newSeriesId)
                .associateBy { requireNotNull(it.recurrenceOccurrenceDayKey) }

        assertEquals(setOf("2026-08-20", "2026-08-22", "2026-08-24"), newOccurrences.keys)

        val live20 = requireNotNull(newOccurrences["2026-08-20"])
        assertFalse(live20.isDeleted)
        assertEquals("New split template", live20.title)
        assertEquals("new split description", live20.description)
        assertEquals(1L, live20.recurrenceSourceSeriesVersion)

        val customized22 = requireNotNull(newOccurrences["2026-08-22"])
        assertFalse(customized22.isDeleted)
        assertEquals("Customized matching 22", customized22.title)
        assertEquals("keep matching customization", customized22.notes)
        assertEquals(1L, customized22.recurrenceSourceSeriesVersion)
        assertEquals(newSeriesId, customized22.recurrenceSeriesId)

        val tombstone24 = requireNotNull(newOccurrences["2026-08-24"])
        assertTrue(tombstone24.isDeleted)
        assertEquals("New split template", tombstone24.title)
        assertEquals(newSeriesId, tombstone24.recurrenceSeriesId)
        assertEquals(1L, tombstone24.recurrenceSourceSeriesVersion)

        val day23Rows =
            db.dayTaskDao().getTasksForDayIncludingDeletedSync("split-plan-23")
        val detached23 =
            requireNotNull(
                day23Rows.firstOrNull { row ->
                    !row.isDeleted &&
                        row.recurrenceSeriesId == null &&
                        row.title == "Customized excluded 23"
                },
            )
        assertTrue(detached23.id != customized23Source.id)
        assertNull(detached23.recurrenceSeriesId)
        assertNull(detached23.recurrenceOccurrenceDayKey)
        assertNull(detached23.recurrenceSourceSeriesVersion)
        assertNull(detached23.recurringTaskId)
        assertNull(detached23.nextOccurrenceTime)
        assertEquals("detach me", detached23.notes)
        assertEquals(
            requireNotNull(days[23]) + 9L * 60L * 60L * 1_000L,
            detached23.scheduledTime,
        )

        val day21Live =
            db.dayTaskDao().getTasksForDayIncludingDeletedSync("split-plan-21")
                .filter { !it.isDeleted }
        val day25Live =
            db.dayTaskDao().getTasksForDayIncludingDeletedSync("split-plan-25")
                .filter { !it.isDeleted }
        assertTrue("Unexpected live D21 rows: $day21Live", day21Live.isEmpty())
        assertTrue("Unexpected live D25 rows: $day25Live", day25Live.isEmpty())

        days.values.forEach { date ->
            val rematerialized = materializer.materializeForDate(date, 90_000L)
            assertTrue(rematerialized.tasksToCreate.isEmpty())
        }

        val logicalRows = mutableListOf<DayTask>()
        for (day in 20..25) {
            logicalRows += db.dayTaskDao().getTasksForDayIncludingDeletedSync("split-plan-$day")
        }
        val logicalKeys =
            logicalRows.mapNotNull { row ->
                val seriesId = row.recurrenceSeriesId ?: return@mapNotNull null
                val dayKey = row.recurrenceOccurrenceDayKey ?: return@mapNotNull null
                "$seriesId@$dayKey"
            }
        assertEquals(logicalKeys.size, logicalKeys.toSet().size)
    }

    @Test
    fun `delete current occurrence leaves series active and blocks only that logical day`() = runTest {
        val days =
            (20..22).associateWith { day ->
                localDayStart(2026, 8, day)
            }

        for ((day, date) in days) {
            db.dayPlanDao().insert(
                DayPlan(
                    id = "delete-plan-$day",
                    date = date,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    version = 1,
                ),
            )
        }

        val selected =
            authoring.createSeriesForPlan(
                dayPlanId = "delete-plan-20",
                title = "Delete one occurrence",
                description = "series stays alive",
                goalId = null,
                projectId = null,
                taskType = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                priority = TaskPriority.MEDIUM,
                estimatedDurationMinutes = 30L,
                points = 5,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule = dailyRule(),
            )
        val seriesId = requireNotNull(selected.recurrenceSeriesId)

        materializer.materializeForDate(
            date = requireNotNull(days[21]),
            now = 21_000L,
        )
        val day21Id = "recurrence:TASK:$seriesId:2026-08-21"
        assertFalse(
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(day21Id),
            ).isDeleted,
        )

        val deleted = authoring.deleteCurrentOccurrence(selected)
        assertTrue(deleted.isDeleted)
        assertEquals(seriesId, deleted.recurrenceSeriesId)
        assertEquals("2026-08-20", deleted.recurrenceOccurrenceDayKey)
        assertEquals(selected.version + 1, deleted.version)

        val storedDeleted =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(selected.id),
            )
        assertTrue(storedDeleted.isDeleted)
        assertEquals(seriesId, storedDeleted.recurrenceSeriesId)
        assertEquals("2026-08-20", storedDeleted.recurrenceOccurrenceDayKey)
        assertEquals(selected.version + 1, storedDeleted.version)

        val storedSeries =
            requireNotNull(db.canonicalRecurringSeriesDao().getById(seriesId))
                .toCanonicalSeries() as RecurringTaskSeries
        assertFalse(storedSeries.isDeleted)
        assertNull(storedSeries.endDayKey)
        assertEquals(1L, storedSeries.version)

        val day21AfterDelete =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(day21Id),
            )
        assertFalse(day21AfterDelete.isDeleted)

        val rematerializedDeletedDay =
            materializer.materializeForDate(
                date = requireNotNull(days[20]),
                now = 30_000L,
            )
        assertTrue(rematerializedDeletedDay.tasksToCreate.isEmpty())

        val futureMaterialization =
            materializer.materializeForDate(
                date = requireNotNull(days[22]),
                now = 31_000L,
            )
        assertEquals(1, futureMaterialization.tasksToCreate.size)

        val day22 =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                    "recurrence:TASK:$seriesId:2026-08-22",
                ),
            )
        assertFalse(day22.isDeleted)
        assertEquals(seriesId, day22.recurrenceSeriesId)
        assertEquals("2026-08-22", day22.recurrenceOccurrenceDayKey)
    }

    @Test
    fun `stop series from occurrence ends schedule and tombstones materialized future`() = runTest {
        val days =
            (20..24).associateWith { day ->
                localDayStart(2026, 8, day)
            }

        for ((day, date) in days) {
            db.dayPlanDao().insert(
                DayPlan(
                    id = "stop-plan-$day",
                    date = date,
                    createdAt = 1_000L,
                    updatedAt = 1_000L,
                    version = 1,
                ),
            )
        }

        val first =
            authoring.createSeriesForPlan(
                dayPlanId = "stop-plan-20",
                title = "Stop canonical series",
                description = "stop from D21",
                goalId = null,
                projectId = null,
                taskType = null,
                linkedProjectIds = emptyList(),
                linkedAttachmentIds = emptyList(),
                priority = TaskPriority.HIGH,
                estimatedDurationMinutes = 45L,
                points = 8,
                executionStrictness = TaskExecutionStrictness.NORMAL,
                rule = dailyRule(),
            )
        val seriesId = requireNotNull(first.recurrenceSeriesId)

        for (day in 21..23) {
            materializer.materializeForDate(
                date = requireNotNull(days[day]),
                now = 20_000L + day,
            )
        }

        val stopOccurrence =
            requireNotNull(
                db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                    "recurrence:TASK:$seriesId:2026-08-21",
                ),
            )
        authoring.stopSeriesFromOccurrence(stopOccurrence)

        val stoppedSeries =
            requireNotNull(db.canonicalRecurringSeriesDao().getById(seriesId))
                .toCanonicalSeries() as RecurringTaskSeries
        assertFalse(stoppedSeries.isDeleted)
        assertEquals("2026-08-20", stoppedSeries.endDayKey)
        assertEquals(2L, stoppedSeries.version)

        val occurrences =
            db.canonicalRecurringSeriesDao()
                .getTaskOccurrencesForSeries(seriesId)
                .associateBy { requireNotNull(it.recurrenceOccurrenceDayKey) }

        val day20 = requireNotNull(occurrences["2026-08-20"])
        assertFalse(day20.isDeleted)
        assertEquals(seriesId, day20.recurrenceSeriesId)

        for (day in 21..23) {
            val dayKey = "2026-08-${day.toString().padStart(2, '0')}"
            val tombstone = requireNotNull(occurrences[dayKey])
            assertTrue(tombstone.isDeleted)
            assertEquals(seriesId, tombstone.recurrenceSeriesId)
            assertEquals(dayKey, tombstone.recurrenceOccurrenceDayKey)
            assertNull(tombstone.recurringTaskId)
            assertNull(tombstone.nextOccurrenceTime)
        }

        for (day in 21..23) {
            val rematerialized =
                materializer.materializeForDate(
                    date = requireNotNull(days[day]),
                    now = 40_000L + day,
                )
            assertTrue(rematerialized.tasksToCreate.isEmpty())
        }

        val afterStop =
            materializer.materializeForDate(
                date = requireNotNull(days[24]),
                now = 50_000L,
            )
        assertTrue(afterStop.tasksToCreate.isEmpty())
        assertNull(
            db.dayTaskDao().getByIdForCanonicalRecurrenceSync(
                "recurrence:TASK:$seriesId:2026-08-24",
            ),
        )
    }

    private fun contextFixture(id: String): AppContext =
        AppContext(
            id = id,
            name = id,
            parentId = null,
            description = null,
            createdAt = 100L,
            updatedAt = 100L,
            isExpanded = true,
            isDeleted = false,
            version = 1L,
            tags = emptyList(),
            relatedLinks = emptyList(),
            order = 0L,
            isAttachmentsExpanded = false,
            defaultViewModeName = null,
            isCompleted = false,
            isContextManagementEnabled = false,
            contextStatus = "NO_PLAN",
            contextStatusText = null,
            contextLogLevel = null,
            totalTimeSpentMinutes = 0L,
            valueImportance = 0f,
            valueImpact = 0f,
            effort = 0f,
            cost = 0f,
            risk = 0f,
            weightEffort = 1f,
            weightCost = 1f,
            weightRisk = 1f,
            rawScore = 0f,
            displayScore = 0,
            scoringStatus = "NOT_ASSESSED",
            showCheckboxes = false,
            roleCode = null,
        )

    private fun dailyRule(): RecurrenceRule =
        RecurrenceRule(
            frequency = RecurrenceFrequency.DAILY,
            interval = 1,
            daysOfWeek = null,
        )

    private fun localDayStart(
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        Calendar.getInstance().run {
            clear()
            set(year, month - 1, day, 0, 0, 0)
            timeInMillis
        }
}
