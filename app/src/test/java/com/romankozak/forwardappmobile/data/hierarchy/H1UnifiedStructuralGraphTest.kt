package com.romankozak.forwardappmobile.data.hierarchy

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Assert.assertThrows
import org.junit.Test

class H1UnifiedStructuralGraphTest {
    private fun p(id: String, parent: String? = null, order: Long = 0) =
        HierarchyPlacement(
            PlacementId(id), HierarchyId.GENERAL,
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id),
            parent?.let(::PlacementId), PlacementKind.PRIMARY,
            order, 1, 1, null, false, 1,
        )

    private fun c(id: String, anchor: String? = null, parent: String? = null, order: Long = 0) =
        BigPictureContainerSnapshot(id, "GENERAL", id, anchor, parent, order, 1, 1, null, false, 1)

    private fun m(id: String, container: String) =
        BigPictureMembershipSnapshot(id, "GENERAL", container, 1, 1, null, false, 1)

    @Test fun `mixed occurrence and container graph preserves exact typed parents`() {
        val placements = listOf(p("a"), p("b", "a"), p("c"))
        val state = BigPicturePresentationSnapshot(
            containers = listOf(c("x", "a"), c("y", "a", "x"), c("root")),
            memberships = listOf(m("b", "y"), m("c", "root")),
        )
        val graph = projectH1UnifiedStructuralGraph(placements, state)

        assertThat(graph.parentOf(H1TypedNode.Occurrence("a"))).isNull()
        assertThat(graph.parentOf(H1TypedNode.Container("x")))
            .isEqualTo(H1TypedNode.Occurrence("a"))
        assertThat(graph.parentOf(H1TypedNode.Container("y")))
            .isEqualTo(H1TypedNode.Container("x"))
        assertThat(graph.parentOf(H1TypedNode.Occurrence("b")))
            .isEqualTo(H1TypedNode.Container("y"))
        assertThat(graph.parentOf(H1TypedNode.Occurrence("c")))
            .isEqualTo(H1TypedNode.Container("root"))
        assertThat(graph.nearestOccurrenceAncestor(H1TypedNode.Occurrence("b"))).isEqualTo("a")
        assertThat(graph.nearestOccurrenceAncestor(H1TypedNode.Occurrence("c"))).isNull()
    }

    @Test fun `root mixed siblings alternate in one deterministic order`() {
        val graph = projectH1UnifiedStructuralGraph(
            listOf(p("a", order = 1), p("b", order = 3)),
            BigPicturePresentationSnapshot(containers = listOf(c("x", order = 0), c("y", order = 2))),
        )
        assertThat(graph.childrenOf(null).map { it.node }).containsExactly(
            H1TypedNode.Container("x"),
            H1TypedNode.Occurrence("a"),
            H1TypedNode.Container("y"),
            H1TypedNode.Occurrence("b"),
        ).inOrder()
    }

    @Test fun `typed identities cannot alias each other`() {
        val graph = projectH1UnifiedStructuralGraph(
            listOf(p("same")), BigPicturePresentationSnapshot(containers = listOf(c("same"))),
        )
        assertThat(graph.childrenOf(null)).hasSize(2)
    }

    @Test fun `same occurrence can move between typed parents retaining descendant ancestry`() {
        val a = p("a")
        val d = p("d")
        val b = p("b", "a")
        val child = p("child", "b")
        val containers = listOf(
            c("x", "a"), c("y", "a"), c("z", "b"), c("w", "d"),
        )
        fun graph(moved: HierarchyPlacement, membership: BigPictureMembershipSnapshot?) =
            projectH1UnifiedStructuralGraph(
                listOf(a, d, moved, child),
                BigPicturePresentationSnapshot(
                    containers = containers,
                    memberships = listOfNotNull(membership),
                ),
            )

        val insideX = graph(b, m("b", "x"))
        assertThat(insideX.parentOf(H1TypedNode.Occurrence("b")))
            .isEqualTo(H1TypedNode.Container("x"))
        val insideY = graph(b.copy(siblingOrder = 4), m("b", "y"))
        assertThat(insideY.parentOf(H1TypedNode.Occurrence("b")))
            .isEqualTo(H1TypedNode.Container("y"))
        val underD = graph(b.copy(parentPlacementId = PlacementId("d")), null)
        assertThat(underD.parentOf(H1TypedNode.Occurrence("b")))
            .isEqualTo(H1TypedNode.Occurrence("d"))
        val insideW = graph(b.copy(parentPlacementId = PlacementId("d")), m("b", "w"))
        assertThat(insideW.parentOf(H1TypedNode.Occurrence("b")))
            .isEqualTo(H1TypedNode.Container("w"))
        val outside = graph(b, null)
        assertThat(outside.parentOf(H1TypedNode.Occurrence("b")))
            .isEqualTo(H1TypedNode.Occurrence("a"))

        for (result in listOf(insideX, insideY, underD, insideW, outside)) {
            assertThat(result.parentOf(H1TypedNode.Occurrence("child")))
                .isEqualTo(H1TypedNode.Occurrence("b"))
            assertThat(result.parentOf(H1TypedNode.Container("z")))
                .isEqualTo(H1TypedNode.Occurrence("b"))
        }
    }

    @Test fun `invalid live anchors and missing membership containers fail closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            projectH1UnifiedStructuralGraph(
                listOf(p("a").copy(isDeleted = true)),
                BigPicturePresentationSnapshot(containers = listOf(c("x", "a"))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            projectH1UnifiedStructuralGraph(
                listOf(p("a")),
                BigPicturePresentationSnapshot(
                    containers = emptyList(),
                    memberships = listOf(m("a", "x")),
                ),
            )
        }
    }

    @Test fun `policy B retained membership falls back to occurrence ancestry without record loss`() {
        val retainedMembership = m("child", "retired").copy(
            updatedAt = 25, syncedAt = 24, version = 7,
        )
        val retiredContainer = c("retired", anchor = "root").copy(
            isDeleted = true, updatedAt = 20, syncedAt = 19, version = 5,
        )
        val history = BigPicturePresentationSnapshot(
            containers = listOf(retiredContainer),
            memberships = listOf(retainedMembership),
        )

        val graph = projectH1UnifiedStructuralGraph(
            listOf(p("root"), p("child", parent = "root")),
            history,
        )

        assertThat(graph.parentOf(H1TypedNode.Occurrence("child")))
            .isEqualTo(H1TypedNode.Occurrence("root"))
        assertThat(graph.childrenOf(H1TypedNode.Occurrence("root")).map { it.node })
            .containsExactly(H1TypedNode.Occurrence("child"))
        assertThat(graph.edges.map { it.node })
            .doesNotContain(H1TypedNode.Container("retired"))

        // Projection must never edit the original wire/ACK history.
        assertThat(history.memberships).containsExactly(retainedMembership)
        assertThat(history.containers).containsExactly(retiredContainer)
        assertThat(retainedMembership.isDeleted).isFalse()
    }

    @Test fun `policy B root membership to retired container falls back to root`() {
        val history = BigPicturePresentationSnapshot(
            containers = listOf(c("retired").copy(isDeleted = true)),
            memberships = listOf(m("root", "retired")),
        )
        val graph = projectH1UnifiedStructuralGraph(listOf(p("root")), history)

        assertThat(graph.parentOf(H1TypedNode.Occurrence("root"))).isNull()
        assertThat(graph.childrenOf(null).map { it.node })
            .containsExactly(H1TypedNode.Occurrence("root"))
        assertThat(history.memberships.single().containerId).isEqualTo("retired")
    }

    @Test fun `deleted membership and retired occurrence do not create live structural edges`() {
        val history = BigPicturePresentationSnapshot(
            containers = listOf(c("active")),
            memberships = listOf(
                m("live", "active").copy(isDeleted = true, version = 2),
                m("retired", "active"),
            ),
        )
        val graph = projectH1UnifiedStructuralGraph(
            listOf(p("live"), p("retired").copy(isDeleted = true)),
            history,
        )

        assertThat(graph.parentOf(H1TypedNode.Occurrence("live"))).isNull()
        assertThat(graph.edges.map { it.node })
            .doesNotContain(H1TypedNode.Occurrence("retired"))
        assertThat(history.memberships).hasSize(2)
    }

    @Test fun `live nested container beneath retired container fails closed`() {
        val invalid = BigPicturePresentationSnapshot(
            containers = listOf(
                c("retired").copy(isDeleted = true),
                c("live-child", parent = "retired"),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            projectH1UnifiedStructuralGraph(emptyList(), invalid)
        }
    }

    @Test fun `occurrence and container structural cycles fail closed`() {
        assertThrows(IllegalArgumentException::class.java) {
            projectH1UnifiedStructuralGraph(
                listOf(p("a", "b"), p("b", "a")),
                BigPicturePresentationSnapshot(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            projectH1UnifiedStructuralGraph(
                emptyList(),
                BigPicturePresentationSnapshot(
                    containers = listOf(c("x", parent = "y"), c("y", parent = "x")),
                ),
            )
        }
    }
}
