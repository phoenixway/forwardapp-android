package com.romankozak.forwardappmobile.shared.core.domain.product

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
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
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.Workspace
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CanonicalProductReadProjectionTest {
    @Test
    fun orientationWithoutEdgesRemainsOneSemanticNode() {
        val ready = project(baseSnapshot())

        assertEquals(1, ready.orientations.size)
        assertEquals("orientation-1", ready.orientations.single().subject.id)
        assertTrue(ready.orientations.single().planningMemberships.isEmpty())
    }

    @Test
    fun sameOrientationParticipatesInDayTacticalAndStrategicWithoutDuplication() {
        val day = scope("day", PlanningScopeKind.DAY)
        val tactical = scope("tactical", PlanningScopeKind.TACTICAL_CYCLE)
        val strategic = scope("strategic", PlanningScopeKind.STRATEGIC_ARC)
        val snapshot =
            baseSnapshot().copy(
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
                currentScopeIds = setOf(tactical.id, strategic.id),
            )

        val orientation = project(snapshot).orientations.single()

        assertEquals(1, project(snapshot).orientations.size)
        assertEquals(
            listOf(PlanningScopeKind.TACTICAL_CYCLE, PlanningScopeKind.DAY, PlanningScopeKind.STRATEGIC_ARC),
            orientation.planningMemberships.map { it.scope.kind },
        )
        assertEquals(listOf(true, false, true), orientation.planningMemberships.map { it.isCurrentScope })
        assertIs<PlanningScopeExtensionProjection.Day>(orientation.planningMemberships[1].extension)
        assertIs<PlanningScopeExtensionProjection.Tactical>(orientation.planningMemberships[0].extension)
        assertIs<PlanningScopeExtensionProjection.Strategic>(orientation.planningMemberships[2].extension)
    }

    @Test
    fun workspaceMembershipAndHierarchyAppearancesRemainIndependentEdges() {
        val workspace = workspace("workspace-1")
        val binding = binding("binding-1", workspace.id, "orientation-1")
        val direct = placement("direct", HierarchyTargetType.MANAGED_SUBJECT, "orientation-1", 2L)
        val throughWorkspace = placement("workspace", HierarchyTargetType.WORKSPACE, workspace.id, 1L)

        val orientation =
            project(
                baseSnapshot().copy(
                    workspaces = listOf(workspace),
                    workspaceBindings = listOf(binding),
                    hierarchyPlacements = listOf(direct, throughWorkspace),
                ),
            ).orientations.single()

        assertEquals(1, orientation.workspaceMemberships.size)
        assertEquals(2, orientation.hierarchyAppearances.size)
        assertEquals(
            setOf(HierarchyAppearanceSource.ORIENTATION, HierarchyAppearanceSource.BOUND_WORKSPACE),
            orientation.hierarchyAppearances.map { it.source }.toSet(),
        )
    }

    @Test
    fun aspectMembershipIsNotPlanningWorkspaceOrHierarchy() {
        val aspectSubject = subject("aspect-1", ManagedSubjectType.ASPECT, "Health")
        val aspect = AspectNode("aspect-1", null, 0L, archived = false)
        val reference =
            AspectOrientationRef(
                id = "aspect-ref",
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                aspectId = "aspect-1",
                orientationId = "orientation-1",
                relationType = AspectOrientationRelationType.BELONGS_TO,
                isPrimary = true,
                order = 0L,
            )

        val orientation =
            project(
                baseSnapshot().copy(
                    subjects = baseSnapshot().subjects + aspectSubject,
                    aspects = listOf(aspect),
                    aspectOrientationRefs = listOf(reference),
                ),
            ).orientations.single()

        assertEquals("aspect-1", orientation.aspectMemberships.single().aspect.subjectId)
        assertTrue(orientation.planningMemberships.isEmpty())
        assertTrue(orientation.workspaceMemberships.isEmpty())
    }

    @Test
    fun brokenCanonicalCommitmentReferenceFailsClosed() {
        val day = scope("day", PlanningScopeKind.DAY)
        val result =
            projectCanonicalProductRead(
                baseSnapshot().copy(
                    planningScopes = listOf(day),
                    planningCommitments =
                        listOf(commitment("broken", day, PlanningRoleCode.DAY_THEME).copy(orientationId = "missing")),
                    dayExtensions = listOf(dayExtension(day)),
                ),
            )

        val failure = assertIs<CanonicalProductReadResult.IntegrityFailure>(result)
        assertTrue(failure.violations.any { it.code == "ORIENTATION_NOT_LIVE" })
    }

    @Test
    fun liveScopeWithoutTypedExtensionFailsClosed() {
        val result =
            projectCanonicalProductRead(
                baseSnapshot().copy(planningScopes = listOf(scope("day", PlanningScopeKind.DAY))),
            )

        val failure = assertIs<CanonicalProductReadResult.IntegrityFailure>(result)
        assertTrue(failure.violations.any { it.code == "MISSING_OR_AMBIGUOUS_SCOPE_EXTENSION" })
    }

    @Test
    fun selectedScopeMustBeCanonicalAndLive() {
        val result =
            projectCanonicalProductRead(
                baseSnapshot().copy(currentScopeIds = setOf(PlanningScopeId("missing"))),
            )

        val failure = assertIs<CanonicalProductReadResult.IntegrityFailure>(result)
        assertTrue(failure.violations.any { it.code == "SCOPE_NOT_LIVE" })
    }

    @Test
    fun brokenHierarchyReferenceFailsClosed() {
        val result =
            projectCanonicalProductRead(
                baseSnapshot().copy(
                    hierarchyPlacements = listOf(placement("broken", HierarchyTargetType.WORKSPACE, "missing", 0L)),
                ),
            )

        val failure = assertIs<CanonicalProductReadResult.IntegrityFailure>(result)
        assertTrue(failure.violations.any { it.code == "TARGET_NOT_LIVE" })
    }

    private fun project(snapshot: CanonicalProductReadSnapshot): CanonicalProductReadResult.Ready =
        assertIs(projectCanonicalProductRead(snapshot))

    private fun baseSnapshot(): CanonicalProductReadSnapshot {
        val subject = subject("orientation-1", ManagedSubjectType.ORIENTATION, "One")
        val orientation =
            OrientationNode(
                subjectId = subject.id,
                kind = OrientationKind.INTENT,
                lifecycle = OrientationLifecycle.ACTIVE,
                lifecycleOrigin = ValueOrigin.EXPLICIT,
                assessment = initialOrientationAssessment(OrientationKind.INTENT),
            )
        return CanonicalProductReadSnapshot(subjects = listOf(subject), orientations = listOf(orientation))
    }

    private fun subject(id: String, type: ManagedSubjectType, title: String) =
        ManagedSubject(id, 1L, 1L, null, false, 1L, type, title, null)

    private fun scope(id: String, kind: PlanningScopeKind) =
        PlanningScope(
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
    ) =
        PlanningCommitment(
            id = PlanningCommitmentId(id),
            scopeId = scope.id,
            orientationId = "orientation-1",
            role = role,
            order = order,
            priorityLevel = null,
            status = PlanningCommitmentStatus.ACTIVE,
            provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL),
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            version = 1L,
            isDeleted = false,
        )

    private fun dayExtension(scope: PlanningScope) =
        DayPlanningScopeExtension(scope.id, 0L, null, null, 1L, 1L, null, 1L, false)

    private fun tacticalExtension(scope: PlanningScope) =
        TacticalPlanningScopeExtension(
            scope.id,
            TacticalPlanningScopeType.TIMEBOXED,
            "2026-W41",
            null,
            1L,
            1L,
            null,
            1L,
            false,
        )

    private fun strategicExtension(scope: PlanningScope) =
        StrategicPlanningScopeExtension(scope.id, "2026-10", 1L, 1L, null, 1L, false)

    private fun workspace(id: String) =
        Workspace(
            id = id,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            nameOverride = "Workspace",
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            order = 0L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY,
            sourceContextId = null,
        )

    private fun binding(id: String, workspaceId: String, subjectId: String) =
        WorkspaceBinding(
            id = id,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            workspaceId = workspaceId,
            subjectId = subjectId,
            bindingType = WorkspaceBindingType.SUPPORTS,
            isPrimary = false,
            order = 0L,
        )

    private fun placement(
        id: String,
        targetType: HierarchyTargetType,
        targetId: String,
        order: Long,
    ) =
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target = HierarchyTargetRef(targetType, targetId),
            parentPlacementId = null,
            placementKind = PlacementKind.PRIMARY,
            siblingOrder = order,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
