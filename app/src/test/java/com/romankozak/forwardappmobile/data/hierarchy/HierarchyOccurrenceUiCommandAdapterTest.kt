package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HierarchyOccurrenceUiCommandAdapterTest {
    @Test
    fun `PRIMARY move maps exact PlacementId and concrete parent occurrence`() {
        val source = occurrence("primary", "child", PlacementKind.PRIMARY, "old")
        val destination = occurrence("parent-link", "parent", PlacementKind.LINK)

        val result =
            HierarchyOccurrenceUiCommandAdapter.move(
                source = source,
                destinationParent = destination,
            )

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Resolved(
                HierarchyOccurrenceCommand.Move(
                    placementId = PlacementId("primary"),
                    newParentPlacementId = PlacementId("parent-link"),
                ),
            ),
            result,
        )
    }

    @Test
    fun `LINK move preserves selected LINK identity instead of target PRIMARY`() {
        val selectedLink = occurrence("selected-link", "shared", PlacementKind.LINK)
        val primary = occurrence("primary", "shared", PlacementKind.PRIMARY)
        val destination = occurrence("destination", "parent", PlacementKind.PRIMARY)

        val result =
            HierarchyOccurrenceUiCommandAdapter.move(
                source = selectedLink,
                destinationParent = destination,
            ) as HierarchyOccurrenceUiCommandAdapter.Result.Resolved

        val command = result.value
        assertEquals(PlacementId("selected-link"), command.placementId)
        assertNotEquals(primary.placementId, command.placementId)
    }

    @Test
    fun `duplicate same-target occurrences remain independently addressable`() {
        val first = occurrence("first", "shared", PlacementKind.PRIMARY)
        val second = occurrence("second", "shared", PlacementKind.LINK)
        val destination = occurrence("destination", "parent", PlacementKind.PRIMARY)

        val result =
            HierarchyOccurrenceUiCommandAdapter.moveMany(
                sources = listOf(first, second),
                destinationParent = destination,
            ) as HierarchyOccurrenceUiCommandAdapter.Result.Resolved

        assertEquals(
            listOf(PlacementId("first"), PlacementId("second")),
            result.value.moves.map { it.placementId },
        )
    }

    @Test
    fun `sibling reorder emits one complete PlacementId ordering`() {
        val a = occurrence("a", "same", PlacementKind.PRIMARY, "parent")
        val b = occurrence("b", "same", PlacementKind.LINK, "parent")
        val c = occurrence("c", "other", PlacementKind.PRIMARY, "parent")

        val result =
            HierarchyOccurrenceUiCommandAdapter.reorderSiblings(
                parentPlacementId = PlacementId("parent"),
                orderedSiblings = listOf(b, c, a),
                expectedSiblingPlacementIds =
                    setOf(
                        PlacementId("a"),
                        PlacementId("b"),
                        PlacementId("c"),
                    ),
            )

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Resolved(
                HierarchyOccurrenceCommand.ReorderSiblings(
                    parentPlacementId = PlacementId("parent"),
                    orderedPlacementIds =
                        listOf(
                            PlacementId("b"),
                            PlacementId("c"),
                            PlacementId("a"),
                        ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `partial sibling reorder fails closed`() {
        val a = occurrence("a", "a", PlacementKind.PRIMARY, "parent")

        val result =
            HierarchyOccurrenceUiCommandAdapter.reorderSiblings(
                parentPlacementId = PlacementId("parent"),
                orderedSiblings = listOf(a),
                expectedSiblingPlacementIds =
                    setOf(PlacementId("a"), PlacementId("missing")),
            )

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Unresolved(
                HierarchyOccurrenceUiCommandAdapter.Reason.INCOMPLETE_SIBLING_OCCURRENCES,
            ),
            result,
        )
    }

    @Test
    fun `remove occurrence remains distinct from target deletion`() {
        val occurrence = occurrence("link", "shared", PlacementKind.LINK)

        val remove =
            HierarchyOccurrenceUiCommandAdapter.removeOccurrence(occurrence)
                as HierarchyOccurrenceUiCommandAdapter.Result.Resolved
        val delete =
            HierarchyOccurrenceUiCommandAdapter.deleteTarget(occurrence.target)

        assertEquals(
            HierarchyDestructiveIntent.RemoveOccurrence(PlacementId("link")),
            remove.value,
        )
        assertEquals(
            HierarchyDestructiveIntent.DeleteTarget(workspaceTarget("shared")),
            delete,
        )
    }

    @Test
    fun `restore addresses exact removed occurrence`() {
        val link = occurrence("removed-link", "shared", PlacementKind.LINK)

        val result =
            HierarchyOccurrenceUiCommandAdapter.restoreOccurrence(link)

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Resolved(
                HierarchyOccurrenceCommand.RestoreOccurrence(
                    PlacementId("removed-link"),
                ),
            ),
            result,
        )
    }

    @Test
    fun `target-only structural source fails closed`() {
        val destination = occurrence("destination", "parent", PlacementKind.PRIMARY)

        val result =
            HierarchyOccurrenceUiCommandAdapter.move(
                source = null,
                destinationParent = destination,
            )

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Unresolved(
                HierarchyOccurrenceUiCommandAdapter.Reason.OCCURRENCE_IDENTITY_REQUIRED,
            ),
            result,
        )
    }

    @Test
    fun `clipboard selected LINK preserves exact LINK carrier`() {
        val selected = occurrence("selected-link", "shared", PlacementKind.LINK)

        val result =
            HierarchyOccurrenceUiCommandAdapter.clipboardOccurrence(
                occurrence = selected,
                sourceKind = HierarchyClipboardSourceKind.CONTEXT_COMPATIBILITY,
            )

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Resolved(
                HierarchyClipboardOccurrence(
                    occurrence = selected,
                    sourceKind = HierarchyClipboardSourceKind.CONTEXT_COMPATIBILITY,
                ),
            ),
            result,
        )
    }

    @Test
    fun `LINK appearance creation is distinct from target clone`() {
        val destination = occurrence("parent-link", "parent", PlacementKind.LINK)

        val result =
            HierarchyOccurrenceUiCommandAdapter.createAppearance(
                target = workspaceTarget("source"),
                destinationParent = destination,
                placementKind = PlacementKind.LINK,
            )

        assertEquals(
            HierarchyOccurrenceUiCommandAdapter.Result.Resolved(
                HierarchyOccurrenceCommand.CreateAppearance(
                    target = workspaceTarget("source"),
                    parentPlacementId = PlacementId("parent-link"),
                    placementKind = PlacementKind.LINK,
                ),
            ),
            result,
        )
    }

    private fun occurrence(
        placementId: String,
        targetId: String,
        kind: PlacementKind,
        parentPlacementId: String? = null,
    ): HierarchyOccurrenceRef =
        HierarchyOccurrenceRef(
            placementId = PlacementId(placementId),
            target = workspaceTarget(targetId),
            parentPlacementId = parentPlacementId?.let(::PlacementId),
            placementKind = kind,
            siblingOrder = 0L,
        )

    private fun workspaceTarget(id: String): HierarchyTargetRef =
        HierarchyTargetRef(
            type = HierarchyTargetType.WORKSPACE,
            id = id,
        )
}
