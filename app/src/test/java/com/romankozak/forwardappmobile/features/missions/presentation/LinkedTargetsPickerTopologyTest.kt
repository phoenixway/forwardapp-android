package com.romankozak.forwardappmobile.features.missions.presentation

import com.romankozak.forwardappmobile.data.hierarchy.ChooserHierarchyItem
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkedTargetsPickerTopologyTest {
    @Test
    fun duplicateTargetAppearancesRemainSeparateButSelectSameTarget() {
        val options =
            listOf(
                ProjectOption(id = "parent-a", name = "Parent A"),
                ProjectOption(id = "parent-b", name = "Parent B"),
                ProjectOption(id = "shared", name = "Shared"),
            )
        val occurrences =
            listOf(
                item("parent-a", "parent-a-placement", null),
                item("parent-b", "parent-b-placement", null),
                item("shared", "shared-a", "parent-a-placement"),
                item("shared", "shared-b", "parent-b-placement"),
            )

        val nodes = buildLinkedPickerNodes(options, occurrences)
        val tree = buildLinkedPickerTree(nodes, query = "", showDescendants = false)

        assertEquals(
            listOf("occurrence:parent-a-placement", "occurrence:parent-b-placement"),
            tree.roots.map { it.key },
        )
        assertEquals(
            listOf("occurrence:shared-a"),
            tree.childrenByKey["occurrence:parent-a-placement"].orEmpty().map { it.key },
        )
        assertEquals(
            listOf("occurrence:shared-b"),
            tree.childrenByKey["occurrence:parent-b-placement"].orEmpty().map { it.key },
        )
        assertEquals(
            listOf("shared", "shared"),
            nodes.filter { it.targetId == "shared" }.map { it.targetId },
        )
    }

    @Test
    fun projectOptionParentIdCannotEstablishTopologyWithoutV2Occurrence() {
        val nodes =
            buildLinkedPickerNodes(
                options =
                    listOf(
                        ProjectOption(id = "parent", name = "Parent"),
                        ProjectOption(id = "child", name = "Child"),
                    ),
                occurrences = emptyList(),
            )

        val child = nodes.single { it.targetId == "child" }
        assertEquals("target-only:child", child.key)
        assertNull(child.parentKey)

        val tree = buildLinkedPickerTree(nodes, query = "", showDescendants = false)
        assertEquals(
            listOf("target-only:child", "target-only:parent"),
            tree.roots.map { it.key },
        )
    }

    @Test
    fun searchAndExpansionFollowOccurrenceAncestryOnly() {
        val options =
            listOf(
                ProjectOption(id = "parent-a", name = "Parent A"),
                ProjectOption(id = "parent-b", name = "Parent B"),
                ProjectOption(id = "shared", name = "Shared"),
                ProjectOption(id = "child", name = "Unique child"),
            )
        val occurrences =
            listOf(
                item("parent-a", "parent-a-placement", null),
                item("parent-b", "parent-b-placement", null),
                item("shared", "shared-a", "parent-a-placement"),
                item("shared", "shared-b", "parent-b-placement"),
                item("child", "child-placement", "shared-a"),
            )
        val nodes = buildLinkedPickerNodes(options, occurrences)

        val tree =
            buildLinkedPickerTree(
                nodes = nodes,
                query = "Unique child",
                showDescendants = false,
            )

        assertEquals(
            setOf(
                "occurrence:parent-a-placement",
                "occurrence:shared-a",
                "occurrence:child-placement",
            ),
            tree.visibleKeys,
        )
        assertEquals(
            setOf("occurrence:parent-a-placement", "occurrence:shared-a"),
            linkedPickerExpandedKeysForQuery(nodes, "Unique child"),
        )
    }

    private fun item(
        targetId: String,
        placementId: String,
        parentPlacementId: String?,
    ): ChooserHierarchyItem =
        ChooserHierarchyItem(
            id = targetId,
            name = targetId,
            description = null,
            order = 0L,
            occurrence =
                HierarchyOccurrenceRef(
                    placementId = PlacementId(placementId),
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, targetId),
                    parentPlacementId = parentPlacementId?.let(::PlacementId),
                    placementKind = PlacementKind.PRIMARY,
                    siblingOrder = 0L,
                ),
        )
}
