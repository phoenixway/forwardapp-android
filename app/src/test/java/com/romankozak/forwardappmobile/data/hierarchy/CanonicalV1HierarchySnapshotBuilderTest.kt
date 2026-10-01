package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV1HierarchySnapshotBuilderTest {
    private val builder = CanonicalV1HierarchySnapshotBuilder()

    @Test
    fun `canonical workspace plus additional parent preserves primary and link appearances`() {
        val snapshot =
            builder.build(
                CanonicalHierarchyEstablishmentInput(
                    workspaces =
                        listOf(
                            workspace("root-a", null, 0),
                            workspace("root-b", null, 1),
                            workspace("shared", "root-a", 0),
                        ),
                    beacons = emptyList(),
                    additionalWorkspaceRoutes =
                        listOf(
                            CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute(
                                parentWorkspaceId = "root-b",
                                childWorkspaceId = "shared",
                                order = 0,
                            ),
                        ),
                ),
            )

        val shared = snapshot.occurrences.filter { it.target.id == "shared" }

        assertEquals(2, shared.size)
        assertEquals(1, shared.count { it.placementKind == PlacementKind.PRIMARY })
        assertEquals(1, shared.count { it.placementKind == PlacementKind.LINK })
        assertTrue(snapshot.diagnostics.isEmpty())
    }

    @Test
    fun `same canonical beacon route projected through two groups gets zero primary with diagnostics`() {
        val rootTarget = subject("root-subject")
        val childTarget = subject("child-subject")

        val snapshot =
            builder.build(
                CanonicalHierarchyEstablishmentInput(
                    workspaces = emptyList(),
                    groups =
                        listOf(
                            CanonicalHierarchyEstablishmentGroupInput("g1", "A", 0, "group-subject-g1"),
                            CanonicalHierarchyEstablishmentGroupInput("g2", "B", 1, "group-subject-g2"),
                        ),
                    beacons =
                        listOf(
                            beacon(
                                id = "root",
                                target = rootTarget,
                                parentId = null,
                                groups = listOf("g1", "g2"),
                            ),
                            beacon(
                                id = "child",
                                target = childTarget,
                                parentId = "root",
                            ),
                        ),
                ),
            )

        val rootOccurrences = snapshot.occurrences.filter { it.target == rootTarget }
        val childOccurrences = snapshot.occurrences.filter { it.target == childTarget }

        assertEquals(2, rootOccurrences.size)
        assertEquals(
            setOf("group-subject-g1", "group-subject-g2"),
            rootOccurrences.map {
                requireNotNull(it.rootGroupScope).groupSubjectId
            }.toSet(),
        )
        assertTrue(
            rootOccurrences.all {
                it.rootGroupScope?.kind == CanonicalV1RootGroupScopeKind.GROUP
            },
        )
        // CURRENT V1 renders this child twice below the grouped parent and
        // once more as a NoGroup root because the child itself has no group.
        assertEquals(3, childOccurrences.size)
        assertTrue(rootOccurrences.all { it.placementKind == PlacementKind.LINK })
        assertTrue(childOccurrences.all { it.placementKind == PlacementKind.LINK })
        assertEquals(
            setOf(rootTarget, childTarget),
            snapshot.diagnostics.map { it.target }.toSet(),
        )
        assertTrue(
            snapshot.diagnostics.all {
                it.code == CanonicalV1HierarchyDiagnosticCode.AMBIGUOUS_PRIMARY_EVIDENCE
            },
        )
    }

    @Test
    fun `operational owner projection and its descendants do not invent primary evidence`() {
        val snapshot =
            builder.build(
                CanonicalHierarchyEstablishmentInput(
                    workspaces =
                        listOf(
                            workspace("owner", null, 0),
                            workspace("child", "owner", 0),
                        ),
                    beacons =
                        listOf(
                            beacon(
                                id = "beacon",
                                target = subject("beacon-subject"),
                                parentId = null,
                                owners = listOf("owner"),
                            ),
                        ),
                ),
            )

        val owner = snapshot.occurrences.single { it.target.id == "owner" }
        val child = snapshot.occurrences.single { it.target.id == "child" }

        assertEquals(PlacementKind.LINK, owner.placementKind)
        assertEquals(CanonicalV1PrimaryEvidence.NONE, owner.primaryEvidence)
        assertEquals(PlacementKind.LINK, child.placementKind)
        assertEquals(CanonicalV1PrimaryEvidence.NONE, child.primaryEvidence)
        assertEquals(owner.occurrenceKey, child.parentOccurrenceKey)
    }

    @Test
    fun `synthetic group and no-beacon scopes never become targets`() {
        val snapshot =
            builder.build(
                CanonicalHierarchyEstablishmentInput(
                    workspaces = listOf(workspace("workspace", null, 0)),
                    groups =
                        listOf(
                            CanonicalHierarchyEstablishmentGroupInput(
                                sourceId = "group",
                                title = "Group",
                                order = 0,
                                canonicalSubjectId = "group-subject",
                            ),
                        ),
                    beacons =
                        listOf(
                            beacon(
                                id = "beacon",
                                target = subject("subject"),
                                parentId = null,
                                groups = listOf("group"),
                            ),
                        ),
                ),
            )

        assertEquals(
            setOf("subject", "workspace"),
            snapshot.occurrences.map { it.target.id }.toSet(),
        )
        assertFalse(snapshot.occurrences.any { it.target.id == "group" })
        val subjectRoot =
            snapshot.occurrences.single {
                it.target.id == "subject" && it.parentOccurrenceKey == null
            }
        assertEquals(
            CanonicalV1RootGroupScopeKind.GROUP,
            subjectRoot.rootGroupScope?.kind,
        )
        assertEquals(
            "group-subject",
            subjectRoot.rootGroupScope?.groupSubjectId,
        )
    }

    @Test
    fun `equal current workspace sort keys preserve captured source ordinal before id`() {
        val snapshot =
            builder.build(
                CanonicalHierarchyEstablishmentInput(
                    workspaces =
                        listOf(
                            CanonicalHierarchyEstablishmentWorkspaceInput(
                                id = "a",
                                name = "same",
                                canonicalParentId = null,
                                order = 0,
                                sourceOrdinal = 1,
                            ),
                            CanonicalHierarchyEstablishmentWorkspaceInput(
                                id = "z",
                                name = "same",
                                canonicalParentId = null,
                                order = 0,
                                sourceOrdinal = 0,
                            ),
                        ),
                    beacons = emptyList(),
                ),
            )

        assertEquals(
            listOf("z", "a"),
            snapshot.occurrences
                .filter { it.parentOccurrenceKey == null }
                .map { it.target.id },
        )
        assertEquals(
            listOf(0L, 1L),
            snapshot.occurrences
                .filter { it.parentOccurrenceKey == null }
                .map { it.siblingOrder },
        )
    }

    @Test
    fun `source neutral establishment evidence preserves frozen deterministic identity and provenance`() {
        val input =
            CanonicalHierarchyEstablishmentInput(
                workspaces =
                    listOf(
                        workspace("owner", null, 0),
                        workspace("owner-child", "owner", 0),
                        workspace("extra-root", null, 1),
                        workspace("shared", "extra-root", 0),
                    ),
                groups =
                    listOf(
                        CanonicalHierarchyEstablishmentGroupInput(
                            sourceId = "group",
                            title = "Core",
                            order = 0,
                            canonicalSubjectId = "group-subject",
                        ),
                    ),
                beacons =
                    listOf(
                        beacon(
                            id = "b1",
                            target = subject("subject-b1"),
                            parentId = null,
                            owners = listOf("owner"),
                            groups = listOf("group"),
                        ),
                        beacon(
                            id = "b2",
                            target = subject("subject-b2"),
                            parentId = null,
                        ),
                        beacon(
                            id = "b-child",
                            target = subject("subject-child"),
                            parentId = "b1",
                        ),
                    ),
                additionalWorkspaceRoutes =
                    listOf(
                        CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute(
                            parentWorkspaceId = "owner",
                            childWorkspaceId = "shared",
                            order = 0,
                        ),
                    ),
                additionalBeaconRoutes =
                    listOf(
                        CanonicalHierarchyEstablishmentAdditionalBeaconRoute(
                            parentSourceId = "b2",
                            childSourceId = "b-child",
                            order = 0,
                        ),
                    ),
            )

        val snapshot = builder.build(input)
        val placements = snapshot.toDeterministicHierarchyPlacements(now = 100L)
        val groupScopes = snapshot.toDeterministicHierarchyPlacementGroupScopes(now = 100L)
        val linkedAppearances =
            snapshot.toDeterministicHierarchyPlacementLinkedAppearances(now = 100L)

        val byOccurrenceKey = snapshot.occurrences.associateBy { it.occurrenceKey }
        val placementsById = placements.associateBy { it.id.value }

        val groupedBeaconKey = "group:5:group/beacon:2:b1"
        val canonicalChildKey = "$groupedBeaconKey/beacon-parent:7:b-child"
        val linkedChildKey = "scope:no-group/beacon:2:b2/beacon-parent-link:7:b-child"
        val ownerKey = "$groupedBeaconKey/beacon-owner:5:owner"
        val ownerChildKey = "$ownerKey/workspace-parent:11:owner-child"

        assertEquals(
            setOf(
                groupedBeaconKey,
                canonicalChildKey,
                linkedChildKey,
                ownerKey,
                ownerChildKey,
            ),
            setOf(
                groupedBeaconKey,
                canonicalChildKey,
                linkedChildKey,
                ownerKey,
                ownerChildKey,
            ).filterTo(linkedSetOf()) { it in byOccurrenceKey },
        )

        assertEquals(
            PlacementKind.PRIMARY,
            byOccurrenceKey.getValue(canonicalChildKey).placementKind,
        )
        assertEquals(
            PlacementKind.LINK,
            byOccurrenceKey.getValue(linkedChildKey).placementKind,
        )
        assertEquals(
            CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT_LINK,
            byOccurrenceKey.getValue(linkedChildKey).sourceAuthority,
        )
        assertEquals(
            CanonicalV1HierarchySourceAuthority.BEACON_OPERATIONAL_OWNER_PROJECTION,
            byOccurrenceKey.getValue(ownerKey).sourceAuthority,
        )
        assertEquals(
            CanonicalV1RootGroupScope.group("group-subject"),
            byOccurrenceKey.getValue(groupedBeaconKey).rootGroupScope,
        )

        snapshot.occurrences.forEach { occurrence ->
            val expectedId =
                CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                    hierarchyId = snapshot.hierarchyId.value,
                    occurrenceKey = occurrence.occurrenceKey,
                ).value
            val placement = placementsById.getValue(expectedId)

            assertEquals(occurrence.target, placement.target)
            assertEquals(occurrence.placementKind, placement.placementKind)
            assertEquals(occurrence.siblingOrder, placement.siblingOrder)

            val expectedParentId =
                occurrence.parentOccurrenceKey?.let { parentKey ->
                    CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                        hierarchyId = snapshot.hierarchyId.value,
                        occurrenceKey = parentKey,
                    )
                }
            assertEquals(expectedParentId, placement.parentPlacementId)
        }

        val groupedBeaconPlacementId =
            CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                hierarchyId = snapshot.hierarchyId.value,
                occurrenceKey = groupedBeaconKey,
            ).value
        assertEquals(
            "2207eda1-f439-59b4-bbe3-082ac84dafda",
            groupedBeaconPlacementId,
        )
        assertEquals(
            "group-subject",
            groupScopes.single { it.placementId == groupedBeaconPlacementId }.groupSubjectId,
        )

        val ownerPlacementId =
            CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                hierarchyId = snapshot.hierarchyId.value,
                occurrenceKey = ownerKey,
            ).value
        assertTrue(linkedAppearances.any { it.placementId == ownerPlacementId })

        val ownerChildPlacementId =
            CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                hierarchyId = snapshot.hierarchyId.value,
                occurrenceKey = ownerChildKey,
            ).value
        assertFalse(linkedAppearances.any { it.placementId == ownerChildPlacementId })
    }

    @Test
    fun `input list order does not change deterministic snapshot`() {
        val base =
            CanonicalHierarchyEstablishmentInput(
                workspaces =
                    listOf(
                        workspace("b", null, 1),
                        workspace("a", null, 0),
                        workspace("c", "a", 0),
                    ),
                beacons = emptyList(),
                additionalWorkspaceRoutes =
                    listOf(
                        CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute("b", "c", 0),
                    ),
            )

        val first = builder.build(base)
        val second =
            builder.build(
                base.copy(
                    workspaces = base.workspaces.reversed(),
                    additionalWorkspaceRoutes = base.additionalWorkspaceRoutes.reversed(),
                ),
            )

        assertEquals(first, second)
        assertEquals(
            listOf(0L, 1L),
            first.occurrences
                .filter { it.parentOccurrenceKey == null }
                .map { it.siblingOrder },
        )
    }

    private fun workspace(
        id: String,
        parentId: String?,
        order: Long,
    ) = CanonicalHierarchyEstablishmentWorkspaceInput(
        id = id,
        name = id,
        canonicalParentId = parentId,
        order = order,
    )

    private fun beacon(
        id: String,
        target: HierarchyTargetRef,
        parentId: String?,
        owners: List<String> = emptyList(),
        groups: List<String> = emptyList(),
    ) = CanonicalHierarchyEstablishmentBeaconInput(
        sourceId = id,
        target = target,
        title = id,
        order = 0,
        canonicalParentSourceId = parentId,
        operationalOwnerWorkspaceIds = owners,
        groupIds = groups,
        groupOrders = groups.associateWith { 0L },
    )

    private fun subject(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)
}
