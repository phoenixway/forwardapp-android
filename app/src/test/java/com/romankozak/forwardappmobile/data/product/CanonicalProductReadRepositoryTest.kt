package com.romankozak.forwardappmobile.data.product

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
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
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadResult
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.product.CanonicalProductReadViolation
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalProductReadRoomSourceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `transactional semantic create emits one coherent product frame`() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val source = CanonicalProductReadRoomSource(database)
            val frames = mutableListOf<CanonicalProductPersistenceFrame>()
            val initialFrameReceived = CompletableDeferred<Unit>()
            val collection =
                launch(Dispatchers.Default) {
                    source.observe().take(2).collect { frame ->
                        frames += frame
                        if (frames.size == 1) initialFrameReceived.complete(Unit)
                    }
                }

            withTimeout(5_000) { initialFrameReceived.await() }
            CanonicalOrientationRepository(database, database.orientationDao())
                .createOrientation(OrientationKind.INTENT, "Atomic frame", now = 10L)
            withTimeout(5_000) { collection.join() }

            assertThat(frames).hasSize(2)
            assertThat(frames).containsNoDuplicates()
            assertThat(frames.all { it is CanonicalProductPersistenceFrame.Ready }).isTrue()
            val updated = (frames.last() as CanonicalProductPersistenceFrame.Ready).snapshot
            assertThat(updated.subjects.single().title).isEqualTo("Atomic frame")
            assertThat(updated.orientations.single().subjectId).isEqualTo(updated.subjects.single().id)
        } finally {
            database.close()
        }
    }
}

class CanonicalProductReadRepositoryTest {
    @Test
    fun `same Orientation in Day Tactical Strategic stays one product identity with current scopes`() =
        runTest {
            val snapshot = allPlanningSnapshot()
            val repository =
                repository(
                    frames = flowOf(CanonicalProductPersistenceFrame.Ready(snapshot)),
                    applicationScope = backgroundScope,
                )

            val result =
                repository
                    .observe(
                        flowOf(
                            setOf(
                                PlanningScopeId("tactical"),
                                PlanningScopeId("strategic"),
                            ),
                        ),
                    ).first()

            val ready = result as CanonicalProductReadResult.Ready
            assertThat(ready.orientations).hasSize(1)

            val orientation = ready.orientations.single()
            assertThat(orientation.orientation.subjectId).isEqualTo("orientation-1")
            assertThat(orientation.planningMemberships.map { it.scope.kind })
                .containsExactly(
                    PlanningScopeKind.TACTICAL_CYCLE,
                    PlanningScopeKind.DAY,
                    PlanningScopeKind.STRATEGIC_ARC,
                ).inOrder()
            assertThat(
                orientation.planningMemberships.associate {
                    it.scope.kind to it.isCurrentScope
                },
            ).containsExactly(
                PlanningScopeKind.DAY,
                false,
                PlanningScopeKind.TACTICAL_CYCLE,
                true,
                PlanningScopeKind.STRATEGIC_ARC,
                true,
            )
        }

    @Test
    fun `membership consumer exposes generic role status and current state`() =
        runTest {
            val repository =
                repository(
                    frames = flowOf(CanonicalProductPersistenceFrame.Ready(allPlanningSnapshot())),
                    applicationScope = backgroundScope,
                )

            val read =
                repository
                    .observePlanningMemberships(
                        orientationId = "orientation-1",
                        currentScopeIds =
                            flowOf(
                                setOf(
                                    PlanningScopeId("tactical"),
                                    PlanningScopeId("strategic"),
                                ),
                            ),
                    ).first() as CanonicalPlanningMembershipRead.Ready

            assertThat(read.memberships).hasSize(3)

            val byKind = read.memberships.associateBy { it.scopeKind }

            assertThat(byKind.getValue(PlanningScopeKind.DAY).role)
                .isEqualTo(PlanningRoleCode.DAY_THEME)
            assertThat(byKind.getValue(PlanningScopeKind.DAY).status)
                .isEqualTo(PlanningCommitmentStatus.ACTIVE)
            assertThat(byKind.getValue(PlanningScopeKind.DAY).isCurrentScope).isFalse()

            assertThat(byKind.getValue(PlanningScopeKind.TACTICAL_CYCLE).role)
                .isEqualTo(PlanningRoleCode.MISSION)
            assertThat(byKind.getValue(PlanningScopeKind.TACTICAL_CYCLE).isCurrentScope).isTrue()

            assertThat(byKind.getValue(PlanningScopeKind.STRATEGIC_ARC).role)
                .isEqualTo(PlanningRoleCode.FOCUS)
            assertThat(byKind.getValue(PlanningScopeKind.STRATEGIC_ARC).isCurrentScope).isTrue()
        }

