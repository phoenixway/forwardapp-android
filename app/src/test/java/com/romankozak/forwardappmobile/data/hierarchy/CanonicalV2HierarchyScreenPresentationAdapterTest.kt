package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2HierarchyScreenPresentationAdapterTest {
    private val readAdapter = CanonicalV2ProductionHierarchyReadAdapter()
    private val screenAdapter = CanonicalV2HierarchyScreenPresentationAdapter()

    @Test
    fun `duplicate Workspace appearances retain exact PlacementId screen identity`() {
        val target =
            HierarchyTargetRef(
                type = HierarchyTargetType.WORKSPACE,
                id = "shared",
            )
        val presentation =
            CanonicalV2WorkspacePresentation(
                id = "shared",
                name = "Shared",
                description = null,
                roleCode = null,
                tags = emptyList(),
            )
        val read =
            readAdapter.read(
                placements =
                    listOf(
                        placement("primary", target, PlacementKind.PRIMARY, order = 10L),
                        placement("link", target, PlacementKind.LINK, order = 20L),
                    ),
                admittedWorkspacePresentations = listOf(presentation),
                managedSubjects = emptyList(),
                presentationProvenance =
                    CanonicalV2HierarchyPresentationProvenance(
                        linkedAppearancePlacementIds = setOf(PlacementId("link")),
                    ),
            )

        val rows =
            screenAdapter.adapt(
                read = read,
                metadata =
                    CanonicalV2HierarchyScreenMetadata(
                        workspacePresentationsByTargetId = mapOf("shared" to presentation),
                        linkedBeaconIdsByWorkspaceTargetId =
                            mapOf("shared" to setOf("beacon-operational-owner")),
                    ),
            )

        assertEquals(3, rows.size)
        assertTrue(rows.first().node is OrientationHierarchyNode.NoBeacon)
        assertNull(rows.first().node.placementId)

        val workspaces =
            rows
                .drop(1)
                .map { it.node as OrientationHierarchyNode.WorkspaceNode }

        assertEquals(listOf("shared", "shared"), workspaces.map { it.id })
        assertEquals(
            listOf(PlacementId("primary"), PlacementId("link")),
            workspaces.map { it.placementId },
        )
        assertEquals(
            listOf(
                HierarchyOccurrenceRef(
                    placementId = PlacementId("primary"),
                    target = target,
                    parentPlacementId = null,
                    placementKind = PlacementKind.PRIMARY,
                    siblingOrder = 10L,
                ),
                HierarchyOccurrenceRef(
                    placementId = PlacementId("link"),
                    target = target,
                    parentPlacementId = null,
                    placementKind = PlacementKind.LINK,
                    siblingOrder = 20L,
                ),
            ),
            workspaces.map { it.occurrence },
        )
        assertEquals(2, workspaces.map { it.occurrence?.placementId }.distinct().size)
        assertEquals(2, workspaces.map { it.structuralKey }.distinct().size)
        assertFalse(workspaces[0].isLinkedAppearance)
        assertTrue(workspaces[1].isLinkedAppearance)
        assertEquals(
            setOf("beacon-operational-owner"),
            workspaces[0].linkedBeaconIds,
        )
        assertEquals(
            listOf(1, 1),
            rows.drop(1).map { it.level },
        )
    }

    @Test
    fun `Workspace deletion cannot combine old occurrences with missing newer metadata`() {
        val workspace = CanonicalV2WorkspacePresentation("w", "Before deletion", null, null, emptyList())
        val read = workspaceRead(workspace)
        val metadata = CanonicalV2HierarchyScreenMetadataAssembler().assemble(read, emptyList(), emptyList())
        val rows = screenAdapter.adapt(read, metadata)
        assertEquals(listOf(PlacementId("primary"), PlacementId("link")),
            rows.mapNotNull { it.node.placementId })
        val deleted = readAdapter.read(emptyList(), emptyList(), emptyList())
        val deletedMetadata = CanonicalV2HierarchyScreenMetadataAssembler().assemble(deleted, listOf(workspace), emptyList())
        assertTrue(screenAdapter.adapt(deleted, deletedMetadata).isEmpty())
    }

    @Test
    fun `Workspace display fields come from the same admitted read as occurrence identity`() {
        val workspace = CanonicalV2WorkspacePresentation("w", "Snapshot title", "Snapshot description", null, listOf("old"))
        val read = workspaceRead(workspace)
        val metadata = CanonicalV2HierarchyScreenMetadataAssembler().assemble(read,
            listOf(workspace.copy(name = "Newer title", description = null, tags = emptyList())), emptyList())
        val rows = screenAdapter.adapt(read, metadata).mapNotNull { it.node as? OrientationHierarchyNode.WorkspaceNode }
        assertEquals(listOf("Snapshot title", "Snapshot title"), rows.map { it.presentation.name })
        assertEquals(listOf("Snapshot description", "Snapshot description"), rows.map { it.presentation.description })
    }

    private fun workspaceRead(workspace: CanonicalV2WorkspacePresentation) = readAdapter.read(
        placements = listOf(
            placement("primary", HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspace.id), PlacementKind.PRIMARY, 0),
            placement("link", HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspace.id), PlacementKind.LINK, 1),
        ), admittedWorkspacePresentations = listOf(workspace), managedSubjects = emptyList(),
    )

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        kind: PlacementKind,
        order: Long,
    ): HierarchyPlacement =
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target = target,
            parentPlacementId = null,
            placementKind = kind,
            siblingOrder = order,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
