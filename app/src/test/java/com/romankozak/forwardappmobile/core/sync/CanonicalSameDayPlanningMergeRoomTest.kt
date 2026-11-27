package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningSyncStore
import com.romankozak.forwardappmobile.data.planning.PlanningSourceRoomFixture
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId
import com.romankozak.forwardappmobile.data.planning.day.toCanonicalDayPlanningScope
import com.romankozak.forwardappmobile.data.planning.day.toCanonicalDayPlanningScopeExtension
import com.romankozak.forwardappmobile.data.planning.day.toSnapshot
import com.romankozak.forwardappmobile.data.planning.toTransportSnapshot
import com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceMaterializationAdapter
import com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceSnapshotMapper
import com.romankozak.forwardappmobile.data.recurrence.CanonicalTaskRecurrenceAuthoringAdapter
import com.romankozak.forwardappmobile.data.recurrence.recurrencePlanningSources
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class CanonicalSameDayPlanningMergeRoomTest {
    @Test
    fun `same day recurring participation rejects a different scope identity and rolls back merge`() = runBlocking {
        PlanningSourceRoomFixture().use { fixture ->
            val db = fixture.db
            val orientationId = fixture.base.orientation()
            val calendarDay = Calendar.getInstance().apply {
                clear()
                set(2026, Calendar.NOVEMBER, 27)
            }.timeInMillis
            val localPlan = DayPlan(
                id = "0d9b1473-c4c8-45f3-8639-bbff30e95a20",
                date = calendarDay,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                version = 1L,
            )
            val peerPlan = localPlan.copy(
                id = "65c3fd79-a80e-4651-aca7-ea121386fae4",
                createdAt = 2_000L,
                updatedAt = 2_000L,
            )
            val planning = CanonicalPlanningSyncStore(db)
            val extensions = CanonicalDayPlanningScopeExtensionSyncStore(db)
            db.dayPlanDao().insert(localPlan)
            val localScope = localPlan.toCanonicalDayPlanningScope().toTransportSnapshot()
            val localExtension = localPlan.toCanonicalDayPlanningScopeExtension().toSnapshot()
            planning.mergeIncoming(scopes = listOf(localScope), commitments = emptyList())
            extensions.mergeIncoming(listOf(localExtension))

            val sources = recurrencePlanningSources(db)
            val materializer = CanonicalRecurrenceMaterializationAdapter(db, Dispatchers.Unconfined, sources)
            val author = CanonicalTaskRecurrenceAuthoringAdapter(db, materializer, sources)
            val localTask = author.createSeriesForPlan(
                dayPlanId = localPlan.id,
                title = "Recurring task",
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
                rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null),
                planningSource = RecurringTaskPlanningSource(RecurringTaskSourceKind.ORIENTATION, orientationId),
            )
            val localSeries = db.canonicalRecurringSeriesDao().getAllSync().single()
            val localCommitment = planning.loadAllCommitments().single()
            assertEquals(recurrenceOccurrenceId(RecurringSeriesKind.TASK, localSeries.id, "2026-11-27"), localTask.id)
            assertEquals(dayTaskParticipationId(localTask.id).value, localCommitment.id)
            assertEquals("DAY.TASK", localCommitment.role)

            val peerCreatedAt = localTask.createdAt + 1_000L
            val peerTask = localTask.copy(dayPlanId = peerPlan.id, createdAt = peerCreatedAt, updatedAt = peerCreatedAt)
            val peerCommitment = localCommitment.copy(
                scopeId = peerPlan.id,
                createdAt = peerCreatedAt,
                updatedAt = peerCreatedAt,
            )
            // This version would be written before the Planning identity check.
            val peerSeries = localSeries.copy(
                ruleInterval = 2,
                version = localSeries.version + 1,
                updatedAt = localSeries.updatedAt + 1_000L,
            )
            val incoming = SnapshotBundle(
                version = 2,
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                dayPlans = listOf(peerPlan.toSnapshot()),
                dayTasks = listOf(CanonicalRecurrenceSnapshotMapper.dayTaskSnapshot(peerTask, peerTask.toSnapshot())),
                recurringSeries = listOf(peerSeries.toSnapshot()),
                planningScopes = listOf(peerPlan.toCanonicalDayPlanningScope().toTransportSnapshot()),
                planningCommitments = listOf(peerCommitment),
                dayPlanningScopeExtensions = listOf(peerPlan.toCanonicalDayPlanningScopeExtension().toSnapshot()),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            )

            val failure = runCatching { createRoomMergeSubject(db).applySnapshotBundle(incoming) }.exceptionOrNull()

            assertTrue("Expected an unverified calendar identity conflict, received $failure", failure is DayPlanMergeIdentityConflict)
            assertEquals(listOf(localPlan), db.dayPlanDao().getAllPlansSync())
            assertEquals(listOf(localTask), db.dayTaskDao().getAllTasksSync())
            assertEquals(listOf(localScope), planning.loadAllScopes())
            assertEquals(listOf(localExtension), extensions.loadAll())
            assertEquals(listOf(localCommitment), planning.loadAllCommitments())
            assertEquals(listOf(localSeries), db.canonicalRecurringSeriesDao().getAllSync())
        }
    }
}
