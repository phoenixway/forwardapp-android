package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2HierarchyConsumerReadinessTest {
    private val adapter = CanonicalV2ProductionHierarchyReadAdapter()
    private val consumers = CanonicalV2HierarchyConsumerReadiness()

    @Test
    fun `CoreLevel nesting is keyed by PlacementId and preserves duplicate Beacon occurrences`() {
        val shared = subjectTarget("shared")
        val child = subjectTarget("child")
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("primary", shared, kind = PlacementKind.PRIMARY, order = 0),
                        placement("link", shared, kind = PlacementKind.LINK, order = 1),
                        placement(
                            "link-child",
                            child,
                            parentId = "link",
                            kind = PlacementKind.LINK,
                            order = 0,
                        ),
                    ),
                admittedWorkspacePresentations = emptyList(),
                managedSubjects =
                    listOf(
                        subject("shared", "Shared"),
                        subject("child", "Child"),
                    ),
                syntheticScopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(shared, shared),
                        ),
                    ),
            )

        val core = consumers.coreLevelOccurrences(read)

        assertEquals(
            listOf("primary", "link", "link-child"),
            core.map { it.placementId.value },
        )
        assertEquals(
            listOf("shared", "shared", "child"),
            core.map { it.beaconPresentationId },
        )
        assertEquals(
            PlacementId("link"),
            core.single { it.placementId.value == "link-child" }.parentPlacementId,
        )
        assertEquals(
            "shared",
            core.single { it.placementId.value == "link-child" }.parentBeaconPresentationId,
        )
        assertTrue(core.all { it.groupPresentationId == null })
    }

    @Test
    fun `Search occurrence ancestry stays distinct for duplicate Beacon target`() {
        val shared = subjectTarget("shared")
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("parent-a", subjectTarget("parent-a"), order = 0),
                        placement("shared-a", shared, parentId = "parent-a", kind = PlacementKind.PRIMARY),
                        placement("parent-b", subjectTarget("parent-b"), order = 1),
                        placement("shared-b", shared, parentId = "parent-b", kind = PlacementKind.LINK),
                    ),
                admittedWorkspacePresentations = emptyList(),
                managedSubjects =
                    listOf(
                        subject("parent-a", "Parent A"),
                        subject("parent-b", "Parent B"),
                        subject("shared", "Shared"),
                    ),
                syntheticScopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets =
                                listOf(
                                    subjectTarget("parent-a"),
                                    subjectTarget("parent-b"),
                                ),
                        ),
                    ),
            )

        assertEquals(
            listOf("parent-a"),
            consumers
                .occurrenceAncestryForSearch(read, PlacementId("shared-a"))
                .map { it.placementId.value },
        )
        assertEquals(
            listOf("parent-b"),
            consumers
                .occurrenceAncestryForSearch(read, PlacementId("shared-b"))
                .map { it.placementId.value },
        )
    }

    @Test
    fun `CoreLevel rejects Workspace structural parent instead of inventing Beacon nesting`() {
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("workspace", workspaceTarget("workspace"), order = 0),
                        placement(
                            "beacon",
                            subjectTarget("beacon"),
                            parentId = "workspace",
                            order = 0,
                        ),
                    ),
                admittedWorkspacePresentations =
                    listOf(workspacePresentation("workspace")),
                managedSubjects =
                    listOf(subject("beacon", "Beacon")),
            )

        val failure =
            runCatching {
                consumers.coreLevelOccurrences(read)
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = HierarchyId.GENERAL,
        target = target,
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun subject(
        id: String,
        title: String,
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = "ORIENTATION",
        title = title,
        description = null,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun workspacePresentation(
        id: String,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = id,
        description = null,
        parentId = null,
        order = 0,
        roleCode = null,
        tags = emptyList(),
    )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)
}
