package com.romankozak.forwardappmobile.data.hierarchy

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Assert.assertThrows
import org.junit.Test

class BigPicturePresentationOrderTest {
    @Test fun `containers and uncontained occurrences share explicit order with stable typed ties`() {
        val state = BigPicturePresentationSnapshot(containers = listOf(c("last", order = 3), c("section", order = 1)))
        val placements = listOf(p("middle", order = 2), p("section", order = 1))
        assertThat(bigPicturePresentationEntries(state, placements).map { it.id })
            .containsExactly("section", "section", "middle", "last").inOrder()
        assertThat(bigPicturePresentationEntries(state, placements).take(2)).containsExactly(
            BigPicturePresentationEntry.Container("section", 1), BigPicturePresentationEntry.Occurrence("section", 1),
        ).inOrder()
    }

    @Test fun `sections wrap arbitrary depth siblings and may nest without changing H1`() {
        val placements = listOf(p("root"), p("child", parent = "root"))
        val state = BigPicturePresentationSnapshot(
            containers = listOf(c("a", anchor = "root"), c("b", anchor = "root", parent = "a")),
            memberships = listOf(m("child", "b")),
        )
        assertThat(bigPicturePresentationEntries(state, placements).map { it.id }).containsExactly("root")
        assertThat(bigPicturePresentationEntries(state, placements, "root").map { it.id }).containsExactly("a")
        assertThat(bigPicturePresentationEntries(state, placements, "root", "a").map { it.id }).containsExactly("b")
        assertThat(bigPicturePresentationEntries(state, placements, "root", "b").map { it.id }).containsExactly("child")
        assertThat(placements.last().parentPlacementId).isEqualTo(PlacementId("root"))
        assertThrows(IllegalArgumentException::class.java) {
            validateBigPicturePresentation(state.copy(containers = state.containers.map { it.copy(anchorPlacementId = null) }), placements)
        }
    }

    @Test fun `deleted section leaves live occurrence uncontained without deleting membership history`() {
        val state = BigPicturePresentationSnapshot(listOf(c("gone").copy(isDeleted = true)), listOf(m("p", "gone")))
        assertThat(bigPicturePresentationEntries(state, listOf(p("p"))).single())
            .isEqualTo(BigPicturePresentationEntry.Occurrence("p", 0))
        assertThat(state.memberships.single().isDeleted).isFalse()
    }

    private fun c(id: String, anchor: String? = null, parent: String? = null, order: Long = 0) =
        BigPictureContainerSnapshot(id, "GENERAL", id, anchor, parent, order, 1, 1, null, false, 1)
    private fun m(id: String, container: String) = BigPictureMembershipSnapshot(id, "GENERAL", container, 1, 1, null, false, 1)
    private fun p(id: String, parent: String? = null, order: Long = 0) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL, HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id),
        parent?.let(::PlacementId), PlacementKind.PRIMARY, order, 1, 1, null, false, 1,
    )
}
