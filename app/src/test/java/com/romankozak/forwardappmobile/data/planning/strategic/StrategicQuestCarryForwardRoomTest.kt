package com.romankozak.forwardappmobile.data.planning.strategic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestStatus
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
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
class StrategicQuestCarryForwardRoomTest {
    @Test fun `historical manual quest carries without cloning identity or rewriting history`() = runBlocking {
        withDatabase { db, runtime ->
            val original = ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Old quest", description = "Plan")
            runtime.addQuest(original)
            val old = requireNotNull(runtime.getById(original.id))
            val sourceCommitment = db.planningDao().getAllCommitments().single()
            val subjects = db.orientationDao().getAllManagedSubjects()
            val mappings = db.orientationDao().getAllLegacyMappings()
            val result = runtime.carryQuest(old.id, "2026-10")
            assertTrue(result.created)
            assertNotEquals(old.id, result.quest.id)
            assertEquals("2026-10", result.quest.arcKey)
            assertEquals(old, runtime.getById(old.id))
            assertEquals(subjects, db.orientationDao().getAllManagedSubjects())
            assertEquals(mappings, db.orientationDao().getAllLegacyMappings())
            assertEquals(setOf(sourceCommitment.orientationId), db.planningDao().getAllCommitments().map { it.orientationId }.toSet())
            assertEquals(sourceCommitment, db.planningDao().getAllCommitments().single { it.id == sourceCommitment.id })
            assertEquals(listOf(result.quest), runtime.observeArcQuests("2026-10").first())
            assertEquals(sourceCommitment.orientationId, result.quest.sourceId)
        }
    }

    @Test fun `repeat returns exact existing quest without duplicate rows`() = runBlocking {
        withDatabase { db, runtime ->
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Quest"))
            val first = runtime.carryQuest("old", "2026-10")
            val second = runtime.carryQuest("old", "2026-10")
            assertFalse(second.created)
            assertEquals(first.quest.id, second.quest.id)
            assertEquals(2, db.arcQuestDao().getAllSync().size)
            assertEquals(2, db.planningDao().getAllCommitments().size)
        }
    }

    @Test fun `equal titles with different canonical subjects do not block carry`() = runBlocking {
        withDatabase { db, runtime ->
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Same"))
            runtime.addQuest(ArcQuestEntity(id = "unrelated", arcKey = "2026-10", title = "Same"))
            assertTrue(runtime.carryQuest("old", "2026-10").created)
            assertEquals(2, runtime.observeArcQuests("2026-10").first().size)
            assertEquals(2, db.orientationDao().getAllManagedSubjects().size)
        }
    }

    @Test fun `existing current participation is identified by subject not source title or carrier`() = runBlocking {
        withDatabase { db, runtime ->
            val orientation = CanonicalOrientationRepository(db, db.orientationDao())
                .createOrientation(OrientationKind.PROJECT, "Project", null, now = 10L)
            val source = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, orientation)
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Old"), source)
            runtime.addQuest(ArcQuestEntity(id = "current", arcKey = "2026-10", title = "Current"), source)
            val result = runtime.carryQuest("old", "2026-10")
            assertFalse(result.created)
            assertEquals("current", result.quest.id)
            assertEquals(1, db.orientationDao().getAllManagedSubjects().size)
        }
    }

    @Test fun `done historical participation stays done while new quest becomes active`() = runBlocking {
        withDatabase { _, runtime ->
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Done", status = ArcQuestStatus.DONE.name))
            val result = runtime.carryQuest("old", "2026-10")
            assertEquals(ArcQuestStatus.ACTIVE.name, result.quest.status)
            assertEquals(ArcQuestStatus.DONE.name, runtime.getById("old")?.status)
            assertNull(result.quest.linkedMissionId)
        }
    }

    @Test fun `deleted source fails without creating current participation`() = runBlocking {
        withDatabase { db, runtime ->
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Deleted"))
            runtime.deleteQuest(requireNotNull(runtime.getById("old")))
            val rows = db.arcQuestDao().getAllSync()
            val commitments = db.planningDao().getAllCommitments()
            assertNotNull(runCatching { runtime.carryQuest("old", "2026-10") }.exceptionOrNull())
            assertEquals(rows, db.arcQuestDao().getAllSync())
            assertEquals(commitments, db.planningDao().getAllCommitments())
        }
    }

    @Test fun `late reference failure rolls back new carrier scope and participation`() = runBlocking {
        withDatabase { db, runtime ->
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Old"))
            val rows = db.arcQuestDao().getAllSync()
            val commitments = db.planningDao().getAllCommitments()
            val scopes = db.planningDao().getAllScopes()
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_carry BEFORE INSERT ON planning_references BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
            assertNotNull(runCatching { runtime.carryQuest("old", "2026-10") }.exceptionOrNull())
            assertEquals(rows, db.arcQuestDao().getAllSync())
            assertEquals(commitments, db.planningDao().getAllCommitments())
            assertEquals(scopes, db.planningDao().getAllScopes())
        }
    }

    @Test fun `removing historical participation after carry does not delete the shared semantic source`() = runBlocking {
        withDatabase { db, runtime ->
            runtime.addQuest(ArcQuestEntity(id = "old", arcKey = "2026-09", title = "Old"))
            val result = runtime.carryQuest("old", "2026-10")
            runtime.deleteQuest(requireNotNull(runtime.getById("old")))
            assertEquals(result.quest, runtime.getById(result.quest.id))
            assertFalse(requireNotNull(db.orientationDao().getManagedSubject(requireNotNull(result.quest.sourceId))).isDeleted)
        }
    }

    @Test fun `menu eligibility admits past arcs only and fails closed on malformed keys`() {
        assertTrue(isHistoricalStrategicArc("2026-09", "2026-10"))
        assertTrue(isHistoricalStrategicArc("2025-12", "2026-01"))
        assertFalse(isHistoricalStrategicArc("2026-10", "2026-10"))
        assertFalse(isHistoricalStrategicArc("2026-11", "2026-10"))
        assertFalse(isHistoricalStrategicArc("unknown", "2026-10"))
    }

    private suspend fun withDatabase(block: suspend (AppDatabase, CanonicalStrategicPlanningAdapter) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try {
            db.strategicPlanningAuthorityStateDao().upsert(StrategicPlanningAuthorityStateEntity(
                version = CanonicalStrategicPlanningAuthorityActivator.CURRENT_VERSION, activatedAt = 1))
            val bridge = QuestOrientationBridge(db.orientationDao(), mockk<CanonicalOrientationBootstrapper>())
            val authority = CanonicalStrategicPlanningAuthorityActivator(db, bridge, mockk(relaxed = true))
            val runtime = CanonicalStrategicPlanningAdapter(db, authority,
                CanonicalPlanningRepository(db, db.planningDao(), db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao()),
                CanonicalOrientationRepository(db, db.orientationDao()), bridge)
            block(db, runtime)
        } finally { db.close() }
    }
}
