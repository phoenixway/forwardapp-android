package com.romankozak.forwardappmobile.data.planning.strategic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningCommitmentRelationRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StrategicArcReorderAtomicityRoomTest {
    @Test fun `successful reorder updates canonical and carrier order together without changing semantics or component order`() = runBlocking {
        fixture { f ->
            val before = f.db.planningDao().getAllCommitments().associateBy { it.id }
            val subjects = f.db.orientationDao().getAllManagedSubjects()
            val orientations = f.db.orientationDao().getAllOrientations()
            val relations = f.db.planningCommitmentRelationDao().getAll()
            val historical = f.db.arcQuestDao().getById("historical")
            f.runtime.reorder(f.desired())
            assertEquals(listOf("b", "c", "a"), f.db.arcQuestDao().getAllSync()
                .filter { it.arcKey == ARC }.sortedBy { it.order }.map { it.id })
            f.db.planningDao().getAllCommitments().forEach { row ->
                val old = before.getValue(row.id)
                if (row.scopeId == f.currentScope()) {
                    assertEquals(old.version + 1, row.version)
                    assertNull(row.syncedAt)
                    val carrier = f.db.arcQuestDao().getAllSync().single { it.sourceId == row.orientationId && it.arcKey == ARC }
                    assertEquals(carrier.order, row.commitmentOrder)
                    assertNull(carrier.syncedAt)
                } else assertEquals(old, row)
            }
            assertEquals(historical, f.db.arcQuestDao().getById("historical"))
            assertEquals(subjects, f.db.orientationDao().getAllManagedSubjects())
            assertEquals(orientations, f.db.orientationDao().getAllOrientations())
            assertEquals(relations, f.db.planningCommitmentRelationDao().getAll())
        }
    }

    @Test fun `unchanged and empty order do not bump versions or dirty records`() = runBlocking {
        fixture { f ->
            val before = f.snapshot()
            f.runtime.reorder(listOf("a", "b", "c").map { requireNotNull(f.runtime.getById(it)) })
            f.runtime.reorder(emptyList())
            assertEquals(before, f.snapshot())
        }
    }

    @Test fun `late carrier SQL failure rolls back earlier carriers commitments and metadata`() = runBlocking {
        fixture { f ->
            val before = f.snapshot()
            f.db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_last_carrier BEFORE UPDATE ON arc_quests WHEN NEW.id = 'a' " +
                    "BEGIN SELECT RAISE(ABORT, 'late carrier failure'); END",
            )
            assertTrue(runCatching { f.runtime.reorder(f.desired()) }.isFailure)
            assertEquals(before, f.snapshot())
        }
    }

    @Test fun `late canonical SQL failure rolls back earlier carrier and canonical changes`() = runBlocking {
        fixture { f ->
            val before = f.snapshot()
            val last = f.db.planningDao().getAllCommitments().single {
                it.orientationId == requireNotNull(f.runtime.getById("a")).sourceId
            }
            f.db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_last_commitment BEFORE INSERT ON planning_commitments WHEN NEW.id = '${last.id}' " +
                    "BEGIN SELECT RAISE(ABORT, 'late canonical failure'); END",
            )
            assertTrue(runCatching { f.runtime.reorder(f.desired()) }.isFailure)
            assertEquals(before, f.snapshot())
        }
    }

    @Test fun `missing late target rolls back all prior writes`() = runBlocking {
        fixture { f ->
            val before = f.snapshot()
            val desired = f.desired().dropLast(1) + ArcQuestEntity(id = "missing", arcKey = ARC, title = "Missing")
            assertTrue(runCatching { f.runtime.reorder(desired) }.isFailure)
            assertEquals(before, f.snapshot())
        }
    }

    @Test fun `cancel after first carrier write restores entire snapshot and next reorder can commit`() = runBlocking {
        fixture { f ->
            val before = f.snapshot()
            val reachedSecondWrite = CompletableDeferred<Long>()
            var writes = 0
            coEvery { f.planning.saveCommitment(any()) } coAnswers {
                f.realPlanning.saveCommitment(firstArg())
                if (++writes == 2) {
                    reachedSecondWrite.complete(requireNotNull(f.db.arcQuestDao().getById("b")).order)
                    awaitCancellation()
                }
            }
            val desired = f.desired()
            val job = launch { f.runtime.reorder(desired) }
            try {
                assertEquals(0L, withTimeout(30_000) { reachedSecondWrite.await() })
            } finally {
                job.cancelAndJoin()
            }
            assertEquals(before, f.snapshot())
            coEvery { f.planning.saveCommitment(any()) } coAnswers { f.realPlanning.saveCommitment(firstArg()) }
            f.runtime.reorder(desired)
            assertEquals(listOf("b", "c", "a"), f.db.arcQuestDao().getAllSync()
                .filter { it.arcKey == ARC }.sortedBy { it.order }.map { it.id })
        }
    }

    private class Fixture(val db: AppDatabase) {
        val realPlanning = CanonicalPlanningRepository(db, db.planningDao(), db.dayPlanningScopeExtensionDao(), db.tacticalPlanningScopeExtensionDao())
        val planning = spyk(realPlanning)
        private val bridge = QuestOrientationBridge(db.orientationDao(), mockk<CanonicalOrientationBootstrapper>())
        val runtime = CanonicalStrategicPlanningAdapter(db,
            CanonicalStrategicPlanningAuthorityActivator(db, bridge, mockk(relaxed = true)), planning,
            CanonicalOrientationRepository(db, db.orientationDao()), bridge)

        suspend fun seed() {
            db.strategicPlanningAuthorityStateDao().upsert(StrategicPlanningAuthorityStateEntity(
                version = CanonicalStrategicPlanningAuthorityActivator.CURRENT_VERSION, activatedAt = 1))
            listOf("a", "b", "c").forEachIndexed { i, id ->
                runtime.addQuest(ArcQuestEntity(id = id, arcKey = ARC, title = id, order = i.toLong()))
            }
            runtime.addQuest(ArcQuestEntity(id = "historical", arcKey = "2026-09", title = "History"))
            val commitments = db.planningDao().getAllCommitments()
            val source = commitments.single { it.orientationId == requireNotNull(runtime.getById("b")).sourceId }
            val target = commitments.single { it.orientationId == requireNotNull(runtime.getById("c")).sourceId }
            CanonicalPlanningCommitmentRelationRepository(db).addRelation(PlanningCommitmentId(source.id), PlanningCommitmentId(target.id))
            db.arcQuestDao().getAllSync().forEach { db.arcQuestDao().update(it.copy(syncedAt = it.updatedAt ?: it.createdAt)) }
            commitments.forEach { db.planningDao().upsertCommitment(it.copy(syncedAt = it.updatedAt ?: it.createdAt)) }
        }

        suspend fun desired() = listOf("b", "c", "a").map { requireNotNull(runtime.getById(it)) }
        suspend fun currentScope() = db.planningDao().getAllCommitments().single {
            it.orientationId == requireNotNull(runtime.getById("a")).sourceId
        }.scopeId
        suspend fun snapshot(): List<Any> = listOf(
            db.arcQuestDao().getAllSync().sortedBy { it.id },
            db.planningDao().getAllCommitments().sortedBy { it.id },
            db.planningDao().getAllScopes().sortedBy { it.id },
            db.orientationDao().getAllManagedSubjects().sortedBy { it.id },
            db.orientationDao().getAllOrientations().sortedBy { it.subjectId },
            db.planningCommitmentRelationDao().getAll().sortedBy { it.id },
        )
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try { val f = Fixture(db); f.seed(); block(f) } finally { db.close() }
    }

    private companion object { const val ARC = "2026-11" }
}
