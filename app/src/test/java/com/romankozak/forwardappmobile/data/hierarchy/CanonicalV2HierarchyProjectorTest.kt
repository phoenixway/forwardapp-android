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

class CanonicalV2HierarchyProjectorTest {
    private val projector = CanonicalV2HierarchyProjector()

    @Test
    fun `root and nested workspace topology comes only from placements`() {
        val root = workspace("root")
        val child = workspace("child")

        val projection =
            project(
                placement("p-root", root, order = 4),
                placement("p-child", child, parentId = "p-root", order = 9),
            )

        assertEquals(
            listOf("p-root", "p-child"),
            projection.occurrences.map { it.placementId.value },
        )
        assertEquals(
            listOf("p-root"),
            projection.rootPlacementIds.map { it.value },
        )
        assertEquals(
            listOf("p-root", "p-child"),
            projection.occurrences[1].occurrencePath.map { it.value },
        )
        assertEquals(1, projection.occurrences[1].depth)
        assertEquals(0, projection.occurrences[1].rootOrder)
    }

    @Test
    fun `PRIMARY and LINK appearances of one target retain distinct occurrence identity`() {
        val shared = workspace("shared")
        val parent = workspace("parent")

        val projection =
            project(
                placement("primary", shared, kind = PlacementKind.PRIMARY, order = 0),
                placement("parent", parent, order = 1),
                placement(
                    "link",
                    shared,
                    parentId = "parent",
                    kind = PlacementKind.LINK,
                    order = 0,
                ),
            )

        val sharedOccurrences = projection.occurrences.filter { it.target == shared }

        assertEquals(2, sharedOccurrences.size)
        assertEquals(
            setOf("primary", "link"),
            sharedOccurrences.map { it.placementId.value }.toSet(),
        )
        assertEquals(
            setOf(PlacementKind.PRIMARY, PlacementKind.LINK),
            sharedOccurrences.map { it.placementKind }.toSet(),
        )
        assertEquals(
            listOf("parent", "link"),
            sharedOccurrences.single { it.placementKind == PlacementKind.LINK }
                .occurrencePath
                .map { it.value },
        )
    }

    @Test
    fun `multiple LINK appearances of one target are not collapsed by stable target id`() {
        val rootA = workspace("root-a")
        val rootB = workspace("root-b")
        val repeated = workspace("repeated")

        val projection =
            project(
                placement("a", rootA, order = 0),
                placement("b", rootB, order = 1),
                placement(
                    "link-a",
                    repeated,
                    parentId = "a",
                    kind = PlacementKind.LINK,
                    order = 0,
                ),
                placement(
                    "link-b",
                    repeated,
                    parentId = "b",
                    kind = PlacementKind.LINK,
                    order = 0,
                ),
            )

        val repeatedOccurrences = projection.occurrences.filter { it.target == repeated }

        assertEquals(2, repeatedOccurrences.size)
        assertEquals(
            listOf(
                listOf("a", "link-a"),
                listOf("b", "link-b"),
            ),
            repeatedOccurrences.map { occurrence ->
                occurrence.occurrencePath.map { it.value }
            },
        )
    }

    @Test
    fun `LINK occurrence owns its explicit placement child subtree`() {
        val root = workspace("root")
        val linked = workspace("linked")
        val child = workspace("child")

        val projection =
            project(
                placement("root", root),
                placement(
                    "linked",
                    linked,
                    parentId = "root",
                    kind = PlacementKind.LINK,
                ),
                placement(
                    "child",
                    child,
                    parentId = "linked",
                    kind = PlacementKind.PRIMARY,
                ),
            )

        val childOccurrence = projection.occurrence(PlacementId("child"))!!

        assertEquals(PlacementId("linked"), childOccurrence.parentPlacementId)
        assertEquals(
            listOf("root", "linked", "child"),
            childOccurrence.occurrencePath.map { it.value },
        )
    }

    @Test
    fun `root and sibling equal-order ties are deterministic by PlacementId`() {
        val projection =
            project(
                placement("root-z", workspace("root-z"), order = 5),
                placement("root-a", workspace("root-a"), order = 5),
                placement("child-z", workspace("child-z"), parentId = "root-a", order = 8),
                placement("child-a", workspace("child-a"), parentId = "root-a", order = 8),
            )

        assertEquals(
            listOf("root-a", "root-z"),
            projection.rootPlacementIds.map { it.value },
        )
        assertEquals(
            listOf("root-a", "child-a", "child-z", "root-z"),
            projection.occurrences.map { it.placementId.value },
        )
        assertEquals(
            listOf(0, 0, 0, 1),
            projection.occurrences.map { it.rootOrder },
        )
    }

