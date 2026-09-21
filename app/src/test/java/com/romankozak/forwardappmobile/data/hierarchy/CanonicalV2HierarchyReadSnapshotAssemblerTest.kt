package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CanonicalV2HierarchyReadSnapshotAssemblerTest {
    private val assembler = CanonicalV2HierarchyReadSnapshotAssembler()

    @Test
    fun `grouped Beacon root uses exact GroupScope and canonical PART_OF`() {
        val read =
            assembler.assemble(
                placements = listOf(placement("beacon-placement", "beacon-subject")),
                admittedWorkspacePresentations = emptyList(),
                managedSubjects =
                    listOf(
                        subject("beacon-subject", "Canonical beacon"),
                        subject("group-subject", "Canonical group"),
                    ),
                legacySubjectMappings =
                    listOf(
                        mapping(
                            "beacon-map",
                            LegacyOrientationSourceType.MAIN_BEACON,
                            "legacy-beacon",
                            "beacon-subject",
                        ),
                        mapping(
                            "group-map",
                            LegacyOrientationSourceType.MAIN_BEACON_GROUP,
                            "legacy-group",
                            "group-subject",
                        ),
                    ),
                relations =
                    listOf(
                        OrientationRelationEntity(
                            id = "membership",
                            fromOrientationId = "beacon-subject",
                            toOrientationId = "group-subject",
                            relationType = OrientationRelationType.PART_OF.name,
                            relationOrder = 3L,
                            createdAt = 1L,
                            updatedAt = 1L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                groupScopes =
                    listOf(
                        HierarchyPlacementGroupScopeEntity(
                            placementId = "beacon-placement",
                            hierarchyId = HierarchyId.GENERAL.value,
                            groupSubjectId = "group-subject",
                            createdAt = 1L,
                            updatedAt = 1L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                legacyGroups =
                    listOf(
                        MainBeaconGroup(
                            id = "legacy-group",
                            title = "Legacy title must not win",
                            order = 7L,
                        ),
                    ),
                presentationProvenance =
                    CanonicalV2HierarchyPresentationProvenance(),
            )

        val group =
            read.presentation.entries[0] as
                CanonicalV2PresentedHierarchyEntry.SyntheticScope
        val beacon =
            read.presentation.entries[1] as
                CanonicalV2PresentedHierarchyEntry.Occurrence

        assertEquals(CanonicalV2SyntheticScopeKind.GROUP, group.kind)
        assertEquals("legacy-group", group.id)
        assertEquals("Canonical group", group.title)
        assertEquals(PlacementId("beacon-placement"), beacon.placementId)
        assertEquals("legacy-beacon", beacon.presentationId)
        assertEquals(1, beacon.level)
    }

    @Test
    fun `missing exact GroupScope provenance fails closed`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                assembler.assemble(
                    placements = listOf(placement("beacon-placement", "beacon-subject")),
                    admittedWorkspacePresentations = emptyList(),
                    managedSubjects =
                        listOf(subject("beacon-subject", "Canonical beacon")),
                    legacySubjectMappings =
                        listOf(
                            mapping(
                                "beacon-map",
                                LegacyOrientationSourceType.MAIN_BEACON,
                                "legacy-beacon",
                                "beacon-subject",
                            ),
                        ),
                    relations = emptyList(),
                    groupScopes = emptyList(),
                    legacyGroups = emptyList(),
                    presentationProvenance =
                        CanonicalV2HierarchyPresentationProvenance(),
                )
            }

        require(
            error.message.orEmpty().contains(
                "Persisted Group-scope provenance must cover every root MANAGED_SUBJECT",
            ),
        )
    }

    private fun placement(
        id: String,
        targetId: String,
    ): HierarchyPlacement =
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target =
                HierarchyTargetRef(
                    type = HierarchyTargetType.MANAGED_SUBJECT,
                    id = targetId,
                ),
            parentPlacementId = null,
            placementKind = PlacementKind.PRIMARY,
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun subject(
        id: String,
        title: String,
    ): ManagedSubjectEntity =
        ManagedSubjectEntity(
            id = id,
            subjectType = "ORIENTATION",
            title = title,
            description = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun mapping(
        id: String,
        sourceType: LegacyOrientationSourceType,
        sourceId: String,
        subjectId: String,
    ): LegacySubjectMappingEntity =
        LegacySubjectMappingEntity(
            id = id,
            sourceType = sourceType.name,
            sourceId = sourceId,
            subjectId = subjectId,
            migrationVersion = 1,
            state = LegacySubjectMappingState.CUT_OVER.name,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
