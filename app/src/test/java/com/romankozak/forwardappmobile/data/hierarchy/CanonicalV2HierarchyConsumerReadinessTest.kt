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
    fun `CoreLevel omits Workspace branch while Big Picture and search retain descendants`() {
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
                        placement("grandchild", subjectTarget("grandchild"), parentId = "beacon"),
                    ),
                admittedWorkspacePresentations =
                    listOf(workspacePresentation("workspace")),
                managedSubjects =
                    listOf(subject("beacon", "Beacon"), subject("grandchild", "Grandchild")),
            )

        assertTrue(consumers.coreLevelOccurrences(read).isEmpty())
        assertEquals(3, read.hierarchy.occurrences.size)
        assertEquals(listOf("workspace", "beacon", "grandchild"),
            read.breadcrumbsToOccurrence(PlacementId("grandchild")).mapNotNull { it.placementId?.value })
    }

    @Test
    fun `CoreLevel keeps historical siblings and omits complete mixed subtree inside historical scope`() {
        val read = adapter.read(
            placements = listOf(
                placement("root", subjectTarget("root")),
                placement("workspace", workspaceTarget("w"), parentId = "root"),
                placement("beacon", subjectTarget("shared"), parentId = "workspace"),
                placement("child", subjectTarget("child"), parentId = "beacon"),
                placement("kept-link", subjectTarget("shared"), parentId = "root", kind = PlacementKind.LINK, order = 1),
            ),
            admittedWorkspacePresentations = listOf(workspacePresentation("w")),
            managedSubjects = listOf(subject("root", "Root"), subject("shared", "Shared"), subject("child", "Child")),
            syntheticScopes = listOf(CanonicalV2SyntheticScopeInput(
                kind = CanonicalV2SyntheticScopeKind.NO_GROUP, id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                title = "No group", order = 0, rootTargets = listOf(subjectTarget("root")),
            )),
        )
        val core = consumers.coreLevelOccurrences(read)
        assertEquals(listOf("root", "kept-link"), core.map { it.placementId.value })
        assertEquals(PlacementId("root"), core.last().parentPlacementId)
        assertEquals(5, read.hierarchy.occurrences.size)
        // Input order cannot bring a grandchild of an excluded node back in.
        assertEquals(core, consumers.coreLevelOccurrences(read.copy(hierarchy = read.hierarchy.copy(
            occurrences = read.hierarchy.occurrences.reversed(),
        ))).reversed())
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
    ) = CanonicalV2WorkspacePresentation(
        id = id,
        name = id,
        description = null,
        roleCode = null,
        tags = emptyList(),
    )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)
}
