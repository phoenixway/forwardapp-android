package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningPriorityLevel
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalTacticalPlanningMaterializationTest {
    @Test
    fun `resolved mission materializes deterministic scope extension and commitment`() {
        val mission = mission(id = 7L)
        val orientationId = legacyManualTacticalMissionSubjectId(7L)
        val input = snapshot(missions = listOf(mission), orientationId = orientationId)

        val first = planCanonicalTacticalPlanningMaterialization(listOf(iteration()), input)
        val second = planCanonicalTacticalPlanningMaterialization(listOf(iteration()), input)

        assertEquals(first, second)
        assertEquals("cycle-1", first.scopes.single().id.value)
        assertEquals(PlanningScopeKind.TACTICAL_CYCLE, first.scopes.single().kind)
        assertEquals(first.scopes.single().id, first.extensions.single().scopeId)
        with(first.commitments.single()) {
            assertEquals(canonicalTacticalMissionCommitmentId("cycle-1", 7L), id)
            assertEquals(orientationId, this.orientationId)
            assertEquals(PlanningRoleCode.MISSION, role)
            assertEquals(4L, order)
            assertEquals(PlanningPriorityLevel.HIGH, priorityLevel)
            assertEquals(PlanningCommitmentStatus.PAUSED, status)
        }
        assertEquals(emptyList<DeferredTacticalMission>(), first.deferredMissions)
    }

    @Test
    fun `different mission identities produce different commitment identities`() {
        assertNotEquals(
            canonicalTacticalMissionCommitmentId("cycle-1", 1L),
            canonicalTacticalMissionCommitmentId("cycle-1", 2L),
        )
    }

    @Test
    fun `heterogeneous backlog and deleted mission without scope are explicitly deferred`() {
        val backlogMission =
            mission(
                id = 8L,
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry",
            )
        val unscoped = mission(id = 9L, iterationId = null, isDeleted = true)
        val snapshot =
            TacticalMissionOrientationResolutionSnapshot(
                missions = listOf(backlogMission, unscoped),
                backlogEntries =
                    listOf(
                        WorkspaceBacklogEntryEntity(
                            id = "entry",
                            workspaceId = "owner",
                            capabilityInstanceId = "backlog",
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
                arcQuests = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
            )

        val plan = planCanonicalTacticalPlanningMaterialization(listOf(iteration()), snapshot)

        assertEquals(emptyList<Any>(), plan.commitments)
        assertEquals(
            listOf(
                TacticalMissionDeferralReason.NON_ORIENTATION_BACKLOG_TARGET,
                TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
            ),
            plan.deferredMissions.map(DeferredTacticalMission::reason),
        )
    }

    @Test
    fun `historical cycleless missions remain explicit standalone deferrals`() {
        val unassigned =
            mission(id = 71L, iterationId = null).copy(
                weekKey = "",
            )
        val weekAlias =
            mission(
                id = 72L,
                iterationId = "2026-W40",
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry",
            ).copy(weekKey = "2026-W40")

        val input =
            snapshot(
                missions = listOf(unassigned),
                orientationId = legacyManualTacticalMissionSubjectId(71L),
            ).copy(
                missions = listOf(unassigned, weekAlias),
                backlogEntries =
                    listOf(
                        WorkspaceBacklogEntryEntity(
                            id = "entry",
                            workspaceId = "owner",
                            capabilityInstanceId = "backlog",
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

        val plan =
            planCanonicalTacticalPlanningMaterialization(
                iterations = emptyList(),
                resolutionSnapshot = input,
            )

        assertEquals(0, plan.scopes.size)
        assertEquals(0, plan.extensions.size)
        assertEquals(0, plan.commitments.size)
        assertEquals(
            listOf(
                DeferredTacticalMission(
                    missionId = 71L,
                    reason = TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
                    sourceId = null,
                ),
                DeferredTacticalMission(
                    missionId = 72L,
                    reason = TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
                    sourceId = "2026-W40",
                ),
            ),
            plan.deferredMissions,
        )
    }

    @Test
    fun `resolved Orientation backlog week alias stays standalone but unknown cycle fails`() {
        val backlog =
            mission(
                id = 81L,
                iterationId = "2026-W40",
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry",
            ).copy(weekKey = "2026-W40")

        val input =
            snapshot(
                missions = listOf(backlog),
                orientationId = legacyManualTacticalMissionSubjectId(81L),
            )

        val plan =
            planCanonicalTacticalPlanningMaterialization(
                iterations = emptyList(),
                resolutionSnapshot = input,
            )

        assertEquals(0, plan.scopes.size)
        assertEquals(0, plan.extensions.size)
        assertEquals(0, plan.commitments.size)
        assertEquals(
            listOf(
                DeferredTacticalMission(
                    missionId = 81L,
                    reason = TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
                    sourceId = "2026-W40",
                ),
            ),
            plan.deferredMissions,
        )

        val unscoped = backlog.copy(id = 82L, iterationId = null)
        val unscopedPlan =
            planCanonicalTacticalPlanningMaterialization(
                iterations = emptyList(),
                resolutionSnapshot = input.copy(
                    missions = listOf(unscoped),
                ),
            )

        assertEquals(0, unscopedPlan.scopes.size)
        assertEquals(0, unscopedPlan.extensions.size)
        assertEquals(0, unscopedPlan.commitments.size)
        assertEquals(
            listOf(
                DeferredTacticalMission(
                    missionId = 82L,
                    reason = TacticalMissionDeferralReason.NO_CANONICAL_SCOPE,
                    sourceId = null,
                ),
            ),
            unscopedPlan.deferredMissions,
        )

        val invalid = backlog.copy(iterationId = "unknown-cycle")
        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(
                iterations = emptyList(),
                resolutionSnapshot = input.copy(
                    missions = listOf(invalid),
                ),
            )
        }
    }

    @Test
    fun `unadopted unscoped manual mission still fails closed`() {
        val mission = mission(id = 74L, iterationId = null)
        val input =
            TacticalMissionOrientationResolutionSnapshot(
                missions = listOf(mission),
                backlogEntries = emptyList(),
                arcQuests = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
            )

        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(
                iterations = emptyList(),
                resolutionSnapshot = input,
            )
        }
    }

    @Test
    fun `unknown non-week iteration remains rejected without any cycles`() {
        val invalid =
            mission(id = 73L, iterationId = "unproven-cycle")
                .copy(weekKey = "2026-W40")

        val snapshot =
            TacticalMissionOrientationResolutionSnapshot(
                missions = listOf(invalid),
                backlogEntries = emptyList(),
                arcQuests = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
            )

        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(
                iterations = emptyList(),
                resolutionSnapshot = snapshot,
            )
        }
    }

    @Test
    fun `live mission without canonical iteration fails closed`() {
        val snapshot =
            TacticalMissionOrientationResolutionSnapshot(
                missions = listOf(mission(id = 9L, iterationId = null)),
                backlogEntries = emptyList(),
                arcQuests = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
            )

        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(listOf(iteration()), snapshot)
        }
    }

    @Test
    fun `live mission referencing unknown canonical iteration fails closed`() {
        val snapshot =
            TacticalMissionOrientationResolutionSnapshot(
                missions = listOf(mission(id = 91L, iterationId = "missing-cycle")),
                backlogEntries = emptyList(),
                arcQuests = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
            )

        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(listOf(iteration()), snapshot)
        }
    }

    @Test
    fun `duplicate live semantic participation fails closed`() {
        val first =
            mission(
                id = 10L,
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry-10",
            )
        val second =
            mission(
                id = 11L,
                sourceType = MissionSourceType.CONTEXT_BACKLOG_ITEM,
                sourceContextId = "owner",
                sourceBacklogItemId = "entry-11",
            )
        val snapshot = snapshot(missions = listOf(first, second), orientationId = "shared")

        assertThrows(IllegalArgumentException::class.java) {
            planCanonicalTacticalPlanningMaterialization(listOf(iteration()), snapshot)
        }
    }

    @Test
    fun `unadopted manual and tombstoned live target are integrity failures`() {
        val unadopted =
            TacticalMissionOrientationResolutionSnapshot(
                missions = listOf(mission(id = 12L)),
                backlogEntries = emptyList(),
                arcQuests = emptyList(),
                mappings = emptyList(),
                subjects = emptyList(),
                orientations = emptyList(),
            )
        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(listOf(iteration()), unadopted)
        }

        val deleted =
            snapshot(
                missions = listOf(mission(id = 13L)),
                orientationId = legacyManualTacticalMissionSubjectId(13L),
                deleted = true,
            )
        assertThrows(IllegalStateException::class.java) {
            planCanonicalTacticalPlanningMaterialization(listOf(iteration()), deleted)
        }
    }

    private fun iteration() =
        TacticalIteration(
            id = "cycle-1",
            title = "Cycle",
            startedAt = 100L,
            plannedEndAt = 200L,
            weekKey = "2026-W40",
            createdAt = 90L,
            updatedAt = 150L,
            version = 2L,
        )

    private fun mission(
        id: Long,
        iterationId: String? = "cycle-1",
        sourceType: MissionSourceType = MissionSourceType.MANUAL,
        sourceContextId: String? = null,
        sourceBacklogItemId: String? = null,
        isDeleted: Boolean = false,
    ) = TacticalMission(
        id = id,
        title = "Mission $id",
        description = null,
        deadline = NO_DEADLINE,
        status = MissionStatus.PAUSED,
        priority = MissionPriority.HIGH,
        projectId = null,
        orderInWeek = 4L,
        iterationId = iterationId,
        sourceType = sourceType,
        sourceContextId = sourceContextId,
        sourceBacklogItemId = sourceBacklogItemId,
        createdAt = 10L,
        updatedAt = 20L,
        isDeleted = isDeleted,
        version = 3L,
    )

    private fun snapshot(
        missions: List<TacticalMission>,
        orientationId: String,
        deleted: Boolean = false,
    ) = TacticalMissionOrientationResolutionSnapshot(
        missions = missions,
        backlogEntries =
            missions.map { mission ->
                WorkspaceBacklogEntryEntity(
                    id = mission.sourceBacklogItemId ?: "unused-${mission.id}",
                    workspaceId = mission.sourceContextId ?: "owner",
                    capabilityInstanceId = "backlog",
                    targetKind = WorkspaceBacklogTargetKind.ORIENTATION.name,
                    targetId = orientationId,
                    entryOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                )
            },
        arcQuests = emptyList(),
        mappings =
            missions.map { mission ->
                com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity(
                    id = "mapping-${mission.id}",
                    sourceType =
                        com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
                            .TACTICAL_MISSION.name,
                    sourceId = mission.id.toString(),
                    subjectId = orientationId,
                    migrationVersion = 1,
                    state =
                        com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
                            .CUT_OVER.name,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                )
            },
        subjects =
            listOf(
                ManagedSubjectEntity(
                    id = orientationId,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = orientationId,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = deleted,
                    version = 1L,
                ),
            ),
        orientations =
            listOf(
                OrientationEntity(
                    subjectId = orientationId,
                    kind = OrientationKind.INTENT.name,
                    lifecycle = null,
                    lifecycleOrigin = ValueOrigin.UNSET.name,
                ),
            ),
    )
}