    @Test
    fun `membership consumer covers none and each individual planning kind`() =
        runTest {
            val noneRepository =
                repository(
                    frames = flowOf(CanonicalProductPersistenceFrame.Ready(baseSnapshot())),
                    applicationScope = backgroundScope,
                )
            val none =
                noneRepository
                    .observePlanningMemberships("orientation-1")
                    .first() as CanonicalPlanningMembershipRead.Ready

            assertThat(none.memberships).isEmpty()

            val cases =
                listOf(
                    PlanningScopeKind.DAY to dayOnlySnapshot(),
                    PlanningScopeKind.TACTICAL_CYCLE to tacticalOnlySnapshot(),
                    PlanningScopeKind.STRATEGIC_ARC to strategicOnlySnapshot(),
                )

            for ((expectedKind, snapshot) in cases) {
                val repository =
                    repository(
                        frames = flowOf(CanonicalProductPersistenceFrame.Ready(snapshot)),
                        applicationScope = backgroundScope,
                    )
                val read =
                    repository
                        .observePlanningMemberships("orientation-1")
                        .first() as CanonicalPlanningMembershipRead.Ready

                assertThat(read.memberships).hasSize(1)
                assertThat(read.memberships.single().scopeKind).isEqualTo(expectedKind)
                assertThat(read.memberships.single().isCurrentScope).isFalse()
            }
        }

    @Test
    fun `historical non-current membership keeps completed status`() =
        runTest {
            val repository =
                repository(
                    frames =
                        flowOf(
                            CanonicalProductPersistenceFrame.Ready(
                                dayOnlySnapshot(
                                    status = PlanningCommitmentStatus.COMPLETED,
                                ),
                            ),
                        ),
                    applicationScope = backgroundScope,
                )

            val read =
                repository
                    .observePlanningMemberships("orientation-1")
                    .first() as CanonicalPlanningMembershipRead.Ready

            val indicator = read.memberships.single()
            assertThat(indicator.scopeKind).isEqualTo(PlanningScopeKind.DAY)
            assertThat(indicator.status).isEqualTo(PlanningCommitmentStatus.COMPLETED)
            assertThat(indicator.isCurrentScope).isFalse()
        }

    @Test
    fun `source frame update reactively updates canonical product result`() =
        runBlocking {
            val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val frames =
                MutableStateFlow<CanonicalProductPersistenceFrame>(
                    CanonicalProductPersistenceFrame.Ready(baseSnapshot(title = "Before")),
                )
            val repository =
                repository(
                    frames = frames,
                    applicationScope = repositoryScope,
                )
            val emissions = mutableListOf<CanonicalProductReadResult>()
            val firstEmission = CompletableDeferred<Unit>()
            val secondEmission = CompletableDeferred<Unit>()

            val collection = launch(Dispatchers.Default) {
                repository
                    .observe()
                    .take(2)
                    .collectIndexed { index, result ->
                        emissions += result
                        if (index == 0) firstEmission.complete(Unit)
                        if (index == 1) secondEmission.complete(Unit)
                    }
            }

            withTimeout(5_000) { firstEmission.await() }
            assertThat(emissions).hasSize(1)

            frames.value =
                CanonicalProductPersistenceFrame.Ready(
                    baseSnapshot(title = "After"),
                )

            withTimeout(5_000) { secondEmission.await() }

            assertThat(emissions).hasSize(2)
            assertThat((emissions[0] as CanonicalProductReadResult.Ready).orientations.single().subject.title)
                .isEqualTo("Before")
            assertThat((emissions[1] as CanonicalProductReadResult.Ready).orientations.single().subject.title)
                .isEqualTo("After")
            collection.cancel()
            repositoryScope.cancel()
        }

    @Test
    fun `persistence integrity failure propagates through product and membership reads`() =
        runTest {
            val violation =
                CanonicalProductReadViolation(
                    path = "persistenceFrame",
                    code = "BROKEN_FRAME",
                    message = "forced",
                )
            val repository =
                repository(
                    frames =
                        flowOf(
                            CanonicalProductPersistenceFrame.IntegrityFailure(
                                listOf(violation),
                            ),
                        ),
                    applicationScope = backgroundScope,
                )

            val product = repository.observe().first()
            val membership = repository.observePlanningMemberships("orientation-1").first()

            assertThat(product).isInstanceOf(CanonicalProductReadResult.IntegrityFailure::class.java)
            assertThat(
                (product as CanonicalProductReadResult.IntegrityFailure).violations,
            ).containsExactly(violation)

            assertThat(membership)
                .isInstanceOf(CanonicalPlanningMembershipRead.IntegrityFailure::class.java)
            assertThat(
                (membership as CanonicalPlanningMembershipRead.IntegrityFailure).violations,
            ).containsExactly(violation)
        }

