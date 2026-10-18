package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Assert.assertThrows
import org.junit.Test

class BigPictureContainersTest {
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Orientation"),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Aspect"),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
    )

    @Test fun `every canonical class may be nested below every canonical class`() {
        for (parent in catalog) for (child in catalog) {
            val root = p("parent", parent.ref)
            val nested = p("child", child.ref, "parent").copy(
                placementKind = if (parent.ref == child.ref) PlacementKind.LINK else PlacementKind.PRIMARY,
            )
            val before = listOf(root, nested)
            val nodes = projectBigPicture(before, catalog)
            assertThat(focusBigPicture(nodes, "parent").children.single().entity?.ref).isEqualTo(child.ref)
            assertThat(nodes.map { it.placement }).containsExactlyElementsIn(before).inOrder()
        }
    }

    @Test fun `generic read model accepts mixed container membership without semantic group identity`() {
        val placements = catalog.map { p(it.ref.id, it.ref) } +
            p("link", catalog.first().ref).copy(placementKind = PlacementKind.LINK, siblingOrder = 1)
        val nodes = projectBigPicture(placements, catalog)
        val section = BigPictureContainer("section-id", "WIP",
            setOf(PlacementId("o"), PlacementId("a"), PlacementId("w")), BigPictureContainerOrigin.PRESENTATION)
        assertThat(bigPictureRootOccurrences(nodes, listOf(section)).map { it.placement.id.value }).containsExactly("link")
        assertThat(nodes.filter { it.placement.id in section.rootPlacementIds }.map { it.entity?.ref })
            .containsExactlyElementsIn(catalog.map { it.ref })
        assertThat(bigPictureRootOccurrences(nodes, emptyList())).hasSize(4)
        assertThat(nodes.filter { it.entity?.ref == catalog.first().ref }).hasSize(2)
        assertThat(section.title).isEqualTo("WIP")
        assertThat(catalog).hasSize(3) // A presentation section is not a Library entity.
    }

    @Test fun `container assignment protects occurrence identity without restricting target class`() {
        val nodes = projectBigPicture(listOf(p("root", catalog[0].ref), p("child", catalog[1].ref, "root")), catalog)
        fun section(id: String, placement: String) = BigPictureContainer(id, id,
            setOf(PlacementId(placement)), BigPictureContainerOrigin.PRESENTATION)
        assertThrows(IllegalArgumentException::class.java) {
            bigPictureRootOccurrences(nodes, listOf(section("s", "missing")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            bigPictureRootOccurrences(nodes, listOf(section("s", "child")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            bigPictureRootOccurrences(nodes, listOf(section("s", "root"), section("t", "root")))
        }
    }

    private fun p(id: String, ref: CanonicalEntityRef, parent: String? = null) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(if (ref is CanonicalEntityRef.Workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, ref.id),
        parent?.let(::PlacementId), PlacementKind.PRIMARY, 0, 1, 1, null, false, 1,
    )
}
