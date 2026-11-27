package com.romankozak.forwardappmobile.data.recurrence

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesOverride
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesTargetOverrides
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot
import com.romankozak.forwardappmobile.core.sync.mergeDayTaskRealizations
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class TaskThemeAssignmentReplacementRoomTest {
    private lateinit var db: AppDatabase
    private lateinit var authoring: CanonicalTaskRecurrenceAuthoringAdapter
    private lateinit var materializer: CanonicalRecurrenceMaterializationAdapter
    private val dailyRule = RecurrenceRule(frequency = RecurrenceFrequency.DAILY, interval = 1, daysOfWeek = null)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val planningSources = recurrencePlanningSources(db)
        materializer = CanonicalRecurrenceMaterializationAdapter(db, Dispatchers.Unconfined, planningSources)
        authoring = CanonicalTaskRecurrenceAuthoringAdapter(db, materializer, planningSources)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `convert split and detach move same day memberships to the replacement tasks`() = runTest {
        val plan = insertPlan(20)
        val future = insertPlan(22)
        val previous = insertPlan(19)
        val source = insertTask(plan, "task")
        val unrelated = insertTask(plan, "unrelated")
        val previousTask = insertTask(previous, "previous")
        val sourceTheme = canonicalDayThemeId(plan.id, "source-theme")
        val sharedTheme = canonicalDayThemeId(plan.id, "shared-theme")
        val sourceThemes = listOf(sharedTheme, sourceTheme).sorted()
        val unrelatedAssignment = DayThemeAssignmentSnapshot(unrelated.id, listOf(sharedTheme))
        val originalDocument = seedDocument(
            plan,
            DayThemeAssignmentSnapshot(source.id, listOf(sharedTheme, sourceTheme, sharedTheme)),
            DayThemeAssignmentSnapshot(source.id, listOf(sourceTheme)),
            unrelatedAssignment,
        )
        val previousDocument = seedDocument(
            previous,
            DayThemeAssignmentSnapshot(previousTask.id, listOf(canonicalDayThemeId(previous.id, "previous-theme"))),
        )

        val converted = convert(source)
        assertRekeyed(originalDocument, converted, sourceThemes, unrelatedAssignment)

        materializer.materializeForDate(future.date, now = 50_000L)
        val futureOccurrence = db.dayTaskDao().getTasksForDayIncludingDeletedSync(future.id).single()
        val futureTheme = canonicalDayThemeId(future.id, "source-theme")
        val futureDocument = seedDocument(future, DayThemeAssignmentSnapshot(futureOccurrence.id, listOf(futureTheme)))
        val beforeSplit = document(plan.id)

        val split = authoring.splitSeriesFromOccurrence(
            task = converted,
            title = converted.title,
            description = converted.description,
            goalId = converted.goalId,
            projectId = converted.projectId,
            taskType = converted.taskType,
            linkedProjectIds = converted.linkedProjectIds.orEmpty(),
            linkedAttachmentIds = converted.linkedAttachmentIds.orEmpty(),
            priority = converted.priority,
            estimatedDurationMinutes = converted.estimatedDurationMinutes,
            points = converted.points,
            executionStrictness = converted.executionStrictness,
            rule = dailyRule.copy(interval = 2),
        )
        assertRekeyed(beforeSplit, split, sourceThemes, unrelatedAssignment)
        val futureReplacement = db.dayTaskDao().getTasksForDayIncludingDeletedSync(future.id).single { !it.isDeleted }
        assertRekeyed(futureDocument, futureReplacement, listOf(futureTheme))

        val beforeDetach = document(plan.id)
        val detached = authoring.detachCurrentOccurrence(split)

        assertRekeyed(beforeDetach, detached, sourceThemes, unrelatedAssignment)
        assertEquals(previousDocument, document(previous.id))
        assertEquals(unrelated, db.dayTaskDao().getTaskById(unrelated.id))
        assertNull(detached.recurrenceSeriesId)
        for (replaced in listOf(source, converted, split, futureOccurrence)) {
            assertTrue(db.dayTaskDao().getTaskById(replaced.id)?.isDeleted == true)
        }
    }

    @Test
    fun `assignment write failure rolls back task conversion`() = runTest {
        val plan = insertPlan(20)
        val task = insertTask(plan, "task")
        val originalDocument = seedDocument(
            plan,
            DayThemeAssignmentSnapshot(task.id, listOf(canonicalDayThemeId(plan.id, "theme"))),
        )
        db.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER reject_assignment_rekey BEFORE INSERT ON day_theme_assignment_documents
                BEGIN SELECT RAISE(ABORT, 'assignment rekey failure'); END""",
        )

        val failure = runCatching { convert(task) }.exceptionOrNull()

        assertTrue(failure.toString(), failure?.message.orEmpty().contains("assignment rekey failure"))
        assertEquals(originalDocument, document(plan.id))
        assertEquals(listOf(task), db.dayTaskDao().getTasksForDayIncludingDeletedSync(plan.id))
        assertTrue(db.canonicalRecurringSeriesDao().getAllSync().isEmpty())
    }

    @Test
    fun `occurrence override survives Room and transport while newer explicit reset wins`() = runTest {
        val plan = insertPlan(20)
        val converted = convert(insertTask(plan, "task"))
        val override = TaskRealizesOverride(
            themes = TaskRealizesTargetOverrides(excluded = setOf("blocked-definition")),
            priorities = TaskRealizesTargetOverrides(
                explicit = setOf("pending-priority-series"),
                inheritedLinks = mapOf("priority-series" to "day-priority"),
            ),
        )
        db.dayTaskDao().update(converted.copy(realizesOverride = override, version = 2))
        val local = requireNotNull(db.dayTaskDao().getTaskById(converted.id))
        assertEquals(override, local.realizesOverride)

        val gson = Gson()
        val snapshot = CanonicalRecurrenceSnapshotMapper.dayTaskSnapshot(local, local.toSnapshot())
        val incoming = gson.fromJson(gson.toJson(snapshot), DayTaskSnapshot::class.java)
        fun decoded(payload: DayTaskSnapshot): DayTask =
            CanonicalRecurrenceSnapshotMapper.dayTaskEntity(payload, payload.toEntity())

        assertEquals(override, decoded(incoming).realizesOverride)
        val legacy = incoming.copy(version = 3, realizesOverride = null)
        assertNull(decoded(legacy).realizesOverride)
        assertEquals(override, mergeDayTaskRealizations(local, legacy, decoded(legacy)).realizesOverride)

        val reset = incoming.copy(version = 3, realizesOverride = TaskRealizesOverride())
        val winning = mergeDayTaskRealizations(local, reset, decoded(reset))
        assertEquals(TaskRealizesOverride(), winning.realizesOverride)
        assertEquals(winning, mergeDayTaskRealizations(winning, incoming, decoded(incoming)))
        val divergent = reset.copy(version = local.version)
        assertThrows(IllegalArgumentException::class.java) {
            mergeDayTaskRealizations(local, divergent, decoded(divergent))
        }
        assertNull(authoring.detachCurrentOccurrence(local).realizesOverride)
    }

    @Test
    fun `override rejects malformed target IDs and unsupported format versions`() {
        val gson = Gson()
        for (json in listOf("""{"version":2}""", """{"version":1,"themes":{"excluded":[123]}}""")) {
            assertThrows(JsonParseException::class.java) { gson.fromJson(json, TaskRealizesOverride::class.java) }
        }
    }

    private suspend fun convert(task: DayTask): DayTask =
        authoring.convertOneOffToSeries(
            task = task,
            title = task.title,
            description = task.description,
            goalId = task.goalId,
            projectId = task.projectId,
            taskType = task.taskType,
            linkedProjectIds = task.linkedProjectIds.orEmpty(),
            linkedAttachmentIds = task.linkedAttachmentIds.orEmpty(),
            priority = task.priority,
            estimatedDurationMinutes = task.estimatedDurationMinutes,
            points = task.points,
            executionStrictness = task.executionStrictness,
            rule = dailyRule,
        )

    private suspend fun assertRekeyed(
        before: DayThemeAssignmentDocumentSnapshot,
        replacement: DayTask,
        themeIds: List<String>,
        vararg unrelated: DayThemeAssignmentSnapshot,
    ) {
        val after = document(before.dayPlanId)
        val expected = listOf(DayThemeAssignmentSnapshot(replacement.id, themeIds)) + unrelated
        assertEquals(expected.sortedBy { it.entityId }, after.assignments)
        assertEquals(before.createdAt, after.createdAt)
        assertEquals(before.version + 1, after.version)
        assertNull(after.syncedAt)
        assertFalse(after.isDeleted)
    }

    private suspend fun document(dayPlanId: String): DayThemeAssignmentDocumentSnapshot =
        requireNotNull(db.canonicalDayThemeDao().getAssignmentDocumentByDayPlanId(dayPlanId)).toCanonicalSnapshot()

    private suspend fun seedDocument(
        plan: DayPlan,
        vararg assignments: DayThemeAssignmentSnapshot,
    ): DayThemeAssignmentDocumentSnapshot {
        val document = DayThemeAssignmentDocumentSnapshot(
            dayPlanId = plan.id,
            assignments = assignments.toList(),
            createdAt = 1_000L,
            updatedAt = 1_000L,
            syncedAt = 2_000L,
            version = 7L,
            isDeleted = false,
        )
        db.canonicalDayThemeDao().upsertAssignmentDocuments(listOf(document.toCanonicalEntity()))
        return document
    }

    private suspend fun insertTask(plan: DayPlan, id: String): DayTask =
        DayTask(id = id, dayPlanId = plan.id, title = id, createdAt = 1_000L, updatedAt = 1_000L, version = 1L)
            .also { db.dayTaskDao().insert(it) }

    private suspend fun insertPlan(day: Int): DayPlan {
        val date = Calendar.getInstance().apply { clear(); set(2026, Calendar.AUGUST, day) }.timeInMillis
        return DayPlan(id = "plan-$day", date = date, createdAt = 1_000L, updatedAt = 1_000L, version = 1L)
            .also { db.dayPlanDao().insert(it) }
    }
}
