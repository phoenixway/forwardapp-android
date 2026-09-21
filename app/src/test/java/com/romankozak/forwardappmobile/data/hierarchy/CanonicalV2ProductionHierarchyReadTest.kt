package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2ProductionHierarchyReadTest {
    private val adapter = CanonicalV2ProductionHierarchyReadAdapter()

    @Test
    fun `topology comes from placements and ignores Workspace presentation parent and order`() {
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("root", workspaceTarget("root"), order = 5),
                        placement("child", workspaceTarget("child"), parentId = "root", order = 7),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        workspacePresentation("root", parentId = "bogus", order = 999),
                        workspacePresentation("child", parentId = null, order = -100),
                    ),
                managedSubjects = emptyList(),
            )

        assertEquals(
            listOf("root", "child"),
            read.occurrencePath(PlacementId("child")).map { it.placementId.value },
        )
        assertEquals(
            PlacementId("root"),
            read.parentOccurrence(PlacementId("child"))?.placementId,
        )
    }

    @Test
    fun `duplicate target PRIMARY and LINK remain distinct`() {
        val shared = workspaceTarget("shared")
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("primary", shared, kind = PlacementKind.PRIMARY, order = 0),
                        placement("link", shared, kind = PlacementKind.LINK, order = 1),
                    ),
                admittedWorkspacePresentations =
                    listOf(workspacePresentation("shared")),
                managedSubjects = emptyList(),
            )

        val appearances = read.workspaceOccurrences("shared")

        assertEquals(listOf("primary", "link"), appearances.map { it.placementId.value })
        assertNotEquals(appearances[0].placementId, appearances[1].placementId)
        assertEquals(
            listOf(PlacementKind.PRIMARY, PlacementKind.LINK),
            appearances.map { it.placementKind },
        )
    }

    @Test
    fun `LINK occurrence owns its own subtree`() {
        val shared = workspaceTarget("shared")
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("primary", shared, kind = PlacementKind.PRIMARY, order = 0),
                        placement("link", shared, kind = PlacementKind.LINK, order = 1),
                        placement(
                            "link-child",
                            workspaceTarget("link-child"),
                            parentId = "link",
                            kind = PlacementKind.LINK,
                        ),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        workspacePresentation("shared"),
                        workspacePresentation("link-child"),
                    ),
                managedSubjects = emptyList(),
            )

        assertEquals(
            listOf("link", "link-child"),
            read.occurrencePath(PlacementId("link-child")).map { it.placementId.value },
        )
        assertEquals(
            listOf("link-child"),
            read.childrenOf(PlacementId("link")).map { it.placementId.value },
        )
    }

    @Test
    fun `root and sibling ordering follows persisted placement order`() {
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("root-b", workspaceTarget("root-b"), order = 20),
                        placement("root-a", workspaceTarget("root-a"), order = 10),
                        placement("child-b", workspaceTarget("child-b"), parentId = "root-a", order = 30),
                        placement("child-a", workspaceTarget("child-a"), parentId = "root-a", order = 5),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        workspacePresentation("root-a"),
                        workspacePresentation("root-b"),
                        workspacePresentation("child-a"),
                        workspacePresentation("child-b"),
                    ),
                managedSubjects = emptyList(),
            )

        assertEquals(
            listOf("root-a", "root-b"),
            read.childrenOf(null).map { it.placementId.value },
        )
        assertEquals(
            listOf("child-a", "child-b"),
            read.childrenOf(PlacementId("root-a")).map { it.placementId.value },
        )
    }

    @Test
    fun `exact occurrence focus stays separate from target navigation policy`() {
        val shared = workspaceTarget("shared")
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("first-link", shared, kind = PlacementKind.LINK, order = 0),
                        placement("primary", shared, kind = PlacementKind.PRIMARY, order = 1),
                    ),
                admittedWorkspacePresentations =
                    listOf(workspacePresentation("shared")),
                managedSubjects = emptyList(),
            )

        assertEquals(
            listOf("first-link"),
            read.occurrencePath(PlacementId("first-link")).map { it.placementId.value },
        )
        assertEquals(
            "first-link",
            read.navigationOccurrence(
                shared,
                CanonicalV2TargetNavigationPolicy.FIRST_VISIBLE,
            )?.placementId?.value,
        )
        assertEquals(
            "primary",
            read.navigationOccurrence(
                shared,
                CanonicalV2TargetNavigationPolicy.PRIMARY_THEN_FIRST_VISIBLE,
            )?.placementId?.value,
        )
    }

    @Test
    fun `breadcrumbs and search ancestry use exact PlacementId path`() {
        val shared = workspaceTarget("shared")
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("root-a", workspaceTarget("a"), order = 0),
                        placement("shared-a", shared, parentId = "root-a", kind = PlacementKind.PRIMARY),
                        placement("root-b", workspaceTarget("b"), order = 1),
                        placement("shared-b", shared, parentId = "root-b", kind = PlacementKind.LINK),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        workspacePresentation("a", name = "A"),
                        workspacePresentation("b", name = "B"),
                        workspacePresentation("shared", name = "Shared"),
                    ),
                managedSubjects = emptyList(),
            )

        assertEquals(
            listOf("root-b"),
            read.structuralAncestors(PlacementId("shared-b")).map { it.placementId.value },
        )
        assertEquals(
            listOf(CANONICAL_V2_NO_BEACON_SCOPE_ID, "b", "shared"),
            read.breadcrumbsToOccurrence(PlacementId("shared-b")).map { it.id },
        )
        assertEquals(
            listOf(
                CanonicalV2BreadcrumbTarget.ORIENTATION_NODE,
                CanonicalV2BreadcrumbTarget.CONTEXT,
                CanonicalV2BreadcrumbTarget.CONTEXT,
            ),
            read.breadcrumbsToOccurrence(PlacementId("shared-b")).map { it.target },
        )
        assertEquals(
            listOf(null, "root-b", "shared-b"),
            read.breadcrumbsToOccurrence(PlacementId("shared-b"))
                .map { it.placementId?.value },
        )
    }

    @Test
    fun `shell free System Workspace stays visible on V2 occurrence path`() {
        val systemId = SystemContexts.INBOX.raw
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement("parent", workspaceTarget("parent")),
                        placement("system", workspaceTarget(systemId), parentId = "parent"),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        workspacePresentation("parent", name = "Parent"),
                        workspacePresentation(
                            systemId,
                            parentId = "legacy-parent-must-be-ignored",
                            name = "Canonical Inbox",
                        ),
                    ),
                managedSubjects = emptyList(),
            )

        assertEquals(
            listOf("parent", "system"),
            read.occurrencePath(PlacementId("system")).map { it.placementId.value },
        )
        assertEquals("Canonical Inbox", read.occurrence(PlacementId("system"))?.title)
    }

    @Test
    fun `linked appearance provenance is never inferred from PlacementKind`() {
        val read =
            adapter.read(
                placements =
                    listOf(
                        placement(
                            "linked-root",
                            workspaceTarget("linked-root"),
                            kind = PlacementKind.LINK,
                        ),
                        placement(
                            "linked-child",
                            workspaceTarget("linked-child"),
                            parentId = "linked-root",
                            kind = PlacementKind.LINK,
                        ),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        workspacePresentation("linked-root"),
                        workspacePresentation("linked-child"),
                    ),
                managedSubjects = emptyList(),
                presentationProvenance =
                    CanonicalV2HierarchyPresentationProvenance(
                        linkedAppearancePlacementIds =
                            setOf(PlacementId("linked-root")),
                    ),
            )

        assertTrue(read.isLinkedAppearance(PlacementId("linked-root")))
        assertFalse(read.isLinkedAppearance(PlacementId("linked-child")))
        assertEquals(
            PlacementKind.LINK,
            read.occurrence(PlacementId("linked-child"))?.placementKind,
        )
    }

    @Test
    fun `malformed V2 graph fails closed`() {
        val failure =
            runCatching {
                adapter.read(
                    placements =
                        listOf(
                            placement(
                                "orphan",
                                workspaceTarget("orphan"),
                                parentId = "missing",
                            ),
                        ),
                    admittedWorkspacePresentations =
                        listOf(workspacePresentation("orphan")),
                    managedSubjects = emptyList(),
                )
            }.exceptionOrNull()

        assertTrue(failure is CanonicalV2HierarchyProjectionException)
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

    private fun workspacePresentation(
        id: String,
        parentId: String? = null,
        order: Long = 0,
        name: String = id,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = null,
        parentId = parentId,
        order = order,
        roleCode = null,
        tags = emptyList(),
    )

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    @Suppress("unused")
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
}