    private fun repository(
        frames: Flow<CanonicalProductPersistenceFrame>,
        applicationScope: CoroutineScope,
    ): CanonicalProductReadRepository {
        val source = mockk<CanonicalProductReadRoomSource>()
        every { source.observe() } returns frames
        return CanonicalProductReadRepository(
            source = source,
            applicationScope = applicationScope,
        )
    }
}

private fun baseSnapshot(
    title: String = "One",
): CanonicalProductReadSnapshot {
    val subject =
        ManagedSubject(
            id = "orientation-1",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            subjectType = ManagedSubjectType.ORIENTATION,
            title = title,
            description = null,
        )
    val orientation =
        OrientationNode(
            subjectId = subject.id,
            kind = OrientationKind.INTENT,
            lifecycle = OrientationLifecycle.ACTIVE,
            lifecycleOrigin = ValueOrigin.EXPLICIT,
            assessment = initialOrientationAssessment(OrientationKind.INTENT),
        )
    return CanonicalProductReadSnapshot(
        subjects = listOf(subject),
        orientations = listOf(orientation),
    )
}

private fun allPlanningSnapshot(): CanonicalProductReadSnapshot {
    val day = scope("day", PlanningScopeKind.DAY)
    val tactical = scope("tactical", PlanningScopeKind.TACTICAL_CYCLE)
    val strategic = scope("strategic", PlanningScopeKind.STRATEGIC_ARC)

    return baseSnapshot().copy(
        planningScopes = listOf(day, tactical, strategic),
        planningCommitments =
            listOf(
                commitment("day-c", day, PlanningRoleCode.DAY_THEME, 2L),
                commitment("tactical-c", tactical, PlanningRoleCode.MISSION, 1L),
                commitment("strategic-c", strategic, PlanningRoleCode.FOCUS, 3L),
            ),
        dayExtensions = listOf(dayExtension(day)),
        tacticalExtensions = listOf(tacticalExtension(tactical)),
        strategicExtensions = listOf(strategicExtension(strategic)),
    )
}

private fun dayOnlySnapshot(
    status: PlanningCommitmentStatus = PlanningCommitmentStatus.ACTIVE,
): CanonicalProductReadSnapshot {
    val day = scope("day", PlanningScopeKind.DAY)
    return baseSnapshot().copy(
        planningScopes = listOf(day),
        planningCommitments =
            listOf(
                commitment(
                    id = "day-c",
                    scope = day,
                    role = PlanningRoleCode.DAY_THEME,
                    status = status,
                ),
            ),
        dayExtensions = listOf(dayExtension(day)),
    )
}

private fun tacticalOnlySnapshot(): CanonicalProductReadSnapshot {
    val tactical = scope("tactical", PlanningScopeKind.TACTICAL_CYCLE)
    return baseSnapshot().copy(
        planningScopes = listOf(tactical),
        planningCommitments =
            listOf(
                commitment(
                    id = "tactical-c",
                    scope = tactical,
                    role = PlanningRoleCode.MISSION,
                ),
            ),
        tacticalExtensions = listOf(tacticalExtension(tactical)),
    )
}

private fun strategicOnlySnapshot(): CanonicalProductReadSnapshot {
    val strategic = scope("strategic", PlanningScopeKind.STRATEGIC_ARC)
    return baseSnapshot().copy(
        planningScopes = listOf(strategic),
        planningCommitments =
            listOf(
                commitment(
                    id = "strategic-c",
                    scope = strategic,
                    role = PlanningRoleCode.FOCUS,
                ),
            ),
        strategicExtensions = listOf(strategicExtension(strategic)),
    )
}

private fun scope(
    id: String,
    kind: PlanningScopeKind,
) = PlanningScope(
    id = PlanningScopeId(id),
    kind = kind,
    lifecycle = PlanningScopeLifecycle.ACTIVE,
    title = null,
    startsAt = null,
    endsAt = null,
    createdAt = 1L,
    updatedAt = 1L,
    syncedAt = null,
    version = 1L,
    isDeleted = false,
)

private fun commitment(
    id: String,
    scope: PlanningScope,
    role: PlanningRoleCode,
    order: Long = 0L,
    status: PlanningCommitmentStatus = PlanningCommitmentStatus.ACTIVE,
) = PlanningCommitment(
    id = PlanningCommitmentId(id),
    scopeId = scope.id,
    orientationId = "orientation-1",
    role = role,
    order = order,
    priorityLevel = null,
    status = status,
    provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL),
    createdAt = 1L,
    updatedAt = 1L,
    syncedAt = null,
    version = 1L,
    isDeleted = false,
)

