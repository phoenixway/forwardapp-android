package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.bigPictureRemovalConfirmation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Test

class BigPictureRemovalConfirmationTest {
    @Test fun `leaf removal confirms only this appearance not source deletion`() {
        val confirmation = bigPictureRemovalConfirmation(H1TypedNode.Occurrence("leaf"), false)
        assertThat(confirmation.recursive).isFalse()
        assertThat(confirmation.confirmLabel).isEqualTo("Прибрати")
        assertThat(confirmation.title).contains("Navigation")
        assertThat(confirmation.message).contains("інші її появи залишаться")
    }

    @Test fun `non-leaf occurrence offers explicit recursive branch removal preserving canonical sources`() {
        val confirmation = bigPictureRemovalConfirmation(H1TypedNode.Occurrence("link"), true)
        assertThat(confirmation.recursive).isTrue()
        assertThat(confirmation.confirmLabel).isEqualTo("Прибрати гілку")
        assertThat(confirmation.title).contains("Navigation")
        assertThat(confirmation.message).contains("вкладені секції")
        assertThat(confirmation.message).contains("не видаляються")
        assertThat(confirmation.message).contains("поза цією гілкою залишаться")
    }

    @Test fun `container removal preserves direct-child promotion even with children`() {
        val confirmation = bigPictureRemovalConfirmation(H1TypedNode.Container("section"), true)
        assertThat(confirmation.recursive).isFalse()
        assertThat(confirmation.message).contains("залишаться рівнем вище")
        assertThat(confirmation.title).contains("Navigation")
    }

    @Test fun `container-only child selects recursive confirmation from the same typed graph`() {
        val placement = HierarchyPlacement(PlacementId("p"), HierarchyId.GENERAL,
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"), null, PlacementKind.LINK, 0, 1, 1, null, false, 1)
        val read = BigPictureReadComposition(projectBigPicture(listOf(placement), listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"))), emptyList(),
            BigPicturePresentationSnapshot(containers = listOf(BigPictureContainerSnapshot(
                "section", "GENERAL", "Section", "p", null, 0, 1, 1, null, false, 1))))
        val ref = H1TypedNode.Occurrence("p")
        assertThat(read.nodes.single().childCount).isEqualTo(0)
        assertThat(BigPictureStructuralIndex(read).childCount(ref)).isEqualTo(1)
        assertThat(bigPictureRemovalConfirmation(ref, BigPictureStructuralIndex(read).childCount(ref) > 0).recursive).isTrue()
    }

    @Test fun `captured leaf confirmation does not turn recursive when the graph later changes`() {
        val node = H1TypedNode.Occurrence("p")
        val captured = bigPictureRemovalConfirmation(node, false)
        val laterRequest = bigPictureRemovalConfirmation(node, true)
        assertThat(captured.recursive).isFalse()
        assertThat(laterRequest.recursive).isTrue()
    }
}
