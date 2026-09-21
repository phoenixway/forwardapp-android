package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2HierarchyScopePlannerTest {
    private val projector = CanonicalV2HierarchyProjector()
    private val planner = CanonicalV2HierarchyScopePlanner()

    @Test
    fun `persisted Group scope selects exact root while H1 keeps structural parent`() {
        val parent = subjectTarget("parent")
        val child = subjectTarget("child")
        val hierarchy =
            project(
                placement("parent-placement", parent),
                placement("child-placement", child, parentId = "parent-placement"),
            )

        val scopes =
            planner.plan(
                hierarchy = hierarchy,
                groups = listOf(group("group-ui", "group-subject", "Core", 0)),
                relations =
                    listOf(
                        partOf(
                            id = "membership",
                            fromSubjectId = "parent",
                            toGroupSubjectId = "group-subject",
                        ),
                    ),
                groupScopes =
                    listOf(
                        scope("parent-placement", "group-subject"),
                    ),
            )

        assertEquals(listOf("group-ui"), scopes.map { it.id })
        assertEquals(
            listOf(PlacementId("parent-placement")),
            scopes.single().rootPlacementIds,
        )
        assertEquals(
            PlacementId("parent-placement"),
            hierarchy.occurrence(PlacementId("child-placement"))?.parentPlacementId,
        )
    }

    @Test
    fun `explicit null scope produces NoGroup without inference from relations`() {
        val root = subjectTarget("subject")
        val hierarchy = project(placement("arbitrary-root", root))

        val scopes =
            planner.plan(
                hierarchy = hierarchy,
                groups = listOf(group("group", "group-subject", "Group", 0)),
                relations =
                    listOf(
                        relation(
                            id = "supports",
                            fromSubjectId = "subject",
                            toSubjectId = "group-subject",
                            type = OrientationRelationType.SUPPORTS,
                        ),
                    ),
                groupScopes = listOf(scope("arbitrary-root", null)),
            )

        assertTrue(
            scopes.single { it.id == "group" }.rootPlacementIds.orEmpty().isEmpty(),
        )
        assertEquals(
            listOf(PlacementId("arbitrary-root")),
            scopes.single { it.id == CANONICAL_V2_NO_GROUP_SCOPE_ID }.rootPlacementIds,
        )
    }

    @Test
    fun `missing occurrence provenance fails closed instead of becoming NoGroup`() {
        val hierarchy =
            project(
                placement("future-random-root", subjectTarget("subject")),
            )

        val failure =
            runCatching {
                planner.plan(
                    hierarchy = hierarchy,
                    groups = emptyList(),
                    relations = emptyList(),
                    groupScopes = emptyList(),
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(
            failure?.message.orEmpty()
                .contains("must cover every root MANAGED_SUBJECT"),
        )
    }

    @Test
    fun `arbitrary duplicate V2 root ids retain independent persisted Group scopes`() {
        val target = subjectTarget("shared")
        val hierarchy =
            project(
                placement(
                    "future-random-first",
                    target,
                    kind = PlacementKind.LINK,
                    order = 0,
                ),
                placement(
                    "future-random-second",
                    target,
                    kind = PlacementKind.LINK,
                    order = 1,
                ),
            )

        val scopes =
            planner.plan(
                hierarchy = hierarchy,
                groups =
                    listOf(
                        group("a", "group-a", "A", 0),
                        group("b", "group-b", "B", 1),
                    ),
                relations =
                    listOf(
                        partOf("a-edge", "shared", "group-a"),
                        partOf("b-edge", "shared", "group-b"),
                    ),
                groupScopes =
                    listOf(
                        scope("future-random-first", "group-a"),
                        scope("future-random-second", "group-b"),
                    ),
            )

        assertEquals(
            listOf(PlacementId("future-random-first")),
            scopes.single { it.id == "a" }.rootPlacementIds,
        )
        assertEquals(
            listOf(PlacementId("future-random-second")),
            scopes.single { it.id == "b" }.rootPlacementIds,
        )
    }

    @Test
    fun `persisted Group scope conflicting with PART_OF fails closed`() {
        val hierarchy =
            project(
                placement("root", subjectTarget("subject")),
            )

        val failure =
            runCatching {
                planner.plan(
                    hierarchy = hierarchy,
                    groups =
                        listOf(
                            group("a", "group-a", "A", 0),
                            group("b", "group-b", "B", 1),
                        ),
                    relations =
                        listOf(
                            partOf("membership", "subject", "group-b"),
                        ),
                    groupScopes =
                        listOf(
                            scope("root", "group-a"),
                        ),
                )
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertTrue(
            failure?.message.orEmpty()
                .contains("does not match PART_OF membership"),
        )
    }

    private fun project(
        vararg placements: HierarchyPlacement,
    ): CanonicalV2HierarchyProjection {
        val targets =
            placements
                .map { it.target }
                .distinct()
                .associateWith {
                    CanonicalV2HierarchyTargetPresentation(
                        target = it,
                        title = it.id,
                    )
                }

        return projector.project(placements.toList(), targets)
    }

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = HierarchyId.GENERAL,
        target = target,
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun scope(
        placementId: String,
        groupSubjectId: String?,
    ) = HierarchyPlacementGroupScopeEntity(
        placementId = placementId,
        hierarchyId = HierarchyId.GENERAL.value,
        groupSubjectId = groupSubjectId,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun group(
        id: String,
        subjectId: String,
        title: String,
        order: Long,
    ) = CanonicalV2HierarchyGroupPresentation(
        id = id,
        canonicalSubjectId = subjectId,
        title = title,
        order = order,
    )

    private fun partOf(
        id: String,
        fromSubjectId: String,
        toGroupSubjectId: String,
    ) = relation(
        id = id,
        fromSubjectId = fromSubjectId,
        toSubjectId = toGroupSubjectId,
        type = OrientationRelationType.PART_OF,
    )

    private fun relation(
        id: String,
        fromSubjectId: String,
        toSubjectId: String,
        type: OrientationRelationType,
    ) = OrientationRelationEntity(
        id = id,
        fromOrientationId = fromSubjectId,
        toOrientationId = toSubjectId,
        relationType = type.name,
        relationOrder = 0,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id)
}
