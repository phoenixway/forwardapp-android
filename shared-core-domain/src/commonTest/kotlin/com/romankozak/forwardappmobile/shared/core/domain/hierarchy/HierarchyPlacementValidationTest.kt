package com.romankozak.forwardappmobile.shared.core.domain.hierarchy

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HierarchyPlacementValidationTest {
    @Test
    fun `workspace primary plus two links is valid`() {
        val placements =
            listOf(
                placement("p1", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.PRIMARY),
                placement("p2", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.LINK),
                placement("p3", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.LINK),
            )

        assertValid(placements)
    }

    @Test
    fun `link may own an independent child subtree`() {
        val placements =
            listOf(
                placement("primary", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.PRIMARY),
                placement("link", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.LINK),
                placement("child", "subject-child", parentId = "link"),
                placement("grandchild", "subject-grandchild", parentId = "child"),
            )

        assertValid(placements)
    }

    @Test
    fun `zero primary with remaining links is valid`() {
        val placements =
            listOf(
                placement("link-1", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.LINK),
                placement("link-2", "workspace", HierarchyTargetType.WORKSPACE, PlacementKind.LINK),
            )

        assertValid(placements)
    }

    @Test
    fun `repeated target identity along finite ancestry is valid`() {
        val placements =
            listOf(
                placement("root", "subject"),
                placement("child", "subject", kind = PlacementKind.LINK, parentId = "root"),
                placement("leaf", "other-subject", parentId = "child"),
            )

        assertValid(placements)
    }

    @Test
    fun `actual placement cycle is rejected`() {
        val placements =
            listOf(
                placement("a", "subject-a", parentId = "b"),
                placement("b", "subject-b", parentId = "a"),
            )

        assertViolation(placements, "CYCLE")
    }

    @Test
    fun `self parent is rejected`() {
        val placements = listOf(placement("self", "subject", parentId = "self"))

        assertViolation(placements, "SELF_PARENT")
    }

    @Test
    fun `cross hierarchy parent is rejected`() {
        val otherHierarchy = HierarchyId("OTHER")
        val placements =
            listOf(
                placement("parent", "parent-target"),
                placement("child", "child-target", hierarchyId = otherHierarchy, parentId = "parent"),
            )

        assertViolation(placements, "CROSS_HIERARCHY_PARENT")
        assertViolation(placements, "UNSUPPORTED_HIERARCHY")
    }

    @Test
    fun `missing live parent is rejected`() {
        val placements = listOf(placement("child", "subject", parentId = "missing"))

        assertViolation(placements, "MISSING_PARENT")
    }

    @Test
    fun `tombstoned parent with live child is rejected`() {
        val placements =
            listOf(
                placement("parent", "old-parent", isDeleted = true),
                placement("child", "subject", parentId = "parent"),
            )

        assertViolation(placements, "TOMBSTONED_PARENT")
    }

    @Test
    fun `two live primary placements for same target and hierarchy are rejected`() {
        val placements =
            listOf(
                placement("primary-1", "subject"),
                placement("primary-2", "subject"),
            )

        assertViolation(placements, "MULTIPLE_PRIMARY")
    }

    @Test
    fun `duplicate same-target siblings as different placements are valid`() {
        val placements =
            listOf(
                placement("parent", "parent-target"),
                placement("link-1", "subject", kind = PlacementKind.LINK, parentId = "parent"),
                placement("link-2", "subject", kind = PlacementKind.LINK, parentId = "parent"),
            )

        assertValid(placements)
    }

    @Test
    fun `missing or deleted target for live placement is rejected`() {
        val placements = listOf(placement("live", "deleted-target"))

        val violations =
            validateProspectiveHierarchy(placements) { target ->
                target.id != "deleted-target"
            }

        assertTrue(violations.any { it.code == "TARGET_NOT_LIVE" })
    }

    @Test
    fun `tombstone may retain historical missing parent and target references`() {
        val placements =
            listOf(
                placement(
                    id = "deleted",
                    targetId = "deleted-target",
                    parentId = "historical-parent",
                    isDeleted = true,
                ),
            )

        val violations = validateProspectiveHierarchy(placements) { false }

        assertTrue(violations.isEmpty(), violations.toString())
    }

    @Test
    fun `duplicate placement identity is rejected globally`() {
        val placements =
            listOf(
                placement("duplicate", "subject-a"),
                placement("duplicate", "subject-b", kind = PlacementKind.LINK),
            )

        assertViolation(placements, "DUPLICATE_PLACEMENT_ID")
    }

    @Test
    fun `unsupported hierarchy is rejected even for a tombstone`() {
        val placements =
            listOf(
                placement(
                    id = "deleted",
                    targetId = "subject",
                    hierarchyId = HierarchyId("OTHER"),
                    isDeleted = true,
                ),
            )

        assertViolation(placements, "UNSUPPORTED_HIERARCHY")
    }

    @Test
    fun `sibling comparison is deterministic by order then placement identity`() {
        val placements =
            listOf(
                placement("b", "subject-b", siblingOrder = 10L),
                placement("c", "subject-c", siblingOrder = 5L),
                placement("a", "subject-a", siblingOrder = 10L),
            )

        val ids = placements.sortedWith(hierarchyPlacementSiblingComparator).map { it.id.value }

        assertTrue(ids == listOf("c", "a", "b"), ids.toString())
    }

    @Test
    fun `blank domain identities fail closed`() {
        assertFailsWith<IllegalArgumentException> { HierarchyId(" ") }
        assertFailsWith<IllegalArgumentException> { PlacementId("") }
        assertFailsWith<IllegalArgumentException> {
            HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, " ")
        }
    }

    private fun assertValid(placements: List<HierarchyPlacement>) {
        val liveTargets = placements.filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.target }
        val violations = validateProspectiveHierarchy(placements) { it in liveTargets }
        assertTrue(violations.isEmpty(), violations.toString())
    }

    private fun assertViolation(
        placements: List<HierarchyPlacement>,
        code: String,
    ) {
        val liveTargets = placements.filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.target }
        val violations = validateProspectiveHierarchy(placements) { it in liveTargets }
        assertTrue(violations.any { it.code == code }, violations.toString())
    }

    private fun placement(
        id: String,
        targetId: String,
        targetType: HierarchyTargetType = HierarchyTargetType.MANAGED_SUBJECT,
        kind: PlacementKind = PlacementKind.PRIMARY,
        hierarchyId: HierarchyId = HierarchyId.GENERAL,
        parentId: String? = null,
        isDeleted: Boolean = false,
        siblingOrder: Long = 0L,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = hierarchyId,
        target = HierarchyTargetRef(targetType, targetId),
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = siblingOrder,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
    )
}