private fun dayExtension(scope: PlanningScope) =
    DayPlanningScopeExtension(
        scopeId = scope.id,
        calendarDayStartMillis = 0L,
        outcome = null,
        plannedBudgetMinutes = null,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        version = 1L,
        isDeleted = false,
    )

private fun tacticalExtension(scope: PlanningScope) =
    TacticalPlanningScopeExtension(
        scopeId = scope.id,
        type = TacticalPlanningScopeType.TIMEBOXED,
        weekKey = "2026-W41",
        closedAt = null,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        version = 1L,
        isDeleted = false,
    )

private fun strategicExtension(scope: PlanningScope) =
    StrategicPlanningScopeExtension(
        scopeId = scope.id,
        legacyArcKey = "2026-10",
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        version = 1L,
        isDeleted = false,
    )

class CanonicalProductTombstoneSelectionTest {
    @Test
    fun `deleted subject and deleted assessment are excluded without violation`() {
        val live = orientationRow("live")
        val tombstone = orientationRow("deleted")
        val violations = mutableListOf<CanonicalProductReadViolation>()

        val selected =
            selectProductLiveOrientationRows(
                subjects = listOf(subjectRow("live", false), subjectRow("deleted", true)),
                orientations = listOf(live, tombstone),
                assessmentsByOrientation =
                    mapOf(
                        "live" to assessmentRow(false),
                        "deleted" to assessmentRow(true),
                    ),
                violations = violations,
            )

        assertThat(selected).containsExactly(live)
        assertThat(violations).isEmpty()
    }

    @Test
    fun `deleted subject with live assessment fails closed`() {
        val violations = mutableListOf<CanonicalProductReadViolation>()

        val selected =
            selectProductLiveOrientationRows(
                subjects = listOf(subjectRow("deleted", true)),
                orientations = listOf(orientationRow("deleted")),
                assessmentsByOrientation = mapOf("deleted" to assessmentRow(false)),
                violations = violations,
            )

        assertThat(selected).isEmpty()
        assertThat(violations.map { it.code })
            .containsExactly("TOMBSTONED_ORIENTATION_LIVE_ASSESSMENT")
    }

    @Test
    fun `orientation without subject fails closed`() {
        val violations = mutableListOf<CanonicalProductReadViolation>()

        val selected =
            selectProductLiveOrientationRows(
                subjects = emptyList(),
                orientations = listOf(orientationRow("orphan")),
                assessmentsByOrientation = emptyMap(),
                violations = violations,
            )

        assertThat(selected).isEmpty()
        assertThat(violations.map { it.code }).containsExactly("ORIENTATION_SUBJECT_INVALID")
    }

    @Test
    fun `ambiguous orientation subject fails closed`() {
        val violations = mutableListOf<CanonicalProductReadViolation>()

        val selected =
            selectProductLiveOrientationRows(
                subjects = listOf(subjectRow("same", false), subjectRow("same", true)),
                orientations = listOf(orientationRow("same")),
                assessmentsByOrientation = emptyMap(),
                violations = violations,
            )

        assertThat(selected).isEmpty()
        assertThat(violations.map { it.code }).containsExactly("ORIENTATION_SUBJECT_INVALID")
    }

    @Test
    fun `live subject with deleted assessment reaches strict assessment validator`() {
        val live = orientationRow("live")
        val violations = mutableListOf<CanonicalProductReadViolation>()

        val selected =
            selectProductLiveOrientationRows(
                subjects = listOf(subjectRow("live", false)),
                orientations = listOf(live),
                assessmentsByOrientation = mapOf("live" to assessmentRow(true)),
                violations = violations,
            )

        assertThat(selected).containsExactly(live)
        assertThat(violations).isEmpty()
    }

    private fun subjectRow(
        subjectId: String,
        deleted: Boolean,
    ): ManagedSubjectEntity =
        mockk<ManagedSubjectEntity>().also { row ->
            every { row.id } returns subjectId
            every { row.isDeleted } returns deleted
        }

    private fun orientationRow(subjectId: String): OrientationEntity =
        mockk<OrientationEntity>().also { row ->
            every { row.subjectId } returns subjectId
        }

    private fun assessmentRow(deleted: Boolean): OrientationAssessmentEntity =
        mockk<OrientationAssessmentEntity>().also { row ->
            every { row.isDeleted } returns deleted
        }
}
