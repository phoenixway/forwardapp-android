package com.romankozak.forwardappmobile.data.planning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenance
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalPlanningRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `scope and commitment round trip preserve canonical fields and role code`() = runBlocking {
        val database = database()
        try {
            seedOrientation(database, ORIENTATION_A)
            val repository = repository(database)
            val scope = scope(title = "Cycle 8", startsAt = 100L, endsAt = 900L)
            val commitment =
                commitment(
                    id = "commitment-theme",
                    orientationId = ORIENTATION_A,
                    role = PlanningRoleCode.DAY_THEME,
                    priorityLevel = PlanningPriorityLevel.HIGH,
                )

            repository.saveScope(scope)
            repository.saveCommitment(commitment)

            assertEquals(scope, repository.getScope(scope.id))
            assertEquals(commitment, repository.getCommitment(commitment.id))
            val row = requireNotNull(database.planningDao().getCommitment(commitment.id.value))
            assertEquals("DAY.THEME", row.role)
            assertEquals("HIGH", row.priorityLevel)
            assertEquals("IMPORTED", row.provenanceKind)
            assertEquals("fixture", row.provenanceSourceType)
            assertEquals("source-1", row.provenanceSourceId)
        } finally {
            database.close()
        }
    }

    @Test
    fun `role scoped reads order by order then durable id`() = runBlocking {
        val database = database()
        try {
            listOf(ORIENTATION_A, ORIENTATION_B, ORIENTATION_C).forEach {
                seedOrientation(database, it)
            }
            val repository = repository(database)
            repository.saveScope(scope())
            repository.saveCommitment(commitment("commitment-z", ORIENTATION_A, order = 2L))
            repository.saveCommitment(commitment("commitment-b", ORIENTATION_B, order = 1L))
            repository.saveCommitment(commitment("commitment-a", ORIENTATION_C, order = 1L))

            assertEquals(
                listOf("commitment-a", "commitment-b", "commitment-z"),
                repository.getLiveCommitments(SCOPE_ID, PlanningRoleCode.MISSION)
                    .map { it.id.value },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `live uniqueness is scoped by scope orientation and role`() = runBlocking {
        val database = database()
        try {
            seedOrientation(database, ORIENTATION_A)
            val repository = repository(database)
            repository.saveScope(scope())
            repository.saveScope(scope(id = OTHER_SCOPE_ID, kind = PlanningScopeKind.DAY))
            repository.saveCommitment(commitment("mission-1", ORIENTATION_A))

            assertIllegalArgument {
                repository.saveCommitment(commitment("mission-duplicate", ORIENTATION_A))
            }

            repository.saveCommitment(
                commitment(
                    id = "focus-same-scope",
                    orientationId = ORIENTATION_A,
                    role = PlanningRoleCode.FOCUS,
                ),
            )
            repository.saveCommitment(
                commitment(
                    id = "mission-other-scope",
                    orientationId = ORIENTATION_A,
                    scopeId = OTHER_SCOPE_ID,
                ),
            )

            repository.tombstoneCommitment(PlanningCommitmentId("mission-1"), now = 300L)
            repository.saveCommitment(commitment("mission-replacement", ORIENTATION_A))

            assertEquals(3, repository.getCommitments(SCOPE_ID).size)
            assertTrue(requireNotNull(repository.getCommitment(PlanningCommitmentId("mission-1"))).isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `live commitment rejects missing or tombstoned targets`() = runBlocking {
        val database = database()
        try {
            val repository = repository(database)
            seedOrientation(database, ORIENTATION_A)

            assertIllegalArgument {
                repository.saveCommitment(commitment("missing-scope", ORIENTATION_A))
            }

            repository.saveScope(scope())
            assertIllegalArgument {
                repository.saveCommitment(commitment("missing-orientation", "orientation-missing"))
            }

            seedOrientation(database, ORIENTATION_B, isDeleted = true)
            assertIllegalArgument {
                repository.saveCommitment(commitment("deleted-orientation", ORIENTATION_B))
            }

            repository.tombstoneScope(SCOPE_ID, now = 400L)
            assertIllegalArgument {
                repository.saveCommitment(commitment("deleted-scope", ORIENTATION_A))
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun `scope tombstone preserves evidence and explicit restore does not restore commitments`() = runBlocking {
        val database = database()
        try {
            seedOrientation(database, ORIENTATION_A)
            val repository = repository(database)
            val originalScope = scope()
            val originalCommitment = commitment("commitment", ORIENTATION_A)
            repository.saveScope(originalScope)
            repository.saveCommitment(originalCommitment)

            val tombstonedScope = repository.tombstoneScope(SCOPE_ID, now = 500L)
            val tombstonedCommitment = requireNotNull(repository.getCommitment(originalCommitment.id))
            assertTrue(tombstonedScope.isDeleted)
            assertEquals(2L, tombstonedScope.version)
            assertTrue(tombstonedCommitment.isDeleted)
            assertEquals(2L, tombstonedCommitment.version)
            assertNull(tombstonedCommitment.syncedAt)
            assertFalse(requireNotNull(database.orientationDao().getManagedSubject(ORIENTATION_A)).isDeleted)

            repository.restoreScope(
                tombstonedScope.copy(
                    lifecycle = PlanningScopeLifecycle.ACTIVE,
                    updatedAt = 600L,
                    version = 3L,
                    isDeleted = false,
                ),
            )
            assertFalse(requireNotNull(repository.getScope(SCOPE_ID)).isDeleted)
            assertTrue(requireNotNull(repository.getCommitment(originalCommitment.id)).isDeleted)

            repository.restoreCommitment(
                tombstonedCommitment.copy(
                    status = PlanningCommitmentStatus.ACTIVE,
                    updatedAt = 700L,
                    version = 3L,
                    isDeleted = false,
                ),
            )
            assertFalse(requireNotNull(repository.getCommitment(originalCommitment.id)).isDeleted)
        } finally {
            database.close()
        }
    }

    private suspend fun assertIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected contract rejection.
        }
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun repository(database: AppDatabase) =
        CanonicalPlanningRepository(database, database.planningDao())

    private suspend fun seedOrientation(
        database: AppDatabase,
        id: String,
        isDeleted: Boolean = false,
    ) {
        database.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = "ORIENTATION",
                    title = id,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = isDeleted,
                    version = 1L,
                ),
            ),
        )
        database.orientationDao().upsertOrientations(
            listOf(
                OrientationEntity(
                    subjectId = id,
                    kind = "GOAL",
                    lifecycle = null,
                    lifecycleOrigin = "UNSET",
                ),
            ),
        )
    }

    private fun scope(
        id: PlanningScopeId = SCOPE_ID,
        kind: PlanningScopeKind = PlanningScopeKind.TACTICAL_CYCLE,
        title: String? = null,
        startsAt: Long? = null,
        endsAt: Long? = null,
    ) =
        PlanningScope(
            id = id,
            kind = kind,
            lifecycle = PlanningScopeLifecycle.DRAFT,
            title = title,
            startsAt = startsAt,
            endsAt = endsAt,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 25L,
            version = 1L,
            isDeleted = false,
        )

    private fun commitment(
        id: String,
        orientationId: String,
        scopeId: PlanningScopeId = SCOPE_ID,
        role: PlanningRoleCode = PlanningRoleCode.MISSION,
        order: Long = 0L,
        priorityLevel: PlanningPriorityLevel? = null,
    ) =
        PlanningCommitment(
            id = PlanningCommitmentId(id),
            scopeId = scopeId,
            orientationId = orientationId,
            role = role,
            order = order,
            priorityLevel = priorityLevel,
            status = PlanningCommitmentStatus.PLANNED,
            provenance =
                PlanningProvenance(
                    kind = PlanningProvenanceKind.IMPORTED,
                    sourceType = "fixture",
                    sourceId = "source-1",
                ),
            createdAt = 11L,
            updatedAt = 21L,
            syncedAt = 26L,
            version = 1L,
            isDeleted = false,
        )

    private companion object {
        val SCOPE_ID = PlanningScopeId("scope")
        val OTHER_SCOPE_ID = PlanningScopeId("scope-other")
        const val ORIENTATION_A = "orientation-a"
        const val ORIENTATION_B = "orientation-b"
        const val ORIENTATION_C = "orientation-c"
    }
}