    @Test
    fun `tombstoned placements do not render`() {
        val projection =
            project(
                placement("live", workspace("live")),
                placement(
                    "dead",
                    workspace("dead"),
                    order = 1,
                    isDeleted = true,
                ),
            )

        assertEquals(
            listOf("live"),
            projection.occurrences.map { it.placementId.value },
        )
    }

    @Test
    fun `missing or deleted target fails closed`() {
        val target = workspace("missing")

        val failure =
            runCatching {
                projector.project(
                    placements = listOf(placement("p", target)),
                    admittedTargets = emptyMap(),
                )
            }.exceptionOrNull() as CanonicalV2HierarchyProjectionException

        assertTrue(failure.violations.any { it.code == "TARGET_NOT_LIVE" })
    }

    @Test
    fun `missing parent and tombstoned parent both fail closed`() {
        val child = workspace("child")

        val missingParentFailure =
            runCatching {
                project(
                    placement("child", child, parentId = "missing"),
                )
            }.exceptionOrNull() as CanonicalV2HierarchyProjectionException

        assertTrue(missingParentFailure.violations.any { it.code == "MISSING_PARENT" })

        val tombstonedParentFailure =
            runCatching {
                project(
                    placement(
                        "dead-parent",
                        workspace("parent"),
                        isDeleted = true,
                    ),
                    placement("child", child, parentId = "dead-parent"),
                )
            }.exceptionOrNull() as CanonicalV2HierarchyProjectionException

        assertTrue(
            tombstonedParentFailure.violations.any { it.code == "TOMBSTONED_PARENT" },
        )
    }

    @Test
    fun `cycle and unsupported hierarchy fail closed`() {
        val cycleFailure =
            runCatching {
                project(
                    placement("a", workspace("a"), parentId = "b"),
                    placement("b", workspace("b"), parentId = "a"),
                )
            }.exceptionOrNull() as CanonicalV2HierarchyProjectionException

        assertTrue(cycleFailure.violations.any { it.code == "CYCLE" })

        val unsupported =
            HierarchyPlacement(
                id = PlacementId("other"),
                hierarchyId = HierarchyId("OTHER"),
                target = workspace("other"),
                parentPlacementId = null,
                placementKind = PlacementKind.PRIMARY,
                siblingOrder = 0,
                createdAt = 1,
                updatedAt = 1,
                syncedAt = null,
                isDeleted = false,
                version = 1,
            )

        val unsupportedFailure =
            runCatching {
                projector.project(
                    placements = listOf(unsupported),
                    admittedTargets =
                        mapOf(
                            unsupported.target to
                                CanonicalV2HierarchyTargetPresentation(
                                    target = unsupported.target,
                                    title = unsupported.target.id,
                                ),
                        ),
                )
            }.exceptionOrNull() as CanonicalV2HierarchyProjectionException

        assertTrue(
            unsupportedFailure.violations.any { it.code == "UNSUPPORTED_HIERARCHY" },
        )
    }

    private fun project(
        vararg placements: HierarchyPlacement,
    ): CanonicalV2HierarchyProjection {
        val admittedTargets =
            placements
                .asSequence()
                .filterNot { it.isDeleted }
                .map { it.target }
                .distinct()
                .associateWith { target ->
                    CanonicalV2HierarchyTargetPresentation(
                        target = target,
                        title = target.id,
                    )
                }

        return projector.project(
            placements = placements.toList(),
            admittedTargets = admittedTargets,
        )
    }

    private fun workspace(id: String): HierarchyTargetRef =
        HierarchyTargetRef(
            type = HierarchyTargetType.WORKSPACE,
            id = id,
        )

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0,
        isDeleted: Boolean = false,
    ): HierarchyPlacement =
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target = target,
            parentPlacementId = parentId?.let(::PlacementId),
            placementKind = kind,
            siblingOrder = order,
            createdAt = 1,
            updatedAt = 1,
            syncedAt = null,
            isDeleted = isDeleted,
            version = 1,
        )
}
