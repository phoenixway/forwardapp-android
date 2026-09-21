package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyPresentationTreeBuilder
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationBeaconInput
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationHierarchyBuilder
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalV1HierarchySnapshotParityTest {
    @Test
    fun `H2 visible occurrence order matches current renderer across supported route types`() {
        val presentations =
            listOf(
                presentation("owner", null, 0),
                presentation("owner-child", "owner", 0),
                presentation("extra-root", null, 1),
                presentation("shared", "extra-root", 0),
                presentation("free-root", null, 2),
                presentation("free-child", "free-root", 0),
            )
        val workspaces =
            presentations.map {
                workspace(
                    id = it.id,
                    parentId = it.parentId,
                    order = it.order,
                )
            }

        val contextLinks =
            listOf(
                ContextParentLink(
                    parentContextId = "owner",
                    childContextId = "shared",
                    order = 0,
                ),
            )
        val beaconLinks =
            listOf(
                MainBeaconParentLink(
                    parentBeaconId = "b2",
                    childBeaconId = "b-child",
                    order = 0,
                ),
            )
        val groups =
            listOf(
                MainBeaconGroup(
                    id = "group",
                    title = "Core",
                    order = 0,
                ),
            )
        val currentBeacons =
            listOf(
                beacon("b1", 0, null, listOf("owner"), listOf("group")),
                beacon("b2", 1, null),
                beacon("b-child", 0, "b1"),
            )

        val current =
            OrientationHierarchyBuilder().build(
                presentationHierarchy =
                    HierarchyPresentationTreeBuilder().build(presentations),
                beacons = currentBeacons,
                groups = groups,
                parentLinks = contextLinks,
                beaconParentLinks = beaconLinks,
                workspaces = workspaces,
            )

        val currentVisibleOccurrenceIds =
            current.mapNotNull { item ->
                when (val node = item.node) {
                    is OrientationHierarchyNode.Beacon -> node.id
                    is OrientationHierarchyNode.ProjectLike -> node.id
                    is OrientationHierarchyNode.Group,
                    OrientationHierarchyNode.NoBeacon,
                    OrientationHierarchyNode.NoGroup,
                    -> null
                }
            }

        val h2 =
            CanonicalV1HierarchySnapshotBuilder().build(
                CanonicalV1HierarchySnapshotInput(
                    workspaces =
                        presentations.mapIndexed { index, it ->
                            CanonicalV1WorkspaceSnapshotInput(
                                id = it.id,
                                name = it.name,
                                parentWorkspaceId = it.parentId,
                                order = it.order,
                                sourceOrdinal = index,
                            )
                        },
                    beacons =
                        currentBeacons.mapIndexed { index, it ->
                            CanonicalV1BeaconSnapshotInput(
                                legacyBeaconId = it.id,
                                target =
                                    HierarchyTargetRef(
                                        HierarchyTargetType.MANAGED_SUBJECT,
                                        it.id,
                                    ),
                                title = it.title,
                                order = it.order,
                                parentBeaconId = it.parentBeaconId,
                                relatedOwnerIds = it.relatedOwnerIds,
                                groupIds = it.groupIds,
                                groupOrders = it.groupOrders,
                                sourceOrdinal = index,
                            )
                        },
                    groups =
                        groups.mapIndexed { index, it ->
                            CanonicalV1BeaconGroupSnapshotInput(
                                id = it.id,
                                title = it.title,
                                order = it.order,
                                canonicalSubjectId = "test-group-subject:${it.id}",
                                sourceOrdinal = index,
                            )
                        },
                    contextParentLinks =
                        contextLinks.mapIndexed { index, it ->
                            CanonicalV1ContextParentLinkSnapshotInput(
                                parentWorkspaceId = it.parentContextId,
                                childWorkspaceId = it.childContextId,
                                order = it.order,
                                sourceOrdinal = index,
                            )
                        },
                    beaconParentLinks =
                        beaconLinks.mapIndexed { index, it ->
                            CanonicalV1BeaconParentLinkSnapshotInput(
                                parentBeaconId = it.parentBeaconId,
                                childBeaconId = it.childBeaconId,
                                order = it.order,
                                sourceOrdinal = index,
                            )
                        },
                ),
            )

        assertEquals(
            currentVisibleOccurrenceIds,
            h2.occurrences.map { it.target.id },
        )
    }

    private fun presentation(
        id: String,
        parentId: String?,
        order: Long,
    ) = HierarchyContextPresentationNode(
        id = id,
        name = id,
        description = null,
        parentId = parentId,
        order = order,
        roleCode = null,
        tags = emptyList(),
    )

    private fun workspace(
        id: String,
        parentId: String?,
        order: Long,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = id,
        descriptionOverride = null,
        parentWorkspaceId = parentId,
        roleCode = null,
        workspaceOrder = order,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        provenance = "CANONICAL_ONLY",
        sourceContextId = null,
    )

    private fun beacon(
        id: String,
        order: Long,
        parentId: String?,
        owners: List<String> = emptyList(),
        groups: List<String> = emptyList(),
    ) = OrientationBeaconInput(
        id = id,
        title = id,
        order = order,
        readinessStatus = MainBeaconReadinessStatus.READY,
        parentBeaconId = parentId,
        relatedOwnerIds = owners,
        groupIds = groups,
        groupOrders = groups.associateWith { 0L },
    )
}
