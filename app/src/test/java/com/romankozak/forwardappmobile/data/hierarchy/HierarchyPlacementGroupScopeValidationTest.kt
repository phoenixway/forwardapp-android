package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyPlacementGroupScopeValidationTest {
    @Test
    fun `arbitrary duplicate root ids retain independent Group provenance`() {
        val support = support()
        val placements =
            listOf(
                placement("future-v2-random-A"),
                placement("future-v2-random-B"),
            )
        val scopes =
            listOf(
                scope("future-v2-random-A", "group-a"),
                scope("future-v2-random-B", "group-b"),
            )

        validateHierarchyPlacementGroupScopes(
            scopes = scopes,
            placements = placements,
            subjects = support.subjects,
            orientations = support.orientations,
            mappings = support.mappings,
            relations = support.relations,
            requireComplete = true,
        )
    }

    @Test
    fun `authoritative stream fails closed when one duplicate root lacks provenance`() {
        val support = support()
        val failure =
            runCatching {
                validateHierarchyPlacementGroupScopes(
                    scopes =
                        listOf(
                            scope("future-v2-random-A", "group-a"),
                        ),
                    placements =
                        listOf(
                            placement("future-v2-random-A"),
                            placement("future-v2-random-B"),
                        ),
                    subjects = support.subjects,
                    orientations = support.orientations,
                    mappings = support.mappings,
                    relations =
                        support.relations.filter {
                            it.toOrientationId == "group-a"
                        },
                    requireComplete = true,
                )
            }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(
            failure?.message.orEmpty()
                .contains("must cover every live root MANAGED_SUBJECT"),
        )
    }

    @Test
    fun `same target cannot mix explicit NoGroup and grouped root`() {
        val support = support()
        val failure =
            runCatching {
                validateHierarchyPlacementGroupScopes(
                    scopes =
                        listOf(
                            scope("future-v2-random-A", null),
                            scope("future-v2-random-B", "group-a"),
                        ),
                    placements =
                        listOf(
                            placement("future-v2-random-A"),
                            placement("future-v2-random-B"),
                        ),
                    subjects = support.subjects,
                    orientations = support.orientations,
                    mappings = support.mappings,
                    relations =
                        support.relations.filter {
                            it.toOrientationId == "group-a"
                        },
                    requireComplete = true,
                )
            }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(
            failure?.message.orEmpty()
                .contains("cannot mix Group and NoGroup"),
        )
    }

    @Test
    fun `Group provenance union must equal target wide PART_OF membership`() {
        val support = support()
        val failure =
            runCatching {
                validateHierarchyPlacementGroupScopes(
                    scopes =
                        listOf(
                            scope("future-v2-random-A", "group-a"),
                            scope("future-v2-random-B", "group-a"),
                        ),
                    placements =
                        listOf(
                            placement("future-v2-random-A"),
                            placement("future-v2-random-B"),
                        ),
                    subjects = support.subjects,
                    orientations = support.orientations,
                    mappings = support.mappings,
                    relations = support.relations,
                    requireComplete = true,
                )
            }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(
            failure?.message.orEmpty()
                .contains("does not match target-wide PART_OF membership"),
        )
    }

    private fun placement(id: String) =
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target =
                HierarchyTargetRef(
                    type = HierarchyTargetType.MANAGED_SUBJECT,
                    id = "shared-subject",
                ),
            parentPlacementId = null,
            placementKind = PlacementKind.LINK,
            siblingOrder = if (id.endsWith("A")) 0L else 1L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun scope(
        placementId: String,
        groupSubjectId: String?,
    ) =
        HierarchyPlacementGroupScopeEntity(
            placementId = placementId,
            hierarchyId = "GENERAL",
            groupSubjectId = groupSubjectId,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun support(): Support {
        val subjects =
            listOf(
                subject("shared-subject"),
                subject("group-a"),
                subject("group-b"),
            )
        val orientations =
            listOf(
                groupOrientation("group-a"),
                groupOrientation("group-b"),
            )
        val mappings =
            listOf(
                groupMapping("legacy-a", "group-a"),
                groupMapping("legacy-b", "group-b"),
            )
        val relations =
            listOf(
                partOf("edge-a", "group-a"),
                partOf("edge-b", "group-b"),
            )
        return Support(subjects, orientations, mappings, relations)
    }

    private fun subject(id: String) =
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

    private fun groupOrientation(id: String) =
        OrientationEntity(
            subjectId = id,
            kind = OrientationKind.MAIN_BEACON_GROUP.name,
            lifecycle = null,
            lifecycleOrigin = "UNSET",
        )

    private fun groupMapping(
        legacyId: String,
        subjectId: String,
    ) =
        LegacySubjectMappingEntity(
            id = "mapping-$legacyId",
            sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
            sourceId = legacyId,
            subjectId = subjectId,
            migrationVersion = 1,
            state = LegacySubjectMappingState.CUT_OVER.name,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun partOf(
        id: String,
        groupSubjectId: String,
    ) =
        OrientationRelationEntity(
            id = id,
            fromOrientationId = "shared-subject",
            toOrientationId = groupSubjectId,
            relationType = OrientationRelationType.PART_OF.name,
            relationOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private data class Support(
        val subjects: List<ManagedSubjectEntity>,
        val orientations: List<OrientationEntity>,
        val mappings: List<LegacySubjectMappingEntity>,
        val relations: List<OrientationRelationEntity>,
    )
}
