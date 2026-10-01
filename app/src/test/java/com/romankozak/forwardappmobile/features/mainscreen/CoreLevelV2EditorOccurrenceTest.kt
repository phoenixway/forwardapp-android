package com.romankozak.forwardappmobile.features.mainscreen

import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2CoreLevelOccurrence
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconEditorState
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLevelV2EditorOccurrenceTest {
    @Test
    fun `exact placement selects canonical parent instead of embedded Beacon topology`() {
        val root = occurrence("root", "parent", null, null)
        val child = occurrence("child-link", "child", "root", "parent")

        val selected =
            resolveCanonicalV2BeaconOccurrence(
                occurrences = listOf(root, child),
                beaconId = "child",
                placementId = "child-link",
            )

        assertEquals("root", selected?.parentPlacementId?.value)
        assertEquals("parent", selected?.parentBeaconPresentationId)
        assertTrue(
            MainBeaconEditorState(
                id = "child",
                placementId = "child-link",
                parentPlacementId = "root",
                parentBeaconId = "parent",
            ).matchesCanonicalV2Occurrence(requireNotNull(selected)),
        )
    }

    @Test
    fun `duplicate target without exact placement fails closed`() {
        val primary = occurrence("primary", "shared", null, null)
        val linked = occurrence("linked", "shared", "parent", "parent-beacon")

        assertNull(
            resolveCanonicalV2BeaconOccurrence(
                occurrences = listOf(primary, linked),
                beaconId = "shared",
                placementId = null,
            ),
        )
        assertEquals(
            linked,
            resolveCanonicalV2BeaconOccurrence(
                occurrences = listOf(primary, linked),
                beaconId = "shared",
                placementId = "linked",
            ),
        )
    }

    @Test
    fun `metadata save rejects a parent change but accepts root occurrence`() {
        val root = occurrence("root", "beacon", null, null)

        assertTrue(
            MainBeaconEditorState(
                id = "beacon",
                placementId = "root",
            ).matchesCanonicalV2Occurrence(root),
        )
        assertFalse(
            MainBeaconEditorState(
                id = "beacon",
                placementId = "root",
                parentPlacementId = "other",
                parentBeaconId = "other-beacon",
            ).matchesCanonicalV2Occurrence(root),
        )
    }

    private fun occurrence(
        placementId: String,
        beaconId: String,
        parentPlacementId: String?,
        parentBeaconId: String?,
    ) = CanonicalV2CoreLevelOccurrence(
        placementId = PlacementId(placementId),
        beaconPresentationId = beaconId,
        parentPlacementId = parentPlacementId?.let(::PlacementId),
        parentBeaconPresentationId = parentBeaconId,
        placementKind = PlacementKind.PRIMARY,
        siblingOrder = 0L,
        groupPresentationId = null,
    )
}
