package com.romankozak.forwardappmobile.data.product

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionRepository
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningScopeExtensionRepository
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningScopeExtensionRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
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
import com.romankozak.forwardappmobile.shared.core.domain.planning.StrategicPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeType
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalOrientationProductProjection
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Epic C live canonical Planning acceptance.
 *
 * Uses real Android Room persistence and production canonical repositories.
 * Exercises the shared Planning authority independently of specialized
 * Strategic/Tactical/Day compatibility-carrier authoring.
 *
 * No production application database or user records are modified.
 */
@RunWith(AndroidJUnit4::class)
class CanonicalPlanningLiveAcceptanceTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DB_NAME)
        database = openDatabase()
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
        context.deleteDatabase(DB_NAME)
    }

    @Test
    fun oneOrientationParticipatesIndependentlyInThreeHorizonsAndSurvivesRestart() = runBlocking {
        var orientationRepository = orientationRepository()
        var planningRepository = planningRepository()

        val orientationId = orientationRepository.createOrientation(
            kind = OrientationKind.THEME,
            title = "EPIC_C_LIVE_ACCEPTANCE_${UUID.randomUUID()}",
            description = "One semantic identity across three planning horizons",
            now = CREATED_AT,
        )

        val strategicId = PlanningScopeId(UUID.randomUUID().toString())
        val tacticalId = PlanningScopeId(UUID.randomUUID().toString())
        val dayId = PlanningScopeId(UUID.randomUUID().toString())

        val strategicScope = scope(strategicId, PlanningScopeKind.STRATEGIC_ARC)
        val tacticalScope = scope(tacticalId, PlanningScopeKind.TACTICAL_CYCLE)
        val dayScope = scope(dayId, PlanningScopeKind.DAY)

        listOf(strategicScope, tacticalScope, dayScope).forEach {
            planningRepository.saveScope(it)
        }

        CanonicalStrategicPlanningScopeExtensionRepository(
            database,
            database.strategicPlanningScopeExtensionDao(),
            database.planningDao(),
        ).save(
            StrategicPlanningScopeExtension(
                scopeId = strategicId,
                legacyArcKey = "epic-c-live-acceptance",
                createdAt = CREATED_AT,
                updatedAt = CREATED_AT,
                syncedAt = null,
                version = 1L,
                isDeleted = false,
            ),
        )

        CanonicalTacticalPlanningScopeExtensionRepository(
            database,
            database.tacticalPlanningScopeExtensionDao(),
            database.planningDao(),
        ).save(
            TacticalPlanningScopeExtension(
                scopeId = tacticalId,
                type = TacticalPlanningScopeType.OPEN_ENDED,
                weekKey = null,
                closedAt = null,
                createdAt = CREATED_AT,
                updatedAt = CREATED_AT,
                syncedAt = null,
                version = 1L,
                isDeleted = false,
            ),
        )

        CanonicalDayPlanningScopeExtensionRepository(
            database,
            database.dayPlanningScopeExtensionDao(),
            database.planningDao(),
        ).save(
            DayPlanningScopeExtension(
                scopeId = dayId,
                calendarDayStartMillis = 0L,
                outcome = null,
                plannedBudgetMinutes = null,
                createdAt = CREATED_AT,
                updatedAt = CREATED_AT,
                syncedAt = null,
                version = 1L,
                isDeleted = false,
            ),
        )

        val strategic = commitment(
            strategicId,
            orientationId,
            PlanningRoleCode.FOCUS,
            "strategic",
        )
        val tactical = commitment(
            tacticalId,
            orientationId,
            PlanningRoleCode.MISSION,
            "tactical",
            priorityLevel = PlanningPriorityLevel.MEDIUM,
        )
        val day = commitment(
            dayId,
            orientationId,
            PlanningRoleCode.DAY_THEME,
            "day",
        )

        listOf(strategic, tactical, day).forEach {
            planningRepository.saveCommitment(it)
        }

        assertOneIdentity(orientationId)

        var product = readSingleProduct()
        assertEquals(orientationId, product.orientation.subjectId)
        assertThreeMemberships(product, strategicId, tacticalId, dayId)
        assertEquals(
            setOf(orientationId),
            product.planningMemberships.map { it.commitment.orientationId }.toSet(),
        )

        val initialTitle = product.subject.title

        // Planning-local mutation must not affect another horizon.
        planningRepository.saveCommitment(
            tactical.copy(
                status = PlanningCommitmentStatus.PAUSED,
                order = 7L,
                updatedAt = MUTATION_AT,
                syncedAt = null,
                version = 2L,
            ),
        )

        assertEquals(
            PlanningCommitmentStatus.PAUSED,
            planningRepository.getCommitment(tactical.id)?.status,
        )
        assertEquals(
            PlanningCommitmentStatus.ACTIVE,
            planningRepository.getCommitment(strategic.id)?.status,
        )
        assertEquals(
            PlanningCommitmentStatus.ACTIVE,
            planningRepository.getCommitment(day.id)?.status,
        )
        assertEquals(strategic, planningRepository.getCommitment(strategic.id))
        assertEquals(day, planningRepository.getCommitment(day.id))
        assertEquals(
            initialTitle,
            database.orientationDao().getManagedSubject(orientationId)?.title,
        )

        // Semantic mutation must be shared by all appearances.
        val updatedTitle = "EPIC_C_UPDATED_$orientationId"
        orientationRepository.updateOrientationSemantics(
            subjectId = orientationId,
            title = updatedTitle,
            description = "Shared semantic update",
            now = SEMANTIC_AT,
        )

        product = readSingleProduct()
        assertEquals(updatedTitle, product.subject.title)
        assertEquals(3, product.planningMemberships.size)
        assertTrue(product.planningMemberships.all {
            it.commitment.orientationId == orientationId
        })
        assertEquals(
            PlanningCommitmentStatus.PAUSED,
            product.planningMemberships.single {
                it.scope.id == tacticalId
            }.commitment.status,
        )

        // Removing DAY participation must not remove semantic identity
        // or Strategic/Tactical participation.
        val removedDay = planningRepository.tombstoneCommitment(
            day.id,
            now = REMOVAL_AT,
        )
        assertTrue(removedDay.isDeleted)

        product = readSingleProduct()
        assertEquals(2, product.planningMemberships.size)
        assertEquals(
            setOf(strategicId, tacticalId),
            product.planningMemberships.map { it.scope.id }.toSet(),
        )
        assertNotNull(database.orientationDao().getManagedSubject(orientationId))
        assertFalse(requireNotNull(
            database.orientationDao().getManagedSubject(orientationId)
        ).isDeleted)

        // Restore the SAME canonical participation identity.
        planningRepository.restoreCommitment(
            removedDay.copy(
                isDeleted = false,
                status = PlanningCommitmentStatus.ACTIVE,
                updatedAt = RESTORE_AT,
                syncedAt = null,
                version = removedDay.version + 1L,
            ),
        )

        product = readSingleProduct()
        assertThreeMemberships(product, strategicId, tacticalId, dayId)

        // Duplicate live participation must be rejected by production validation.
        try {
            planningRepository.saveCommitment(
                day.copy(
                    id = PlanningCommitmentId(UUID.randomUUID().toString()),
                ),
            )
            fail("Duplicate live DAY.THEME participation was accepted")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("DUPLICATE_LIVE_PARTICIPATION"))
        }

        assertEquals(
            1,
            planningRepository.getLiveCommitments(dayId, PlanningRoleCode.DAY_THEME).size,
        )
        assertOneIdentity(orientationId)

        // Current scope selection is caller-owned derived read state.
        val currentProduct = readSingleProduct(setOf(tacticalId))
        assertEquals(
            setOf(tacticalId),
            currentProduct.planningMemberships
                .filter { it.isCurrentScope }
                .map { it.scope.id }
                .toSet(),
        )
        assertTrue(readSingleProduct().planningMemberships.none { it.isCurrentScope })

        // Cross a genuine disk persistence boundary.
        database.close()
        database = openDatabase()
        orientationRepository = orientationRepository()
        planningRepository = planningRepository()

        assertOneIdentity(orientationId)
        assertEquals(updatedTitle, database.orientationDao().getManagedSubject(orientationId)?.title)

        listOf(strategicScope, tacticalScope, dayScope).forEach {
            assertEquals(it, planningRepository.getScope(it.id))
        }

        assertEquals(strategic, planningRepository.getCommitment(strategic.id))
        assertEquals(
            PlanningCommitmentStatus.PAUSED,
            planningRepository.getCommitment(tactical.id)?.status,
        )
        assertEquals(7L, planningRepository.getCommitment(tactical.id)?.order)

        val restoredDay = requireNotNull(planningRepository.getCommitment(day.id))
        assertFalse(restoredDay.isDeleted)
        assertEquals(3L, restoredDay.version)

        product = readSingleProduct()
        assertEquals(updatedTitle, product.subject.title)
        assertThreeMemberships(product, strategicId, tacticalId, dayId)
        assertTrue(product.planningMemberships.none { it.isCurrentScope })

        assertEquals(
            3,
            database.planningDao().getAllCommitments()
                .count { !it.isDeleted && it.orientationId == orientationId },
        )

        println(
            "EPIC_C_LIVE_ACCEPTANCE_PASS " +
                "orientationId=$orientationId " +
                "strategicScopeId=${strategicId.value} " +
                "tacticalScopeId=${tacticalId.value} " +
                "dayScopeId=${dayId.value}"
        )
    }

    private fun openDatabase(): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DB_NAME,
        ).allowMainThreadQueries().build()

    private fun orientationRepository() =
        CanonicalOrientationRepository(database, database.orientationDao())

    private fun planningRepository() =
        CanonicalPlanningRepository(
            database,
            database.planningDao(),
            database.dayPlanningScopeExtensionDao(),
            database.tacticalPlanningScopeExtensionDao(),
        )

    private fun scope(
        id: PlanningScopeId,
        kind: PlanningScopeKind,
    ) = PlanningScope(
        id = id,
        kind = kind,
        lifecycle = PlanningScopeLifecycle.ACTIVE,
        title = "Epic C ${kind.name}",
        startsAt = null,
        endsAt = null,
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        syncedAt = null,
        version = 1L,
        isDeleted = false,
    )

    private fun commitment(
        scopeId: PlanningScopeId,
        orientationId: String,
        role: PlanningRoleCode,
        label: String,
        priorityLevel: PlanningPriorityLevel? = null,
    ) = PlanningCommitment(
        id = PlanningCommitmentId(UUID.randomUUID().toString()),
        scopeId = scopeId,
        orientationId = orientationId,
        role = role,
        order = 0L,
        priorityLevel = priorityLevel,
        status = PlanningCommitmentStatus.ACTIVE,
        provenance = PlanningProvenance(
            kind = PlanningProvenanceKind.MANUAL,
            sourceType = "EPIC_C_LIVE_ACCEPTANCE",
            sourceId = label,
        ),
        createdAt = CREATED_AT,
        updatedAt = CREATED_AT,
        syncedAt = null,
        version = 1L,
        isDeleted = false,
    )

    private suspend fun assertOneIdentity(orientationId: String) {
        assertEquals(
            1,
            database.orientationDao().getAllManagedSubjects()
                .count { it.id == orientationId },
        )
        assertEquals(
            1,
            database.orientationDao().getAllOrientations()
                .count { it.subjectId == orientationId },
        )
    }

    private suspend fun readSingleProduct(
        currentScopeIds: Set<PlanningScopeId> = emptySet(),
    ): CanonicalOrientationProductProjection {
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repository = CanonicalProductReadRepository(
                source = CanonicalProductReadRoomSource(database),
                applicationScope = applicationScope,
            )
            val result = withTimeout(15_000L) {
                repository.observe(flowOf(currentScopeIds)).first()
            }
            return when (result) {
                is CanonicalProductReadResult.Ready -> {
                    assertEquals(1, result.orientations.size)
                    result.orientations.single()
                }
                is CanonicalProductReadResult.IntegrityFailure ->
                    throw AssertionError("Canonical product integrity failure: ${result.violations}")
            }
        } finally {
            applicationScope.cancel()
        }
    }

    private fun assertThreeMemberships(
        product: CanonicalOrientationProductProjection,
        strategicId: PlanningScopeId,
        tacticalId: PlanningScopeId,
        dayId: PlanningScopeId,
    ) {
        assertEquals(3, product.planningMemberships.size)
        assertEquals(
            setOf(
                Triple(strategicId, PlanningScopeKind.STRATEGIC_ARC, PlanningRoleCode.FOCUS),
                Triple(tacticalId, PlanningScopeKind.TACTICAL_CYCLE, PlanningRoleCode.MISSION),
                Triple(dayId, PlanningScopeKind.DAY, PlanningRoleCode.DAY_THEME),
            ),
            product.planningMemberships.map {
                Triple(it.scope.id, it.scope.kind, it.commitment.role)
            }.toSet(),
        )
    }

    private companion object {
        const val DB_NAME = "epic_c_canonical_planning_live_acceptance.db"
        const val CREATED_AT = 10_000L
        const val MUTATION_AT = 20_000L
        const val SEMANTIC_AT = 30_000L
        const val REMOVAL_AT = 40_000L
        const val RESTORE_AT = 50_000L
    }
}
