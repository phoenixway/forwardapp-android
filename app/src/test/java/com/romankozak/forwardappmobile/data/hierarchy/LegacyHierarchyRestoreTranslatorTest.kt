package com.romankozak.forwardappmobile.data.hierarchy

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupMemberSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconParentLinkSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyHierarchyRestoreTranslatorTest {
    private val subject = LegacyHierarchyRestoreTranslator()

    @Test
    fun `V2 translation reads topology only from source evidence`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("root", order = 3L),
                        workspace("child", parentId = "root", order = 7L),
                    ),
            )
        val neutralCanonical =
            source.copy(
                workspaces =
                    requireNotNull(source.workspaces).map {
                        it.copy(parentWorkspaceId = null, workspaceOrder = 0L)
                    },
            )

        val result =
            subject.translate(
                source = source,
                canonical = neutralCanonical,
            )

        val placements = requireNotNull(result.hierarchyPlacements)
        val root = placements.single { it.targetId == "root" }
        val child = placements.single { it.targetId == "child" }
        assertEquals(root.id, child.parentPlacementId)
        assertTrue(
            requireNotNull(result.workspaces).all {
                it.parentWorkspaceId == null && it.workspaceOrder == 0L
            },
        )
    }

    @Test
    fun `V2 complete native hierarchy bypasses translator including authoritative empty`() {
        val native =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces = listOf(workspace("root")),
                mainBeacons =
                    listOf(
                        beacon(
                            id = "legacy-beacon-child",
                            parentId = "legacy-beacon-parent",
                            order = 17L,
                        ),
                    ),
                hierarchyPlacements = listOf(h1("native", "root")),
                hierarchyPlacementGroupScopes = emptyList(),
                hierarchyPlacementLinkedAppearances = emptyList(),
                contextParentLinks =
                    listOf(
                        ContextParentLinkSnapshot(
                            parentContextId = "legacy-parent",
                            childContextId = "legacy-child",
                            order = 0L,
                            createdAt = 1L,
                            updatedAt = 1L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                mainBeaconParentLinks =
                    listOf(
                        MainBeaconParentLinkSnapshot(
                            parentBeaconId = "legacy-beacon-parent",
                            childBeaconId = "legacy-beacon-child",
                            order = 0L,
                            createdAt = 1L,
                            updatedAt = 1L,
                        ),
                    ),
            )
        val nativeResult =
            subject.translate(
                source = native,
                canonical = native,
            )
        assertEquals(native.hierarchyPlacements, nativeResult.hierarchyPlacements)
        assertTrue(requireNotNull(nativeResult.hierarchyPlacementGroupScopes).isEmpty())
        assertTrue(requireNotNull(nativeResult.hierarchyPlacementLinkedAppearances).isEmpty())
        assertTrue(nativeResult.contextParentLinks.isEmpty())
        assertTrue(nativeResult.mainBeaconParentLinks.isEmpty())
        val restoredBeacon = nativeResult.mainBeacons.single()
        assertNull(restoredBeacon.parentBeaconId)
        assertEquals(17L, restoredBeacon.order)
        val restoredWorkspace = requireNotNull(nativeResult.workspaces).single()
        assertNull(restoredWorkspace.parentWorkspaceId)
        assertEquals(0L, restoredWorkspace.workspaceOrder)

        val explicitEmpty =
            native.copy(
                hierarchyPlacements = emptyList(),
                hierarchyPlacementLinkedAppearances = null,
            )
        val emptyResult =
            subject.translate(
                source = explicitEmpty,
                canonical = explicitEmpty,
            )
        assertNotNull(emptyResult.hierarchyPlacements)
        assertTrue(requireNotNull(emptyResult.hierarchyPlacements).isEmpty())
        assertNotNull(emptyResult.hierarchyPlacementGroupScopes)
        assertTrue(requireNotNull(emptyResult.hierarchyPlacementGroupScopes).isEmpty())
        assertNotNull(emptyResult.hierarchyPlacementLinkedAppearances)
        assertTrue(requireNotNull(emptyResult.hierarchyPlacementLinkedAppearances).isEmpty())
    }

    @Test
    fun `V2 pre-v177 native H1 recovers linked provenance only after exact deterministic parity`() {
        val legacy =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("root-a", order = 0L),
                        workspace("root-b", order = 1L),
                        workspace("shared", parentId = "root-a", order = 0L),
                    ),
                contextParentLinks =
                    listOf(
                        ContextParentLinkSnapshot(
                            parentContextId = "root-b",
                            childContextId = "shared",
                            order = 0L,
                            createdAt = 1L,
                            updatedAt = 1L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
            )

        val translated =
            subject.translate(
                source = legacy,
                canonical = legacy,
            )
        val expectedH1 = requireNotNull(translated.hierarchyPlacements)
        val expectedLinked =
            requireNotNull(translated.hierarchyPlacementLinkedAppearances)
        requireNotNull(translated.workspaces).forEach { workspace ->
            assertNull(workspace.parentWorkspaceId)
            assertEquals(0L, workspace.workspaceOrder)
        }

        val preV177 =
            legacy.copy(
                hierarchyPlacements = expectedH1,
                hierarchyPlacementGroupScopes =
                    requireNotNull(translated.hierarchyPlacementGroupScopes),
                hierarchyPlacementLinkedAppearances = null,
            )

        val recovered =
            subject.translate(
                source = preV177,
                canonical = preV177,
            )

        assertEquals(expectedH1, recovered.hierarchyPlacements)
        assertEquals(expectedLinked, recovered.hierarchyPlacementLinkedAppearances)
        assertEquals(1, expectedLinked.size)

        val linkedPlacementId = expectedLinked.single().placementId
        val linkedPlacement = expectedH1.single { it.id == linkedPlacementId }
        assertEquals("shared", linkedPlacement.targetId)
        assertEquals("LINK", linkedPlacement.placementKind)
    }

    @Test
    fun `V2 pre-v177 native H1 missing linked provenance fails when legacy structure diverges`() {
        val legacy =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("root"),
                        workspace("child", parentId = "root"),
                    ),
            )
        val translated =
            subject.translate(
                source = legacy,
                canonical = legacy,
            )
        val native =
            requireNotNull(translated.hierarchyPlacements)
                .map { placement ->
                    if (placement.targetId == "child") {
                        placement.copy(siblingOrder = placement.siblingOrder + 99L)
                    } else {
                        placement
                    }
                }
        val preV177 =
            legacy.copy(
                hierarchyPlacements = native,
                hierarchyPlacementGroupScopes =
                    requireNotNull(translated.hierarchyPlacementGroupScopes),
                hierarchyPlacementLinkedAppearances = null,
            )

        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                subject.translate(
                    source = preV177,
                    canonical = preV177,
                )
            }

        assertTrue(
            failure.message?.contains("does not exactly match deterministic legacy V1 structure") == true,
        )
    }

    @Test
    fun `V2 native ManagedSubject root without GroupScope provenance fails closed`() {
        val native =
            SnapshotBundle(
                exportedAt = 100L,
                hierarchyPlacements =
                    listOf(
                        h1("native-subject-root", "subject").copy(
                            targetType = "MANAGED_SUBJECT",
                        ),
                    ),
            )

        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                subject.translate(
                    source = native,
                    canonical = native,
                )
            }

        assertTrue(
            failure.message?.contains("requires explicit Hierarchy GroupScope provenance") == true,
        )
    }

    @Test
    fun `V2 restore with no hierarchy evidence canonicalizes absent H1 to explicit empty`() {
        val source = SnapshotBundle(exportedAt = 100L)

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        assertNotNull(result.hierarchyPlacements)
        assertTrue(requireNotNull(result.hierarchyPlacements).isEmpty())
        assertNotNull(result.hierarchyPlacementGroupScopes)
        assertTrue(requireNotNull(result.hierarchyPlacementGroupScopes).isEmpty())
        assertNotNull(result.hierarchyPlacementLinkedAppearances)
        assertTrue(requireNotNull(result.hierarchyPlacementLinkedAppearances).isEmpty())
    }

    @Test
    fun `historical Workspace JSON keeps embedded topology for Restore translation`() {
        val json =
            """
            {
              "exportedAt": 100,
              "workspaces": [
                {
                  "id": "root",
                  "nameOverride": "root",
                  "descriptionOverride": null,
                  "parentWorkspaceId": null,
                  "roleCode": null,
                  "workspaceOrder": 3,
                  "createdAt": 1,
                  "updatedAt": 1,
                  "syncedAt": null,
                  "isDeleted": false,
                  "version": 1,
                  "provenance": "CANONICAL_ONLY",
                  "sourceContextId": null
                },
                {
                  "id": "child",
                  "nameOverride": "child",
                  "descriptionOverride": null,
                  "parentWorkspaceId": "root",
                  "roleCode": null,
                  "workspaceOrder": 17,
                  "createdAt": 1,
                  "updatedAt": 1,
                  "syncedAt": null,
                  "isDeleted": false,
                  "version": 1,
                  "provenance": "CANONICAL_ONLY",
                  "sourceContextId": null
                }
              ]
            }
            """.trimIndent()

        val decoded = Gson().fromJson(json, SnapshotBundle::class.java)
        val decodedChild = requireNotNull(decoded.workspaces).single { it.id == "child" }

        assertEquals("root", decodedChild.parentWorkspaceId)
        assertEquals(17L, decodedChild.workspaceOrder)

        val canonical =
            decoded.copy(
                workspaces =
                    requireNotNull(decoded.workspaces).map {
                        it.copy(parentWorkspaceId = null, workspaceOrder = 0L)
                    },
            )

        val translated =
            subject.translate(
                source = decoded,
                canonical = canonical,
            )

        val placements = requireNotNull(translated.hierarchyPlacements)
        val rootPlacement = placements.single { it.targetId == "root" }
        val childPlacement = placements.single { it.targetId == "child" }

        assertEquals(rootPlacement.id, childPlacement.parentPlacementId)
        assertTrue(
            requireNotNull(translated.workspaces).all {
                it.parentWorkspaceId == null && it.workspaceOrder == 0L
            },
        )
    }

    @Test
    fun `V2 Workspace restore translation is deterministic and preserves parent occurrence`() {
        val firstSource =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("child", parentId = "root", order = 7L),
                        workspace("root", order = 3L),
                    ),
            )
        val secondSource =
            firstSource.copy(
                workspaces = requireNotNull(firstSource.workspaces).reversed(),
            )

        val first =
            subject.translate(
                source = firstSource,
                canonical = firstSource,
            )
        val second =
            subject.translate(
                source = secondSource,
                canonical = secondSource,
            )

        assertEquals(first.hierarchyPlacements, second.hierarchyPlacements)
        assertEquals(
            first.hierarchyPlacementGroupScopes,
            second.hierarchyPlacementGroupScopes,
        )
        assertEquals(
            first.hierarchyPlacementLinkedAppearances,
            second.hierarchyPlacementLinkedAppearances,
        )
        assertTrue(requireNotNull(first.hierarchyPlacementGroupScopes).isEmpty())
        assertTrue(requireNotNull(first.hierarchyPlacementLinkedAppearances).isEmpty())

        val placements = requireNotNull(first.hierarchyPlacements)
        assertEquals(2, placements.size)
        val root = placements.single { it.targetId == "root" }
        val child = placements.single { it.targetId == "child" }

        assertEquals("WORKSPACE", root.targetType)
        assertEquals("WORKSPACE", child.targetType)
        assertNull(root.parentPlacementId)
        assertEquals(root.id, child.parentPlacementId)
        assertEquals("PRIMARY", root.placementKind)
        assertEquals("PRIMARY", child.placementKind)
    }

    @Test
    fun `historical Workspace with missing parent is deterministically projected as root`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("child", parentId = "missing-parent", order = 17L),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        val placement = requireNotNull(result.hierarchyPlacements).single()
        assertEquals("child", placement.targetId)
        assertNull(placement.parentPlacementId)
        assertEquals("LINK", placement.placementKind)
        assertEquals(0L, placement.siblingOrder)

        val restoredWorkspace = requireNotNull(result.workspaces).single()
        assertNull(restoredWorkspace.parentWorkspaceId)
        assertEquals(0L, restoredWorkspace.workspaceOrder)
    }

    @Test
    fun `historical cyclic Workspace parent evidence does not invent primary authority`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("a", parentId = "b", order = 0L),
                        workspace("b", parentId = "a", order = 0L),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        assertTrue(requireNotNull(result.hierarchyPlacements).isEmpty())
        assertTrue(requireNotNull(result.hierarchyPlacementGroupScopes).isEmpty())
        assertTrue(requireNotNull(result.hierarchyPlacementLinkedAppearances).isEmpty())

        assertTrue(
            requireNotNull(result.workspaces).all {
                it.parentWorkspaceId == null && it.workspaceOrder == 0L
            },
        )
    }

    @Test
    fun `historical Workspace order anomalies deterministically densify sibling order`() {
        val firstSource =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("z", order = -5L),
                        workspace("a", order = -5L),
                        workspace("m", order = 99L),
                    ),
            )
        val secondSource =
            firstSource.copy(
                workspaces = requireNotNull(firstSource.workspaces).reversed(),
            )

        val first =
            subject.translate(
                source = firstSource,
                canonical = firstSource,
            )
        val second =
            subject.translate(
                source = secondSource,
                canonical = secondSource,
            )

        assertEquals(first.hierarchyPlacements, second.hierarchyPlacements)

        val roots =
            requireNotNull(first.hierarchyPlacements)
                .filter { it.parentPlacementId == null }
                .sortedBy { it.siblingOrder }

        assertEquals(listOf("a", "z", "m"), roots.map { it.targetId })
        assertEquals(listOf(0L, 1L, 2L), roots.map { it.siblingOrder })

        assertTrue(
            requireNotNull(first.workspaces).all {
                it.parentWorkspaceId == null && it.workspaceOrder == 0L
            },
        )
    }

    @Test
    fun `semantic-only Beacon relations do not trigger legacy hierarchy reconstruction`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeaconGroupMembers =
                    listOf(
                        MainBeaconGroupMemberSnapshot(
                            groupId = "group",
                            beaconId = "beacon",
                            order = 0L,
                        ),
                    ),
                mainBeaconContextCrossRefs =
                    listOf(
                        MainBeaconContextCrossRefSnapshot(
                            beaconId = "beacon",
                            contextId = "workspace",
                            order = 0L,
                        ),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        assertNotNull(result.hierarchyPlacements)
        assertTrue(requireNotNull(result.hierarchyPlacements).isEmpty())
    }

    @Test
    fun `V2 legacy Beacon without live CUT_OVER target fails closed`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons = listOf(beacon("beacon")),
            )

        assertThrows(IllegalArgumentException::class.java) {
            subject.translate(
                source = source,
                canonical = source,
            )
        }
    }

    @Test
    fun `V2 legacy Beacon uses canonical ManagedSubject target`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons = listOf(beacon("beacon")),
                managedSubjects = listOf(managedSubject("subject")),
                legacySubjectMappings = listOf(beaconMapping("beacon", "subject")),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        val placement = requireNotNull(result.hierarchyPlacements).single()
        assertEquals("MANAGED_SUBJECT", placement.targetType)
        assertEquals("subject", placement.targetId)
    }

    @Test
    fun `V2 restore consumes embedded Beacon topology into H1 and neutralizes transport rows`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons =
                    listOf(
                        beacon("parent"),
                        beacon("child", parentId = "parent", order = 13L),
                    ),
                managedSubjects =
                    listOf(
                        managedSubject("parent-subject"),
                        managedSubject("child-subject"),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping("parent", "parent-subject"),
                        beaconMapping("child", "child-subject"),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        val placements = requireNotNull(result.hierarchyPlacements)
        val parentPlacement = placements.single { it.targetId == "parent-subject" }
        val childPlacement = placements.single { it.targetId == "child-subject" }
        assertEquals(parentPlacement.id, childPlacement.parentPlacementId)
        assertTrue(result.mainBeacons.all { it.parentBeaconId == null })
        assertEquals(
            mapOf("parent" to 0L, "child" to 13L),
            result.mainBeacons.associate { it.id to it.order },
        )
    }

    @Test
    fun `historical Beacon with missing parent is deterministically projected as root without primary authority`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons = listOf(beacon("child", parentId = "missing-parent", order = 17L)),
                managedSubjects = listOf(managedSubject("child-subject")),
                legacySubjectMappings = listOf(beaconMapping("child", "child-subject")),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        val placement = requireNotNull(result.hierarchyPlacements).single()
        assertEquals("child-subject", placement.targetId)
        assertNull(placement.parentPlacementId)
        assertEquals("LINK", placement.placementKind)
        assertEquals(0L, placement.siblingOrder)
        val restoredBeacon = result.mainBeacons.single()
        assertNull(restoredBeacon.parentBeaconId)
        assertEquals(17L, restoredBeacon.order)
    }

    @Test
    fun `historical cyclic Beacon parent evidence does not invent primary authority`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons =
                    listOf(
                        beacon("a", parentId = "b"),
                        beacon("b", parentId = "a"),
                    ),
                managedSubjects =
                    listOf(
                        managedSubject("a-subject"),
                        managedSubject("b-subject"),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping("a", "a-subject"),
                        beaconMapping("b", "b-subject"),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        assertTrue(requireNotNull(result.hierarchyPlacements).isEmpty())
        assertTrue(requireNotNull(result.hierarchyPlacementGroupScopes).isEmpty())
        assertTrue(requireNotNull(result.hierarchyPlacementLinkedAppearances).isEmpty())
        assertTrue(result.mainBeacons.all { it.parentBeaconId == null && it.order == 0L })
    }

    @Test
    fun `historical Beacon order anomalies deterministically densify sibling order`() {
        val firstSource =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons =
                    listOf(
                        beacon("z", order = -5L),
                        beacon("a", order = -5L),
                        beacon("m", order = 99L),
                    ),
                managedSubjects =
                    listOf(
                        managedSubject("z-subject"),
                        managedSubject("a-subject"),
                        managedSubject("m-subject"),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping("z", "z-subject"),
                        beaconMapping("a", "a-subject"),
                        beaconMapping("m", "m-subject"),
                    ),
            )
        val secondSource = firstSource.copy(mainBeacons = firstSource.mainBeacons.reversed())

        val first =
            subject.translate(
                source = firstSource,
                canonical = firstSource,
            )
        val second =
            subject.translate(
                source = secondSource,
                canonical = secondSource,
            )

        val firstOrder = requireNotNull(first.hierarchyPlacements).map { it.targetId to it.siblingOrder }
        val secondOrder = requireNotNull(second.hierarchyPlacements).map { it.targetId to it.siblingOrder }
        assertEquals(
            listOf("a-subject" to 0L, "z-subject" to 1L, "m-subject" to 2L),
            firstOrder,
        )
        assertEquals(firstOrder, secondOrder)
    }

    @Test
    fun `additional parent preserves duplicate LINK occurrence subtree`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                workspaces =
                    listOf(
                        workspace("root-a", order = 0L),
                        workspace("root-b", order = 1L),
                        workspace("shared", parentId = "root-a", order = 0L),
                        workspace("leaf", parentId = "shared", order = 0L),
                    ),
                contextParentLinks =
                    listOf(
                        ContextParentLinkSnapshot(
                            parentContextId = "root-b",
                            childContextId = "shared",
                            order = 0L,
                            createdAt = 1L,
                            updatedAt = 1L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        val placements = requireNotNull(result.hierarchyPlacements)
        val shared = placements.filter { it.targetId == "shared" }
        val leaf = placements.filter { it.targetId == "leaf" }

        assertEquals(2, shared.size)
        assertEquals(1, shared.count { it.placementKind == "PRIMARY" })
        assertEquals(1, shared.count { it.placementKind == "LINK" })

        assertEquals(2, leaf.size)
        assertEquals(1, leaf.count { it.placementKind == "PRIMARY" })
        assertEquals(1, leaf.count { it.placementKind == "LINK" })

        val linkedShared = shared.single { it.placementKind == "LINK" }
        val linkedLeaf = leaf.single { it.placementKind == "LINK" }
        assertEquals(linkedShared.id, linkedLeaf.parentPlacementId)

        val linkedProvenance =
            requireNotNull(result.hierarchyPlacementLinkedAppearances)
        assertEquals(listOf(linkedShared.id), linkedProvenance.map { it.placementId })
        assertTrue(linkedProvenance.none { it.placementId == linkedLeaf.id })
        assertTrue(result.contextParentLinks.isEmpty())
    }

    @Test
    fun `synthetic Group scope is not persisted as hierarchy target`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons = listOf(beacon("beacon")),
                mainBeaconGroups = listOf(group("group", 0L)),
                mainBeaconGroupMembers =
                    listOf(
                        MainBeaconGroupMemberSnapshot(
                            groupId = "group",
                            beaconId = "beacon",
                            order = 0L,
                        ),
                    ),
                managedSubjects =
                    listOf(
                        managedSubject("subject"),
                        managedSubject("group-subject"),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping("beacon", "subject"),
                        groupMapping("group", "group-subject"),
                    ),
            )

        val result =
            subject.translate(
                source = source,
                canonical = source,
            )

        val placements = requireNotNull(result.hierarchyPlacements)
        assertTrue(placements.isNotEmpty())
        assertTrue(placements.none { it.targetId == "group" })
        assertTrue(placements.all { it.targetType == "MANAGED_SUBJECT" })

        val scopes = requireNotNull(result.hierarchyPlacementGroupScopes)
        assertEquals(1, scopes.size)
        assertEquals(placements.single().id, scopes.single().placementId)
        assertEquals("group-subject", scopes.single().groupSubjectId)
    }

    @Test
    fun `ambiguous PRIMARY evidence from two Group projections fails restore translation`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons = listOf(beacon("beacon")),
                mainBeaconGroups =
                    listOf(
                        group("g1", 0L),
                        group("g2", 1L),
                    ),
                mainBeaconGroupMembers =
                    listOf(
                        MainBeaconGroupMemberSnapshot(
                            groupId = "g1",
                            beaconId = "beacon",
                            order = 0L,
                        ),
                        MainBeaconGroupMemberSnapshot(
                            groupId = "g2",
                            beaconId = "beacon",
                            order = 0L,
                        ),
                    ),
                managedSubjects =
                    listOf(
                        managedSubject("subject"),
                        managedSubject("group-subject-g1"),
                        managedSubject("group-subject-g2"),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping("beacon", "subject"),
                        groupMapping("g1", "group-subject-g1"),
                        groupMapping("g2", "group-subject-g2"),
                    ),
            )

        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                subject.translate(
                    source = source,
                    canonical = source,
                )
            }

        assertTrue(failure.message?.contains("ambiguous PRIMARY") == true)
    }

    @Test
    fun `duplicate canonical legacy mapping source identity fails closed`() {
        val source =
            SnapshotBundle(
                exportedAt = 100L,
                mainBeacons = listOf(beacon("beacon")),
                managedSubjects =
                    listOf(
                        managedSubject("subject-a"),
                        managedSubject("subject-b"),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping("beacon", "subject-a"),
                        beaconMapping("beacon", "subject-b").copy(id = "mapping-beacon-duplicate"),
                    ),
            )

        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                subject.translate(
                    source = source,
                    canonical = source,
                )
            }

        assertTrue(failure.message?.contains("duplicate source identity") == true)
    }

    private fun workspace(
        id: String,
        parentId: String? = null,
        order: Long = 0L,
    ) = WorkspaceSnapshot(
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
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
        sourceContextId = null,
    )

    private fun beacon(
        id: String,
        parentId: String? = null,
        order: Long = 0L,
    ) =
        MainBeaconSnapshot(
            id = id,
            title = id,
            description = null,
            whyItMatters = null,
            successShape = null,
            failureShape = null,
            antiGoal = null,
            decisionImpact = null,
            readinessStatus = "READY",
            blockerText = null,
            nextActionText = null,
            parentBeaconId = parentId,
            order = order,
            isExpanded = true,
            updatedAt = 1L,
            createdAt = 1L,
        )

    private fun group(
        id: String,
        order: Long,
    ) = MainBeaconGroupSnapshot(
        id = id,
        title = id,
        description = null,
        order = order,
        updatedAt = 1L,
        createdAt = 1L,
    )

    private fun managedSubject(id: String) =
        ManagedSubjectEntity(
            id = id,
            subjectType = ManagedSubjectType.ORIENTATION.name,
            title = id,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun beaconMapping(
        beaconId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = "mapping-$beaconId",
        sourceType = LegacyOrientationSourceType.MAIN_BEACON.name,
        sourceId = beaconId,
        subjectId = subjectId,
        migrationVersion = 1,
        state = LegacySubjectMappingState.CUT_OVER.name,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun groupMapping(
        groupId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = "mapping-group-$groupId",
        sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
        sourceId = groupId,
        subjectId = subjectId,
        migrationVersion = 1,
        state = LegacySubjectMappingState.CUT_OVER.name,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun h1(
        id: String,
        targetId: String,
    ) = HierarchyPlacementSnapshot(
        id = id,
        hierarchyId = "GENERAL",
        targetType = "WORKSPACE",
        targetId = targetId,
        parentPlacementId = null,
        placementKind = "PRIMARY",
        siblingOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )
}
