package com.romankozak.forwardappmobile.data.planning.tactical

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalTacticalPlanningAdapterRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `reader uses canonical scope and extension instead of stale compatibility row`() =
        runBlocking {
            withDatabase { db ->
                val canonical = iteration(title = "Canonical")
                seedActivated(db, canonical)

                db.tacticalIterationDao().insert(
                    canonical.copy(
                        title = "STALE LEGACY",
                        status = TacticalIterationStatus.CLOSED,
                    ),
                )

                val projected = adapter(db).observeIterations().first().single()

                assertEquals("Canonical", projected.title)
                assertEquals(TacticalIterationStatus.ACTIVE, projected.status)
                assertEquals(canonical.weekKey, projected.weekKey)
            }
        }

    @Test
    fun `writer updates canonical owners and only projects compatibility row`() =
        runBlocking {
            withDatabase { db ->
                val initial = iteration(title = "Initial")
                seedActivated(db, initial)
                val runtime = adapter(db)

                val updated =
                    requireNotNull(runtime.getCurrentIteration()).copy(
                        title = "Updated",
                        status = TacticalIterationStatus.CLOSED,
                        closedAt = 500L,
                        updatedAt = 500L,
                        syncedAt = null,
                        version = initial.version + 1L,
                    )

                runtime.saveIteration(updated)

                assertEquals(
                    "Updated",
                    requireNotNull(db.planningDao().getScope(initial.id)).title,
                )
                assertEquals(
                    500L,
                    requireNotNull(
                        db.tacticalPlanningScopeExtensionDao().get(initial.id),
                    ).closedAt,
                )
                assertEquals(
                    "Updated",
                    requireNotNull(db.tacticalIterationDao().getById(initial.id)).title,
                )
                assertEquals(
                    TacticalIterationStatus.CLOSED,
                    requireNotNull(db.tacticalIterationDao().getById(initial.id)).status,
                )
            }
        }

    @Test
    fun `missing marker fails closed instead of reading specialized iteration`() =
        runBlocking {
            withDatabase { db ->
                db.tacticalIterationDao().insert(iteration())

                val failure = runCatching { adapter(db).getCurrentIteration() }.exceptionOrNull()

                assertTrue(
                    failure?.message?.contains("not activated") == true,
                )
            }
        }

    @Test
    fun `missing canonical extension fails closed instead of falling back`() =
        runBlocking {
            withDatabase { db ->
                val source = iteration()
                db.planningDao().upsertScope(
                    source.toCanonicalTacticalPlanningScope().toPlanningScopeEntity(),
                )
                db.tacticalPlanningAuthorityStateDao().upsert(
                    TacticalPlanningAuthorityStateEntity(
                        version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION,
                        activatedAt = 1L,
                    ),
                )
                db.tacticalIterationDao().insert(source)

                val failure = runCatching { adapter(db).getCurrentIteration() }.exceptionOrNull()

                assertTrue(
                    failure?.message?.contains("missing its extension") == true,
                )
            }
        }

    private suspend fun seedActivated(
        db: AppDatabase,
        source: TacticalIteration,
    ) {
        db.planningDao().upsertScope(
            source.toCanonicalTacticalPlanningScope().toPlanningScopeEntity(),
        )
        db.tacticalPlanningScopeExtensionDao().upsert(
            source.toCanonicalTacticalPlanningScopeExtension().toEntity(),
        )
        db.tacticalPlanningAuthorityStateDao().upsert(
            TacticalPlanningAuthorityStateEntity(
                version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION,
                activatedAt = 1L,
            ),
        )
    }

    private fun adapter(db: AppDatabase): CanonicalTacticalPlanningAdapter =
        CanonicalTacticalPlanningAdapter(
            database = db,
            authorityActivator =
                CanonicalTacticalPlanningAuthorityActivator(
                    database = db,
                    manualIntentAdopter = mockk(relaxed = true),
                    questOrientationBridge = mockk<QuestOrientationBridge>(relaxed = true),
                ),
        )

    private fun iteration(
        title: String = "Cycle",
    ) = TacticalIteration(
        id = "cycle",
        title = title,
        startedAt = 100L,
        plannedEndAt = 700L,
        status = TacticalIterationStatus.ACTIVE,
        type = TacticalIterationType.TIMEBOXED,
        weekKey = "2026-W40",
        createdAt = 90L,
        updatedAt = 100L,
        version = 3L,
    )

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }
}
