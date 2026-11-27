package com.romankozak.forwardappmobile.data.planning

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.planning.DayPlanningScopeExtensionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity
import com.romankozak.forwardappmobile.data.planning.day.PersonalPlanningDay
import com.romankozak.forwardappmobile.data.recurrence.CanonicalRecurrenceMaterializationAdapter
import com.romankozak.forwardappmobile.data.recurrence.recurrencePlanningSources
import com.romankozak.forwardappmobile.data.recurrence.toAndroidEntity
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusTemplate
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyedRecurrencePlanningMetadataRoomTest {
    @Test
    fun `initial recurrence and Planning creation use the keyed day while merge converges on the latest actual clock`() = runBlocking {
        PlanningSourceRoomFixture().use { fixture ->
            val db = fixture.db
            val orientationId = fixture.base.orientation()
            val day = PersonalPlanningDay(LocalDate.parse("2026-11-28"), ZoneId.systemDefault())
            val dayKey = day.date.toString()
            val early = day.start + 15_000L
            val later = early + 1_000L
            db.dayPlanDao().insert(DayPlan(id = day.scopeId, date = day.start))
            db.planningDao().upsertScope(
                PlanningScopeEntity(day.scopeId, "DAY", "ACTIVE", null, null, null, 0, early, null, 1, false),
            )
            db.dayPlanningScopeExtensionDao().upsert(
                DayPlanningScopeExtensionEntity(
                    day.scopeId, day.start, null, null, 0, early, null, 1, false,
                    localDate = dayKey, timeZoneId = day.zone.id,
                ),
            )
            val rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null)
            val seriesCreatedAt = day.start - 1_000L
            db.canonicalRecurringSeriesDao().insertAll(
                listOf(
                    RecurringTaskSeries(
                        "task-series", seriesCreatedAt, seriesCreatedAt, null, false, 1, rule, dayKey, null,
                        RecurringTaskTemplate(
                            "Task", null, null, emptyList(), emptyList(), TaskPriority.NONE, null, 0,
                            planningSource = RecurringTaskPlanningSource(RecurringTaskSourceKind.ORIENTATION, orientationId),
                        ),
                    ).toAndroidEntity(),
                    RecurringFocusSeries(
                        "focus-series", seriesCreatedAt, seriesCreatedAt, null, false, 1, rule, dayKey, null,
                        RecurringFocusTemplate("Focus", null, emptyList(), null, orientationId),
                    ).toAndroidEntity(),
                ),
            )

            val materializer = CanonicalRecurrenceMaterializationAdapter(db, Dispatchers.Unconfined, recurrencePlanningSources(db))
            val plan = materializer.materializeForDate(day.start, early)
            val task = plan.tasksToCreate.single()
            val focus = plan.focusItemsToCreate.single()
            assertEquals(day.start, task.createdAt)
            assertEquals(day.start, focus.createdAt)
            assertEquals(seriesCreatedAt, task.order)
            assertEquals(seriesCreatedAt, focus.order)
            assertEquals(early, task.updatedAt)
            assertEquals(early, focus.updatedAt)

            val planning = CanonicalPlanningSyncStore(db)
            val references = CanonicalPlanningReferenceSyncStore(db)
            val localCommitment = planning.loadAllCommitments().single()
            val localReferences = db.planningReferenceDao().getAll()
            assertEquals(day.start, localCommitment.createdAt)
            assertEquals(task.order, localCommitment.order)
            assertEquals(2, localReferences.size)
            assertTrue(localReferences.all { it.createdAt == day.start && it.updatedAt == early })

            val peerCommitment = localCommitment.copy(updatedAt = later)
            val peerReferences = localReferences.map { it.copy(updatedAt = later) }
            planning.mergeIncoming(emptyList(), listOf(peerCommitment))
            references.merge(peerReferences)
            planning.mergeIncoming(emptyList(), listOf(localCommitment))
            references.merge(localReferences)

            assertEquals(peerCommitment, planning.loadAllCommitments().single())
            assertEquals(listOf(peerCommitment), planning.loadCommitmentsChangedSince(early))
            assertEquals(peerReferences, db.planningReferenceDao().getAll())
            val conflict = runCatching {
                planning.mergeIncoming(emptyList(), listOf(peerCommitment.copy(order = task.order + 1)))
            }.exceptionOrNull()
            assertTrue(conflict is PlanningMergeConflictException)
        }
    }
}
