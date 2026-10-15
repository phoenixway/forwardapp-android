package com.romankozak.forwardappmobile.data.planning.tactical

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.data.planning.toPlanningScopeEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleRegistry
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalTacticalMissionAdapterRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical commitment and Orientation beat stale specialized common fields`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)

                val orientationId =
                    orientationRepository(db).createOrientation(
                        kind = OrientationKind.INTENT,
                        title = "Canonical title",
                        description = "Canonical description",
                        now = 10L,
                    )

                val stale =
                    mission(
                        id = 7L,
                        title = "STALE title",
                        description = "STALE description",
                        orderInWeek = 1L,
                        status = MissionStatus.ACTIVE,
                        priority = MissionPriority.LOW,
                    )
                db.tacticalMissionDao().insertMissionRaw(stale)

                planningRepository(db).saveCommitment(
                    stale.toCanonicalMissionCommitment(orientationId)
                        .copy(
                            order = 42L,
                            status = PlanningCommitmentStatus.COMPLETED,
                            priorityLevel = PlanningPriorityLevel.CRITICAL,
                        ),
                )

                val projected = adapter(db).getMissionById(7L)!!

                assertEquals("Canonical title", projected.title)
                assertEquals("Canonical description", projected.description)
                assertEquals(42L, projected.orderInWeek)
                assertEquals(MissionStatus.COMPLETED, projected.status)
                assertEquals(MissionPriority.CRITICAL, projected.priority)

                // Tactical-only dimension is still specialized.
                assertEquals(stale.order, projected.order)
            }
        }

    @Test
    fun `historical unscoped manual mission remains readable editable and deletable`() =
        runBlocking {
            withDatabase { db ->
                seedCyclelessLegacyAuthority(db)

                val original =
                    mission(
                        id = 71L,
                        title = "Historical manual",
                        status = MissionStatus.COMPLETED,
                    ).copy(
                        iterationId = null,
                        weekKey = "",
                    )

                db.tacticalMissionDao().insertMissionRaw(original)
                LegacyManualTacticalMissionIntentAdopter(
                    db,
                    orientationRepository(db),
                ).adopt(original.id)

                val runtime = adapter(db)

                assertEquals(original, runtime.getMissionById(71L))
                assertEquals(
                    listOf(original),
                    runtime.getAllMissions().first(),
                )

                val edited =
                    original.copy(
                        title = "Edited historical manual",
                        orderInWeek = 9L,
                        version = original.version + 1L,
                    )

                runtime.updateMission(edited)

                assertEquals(edited, runtime.getMissionById(71L))
                assertEquals(
                    null,
                    db.tacticalMissionDao()
                        .getMissionById(71L)
                        ?.iterationId,
                )
                assertTrue(db.planningDao().getAllCommitments().isEmpty())

                runtime.deleteMissionById(71L)

                assertNull(runtime.getMissionById(71L))
                assertTrue(
                    db.tacticalMissionDao()
                        .getMissionById(71L)
                        ?.isDeleted == true,
                )
                assertTrue(db.planningDao().getAllCommitments().isEmpty())
            }
        }

    @Test
    fun `unadopted unscoped manual cannot bypass canonical participation`() =
        runBlocking {
            withDatabase { db ->
                seedCyclelessLegacyAuthority(db)

                db.tacticalMissionDao().insertMissionRaw(
                    mission(
                        id = 75L,
                        title = "Unadopted manual",
                    ).copy(
                        iterationId = null,
                        weekKey = "",
                    ),
                )

                val failure =
                    runCatching { adapter(db).getMissionById(75L) }
                        .exceptionOrNull()

                assertNotNull(failure)
                assertTrue(
                    failure?.message?.contains("verified canonical INTENT mapping") == true,
                )
                assertTrue(db.planningDao().getAllCommitments().isEmpty())
            }
        }

    @Test
    fun `resolved backlog week alias uses canonical Orientation without Tactical cycle`() =
        runBlocking {
            withDatabase { db ->
                seedCyclelessLegacyAuthority(db)
                seedDeferredBacklog(db)

                val orientationId =
                    orientationRepository(db).createOrientation(
                        kind = OrientationKind.INTENT,
                        title = "Canonical backlog title",
                        description = "Canonical backlog description",
                    )

                db.workspaceBacklogEntryDao().upsert(
                    listOf(
                        WorkspaceBacklogEntryEntity(
                            id = "entry",
                            workspaceId = "owner",
                            capabilityInstanceId = "cap-owner",
                            targetKind = WorkspaceBacklogTargetKind.ORIENTATION.name,
                            targetId = orientationId,
                            entryOrder = 0L,
                            createdAt = 1L,
                            updatedAt = 2L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 2L,
                        ),
                    ),
                )

                val historical =
                    mission(
                        id = 81L,
                        title = "Stale compatibility title",
                        sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                        sourceContextId = "owner",
                        sourceBacklogItemId = "entry",
                    ).copy(
                        iterationId = "2026-W40",
                        weekKey = "2026-W40",
                    )

                db.tacticalMissionDao().insertMissionRaw(historical)
                val runtime = adapter(db)

                val projected = runtime.getMissionById(81L)
                assertEquals("Canonical backlog title", projected?.title)
                assertEquals(
                    "Canonical backlog description",
                    projected?.description,
                )
                assertEquals(
                    "Canonical backlog title",
                    runtime.getAllMissions().first().single().title,
                )
                assertEquals("2026-W40", projected?.iterationId)

                val edited =
                    historical.copy(
                        title = "Edited canonical backlog title",
                        description = "Edited canonical backlog description",
                        updatedAt = System.currentTimeMillis(),
                        version = historical.version + 1L,
                    )

                runtime.updateMission(edited)

                assertEquals(
                    edited.title,
                    runtime.getMissionById(81L)?.title,
                )
                assertEquals(
                    edited.title,
                    db.orientationDao().getManagedSubject(orientationId)?.title,
                )
                assertEquals(
                    "2026-W40",
                    db.tacticalMissionDao().getMissionById(81L)?.iterationId,
                )
                assertTrue(db.planningDao().getAllScopes().isEmpty())
                assertTrue(db.planningDao().getAllCommitments().isEmpty())

                runtime.deleteMissionById(81L)

                assertNull(runtime.getMissionById(81L))
                assertTrue(
                    db.tacticalMissionDao().getMissionById(81L)?.isDeleted == true,
                )
                assertTrue(
                    db.planningDao().getAllCommitments().isEmpty(),
                )

                val unknown =
                    historical.copy(
                        id = 82L,
                        iterationId = "unknown-cycle",
                    )

                db.tacticalMissionDao().insertMissionRaw(unknown)

                val failure =
                    runCatching { runtime.getMissionById(82L) }
                        .exceptionOrNull()

                assertNotNull(failure)
                assertTrue(
                    failure?.message?.contains(
                        "canonical MISSION participation is missing"
                    ) == true,
                )
            }
        }

    @Test
    fun `resolved backlog with null iteration is readable editable and deletable`() =
        runBlocking {
            withDatabase { db ->
                seedCyclelessLegacyAuthority(db)
                seedDeferredBacklog(db)

                val orientationId =
                    orientationRepository(db).createOrientation(
                        kind = OrientationKind.INTENT,
                        title = "Canonical cycleless backlog",
                        description = "Canonical description",
                    )

                db.workspaceBacklogEntryDao().upsert(
                    listOf(
                        WorkspaceBacklogEntryEntity(
                            id = "entry",
                            workspaceId = "owner",
                            capabilityInstanceId = "cap-owner",
                            targetKind = WorkspaceBacklogTargetKind.ORIENTATION.name,
                            targetId = orientationId,
                            entryOrder = 0L,
                            createdAt = 1L,
                            updatedAt = 2L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 2L,
                        ),
                    ),
                )

                val original =
                    mission(
                        id = 84L,
                        title = "Stale specialized title",
                        sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                        sourceContextId = "owner",
                        sourceBacklogItemId = "entry",
                    ).copy(
                        iterationId = null,
                        weekKey = "2026-W40",
                    )

                db.tacticalMissionDao().insertMissionRaw(original)
                val runtime = adapter(db)

                assertEquals(
                    "Canonical cycleless backlog",
                    runtime.getMissionById(84L)?.title,
                )
                assertEquals(
                    "Canonical cycleless backlog",
                    runtime.getAllMissions().first().single().title,
                )

                val edited =
                    original.copy(
                        title = "Edited canonical cycleless backlog",
                        updatedAt = System.currentTimeMillis(),
                        version = original.version + 1L,
                    )

                runtime.updateMission(edited)

                assertEquals(
                    edited.title,
                    runtime.getMissionById(84L)?.title,
                )
                assertEquals(
                    edited.title,
                    db.orientationDao().getManagedSubject(orientationId)?.title,
                )
                assertNull(
                    db.tacticalMissionDao().getMissionById(84L)?.iterationId,
                )
                assertTrue(db.planningDao().getAllScopes().isEmpty())
                assertTrue(db.planningDao().getAllCommitments().isEmpty())

                runtime.deleteMissionById(84L)

                assertNull(runtime.getMissionById(84L))
                assertTrue(
                    db.tacticalMissionDao().getMissionById(84L)?.isDeleted == true,
                )
                assertTrue(db.planningDao().getAllCommitments().isEmpty())
            }
        }

    @Test
    fun `historical week alias remains readable and reorderable without fake scope`() =
        runBlocking {
            withDatabase { db ->
                seedCyclelessLegacyAuthority(db)
                seedDeferredBacklog(db)

                val alias =
                    mission(
                        id = 72L,
                        title = "Legacy week alias",
                        sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                        sourceContextId = "owner",
                        sourceBacklogItemId = "entry",
                    ).copy(
                        iterationId = "2026-W40",
                        weekKey = "2026-W40",
                    )

                db.tacticalMissionDao().insertMissionRaw(alias)

                val runtime = adapter(db)

                assertEquals(alias, runtime.getMissionById(72L))

                runtime.reorderMissions(listOf(alias))

                val projected = runtime.getMissionById(72L)

                assertNotNull(projected)
                assertEquals("2026-W40", projected?.iterationId)
                assertEquals("2026-W40", projected?.weekKey)
                assertEquals(0L, projected?.orderInWeek)

                assertTrue(db.planningDao().getAllScopes().isEmpty())
                assertTrue(db.planningDao().getAllCommitments().isEmpty())
            }
        }

    @Test
    fun `eligible mission missing canonical commitment fails closed`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                db.tacticalMissionDao().insertMissionRaw(
                    mission(
                        id = 7L,
                        sourceType = MissionSourceType.MANUAL,
                    ),
                )

                val failure =
                    runCatching { adapter(db).getMissionById(7L) }
                        .exceptionOrNull()

                assertNotNull(failure)
                assertTrue(
                    failure?.message?.contains("missing canonical MISSION participation") == true,
                )
            }
        }

    @Test
    fun `unresolved heterogeneous backlog remains specialized only`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                seedDeferredBacklog(db)

                val deferred =
                    mission(
                        id = 8L,
                        title = "Deferred",
                        sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                        sourceContextId = "owner",
                        sourceBacklogItemId = "entry",
                    )
                db.tacticalMissionDao().insertMissionRaw(deferred)

                val projected = adapter(db).getMissionById(8L)

                assertEquals(deferred, projected)
                assertEquals(0, db.planningDao().getAllCommitments().size)
            }
        }

    @Test
    fun `fresh MANUAL authoring creates USER INTENT and canonical MISSION without legacy mapping`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)

                val insertedId =
                    adapter(db).insertMission(
                        mission(
                            id = 0L,
                            title = "Fresh mission",
                            description = "Fresh semantics",
                            sourceType = MissionSourceType.MANUAL,
                            version = 1L,
                        ),
                    )

                assertTrue(insertedId > 0L)

                val commitment =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .single()

                assertEquals(insertedId.toString(), commitment.provenance.sourceId)
                assertEquals("TACTICAL_MISSION", commitment.provenance.sourceType)
                assertEquals(
                    canonicalTacticalMissionCommitmentId("cycle", insertedId),
                    commitment.id,
                )

                val subject =
                    requireNotNull(
                        db.orientationDao().getManagedSubject(commitment.orientationId),
                    )
                assertEquals(ManagedSubjectType.ORIENTATION.name, subject.subjectType)
                assertEquals("Fresh mission", subject.title)
                assertEquals("Fresh semantics", subject.description)

                val orientation =
                    db.orientationDao().getAllOrientations()
                        .single { it.subjectId == commitment.orientationId }
                assertEquals(OrientationKind.INTENT.name, orientation.kind)

                val revision =
                    db.orientationDao().getAllAssessmentRevisions()
                        .single { it.orientationId == commitment.orientationId }
                assertEquals(AssessmentRevisionSource.USER.name, revision.source)

                assertNull(
                    db.orientationDao().getLegacyMapping(
                        com.romankozak.forwardappmobile.shared.core.models.orientation
                            .LegacyOrientationSourceType.TACTICAL_MISSION.name,
                        insertedId.toString(),
                    ),
                )
            }
        }

    @Test
    fun `eligible delete tombstones commitment and specialized projection but preserves Orientation`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                val runtime = adapter(db)

                val missionId =
                    runtime.insertMission(
                        mission(
                            id = 0L,
                            title = "Delete me",
                            sourceType = MissionSourceType.MANUAL,
                            version = 1L,
                        ),
                    )

                val before =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .single()
                val orientationId = before.orientationId

                runtime.deleteMissionById(missionId)

                val after =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .single()

                assertTrue(after.isDeleted)
                assertEquals(before.version + 1L, after.version)

                val row =
                    requireNotNull(db.tacticalMissionDao().getMissionById(missionId))
                assertTrue(row.isDeleted)

                val subject =
                    requireNotNull(db.orientationDao().getManagedSubject(orientationId))
                assertFalse(subject.isDeleted)
                assertNotNull(
                    db.orientationDao().getAllOrientations()
                        .singleOrNull { it.subjectId == orientationId },
                )
            }
        }

    @Test
    fun `reorder writes canonical commitment order`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                val runtime = adapter(db)

                val firstId =
                    runtime.insertMission(
                        mission(
                            id = 0L,
                            title = "First",
                            order = 10L,
                            orderInWeek = 10L,
                            version = 1L,
                        ),
                    )
                val secondId =
                    runtime.insertMission(
                        mission(
                            id = 0L,
                            title = "Second",
                            order = 20L,
                            orderInWeek = 20L,
                            version = 1L,
                        ),
                    )

                val first = requireNotNull(runtime.getMissionById(firstId))
                val second = requireNotNull(runtime.getMissionById(secondId))

                runtime.reorderMissions(listOf(second, first))

                val commitments =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .associateBy { it.provenance.sourceId }

                assertEquals(0L, commitments[secondId.toString()]?.order)
                assertEquals(1L, commitments[firstId.toString()]?.order)

                val projected =
                    runtime.getAllMissions().first()
                        .associateBy(TacticalMission::id)
                assertEquals(0L, projected[secondId]?.orderInWeek)
                assertEquals(1L, projected[firstId]?.orderInWeek)
            }
        }

    @Test
    fun `authoring without iteration is admitted to canonical current Tactical scope`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                val runtime = adapter(db)

                val id =
                    runtime.insertMission(
                        mission(
                            id = 0L,
                            title = "Scope admission",
                        ).copy(iterationId = null),
                    )

                val commitment =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .single()

                assertEquals("cycle", commitment.scopeId.value)
                assertEquals("cycle", runtime.getMissionById(id)?.iterationId)
            }
        }

    @Test
    fun `authoring without iteration fails closed when no current Tactical scope exists`() =
        runBlocking {
            withDatabase { db ->
                db.tacticalPlanningAuthorityStateDao().upsert(
                    TacticalPlanningAuthorityStateEntity(
                        version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION,
                        activatedAt = 100L,
                    ),
                )

                val failure =
                    runCatching {
                        adapter(db).insertMission(
                            mission(
                                id = 0L,
                                title = "No scope",
                            ).copy(iterationId = null),
                        )
                    }.exceptionOrNull()

                assertNotNull(failure)
                assertTrue(
                    failure?.message?.contains(
                        "requires a current canonical Tactical cycle",
                    ) == true,
                )
                assertTrue(db.tacticalMissionDao().getAllMissions().first().isEmpty())
            }
        }

    @Test
    fun `eligible semantic edit writes canonical Orientation subject without fake assessment revision`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                val runtime = adapter(db)

                val id =
                    runtime.insertMission(
                        mission(
                            id = 0L,
                            title = "Before",
                            description = "Old description",
                        ),
                    )

                val commitment =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .single()
                val orientationId = commitment.orientationId

                val revisionsBefore =
                    db.orientationDao().getAllAssessmentRevisions()
                        .count { it.orientationId == orientationId }

                val projected = requireNotNull(runtime.getMissionById(id))
                runtime.updateMission(
                    projected.copy(
                        title = "  After  ",
                        description = "  New description  ",
                        updatedAt = 100L,
                        version = projected.version + 1L,
                    ),
                )

                val subject =
                    requireNotNull(
                        db.orientationDao().getManagedSubject(orientationId),
                    )
                assertEquals("After", subject.title)
                assertEquals("New description", subject.description)

                val revisionsAfter =
                    db.orientationDao().getAllAssessmentRevisions()
                        .count { it.orientationId == orientationId }
                assertEquals(revisionsBefore, revisionsAfter)

                val reread = requireNotNull(runtime.getMissionById(id))
                assertEquals("After", reread.title)
                assertEquals("New description", reread.description)
            }
        }

    @Test
    fun `canonical auto order and reorder preserve legacy mission order`() =
        runBlocking {
            withDatabase { db ->
                seedActivatedScope(db)
                val runtime = adapter(db)

                val firstId =
                    runtime.insertMission(
                        mission(
                            id = 0L,
                            title = "Existing",
                            order = 900L,
                            orderInWeek = 5L,
                        ),
                    )

                val secondId =
                    runtime.insertMissionWithAutoOrder(
                        mission(
                            id = 0L,
                            title = "Auto",
                            order = 700L,
                            orderInWeek = 999L,
                        ),
                    )

                val afterInsert =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .associateBy { it.provenance.sourceId }

                assertEquals(4L, afterInsert[secondId.toString()]?.order)
                assertEquals(
                    700L,
                    requireNotNull(
                        db.tacticalMissionDao().getMissionById(secondId),
                    ).order,
                )

                val first = requireNotNull(runtime.getMissionById(firstId))
                val second = requireNotNull(runtime.getMissionById(secondId))

                runtime.reorderMissions(listOf(second, first))

                assertEquals(
                    900L,
                    requireNotNull(
                        db.tacticalMissionDao().getMissionById(firstId),
                    ).order,
                )
                assertEquals(
                    700L,
                    requireNotNull(
                        db.tacticalMissionDao().getMissionById(secondId),
                    ).order,
                )

                val afterReorder =
                    db.planningDao().getAllCommitments()
                        .map { it.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL) }
                        .associateBy { it.provenance.sourceId }

                assertEquals(0L, afterReorder[secondId.toString()]?.order)
                assertEquals(1L, afterReorder[firstId.toString()]?.order)
            }
        }

    @Test
    fun `missing Tactical authority marker fails closed`() =
        runBlocking {
            withDatabase { db ->
                db.tacticalMissionDao().insertMissionRaw(mission(id = 7L))

                val failure =
                    runCatching { adapter(db).getMissionById(7L) }
                        .exceptionOrNull()

                assertTrue(
                    failure?.message?.contains("not activated") == true,
                )
            }
        }

    private suspend fun seedCyclelessLegacyAuthority(db: AppDatabase) {
        db.tacticalPlanningAuthorityStateDao().upsert(
            TacticalPlanningAuthorityStateEntity(
                version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION,
                activatedAt = 100L,
            ),
        )
    }

    private suspend fun seedActivatedScope(db: AppDatabase) {
        val iteration =
            TacticalIteration(
                id = "cycle",
                title = "Cycle",
                startedAt = 10L,
                plannedEndAt = 20L,
                weekKey = "2026-W40",
                createdAt = 1L,
                updatedAt = 2L,
                version = 1L,
            )

        db.planningDao().upsertScope(
            iteration.toCanonicalTacticalPlanningScope().toPlanningScopeEntity(),
        )
        db.tacticalPlanningScopeExtensionDao().upsert(
            iteration.toCanonicalTacticalPlanningScopeExtension().toEntity(),
        )
        db.tacticalPlanningAuthorityStateDao().upsert(
            TacticalPlanningAuthorityStateEntity(
                version = CanonicalTacticalPlanningAuthorityActivator.CURRENT_VERSION,
                activatedAt = 100L,
            ),
        )
    }

    private suspend fun seedDeferredBacklog(db: AppDatabase) {
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
                    targetId = "not-an-orientation",
                    entryOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun mission(
        id: Long,
        title: String = "Mission",
        description: String? = null,
        order: Long = 3L,
        orderInWeek: Long = order,
        status: MissionStatus = MissionStatus.ACTIVE,
        priority: MissionPriority = MissionPriority.MEDIUM,
        sourceType: MissionSourceType = MissionSourceType.MANUAL,
        sourceContextId: String? = null,
        sourceBacklogItemId: String? = null,
        version: Long = 1L,
    ) = TacticalMission(
        id = id,
        title = title,
        description = description,
        deadline = NO_DEADLINE,
        status = status,
        priority = priority,
        projectId = null,
        order = order,
        weekKey = "2026-W40",
        iterationId = "cycle",
        orderInWeek = orderInWeek,
        sourceType = sourceType,
        sourceContextId = sourceContextId,
        sourceBacklogItemId = sourceBacklogItemId,
        createdAt = 10L,
        updatedAt = 20L,
        version = version,
    )

    private fun adapter(db: AppDatabase): CanonicalTacticalMissionAdapter {
        val activator =
            CanonicalTacticalPlanningAuthorityActivator(
                database = db,
                manualIntentAdopter = mockk(relaxed = true),
                questOrientationBridge = mockk<QuestOrientationBridge>(relaxed = true),
            )

        return CanonicalTacticalMissionAdapter(
            database = db,
            authorityActivator = activator,
            orientationResolver = TacticalMissionOrientationResolver(db),
            orientationRepository = orientationRepository(db),
            planningRepository = planningRepository(db),
            tacticalPlanningAdapter =
                CanonicalTacticalPlanningAdapter(
                    database = db,
                    authorityActivator = activator,
                ),
        )
    }

    private fun orientationRepository(db: AppDatabase) =
        CanonicalOrientationRepository(db, db.orientationDao())

    private fun planningRepository(db: AppDatabase) =
        CanonicalPlanningRepository(
            db,
            db.planningDao(),
            db.dayPlanningScopeExtensionDao(),
            db.tacticalPlanningScopeExtensionDao(),
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
