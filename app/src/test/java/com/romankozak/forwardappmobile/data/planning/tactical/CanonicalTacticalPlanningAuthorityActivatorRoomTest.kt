package com.romankozak.forwardappmobile.data.planning.tactical

import android.content.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentEntity
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalTacticalPlanningAuthorityActivatorRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `activation materializes exact aggregate writes marker last and reruns inert`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = true)
            val activator = activator(db)

            val first = activator.ensureActivated(now = 100L)
            val second = activator.ensureActivated(now = 200L)

            assertEquals(true, first.performed)
            assertFalse(second.performed)
            assertEquals(1, first.scopes)
            assertEquals(1, first.commitments)
            assertEquals(0, first.deferredMissions)
            assertEquals(1, db.planningDao().getAllScopes().size)
            assertEquals(1, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(1, db.planningDao().getAllCommitments().size)
            assertEquals(100L, requireNotNull(db.tacticalPlanningAuthorityStateDao().get()).activatedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `integrity failure rolls planning transaction back and leaves marker inactive`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = false)

            runCatching { activator(db).ensureActivated() }
                .onSuccess { error("Activation unexpectedly succeeded") }

            assertEquals(null, db.tacticalPlanningAuthorityStateDao().get())
            assertEquals(0, db.planningDao().getAllScopes().size)
            assertEquals(0, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `semantic preparation rolls back when later planning preflight fails`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = false)
            val subjectId = legacyManualTacticalMissionSubjectId(7L)

            val expected =
                requireNotNull(db.tacticalIterationDao().getById("cycle"))
                    .toCanonicalTacticalPlanningScope()
            db.planningDao().upsertScope(
                expected.copy(title = "Divergent").toPlanningScopeEntity(),
            )

            val adopter =
                LegacyManualTacticalMissionIntentAdopter(
                    database = db,
                    canonicalOrientationRepository =
                        CanonicalOrientationRepository(db, db.orientationDao()),
                )
            val questBridge = mockk<QuestOrientationBridge>()
            coEvery { questBridge.ensureCutOver() } returns Unit

            runCatching {
                CanonicalTacticalPlanningAuthorityActivator(db, adopter, questBridge)
                    .ensureActivated(now = 100L)
            }.onSuccess {
                error("Activation unexpectedly succeeded")
            }

            assertNull(db.orientationDao().getManagedSubject(subjectId))
            assertEquals(
                emptyList<LegacySubjectMappingEntity>(),
                db.orientationDao().getAllLegacyMappings()
                    .filter {
                        it.sourceType == LegacyOrientationSourceType.TACTICAL_MISSION.name &&
                            it.sourceId == "7"
                    },
            )
            assertNull(db.tacticalPlanningAuthorityStateDao().get())
            assertEquals("Divergent", requireNotNull(db.planningDao().getScope("cycle")).title)
            assertEquals(0, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `typed heterogeneous backlog deferral does not block activation`() = runBlocking {
        val db = database()
        try {
            db.tacticalIterationDao().insert(
                TacticalIteration(
                    id = "cycle",
                    title = "Cycle",
                    startedAt = 10L,
                    plannedEndAt = 20L,
                    weekKey = "2026-W40",
                    createdAt = 1L,
                    updatedAt = 2L,
                    version = 1L,
                ),
            )
            db.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = "owner",
                        nameOverride = "Owner",
                        descriptionOverride = null,
                        roleCode = null,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            )
            db.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    WorkspaceCapabilityInstanceEntity(
                        id = "cap-owner",
                        workspaceId = "owner",
                        capabilityType = "BACKLOG",
                        instanceKey = "default",
                        capabilityOrder = 0L,
                        state = "ACTIVE",
                        configurationVersion = 1,
                        configuration = "{}",
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )
            db.workspaceBacklogEntryDao().upsert(
                listOf(
                    WorkspaceBacklogEntryEntity(
                        id = "entry",
                        workspaceId = "owner",
                        capabilityInstanceId = "cap-owner",
                        targetKind = WorkspaceBacklogTargetKind.WORKSPACE.name,
                        targetId = "workspace",
                        entryOrder = 0L,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )
            // Retired Context is provenance evidence, not physical
            // TacticalMission ownership. Workspace remains canonical.
            db.contextDao().insertContexts(
                listOf(
                    ContextEntity(
                        id = "owner",
                        name = "Retired owner",
                        description = null,
                        parentId = null,
                        createdAt = 1L,
                        updatedAt = 1L,
                        isDeleted = true,
                    ),
                ),
            )

            // The validated writer converts legacy compatibility input
            // into canonical Workspace ownership before persistence.
            db.tacticalMissionDao().insertMission(
                TacticalMission(
                    id = 8L,
                    title = "Deferred",
                    description = null,
                    deadline = NO_DEADLINE,
                    projectId = "owner",
                    iterationId = "cycle",
                    sourceType =
                        com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
                            .CONTEXT_BACKLOG_ITEM,
                    sourceContextId = "owner",
                    sourceBacklogItemId = "entry",
                    createdAt = 1L,
                    updatedAt = 2L,
                    version = 1L,
                ),
            )

            val adopter = mockk<LegacyManualTacticalMissionIntentAdopter>(relaxed = true)
            val questBridge = mockk<QuestOrientationBridge>()
            coEvery { questBridge.ensureCutOver() } returns Unit

            val activator =
                CanonicalTacticalPlanningAuthorityActivator(db, adopter, questBridge)
            val result = activator.ensureActivated(now = 100L)

            assertEquals(true, result.performed)
            assertEquals(1, result.scopes)
            assertEquals(0, result.commitments)
            assertEquals(1, result.deferredMissions)
            assertEquals(1, db.planningDao().getAllScopes().size)
            assertEquals(1, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
            assertEquals(100L, requireNotNull(db.tacticalPlanningAuthorityStateDao().get()).activatedAt)

            val beforeProjection =
                requireNotNull(db.tacticalMissionDao().getMissionById(8L))
            assertNull(beforeProjection.projectId)
            assertEquals("owner", beforeProjection.projectWorkspaceId)

            activator.projectCompatibilityRows()

            val afterProjection =
                requireNotNull(db.tacticalMissionDao().getMissionById(8L))

            // Deferred projection must not rewrite an already
            // canonicalized carrier or lose its specialized payload.
            assertEquals(beforeProjection, afterProjection)
            assertNull(afterProjection.projectId)
            assertEquals("owner", afterProjection.projectWorkspaceId)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `extra tactical canonical scope fails exact parity and leaves marker inactive`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = true)

            val expected =
                requireNotNull(db.tacticalIterationDao().getById("cycle"))
                    .toCanonicalTacticalPlanningScope()

            db.planningDao().upsertScope(
                expected.copy(
                    id =
                        com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId(
                            "extra-cycle",
                        ),
                    title = "Extra",
                ).toPlanningScopeEntity(),
            )

            runCatching { activator(db).ensureActivated(now = 100L) }
                .onSuccess { error("Activation unexpectedly succeeded") }

            assertNull(db.tacticalPlanningAuthorityStateDao().get())
            assertEquals(
                listOf("extra-cycle"),
                db.planningDao().getAllScopes().map { it.id }.sorted(),
            )
            assertEquals(0, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `marker insert failure rolls back materialized tactical planning`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = true)

            db.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_tactical_planning_marker
                BEFORE INSERT ON tactical_planning_authority_state
                BEGIN
                    SELECT RAISE(ABORT, 'forced tactical marker failure');
                END
                """.trimIndent(),
            )

            runCatching { activator(db).ensureActivated(now = 100L) }
                .onSuccess { error("Activation unexpectedly succeeded") }

            assertNull(db.tacticalPlanningAuthorityStateDao().get())
            assertEquals(0, db.planningDao().getAllScopes().size)
            assertEquals(0, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `divergent preexisting deterministic scope fails closed and leaves marker inactive`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = true)

            val expected =
                requireNotNull(db.tacticalIterationDao().getById("cycle"))
                    .toCanonicalTacticalPlanningScope()

            db.planningDao().upsertScope(
                expected.copy(title = "Divergent").toPlanningScopeEntity(),
            )

            runCatching { activator(db).ensureActivated(now = 100L) }
                .onSuccess { error("Activation unexpectedly succeeded") }

            assertNull(db.tacticalPlanningAuthorityStateDao().get())
            assertEquals("Divergent", requireNotNull(db.planningDao().getScope("cycle")).title)
            assertEquals(0, db.tacticalPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `compatibility projection refreshes mission common fields and preserves Tactical-only carrier fields`() =
        runBlocking {
            val db = database()
            try {
                seed(db, includeMapping = true)
                val activator = activator(db)
                activator.ensureActivated(now = 100L)

                val stale =
                    requireNotNull(db.tacticalMissionDao().getMissionById(7L)).copy(
                        title = "STALE TITLE",
                        description = "STALE DESCRIPTION",
                        deadline = 12_345L,
                        status = MissionStatus.PAUSED,
                        priority = MissionPriority.LOW,
                        order = 777L,
                        orderInWeek = 999L,
                        updatedAt = 999L,
                        syncedAt = 999L,
                        version = 99L,
                    )
                db.tacticalMissionDao().updateMissionRaw(stale)

                val subjectId = legacyManualTacticalMissionSubjectId(7L)
                val subject = requireNotNull(db.orientationDao().getManagedSubject(subjectId))
                db.orientationDao().upsertManagedSubjects(
                    listOf(
                        subject.copy(
                            title = "Canonical Mission",
                            description = "Canonical Description",
                            updatedAt = 20L,
                            syncedAt = 21L,
                            version = 5L,
                        ),
                    ),
                )

                val commitment =
                    db.planningDao()
                        .getAllCommitments()
                        .single()
                        .toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)
                        .copy(
                            order = 42L,
                            status = PlanningCommitmentStatus.COMPLETED,
                            priorityLevel = PlanningPriorityLevel.CRITICAL,
                            updatedAt = 20L,
                            syncedAt = 21L,
                            version = 5L,
                        )
                db.planningDao().upsertCommitments(
                    listOf(commitment.toPlanningCommitmentEntity()),
                )

                activator.projectCompatibilityRows()

                val projected = requireNotNull(db.tacticalMissionDao().getMissionById(7L))
                assertEquals("Canonical Mission", projected.title)
                assertEquals("Canonical Description", projected.description)
                assertEquals("cycle", projected.iterationId)
                assertEquals(42L, projected.orderInWeek)
                assertEquals(MissionStatus.COMPLETED, projected.status)
                assertEquals(MissionPriority.CRITICAL, projected.priority)
                assertEquals(commitment.createdAt, projected.createdAt)
                assertEquals(20L, projected.updatedAt)
                assertEquals(21L, projected.syncedAt)
                assertEquals(5L, projected.version)
                assertFalse(projected.isDeleted)

                // Tactical-only / legacy compatibility fields are not canonicalized.
                assertEquals(12_345L, projected.deadline)
                assertEquals(777L, projected.order)

                db.planningDao().upsertCommitments(
                    listOf(
                        commitment.copy(
                            status = PlanningCommitmentStatus.DROPPED,
                            updatedAt = 30L,
                            syncedAt = 31L,
                            isDeleted = true,
                            version = 6L,
                        ).toPlanningCommitmentEntity(),
                    ),
                )

                activator.projectCompatibilityRows()

                val tombstoned = requireNotNull(db.tacticalMissionDao().getMissionById(7L))
                assertTrue(tombstoned.isDeleted)
                assertEquals(MissionStatus.INACTIVE, tombstoned.status)
                assertEquals(42L, tombstoned.orderInWeek)
                assertEquals(30L, tombstoned.updatedAt)
                assertEquals(31L, tombstoned.syncedAt)
                assertEquals(6L, tombstoned.version)
                assertEquals(12_345L, tombstoned.deadline)
                assertEquals(777L, tombstoned.order)
            } finally {
                db.close()
            }
        }

    @Test
    fun `compatibility projection fails closed when canonical mission carrier is missing`() =
        runBlocking {
            val db = database()
            try {
                seed(db, includeMapping = true)
                val activator = activator(db)
                activator.ensureActivated(now = 100L)

                db.tacticalMissionDao().deleteMissionById(7L)

                val failure =
                    runCatching {
                        activator.projectCompatibilityRows()
                    }.exceptionOrNull()

                assertTrue(
                    failure?.message?.contains(
                        "missing compatibility row 7",
                    ) == true,
                )
                assertNull(db.tacticalMissionDao().getMissionById(7L))
            } finally {
                db.close()
            }
        }

    private fun activator(db: AppDatabase): CanonicalTacticalPlanningAuthorityActivator {
        val adopter = mockk<LegacyManualTacticalMissionIntentAdopter>()
        coEvery { adopter.adopt(any()) } returns
            LegacyManualIntentAdoptionResult.AlreadyAdopted(legacyManualTacticalMissionSubjectId(7L))
        val questBridge = mockk<QuestOrientationBridge>()
        coEvery { questBridge.ensureCutOver() } returns Unit
        return CanonicalTacticalPlanningAuthorityActivator(db, adopter, questBridge)
    }

    private suspend fun seed(db: AppDatabase, includeMapping: Boolean) {
        db.tacticalIterationDao().insert(
            TacticalIteration(
                id = "cycle",
                title = "Cycle",
                startedAt = 10L,
                plannedEndAt = 20L,
                weekKey = "2026-W40",
                createdAt = 1L,
                updatedAt = 2L,
                version = 1L,
            ),
        )
        db.tacticalMissionDao().insertMissionRaw(
            TacticalMission(
                id = 7L,
                title = "Mission",
                description = null,
                deadline = NO_DEADLINE,
                projectId = null,
                iterationId = "cycle",
                createdAt = 1L,
                updatedAt = 2L,
                version = 1L,
            ),
        )
        if (!includeMapping) return
        val subjectId = legacyManualTacticalMissionSubjectId(7L)
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = subjectId,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = "Mission",
                    description = null,
                    createdAt = 1L,
                    updatedAt = 2L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.orientationDao().upsertOrientations(
            listOf(OrientationEntity(subjectId, OrientationKind.INTENT.name, null, ValueOrigin.UNSET.name)),
        )
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "mapping",
                    sourceType = LegacyOrientationSourceType.TACTICAL_MISSION.name,
                    sourceId = "7",
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    createdAt = 1L,
                    updatedAt = 2L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
