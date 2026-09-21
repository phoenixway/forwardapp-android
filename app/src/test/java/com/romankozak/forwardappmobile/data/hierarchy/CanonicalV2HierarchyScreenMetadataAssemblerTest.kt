package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalV2HierarchyScreenMetadataAssemblerTest {
    private val assembler = CanonicalV2HierarchyScreenMetadataAssembler()

    @Test
    fun `Group beacon count follows only V2 synthetic presentation membership`() {
        val workspace = workspace("workspace")
        val groupScope =
            CanonicalV2PresentedHierarchyEntry.SyntheticScope(
                kind = CanonicalV2SyntheticScopeKind.GROUP,
                id = "group-a",
                title = "Group A",
            )
        val noBeaconScope =
            CanonicalV2PresentedHierarchyEntry.SyntheticScope(
                kind = CanonicalV2SyntheticScopeKind.NO_BEACON,
                id = CANONICAL_V2_NO_BEACON_SCOPE_ID,
                title = "No beacon",
            )
        val rootBeacon =
            occurrence(
                id = "root",
                targetType = HierarchyTargetType.MANAGED_SUBJECT,
                targetId = "subject-root",
                presentationId = "beacon-root",
                level = 1,
            )
        val childBeacon =
            occurrence(
                id = "child",
                targetType = HierarchyTargetType.MANAGED_SUBJECT,
                targetId = "subject-child",
                presentationId = "beacon-child",
                parentPlacementId = PlacementId("root"),
                level = 2,
            )
        val workspaceOccurrence =
            occurrence(
                id = "workspace-placement",
                targetType = HierarchyTargetType.WORKSPACE,
                targetId = "workspace",
                presentationId = "workspace",
                level = 1,
            )

        val read =
            CanonicalV2ProductionHierarchyRead(
                hierarchy =
                    CanonicalV2HierarchyProjection(
                        hierarchyId = HierarchyId.GENERAL,
                        occurrences =
                            listOf(
                                projected(rootBeacon),
                                projected(childBeacon),
                                projected(workspaceOccurrence),
                            ),
                        rootPlacementIds =
                            listOf(
                                rootBeacon.placementId,
                                workspaceOccurrence.placementId,
                            ),
                    ),
                presentation =
                    CanonicalV2HierarchyPresentationProjection(
                        entries =
                            listOf(
                                groupScope,
                                rootBeacon,
                                childBeacon,
                                noBeaconScope,
                                workspaceOccurrence,
                            ),
                    ),
            )

        val metadata =
            assembler.assemble(
                read = read,
                workspacePresentations = listOf(workspace),
                beacons =
                    listOf(
                        operationalBeacon(
                            id = "beacon-root",
                            owners = listOf("workspace", "retired-owner"),
                        ),
                        operationalBeacon(
                            id = "beacon-child",
                            owners = emptyList(),
                        ),
                    ),
            )

        assertEquals(
            2,
            metadata.groupsBySyntheticScopeId.getValue("group-a").beaconCount,
        )
        assertEquals(
            setOf("beacon-root"),
            metadata.linkedBeaconIdsByWorkspaceTargetId.getValue("workspace"),
        )
        assertEquals(
            1,
            metadata.beaconsByPresentationId.getValue("beacon-root").relatedOwnerCount,
        )
    }

    private fun workspace(id: String) =
        HierarchyContextPresentationNode(
            id = id,
            name = id,
            description = null,
            parentId = "legacy-parent-is-metadata-only",
            order = 999L,
            roleCode = null,
            tags = emptyList(),
        )

    private fun operationalBeacon(
        id: String,
        owners: List<String>,
    ) = CanonicalV2HierarchyScreenOperationalBeaconMetadata(
        presentationId = id,
        readinessStatus = MainBeaconReadinessStatus.READY,
        relatedOwnerIds = owners,
    )

    private fun occurrence(
        id: String,
        targetType: HierarchyTargetType,
        targetId: String,
        presentationId: String,
        parentPlacementId: PlacementId? = null,
        level: Int,
    ) = CanonicalV2PresentedHierarchyEntry.Occurrence(
        placementId = PlacementId(id),
        target =
            HierarchyTargetRef(
                type = targetType,
                id = targetId,
            ),
        parentPlacementId = parentPlacementId,
        placementKind = PlacementKind.PRIMARY,
        siblingOrder = 0L,
        presentationId = presentationId,
        title = presentationId,
        level = level,
    )

    private fun projected(
        entry: CanonicalV2PresentedHierarchyEntry.Occurrence,
    ) = CanonicalV2HierarchyOccurrence(
        placementId = entry.placementId,
        target = entry.target,
        placementKind = entry.placementKind,
        parentPlacementId = entry.parentPlacementId,
        occurrencePath =
            entry.parentPlacementId?.let { listOf(it, entry.placementId) }
                ?: listOf(entry.placementId),
        siblingOrder = entry.siblingOrder,
        rootOrder = 0,
        depth = entry.level - 1,
        presentation =
            CanonicalV2HierarchyTargetPresentation(
                target = entry.target,
                title = entry.title,
                presentationId = entry.presentationId,
            ),
    )
}
