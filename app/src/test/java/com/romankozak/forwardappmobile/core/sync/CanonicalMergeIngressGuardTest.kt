package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.InboxRecordSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupMemberSnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalMergeIngressGuardTest {
    @Test
    fun `merge rejects ordinary restore only backlog and inbox`() {
        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalMergeIngress(
                SnapshotBundle(backlogItems = listOf(backlog("ordinary"))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalMergeIngress(
                SnapshotBundle(inbox = listOf(inbox("ordinary"))),
            )
        }
    }

    @Test
    fun `CURRENT production mode keeps existing hierarchy ingress behavior`() {
        requireCanonicalMergeIngress(
            SnapshotBundle(workspaces = listOf(workspace("w"))),
        )
    }

    @Test
    fun `future V2 mode rejects legacy structural payload without H1`() {
        assertThrows(IllegalArgumentException::class.java) {
            requireCanonicalMergeIngress(
                bundle = SnapshotBundle(workspaces = listOf(workspace("w"))),
                hierarchyAuthorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
            )
        }
    }

    @Test
    fun `future V2 mode admits hierarchy bearing payload with H1 present or empty`() {
        requireCanonicalMergeIngress(
            bundle =
                SnapshotBundle(
                    workspaces = listOf(workspace("w")),
                    hierarchyPlacements = listOf(h1()),
                    hierarchyPlacementGroupScopes = emptyList(),
                    hierarchyPlacementLinkedAppearances = emptyList(),
                ),
            hierarchyAuthorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
        )
        requireCanonicalMergeIngress(
            bundle =
                SnapshotBundle(
                    workspaces = listOf(workspace("w")),
                    hierarchyPlacements = emptyList(),
                    hierarchyPlacementGroupScopes = emptyList(),
                    hierarchyPlacementLinkedAppearances = emptyList(),
                ),
            hierarchyAuthorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
        )
    }

    @Test
    fun `future V2 mode rejects partial canonical hierarchy transport`() {
        listOf(
            SnapshotBundle(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = null,
                hierarchyPlacementLinkedAppearances = null,
            ),
            SnapshotBundle(
                hierarchyPlacements = null,
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = null,
            ),
            SnapshotBundle(
                hierarchyPlacements = null,
                hierarchyPlacementGroupScopes = null,
                hierarchyPlacementLinkedAppearances = emptyList(),
            ),
            SnapshotBundle(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = null,
            ),
            SnapshotBundle(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementGroupScopes = null,
                hierarchyPlacementLinkedAppearances = emptyList(),
            ),
            SnapshotBundle(
                hierarchyPlacements = null,
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
            ),
        ).forEach { bundle ->
            assertThrows(IllegalArgumentException::class.java) {
                requireCanonicalMergeIngress(
                    bundle = bundle,
                    hierarchyAuthorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
                )
            }
        }
    }

    @Test
    fun `future V2 mode admits semantic only beacon relations without H1`() {
        requireCanonicalMergeIngress(
            bundle =
                SnapshotBundle(
                    mainBeaconGroupMembers =
                        listOf(MainBeaconGroupMemberSnapshot("group", "beacon", 0L)),
                    mainBeaconContextCrossRefs =
                        listOf(MainBeaconContextCrossRefSnapshot("beacon", "w", 0L)),
                ),
            hierarchyAuthorityMode = HierarchyPlacementAuthorityMode.V2_AUTHORITY,
        )
    }

    @Test
    fun `canonical presence and exact System compatibility remain admitted`() {
        requireCanonicalMergeIngress(
            SnapshotBundle(
                backlogItems = listOf(backlog("ordinary")),
                inbox = listOf(inbox("ordinary")),
                workspaceBacklogEntries = emptyList(),
                workspaceInboxRecords = emptyList(),
            ),
        )
        requireCanonicalMergeIngress(
            SnapshotBundle(
                backlogItems = listOf(backlog(SystemContexts.INBOX.raw)),
                inbox = listOf(inbox(SystemContexts.INBOX.raw)),
            ),
        )
    }

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun h1() =
        HierarchyPlacementSnapshot(
            id = "p",
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = "w",
            parentPlacementId = null,
            placementKind = "PRIMARY",
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun backlog(ownerId: String) =
        BacklogItemSnapshot("backlog-$ownerId", ownerId, "NOTE", "note", 0L, 1L, 1L, false)

    private fun inbox(ownerId: String) =
        InboxRecordSnapshot("inbox-$ownerId", ownerId, "text", 1L, 0L, 1L, false, 1L, false)
}
