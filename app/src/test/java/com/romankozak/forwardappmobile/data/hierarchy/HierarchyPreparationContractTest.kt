package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyPreparationContractTest {
    @Test
    fun `placement conversion preserves concrete occurrence identity`() {
        val placement =
            placement(
                placementId = "link-occurrence",
                targetId = "shared-target",
                parentPlacementId = "parent-occurrence",
                kind = PlacementKind.LINK,
                siblingOrder = 7L,
            )

        val ref = placement.toHierarchyOccurrenceRef()

        assertEquals(PlacementId("link-occurrence"), ref.placementId)
        assertEquals(workspaceTarget("shared-target"), ref.target)
        assertEquals(PlacementId("parent-occurrence"), ref.parentPlacementId)
        assertEquals(PlacementKind.LINK, ref.placementKind)
        assertEquals(7L, ref.siblingOrder)
    }

    @Test
    fun `V2 projection occurrence conversion preserves occurrence identity`() {
        val occurrence =
            CanonicalV2HierarchyOccurrence(
                placementId = PlacementId("occurrence"),
                target = workspaceTarget("workspace"),
                placementKind = PlacementKind.PRIMARY,
                parentPlacementId = PlacementId("parent"),
                occurrencePath =
                    listOf(
                        PlacementId("parent"),
                        PlacementId("occurrence"),
                    ),
                siblingOrder = 3L,
                rootOrder = 0,
                depth = 1,
                presentation =
                    CanonicalV2HierarchyTargetPresentation(
                        target = workspaceTarget("workspace"),
                        title = "Workspace",
                    ),
            )

        val ref = occurrence.toHierarchyOccurrenceRef()

        assertEquals(occurrence.placementId, ref.placementId)
        assertEquals(occurrence.target, ref.target)
        assertEquals(occurrence.parentPlacementId, ref.parentPlacementId)
        assertEquals(occurrence.placementKind, ref.placementKind)
        assertEquals(occurrence.siblingOrder, ref.siblingOrder)
    }

    @Test
    fun `same target PRIMARY and LINK occurrences remain distinct`() {
        val target = workspaceTarget("shared")
        val primary =
            placement(
                placementId = "primary",
                targetId = target.id,
                kind = PlacementKind.PRIMARY,
                siblingOrder = 0L,
            ).toHierarchyOccurrenceRef()
        val link =
            placement(
                placementId = "link",
                targetId = target.id,
                kind = PlacementKind.LINK,
                siblingOrder = 1L,
            ).toHierarchyOccurrenceRef()

        assertEquals(target, primary.target)
        assertEquals(target, link.target)
        assertTrue(primary.placementId != link.placementId)
        assertEquals(PlacementKind.PRIMARY, primary.placementKind)
        assertEquals(PlacementKind.LINK, link.placementKind)
    }

    @Test
    fun `LINK owned child occurrence is representable without target ancestry`() {
        val ownerLink =
            placement(
                placementId = "owner-link",
                targetId = "owner",
                kind = PlacementKind.LINK,
                siblingOrder = 0L,
            ).toHierarchyOccurrenceRef()
        val child =
            placement(
                placementId = "child-primary",
                targetId = "child",
                parentPlacementId = ownerLink.placementId.value,
                kind = PlacementKind.PRIMARY,
                siblingOrder = 0L,
            ).toHierarchyOccurrenceRef()

        assertEquals(PlacementKind.LINK, ownerLink.placementKind)
        assertEquals(ownerLink.placementId, child.parentPlacementId)
        assertTrue(ownerLink.target != child.target)
    }

    @Test
    fun `clipboard carrier preserves selected PRIMARY and LINK occurrence identities`() {
        val primary =
            HierarchyClipboardOccurrence(
                occurrence =
                    placement(
                        placementId = "primary",
                        targetId = "same",
                        kind = PlacementKind.PRIMARY,
                    ).toHierarchyOccurrenceRef(),
                sourceKind = HierarchyClipboardSourceKind.WORKSPACE,
            )
        val link =
            HierarchyClipboardOccurrence(
                occurrence =
                    placement(
                        placementId = "link",
                        targetId = "same",
                        kind = PlacementKind.LINK,
                    ).toHierarchyOccurrenceRef(),
                sourceKind = HierarchyClipboardSourceKind.CONTEXT_COMPATIBILITY,
            )

        val intent =
            HierarchyClipboardIntent(
                operation = HierarchyClipboardOperation.CUT,
                sources = listOf(primary, link),
                destination = HierarchyClipboardDestination.NoBeacon,
            )

        assertEquals(
            listOf(PlacementId("primary"), PlacementId("link")),
            intent.sources.map { it.occurrence.placementId },
        )
        assertEquals(
            listOf(PlacementKind.PRIMARY, PlacementKind.LINK),
            intent.sources.map { it.occurrence.placementKind },
        )
        assertTrue(intent.destination is HierarchyClipboardDestination.NoBeacon)
    }

    @Test
    fun `Beacon and Group clipboard destinations preserve selected occurrence identity`() {
        val source =
            HierarchyClipboardOccurrence(
                occurrence =
                    placement(
                        placementId = "beacon-occurrence",
                        targetId = "subject",
                        kind = PlacementKind.LINK,
                    ).toHierarchyOccurrenceRef(),
                sourceKind = HierarchyClipboardSourceKind.BEACON,
                legacySourceId = "legacy-beacon",
            )

        val beaconIntent =
            HierarchyClipboardIntent(
                operation = HierarchyClipboardOperation.CUT,
                sources = listOf(source),
                destination =
                    HierarchyClipboardDestination.BeaconOwner(
                        legacyBeaconId = "target-beacon",
                        occurrence =
                            placement(
                                placementId = "target-beacon-occurrence",
                                targetId = "target-beacon-subject",
                                kind = PlacementKind.PRIMARY,
                            ).toHierarchyOccurrenceRef(),
                    ),
            )
        val groupIntent =
            beaconIntent.copy(
                destination = HierarchyClipboardDestination.Group("canonical-group-subject"),
            )

        assertTrue(beaconIntent.destination is HierarchyClipboardDestination.BeaconOwner)
        assertTrue(groupIntent.destination is HierarchyClipboardDestination.Group)
        assertEquals(PlacementId("beacon-occurrence"), source.occurrence.placementId)
    }

    @Test
    fun `occurrence removal and target deletion are different intents`() {
        val removal =
            HierarchyDestructiveIntent.RemoveOccurrence(
                placementId = PlacementId("occurrence"),
            )
        val deletion =
            HierarchyDestructiveIntent.DeleteTarget(
                target = workspaceTarget("workspace"),
            )

        assertTrue(removal is HierarchyDestructiveIntent.RemoveOccurrence)
        assertTrue(deletion is HierarchyDestructiveIntent.DeleteTarget)
    }

    @Test
    fun `persisted hierarchy target type cannot encode synthetic Group NoGroup or NoBeacon`() {
        assertEquals(
            setOf(
                HierarchyTargetType.MANAGED_SUBJECT,
                HierarchyTargetType.WORKSPACE,
            ),
            HierarchyTargetType.values().toSet(),
        )

        val noBeacon: HierarchyClipboardDestination = HierarchyClipboardDestination.NoBeacon
        val group: HierarchyClipboardDestination = HierarchyClipboardDestination.Group("canonical-group-subject")

        assertTrue(noBeacon is HierarchyClipboardDestination.NoBeacon)
        assertTrue(group is HierarchyClipboardDestination.Group)
    }

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private fun placement(
        placementId: String,
        targetId: String,
        parentPlacementId: String? = null,
        kind: PlacementKind,
        siblingOrder: Long = 0L,
    ) = HierarchyPlacement(
        id = PlacementId(placementId),
        hierarchyId = HierarchyId.GENERAL,
        target = workspaceTarget(targetId),
        parentPlacementId = parentPlacementId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = siblingOrder,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )
}
