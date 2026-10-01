package com.romankozak.forwardappmobile.features.contexts.ui.context_chooser

import com.romankozak.forwardappmobile.data.hierarchy.ChooserHierarchyItem
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChooserOccurrenceTopologyTest {
    @Test
    fun duplicateWorkspaceAppearancesRetainIndependentParentsAndChildren() {
        val firstParent = item("parent-a", "parent-a-placement", null, "Parent A")
        val secondParent = item("parent-b", "parent-b-placement", null, "Parent B")
        val firstAppearance =
            item("shared", "shared-a", "parent-a-placement", "Shared")
        val secondAppearance =
            item("shared", "shared-b", "parent-b-placement", "Shared")
        val firstChild =
            item("first-child", "first-child-placement", "shared-a", "First child")
        val projects =
            listOf(firstParent, secondParent, firstAppearance, secondAppearance, firstChild)

        val unfiltered = buildChooserUiState(projects, filter = "", showDescendants = false)
        assertEquals(
            listOf("parent-a-placement", "parent-b-placement"),
            unfiltered.topLevelProjects.map { it.occurrence.placementId.value },
        )
        assertEquals(
            listOf("shared-a"),
            unfiltered.childMap["parent-a-placement"].orEmpty()
                .map { it.occurrence.placementId.value },
        )
        assertEquals(
            listOf("shared-b"),
            unfiltered.childMap["parent-b-placement"].orEmpty()
                .map { it.occurrence.placementId.value },
        )
        assertEquals(
            listOf("first-child-placement"),
            unfiltered.childMap["shared-a"].orEmpty()
                .map { it.occurrence.placementId.value },
        )
        assertFalse(unfiltered.childMap.containsKey("shared-b"))

        val filtered = buildChooserUiState(projects, filter = "Shared", showDescendants = true)
        assertEquals(
            listOf("parent-a-placement", "parent-b-placement"),
            filtered.topLevelProjects.map { it.occurrence.placementId.value },
        )
        assertEquals(
            listOf("first-child-placement"),
            filtered.childMap["shared-a"].orEmpty()
                .map { it.occurrence.placementId.value },
        )
        assertFalse(filtered.childMap.containsKey("shared-b"))
    }

    @Test
    fun exactChildSearchShowsOnlyItsOwnAppearanceAncestry() {
        val firstParent = item("parent-a", "parent-a-placement", null, "Parent A")
        val secondParent = item("parent-b", "parent-b-placement", null, "Parent B")
        val firstAppearance =
            item("shared", "shared-a", "parent-a-placement", "Shared")
        val secondAppearance =
            item("shared", "shared-b", "parent-b-placement", "Shared")
        val firstChild =
            item("first-child", "first-child-placement", "shared-a", "Unique child")

        val filtered =
            buildChooserUiState(
                listOf(firstParent, secondParent, firstAppearance, secondAppearance, firstChild),
                filter = "Unique child",
                showDescendants = false,
            )

        assertEquals(
            listOf("parent-a-placement"),
            filtered.topLevelProjects.map { it.occurrence.placementId.value },
        )
        assertEquals(
            listOf("shared-a"),
            filtered.childMap["parent-a-placement"].orEmpty()
                .map { it.occurrence.placementId.value },
        )
        assertEquals(
            listOf("first-child-placement"),
            filtered.childMap["shared-a"].orEmpty()
                .map { it.occurrence.placementId.value },
        )
        assertFalse(filtered.childMap["parent-b-placement"].orEmpty().isNotEmpty())
    }

    private fun item(
        targetId: String,
        placementId: String,
        parentPlacementId: String?,
        name: String,
    ): ChooserHierarchyItem =
        ChooserHierarchyItem(
            id = targetId,
            name = name,
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
