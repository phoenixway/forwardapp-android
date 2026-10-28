package com.romankozak.forwardappmobile.data.planning.tactical

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TacticalMissionCarryForwardRoomTest {
    @Test fun `canonical Mission carry retains COMPONENT_OF Strategic focus`() = runBlocking {
        withDatabase { db ->
            seedCycle(db, "old")
            val runtime = adapter(db)
            val previousId = runtime.insertMission(historical().copy(id = 0, iterationId = "old"))
            val previous = requireNotNull(runtime.getMissionById(previousId))
            val source = db.planningDao().getAllCommitments().single()
            val planning = CanonicalPlanningRepository(db, db.planningDao(), db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao())
            val parentSubject = CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.ONGOING_STANDARD, "Strategic focus")
            val arc = com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope(
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId("arc"),
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind.STRATEGIC_ARC,
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle.ACTIVE, "Arc", null, null, 1, 1, null, 1, false)
            planning.saveScope(arc)
            val parent = com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment(
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId("focus"), arc.id, parentSubject,
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode.FOCUS, 0, null,
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus.ACTIVE,
                com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance(com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind.MANUAL),
                1, 1, null, 1, false)
            planning.saveCommitment(parent)
            val relations = com.romankozak.forwardappmobile.data.planning.CanonicalPlanningCommitmentRelationRepository(db)
            relations.addRelation(com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId(source.id), parent.id)
            val carried = runtime.insertMissionWithAutoOrder(carried(previous))
            val next = db.planningDao().getAllCommitments().single { it.provenanceSourceType == "TACTICAL_MISSION" && it.provenanceSourceId == carried.toString() }
            assertEquals(parent.id, relations.observeOutgoing(com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId(next.id)).first().single().targetCommitmentId)
            assertEquals(source, db.planningDao().getCommitment(source.id))
        }
    }
    @Test fun `adopted unscoped historical mission carries exact semantic identity without rewriting history`() = runBlocking {
        withDatabase { db ->
            val previous = historical()
            val subjectId = seedHistorical(db, previous)
            val subjectsBefore = db.orientationDao().getAllManagedSubjects()
            val id = adapter(db).insertMissionWithAutoOrder(carried(previous))

            assertNotEquals(previous.id, id)
            val participation = db.planningDao().getAllCommitments().single()
            assertEquals(subjectId, participation.orientationId)
            assertEquals("cycle", participation.scopeId)
            assertEquals(id.toString(), participation.provenanceSourceId)
            assertEquals(previous, db.tacticalMissionDao().getMissionById(previous.id))
            assertEquals(subjectsBefore, db.orientationDao().getAllManagedSubjects())
            assertEquals(previous.id, db.tacticalMissionDao().getMissionById(id)?.carriedFromMissionId)
            assertEquals(2, adapter(db).getAllMissions().first().size)
        }
    }

    @Test fun `historical week alias carries without inventing an old cycle or commitment`() = runBlocking {
        withDatabase { db ->
            val previous = historical().copy(iterationId = "2026-W39", weekKey = "2026-W39")
            val subjectId = seedHistorical(db, previous)
            adapter(db).insertMissionWithAutoOrder(carried(previous))
            assertEquals(subjectId, db.planningDao().getAllCommitments().single().orientationId)
            assertEquals(listOf("cycle"), db.planningDao().getAllScopes().map { it.id })
            assertEquals(previous, db.tacticalMissionDao().getMissionById(previous.id))
        }
    }

    @Test fun `ordinary canonical predecessor still reuses existing participation subject`() = runBlocking {
        withDatabase { db ->
            seedCycle(db, "old")
            val runtime = adapter(db)
            val previousId = runtime.insertMission(historical().copy(id = 0, iterationId = "old"))
            val previous = requireNotNull(runtime.getMissionById(previousId))
            val before = db.planningDao().getAllCommitments().single()
            runtime.insertMissionWithAutoOrder(carried(previous))
            val after = db.planningDao().getAllCommitments()
            assertEquals(2, after.size)
            assertEquals(setOf(before.orientationId), after.map { it.orientationId }.toSet())
            assertEquals(before, after.single { it.id == before.id })
        }
    }

    @Test fun `missing participation on an eligible modern mission still fails closed atomically`() = runBlocking {
        withDatabase { db ->
            val previous = historical().copy(iterationId = "cycle")
            db.tacticalMissionDao().insertMissionRaw(previous)
            assertFailedWithoutWrites(db, previous)
        }
    }

    @Test fun `unadopted historical mission cannot fabricate a semantic target`() = runBlocking {
        withDatabase { db ->
            val previous = historical()
            db.tacticalMissionDao().insertMissionRaw(previous)
            assertFailedWithoutWrites(db, previous)
            assertTrue(db.orientationDao().getAllManagedSubjects().isEmpty())
        }
    }

    @Test fun `deleted historical source cannot be carried or resurrected`() = runBlocking {
        withDatabase { db ->
            val original = historical()
            seedHistorical(db, original)
            val previous = original.copy(isDeleted = true)
            db.tacticalMissionDao().insertMissionRaw(previous)
            assertFailedWithoutWrites(db, previous)
        }
    }

    @Test fun `retired participation cannot fall back to historical mapping`() = runBlocking {
        withDatabase { db ->
            val previous = historical()
            val subjectId = seedHistorical(db, previous)
            val retired = previous.copy(iterationId = "cycle").toCanonicalMissionCommitment(subjectId)
            planning(db).saveCommitment(retired)
            planning(db).tombstoneCommitment(retired.id, now = 100L)
            assertFailedWithoutWrites(db, previous)
            assertTrue(db.planningDao().getAllCommitments().single().isDeleted)
        }
    }

    @Test fun `deleted semantic target rolls back new carrier and participation`() = runBlocking {
        withDatabase { db ->
            val previous = historical()
            val id = seedHistorical(db, previous)
            val subject = requireNotNull(db.orientationDao().getManagedSubject(id))
            db.orientationDao().upsertManagedSubjects(listOf(subject.copy(isDeleted = true)))
            assertFailedWithoutWrites(db, previous)
        }
    }

    private suspend fun assertFailedWithoutWrites(db: AppDatabase, previous: TacticalMission) {
        val rows = db.tacticalMissionDao().getAllMissionsSync()
        val commitments = db.planningDao().getAllCommitments()
        val subjects = db.orientationDao().getAllManagedSubjects()
        assertNotNull(runCatching { adapter(db).insertMissionWithAutoOrder(carried(previous)) }.exceptionOrNull())
        assertEquals(rows, db.tacticalMissionDao().getAllMissionsSync())
        assertEquals(commitments, db.planningDao().getAllCommitments())
        assertEquals(subjects, db.orientationDao().getAllManagedSubjects())
    }

    private fun historical() = TacticalMission(
        id = 191, title = "Historical mission", description = "Instructions", deadline = NO_DEADLINE,
        status = MissionStatus.COMPLETED, projectId = null, iterationId = null, weekKey = "2026-W39",
        createdAt = 10, updatedAt = 20, version = 3,
    )

    private fun carried(previous: TacticalMission) = previous.copy(
        id = 0, iterationId = "cycle", weekKey = "2026-W43", carriedFromMissionId = previous.id,
        sourceType = MissionSourceType.PREVIOUS_WEEK, status = MissionStatus.ACTIVE,
        createdAt = 100, updatedAt = 100, version = 1, syncedAt = null,
    )

    private suspend fun seedHistorical(db: AppDatabase, previous: TacticalMission): String {
        db.tacticalMissionDao().insertMissionRaw(previous)
        LegacyManualTacticalMissionIntentAdopter(db, orientation(db)).adopt(previous.id)
        return requireNotNull(db.orientationDao().getAllLegacyMappings().singleOrNull()).subjectId
    }

    private suspend fun seedCycle(db: AppDatabase, id: String) {
        val cycle = TacticalIteration(id = id, title = id, startedAt = 10, plannedEndAt = 20,
            weekKey = "2026-W43", createdAt = 1, updatedAt = 2, version = 1)
        db.planningDao().upsertScope(cycle.toCanonicalTacticalPlanningScope().toPlanningScopeEntity())
        db.tacticalPlanningScopeExtensionDao().upsert(cycle.toCanonicalTacticalPlanningScopeExtension().toEntity())
    }

    private fun orientation(db: AppDatabase) = CanonicalOrientationRepository(db, db.orientationDao())
    private fun planning(db: AppDatabase) = CanonicalPlanningRepository(db, db.planningDao(),
        db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao())
    private fun adapter(db: AppDatabase): CanonicalTacticalMissionAdapter {
        val activator = CanonicalTacticalPlanningAuthorityActivator(db, mockk(relaxed = true), mockk<QuestOrientationBridge>(relaxed = true))
        return CanonicalTacticalMissionAdapter(db, activator, TacticalMissionOrientationResolver(db), orientation(db),
            planning(db), CanonicalTacticalPlanningAdapter(db, activator))
    }

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            seedCycle(db, "cycle")
            db.tacticalPlanningAuthorityStateDao().upsert(TacticalPlanningAuthorityStateEntity(
                version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION, activatedAt = 100))
            block(db)
        } finally { db.close() }
    }
}
