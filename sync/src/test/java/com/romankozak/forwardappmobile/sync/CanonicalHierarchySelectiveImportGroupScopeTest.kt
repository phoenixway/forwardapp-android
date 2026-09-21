package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconContextCrossRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconGroupSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalHierarchySelectiveImportGroupScopeTest {
    private val filter = SnapshotBundleSelectiveImportFilter()

    @Test
    fun `selected Beacon root retains exact GroupScope and canonical PART_OF provenance`() {
        val source =
            beaconSource(
                placements = listOf(beaconPlacement("beacon-root")),
                scopes = listOf(scope("beacon-root", "group-subject")),
                grouped = true,
            )

        val filtered = filter.filter(source, WorkspaceSelectiveImportSelection())

        val placement = requireNotNull(filtered.hierarchyPlacements).single()
        assertEquals("beacon-root", placement.id)
        assertEquals("beacon-subject", placement.targetId)
        assertNull(placement.parentPlacementId)

        val selectedScope = requireNotNull(filtered.hierarchyPlacementGroupScopes).single()
        assertEquals("beacon-root", selectedScope.placementId)
        assertEquals("group-subject", selectedScope.groupSubjectId)

        assertEquals(
            setOf("beacon-subject", "group-subject"),
            filtered.managedSubjects.orEmpty().mapTo(linkedSetOf()) { it.id },
        )
        assertEquals(
            listOf("PART_OF"),
            filtered.orientationRelations.orEmpty().map { it.relationType },
        )
        assertEquals(
            setOf("mapping-beacon", "mapping-group"),
            filtered.legacySubjectMappings.orEmpty().mapTo(linkedSetOf()) { it.id },
        )
    }

    @Test
    fun `NoGroup is explicit and PART_OF cannot be inferred or silently ignored`() {
        val noGroup =
            beaconSource(
                placements = listOf(beaconPlacement("beacon-root")),
                scopes = listOf(scope("beacon-root", null)),
                grouped = false,
            )

        val filtered = filter.filter(noGroup, WorkspaceSelectiveImportSelection())
        assertNotNull(filtered.hierarchyPlacementGroupScopes)
        assertEquals(null, filtered.hierarchyPlacementGroupScopes!!.single().groupSubjectId)
        assertTrue(filtered.orientationRelations.orEmpty().isEmpty())

        val inconsistent =
            noGroup.copy(
                mainBeaconGroups = listOf(group("group")),
                managedSubjects =
                    noGroup.managedSubjects.orEmpty() +
                        subject("group-subject"),
                orientations =
                    noGroup.orientations.orEmpty() +
                        orientation("group-subject", OrientationKind.MAIN_BEACON_GROUP.name),
                legacySubjectMappings =
                    noGroup.legacySubjectMappings.orEmpty() +
                        groupMapping(),
                orientationRelations =
                    listOf(partOf("beacon-subject", "group-subject")),
            )

        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(inconsistent, WorkspaceSelectiveImportSelection())
        }
    }

    @Test
    fun `scope for unselected non-Beacon MANAGED_SUBJECT occurrence is removed rather than inherited`() {
        val source =
            canonicalSource(
                subjects = listOf(subject("other-subject")),
                orientations =
                    listOf(
                        orientation("other-subject", OrientationKind.GOAL.name),
                    ),
                placements =
                    listOf(
                        beaconPlacement(
                            id = "other-root",
                            targetId = "other-subject",
                        ),
                    ),
                scopes = listOf(scope("other-root", null)),
            )

        val filtered = filter.filter(source, WorkspaceSelectiveImportSelection())

        assertNotNull(filtered.hierarchyPlacements)
        assertTrue(filtered.hierarchyPlacements.orEmpty().isEmpty())
        assertNotNull(filtered.hierarchyPlacementGroupScopes)
        assertTrue(filtered.hierarchyPlacementGroupScopes.orEmpty().isEmpty())
        assertTrue(filtered.managedSubjects.orEmpty().isEmpty())
    }

    @Test
    fun `missing required root scope and malformed canonical Group dependency fail closed`() {
        val missingScope =
            beaconSource(
                placements = listOf(beaconPlacement("beacon-root")),
                scopes = emptyList(),
                grouped = false,
            )

        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(missingScope, WorkspaceSelectiveImportSelection())
        }

        val malformedGroup =
            beaconSource(
                placements = listOf(beaconPlacement("beacon-root")),
                scopes = listOf(scope("beacon-root", "group-subject")),
                grouped = true,
            ).copy(
                orientations =
                    listOf(
                        orientation("beacon-subject", OrientationKind.MAIN_BEACON.name),
                        orientation("group-subject", OrientationKind.GOAL.name),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(malformedGroup, WorkspaceSelectiveImportSelection())
        }
    }

    @Test
    fun `PART_OF MainBeacon parent and operational owner never become H1 parents`() {
        val source =
            beaconSource(
                placements =
                    listOf(
                        beaconPlacement("parent-beacon-root", "parent-subject"),
                        beaconPlacement("child-beacon-root", "beacon-subject"),
                        workspacePlacement("workspace-root", "workspace"),
                    ),
                scopes =
                    listOf(
                        scope("parent-beacon-root", null),
                        scope("child-beacon-root", "group-subject"),
                    ),
                grouped = true,
                beacons =
                    listOf(
                        beacon("parent-beacon", subjectParentId = null),
                        beacon("beacon", subjectParentId = "parent-beacon"),
                    ),
                extraSubjects =
                    listOf(subject("parent-subject")),
                extraOrientations =
                    listOf(
                        orientation(
                            "parent-subject",
                            OrientationKind.MAIN_BEACON.name,
                        ),
                    ),
                extraMappings =
                    listOf(
                        beaconMapping(
                            beaconId = "parent-beacon",
                            subjectId = "parent-subject",
                            id = "mapping-parent-beacon",
                        ),
                    ),
                workspaces = listOf(workspace("workspace")),
            ).copy(
                mainBeaconContextCrossRefs =
                    listOf(
                        MainBeaconContextCrossRefSnapshot(
                            beaconId = "beacon",
                            contextId = "workspace",
                            order = 0L,
                        ),
                    ),
            )

        val filtered =
            filter.filter(
                source,
                WorkspaceSelectiveImportSelection(
                    selectedContextIds = setOf("workspace"),
                ),
            )

        val byId = filtered.hierarchyPlacements.orEmpty().associateBy { it.id }
        assertNull(byId.getValue("child-beacon-root").parentPlacementId)
        assertNull(byId.getValue("parent-beacon-root").parentPlacementId)
        assertNull(byId.getValue("workspace-root").parentPlacementId)

        // Semantic Group membership remains semantic only.
        assertEquals(
            "group-subject",
            filtered.orientationRelations.orEmpty()
                .single { it.fromOrientationId == "beacon-subject" }
                .toOrientationId,
        )
    }

    private fun beaconSource(
        placements: List<HierarchyPlacementSnapshot>,
        scopes: List<HierarchyPlacementGroupScopeSnapshot>,
        grouped: Boolean,
        beacons: List<MainBeaconSnapshot> = listOf(beacon("beacon")),
        extraSubjects: List<ManagedSubjectEntity> = emptyList(),
        extraOrientations: List<OrientationEntity> = emptyList(),
        extraMappings: List<LegacySubjectMappingEntity> = emptyList(),
        workspaces: List<WorkspaceEntity> = emptyList(),
    ): SnapshotBundle {
        val groupSubjects =
            if (grouped) listOf(subject("group-subject")) else emptyList()
        val groupOrientations =
            if (grouped) {
                listOf(
                    orientation(
                        "group-subject",
                        OrientationKind.MAIN_BEACON_GROUP.name,
                    ),
                )
            } else {
                emptyList()
            }
        val groupMappings =
            if (grouped) listOf(groupMapping()) else emptyList()
        val groups =
            if (grouped) listOf(group("group")) else emptyList()
        val relations =
            if (grouped) {
                listOf(partOf("beacon-subject", "group-subject"))
            } else {
                emptyList()
            }

        return canonicalSource(
            beacons = beacons,
            groups = groups,
            subjects =
                listOf(subject("beacon-subject")) +
                    extraSubjects +
                    groupSubjects,
            orientations =
                listOf(
                    orientation(
                        "beacon-subject",
                        OrientationKind.MAIN_BEACON.name,
                    ),
                ) + extraOrientations + groupOrientations,
            mappings =
                listOf(beaconMapping()) +
                    extraMappings +
                    groupMappings,
            relations = relations,
            workspaces = workspaces,
            placements = placements,
            scopes = scopes,
        )
    }

    private fun canonicalSource(
        beacons: List<MainBeaconSnapshot> = emptyList(),
        groups: List<MainBeaconGroupSnapshot> = emptyList(),
        subjects: List<ManagedSubjectEntity> = emptyList(),
        orientations: List<OrientationEntity> = emptyList(),
        mappings: List<LegacySubjectMappingEntity> = emptyList(),
        relations: List<OrientationRelationEntity> = emptyList(),
        workspaces: List<WorkspaceEntity> = emptyList(),
        placements: List<HierarchyPlacementSnapshot>,
        scopes: List<HierarchyPlacementGroupScopeSnapshot>,
    ) =
        SnapshotBundle(
            version = 2,
            mainBeacons = beacons,
            mainBeaconGroups = groups,
            managedSubjects = subjects,
            orientations = orientations,
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = mappings,
            orientationRelations = relations,
            aspectOrientationRefs = emptyList(),
            workspaces = workspaces,
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
            hierarchyPlacements = placements,
            hierarchyPlacementGroupScopes = scopes,
            hierarchyPlacementLinkedAppearances = emptyList(),
        )

    private fun beacon(
        id: String,
        subjectParentId: String? = null,
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
            parentBeaconId = subjectParentId,
            order = 0L,
            isExpanded = true,
            updatedAt = 2L,
            createdAt = 1L,
        )

    private fun group(id: String) =
        MainBeaconGroupSnapshot(
            id = id,
            title = id,
            description = null,
            order = 0L,
            updatedAt = 2L,
            createdAt = 1L,
        )

    private fun subject(id: String) =
        ManagedSubjectEntity(
            id = id,
            subjectType = ManagedSubjectType.ORIENTATION.name,
            title = id,
            description = null,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun orientation(
        subjectId: String,
        kind: String,
    ) =
        OrientationEntity(
            subjectId = subjectId,
            kind = kind,
            lifecycle = "ACTIVE",
            lifecycleOrigin = "EXPLICIT",
        )

    private fun beaconMapping(
        beaconId: String = "beacon",
        subjectId: String = "beacon-subject",
        id: String = "mapping-beacon",
    ) =
        LegacySubjectMappingEntity(
            id = id,
            sourceType = LegacyOrientationSourceType.MAIN_BEACON.name,
            sourceId = beaconId,
            subjectId = subjectId,
            migrationVersion = 1,
            state = LegacySubjectMappingState.CUT_OVER.name,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun groupMapping() =
        LegacySubjectMappingEntity(
            id = "mapping-group",
            sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
            sourceId = "group",
            subjectId = "group-subject",
            migrationVersion = 1,
            state = LegacySubjectMappingState.CUT_OVER.name,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun partOf(
        from: String,
        to: String,
    ) =
        OrientationRelationEntity(
            id = "part-of-$from-$to",
            fromOrientationId = from,
            toOrientationId = to,
            relationType = OrientationRelationType.PART_OF.name,
            relationOrder = null,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun beaconPlacement(
        id: String,
        targetId: String = "beacon-subject",
    ) =
        HierarchyPlacementSnapshot(
            id = id,
            hierarchyId = "GENERAL",
            targetType = "MANAGED_SUBJECT",
            targetId = targetId,
            parentPlacementId = null,
            placementKind = "PRIMARY",
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun workspacePlacement(
        id: String,
        targetId: String,
    ) =
        HierarchyPlacementSnapshot(
            id = id,
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = targetId,
            parentPlacementId = null,
            placementKind = "PRIMARY",
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun scope(
        placementId: String,
        groupSubjectId: String?,
    ) =
        HierarchyPlacementGroupScopeSnapshot(
            placementId = placementId,
            hierarchyId = "GENERAL",
            groupSubjectId = groupSubjectId,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )
}
