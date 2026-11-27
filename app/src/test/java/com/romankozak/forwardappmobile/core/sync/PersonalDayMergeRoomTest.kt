package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningSyncStore
import com.romankozak.forwardappmobile.data.planning.PlanningSourceRoomFixture
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningAdapter
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.day.PersonalPlanningDay
import com.romankozak.forwardappmobile.data.planning.day.verifyPersonalPlanningDay
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class PersonalDayMergeRoomTest {
    private val key = PersonalPlanningDay(LocalDate.parse("2030-03-31"), ZoneId.of("Europe/Kyiv"))

    @Test
    fun independentCreationConvergesInEitherDirectionAndPreservesSubsequentEdits() = runBlocking {
        for (startOnSecond in listOf(false, true)) {
            PlanningSourceRoomFixture().use { a ->
                PlanningSourceRoomFixture().use { b ->
                    val left = a.db
                    val right = b.db
                    val first = if (startOnSecond) right else left
                    val second = if (startOnSecond) left else right
                    assertEquals(key.scopeId, adapter(left).createOrUpdate(key.start, null, 1000, key.zone).id)
                    assertEquals(key.scopeId, adapter(right).createOrUpdate(key.start, null, 2000, key.zone).id)
                    // Opening it again leaves both devices on their common first revision.
                    adapter(right).createOrUpdate(key.start, null, 3000, key.zone)
                    val pull = createRoomMergeSubject(first)
                    val push = createRoomMergeSubject(second)
                    pull.applySnapshotBundle(export(second))
                    push.applySnapshotBundle(export(first))
                    val initial = export(first)
                    assertEquals(initial, export(second))
                    assertEquals(1L, initial.planningScopes!!.single().version)
                    assertEquals(2000L, initial.planningScopes!!.single().updatedAt)
                    assertEquals(key.zone.id, initial.dayPlanningScopeExtensions!!.single().timeZoneId)
                    pull.applySnapshotBundle(initial)
                    assertEquals(initial, export(first))

                    adapter(second).updatePlannedBudget(key.scopeId, 45, 4000)
                    pull.applySnapshotBundle(export(second))
                    adapter(first).updateStatus(key.scopeId, DayStatus.IN_PROGRESS, 5000)
                    push.applySnapshotBundle(export(first))
                    assertEquals(45L, adapter(second).get(key.scopeId)?.predictedDurationMinutes)
                    assertEquals(DayStatus.IN_PROGRESS, adapter(second).get(key.scopeId)?.status)
                    val edited = export(first)
                    pull.applySnapshotBundle(initial)
                    assertEquals(edited.planningScopes, export(first).planningScopes)
                    assertEquals(edited.dayPlanningScopeExtensions, export(first).dayPlanningScopeExtensions)
                }
            }
        }
    }

    @Test
    fun equalVersionIncompatibleDayEditRollsBackCarrierAndScope() = runBlocking {
        PlanningSourceRoomFixture().use { a ->
            PlanningSourceRoomFixture().use { b ->
                adapter(a.db).createOrUpdate(key.start, null, 1000, key.zone)
                adapter(b.db).createOrUpdate(key.start, null, 2000, key.zone)
                adapter(a.db).createOrUpdate(key.start, "Left", 3000, key.zone)
                adapter(b.db).createOrUpdate(key.start, "Right", 4000, key.zone)
                val before = export(a.db)
                val peer = export(b.db)
                val failure = runCatching { createRoomMergeSubject(a.db).applySnapshotBundle(peer) }.exceptionOrNull()
                assertTrue(failure is IllegalArgumentException)
                assertEquals(before, export(a.db))
            }
        }
    }

    @Test
    fun equalMidnightInDifferentZonesUsesTwoPlansAndLegacyEvidenceStaysUnknown() = runBlocking {
        PlanningSourceRoomFixture().use { f ->
            val other = PersonalPlanningDay(key.date, ZoneId.of("Europe/Helsinki"))
            assertEquals(key.start, other.start)
            assertNotEquals(key.scopeId, other.scopeId)
            val plans = adapter(f.db)
            plans.createOrUpdate(key.start, null, 1000, key.zone)
            plans.createOrUpdate(other.start, null, 2000, other.zone)
            assertEquals(listOf(key.scopeId), f.db.dayPlanDao().getActivePersonalDayPlans(key.start, key.scopeId).map { it.id })
            assertEquals(listOf(other.scopeId), f.db.dayPlanDao().getActivePersonalDayPlans(other.start, other.scopeId).map { it.id })
            assertNull(verifyPersonalPlanningDay("old", key.start, null, null))
            assertFalse(runCatching { verifyPersonalPlanningDay(key.scopeId, key.start, key.date.toString(), other.zone.id) }.isSuccess)
        }
    }

    private fun adapter(db: AppDatabase) =
        CanonicalDayPlanningAdapter(db, db.dayPlanDao(), db.planningDao(), db.dayPlanningScopeExtensionDao(), mockk(relaxed = true))

    private suspend fun export(db: AppDatabase): SnapshotBundle {
        val planning = CanonicalPlanningSyncStore(db)
        return SnapshotBundle(
            version = 2,
            planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
            dayPlans = db.dayPlanDao().getAllPlansSync().map { it.toSnapshot() },
            planningScopes = planning.loadAllScopes(),
            planningCommitments = planning.loadAllCommitments(),
            dayPlanningScopeExtensions = CanonicalDayPlanningScopeExtensionSyncStore(db).loadAll(),
            tacticalPlanningScopeExtensions = emptyList(),
            strategicPlanningScopeExtensions = emptyList(),
        )
    }
}
