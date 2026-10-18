package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Assert.assertThrows
import org.junit.Test

class BigPictureProjectionTest {
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Orientation"),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Aspect"),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
    )

    @Test fun `mixed canonical entities follow placement nesting and navigate by target identity`() {
        val nodes = projectBigPicture(listOf(p("po", "o"), p("pa", "a", "po"), p("pw", "w", "pa", workspace = true)), catalog)
        assertThat(nodes.map { it.depth }).containsExactly(0, 1, 2).inOrder()
        assertThat(nodes.map { it.childCount }).containsExactly(1, 1, 0).inOrder()
        assertThat(nodes.map { it.entity?.title }).containsExactly("Orientation", "Aspect", "Workspace").inOrder()
        assertThat(nodes.map(::bigPictureDestination)).containsExactly(
            NavTarget.SemanticEntityEditor("o"), NavTarget.SemanticEntityEditor("a"), NavTarget.Workspace("w"),
        ).inOrder()
    }

    @Test fun `PRIMARY and LINK for same target remain independent occurrences and subtrees`() {
        val nodes = projectBigPicture(listOf(
            p("primary", "o"), p("link", "o", order = 1, kind = PlacementKind.LINK),
            p("child", "a", "link"),
        ), catalog)
        assertThat(nodes.map { it.placement.id.value }).containsExactly("primary", "link", "child").inOrder()
        assertThat(nodes.take(2).map { it.placement.placementKind }).containsExactly(PlacementKind.PRIMARY, PlacementKind.LINK).inOrder()
        assertThat(bigPictureDestination(nodes[0])).isEqualTo(bigPictureDestination(nodes[1]))
        assertThat(nodes[2].placement.parentPlacementId).isEqualTo(PlacementId("link"))
    }

    @Test fun `typed target resolution survives raw ID collisions`() {
        val collision = listOf(
            CanonicalEntitySummary(CanonicalEntityRef.Aspect("same"), "Aspect title"),
            CanonicalEntitySummary(CanonicalEntityRef.Workspace("same"), "Workspace title"),
        )
        val nodes = projectBigPicture(listOf(p("a", "same"), p("w", "same", workspace = true)), collision)
        assertThat(nodes.map(::bigPictureDestination)).containsExactly(NavTarget.SemanticEntityEditor("same"), NavTarget.Workspace("same")).inOrder()
        assertThat(nodes.map { it.entity?.title }).containsExactly("Aspect title", "Workspace title").inOrder()
    }

    @Test fun `multiple roots and siblings use order then placement id`() {
        val nodes = projectBigPicture(listOf(
            p("r2", "o", order = 2), p("r1", "w", order = 1, workspace = true),
            p("b", "a", "r1", order = 2), p("a", "o2", "r1", order = 2),
        ), catalog)
        assertThat(nodes.map { it.placement.id.value }).containsExactly("r1", "a", "b", "r2").inOrder()
    }

    @Test fun `deep hierarchy projects iteratively`() {
        val chain = (0..250).map { i -> p("p$i", "s$i", if (i == 0) null else "p${i - 1}") }
        val nodes = projectBigPicture(chain.reversed(), emptyList())
        assertThat(nodes).hasSize(251)
        assertThat(nodes.last().depth).isEqualTo(250)
    }

    @Test fun `tombstones and other hierarchy scopes do not enter GENERAL`() {
        val nodes = projectBigPicture(listOf(
            p("live", "o"), p("deleted", "a").copy(isDeleted = true),
            p("other", "w", workspace = true).copy(hierarchyId = HierarchyId("OTHER")),
        ), catalog)
        assertThat(nodes.map { it.placement.id.value }).containsExactly("live")
    }

    @Test fun `missing ambiguous or duplicate catalog targets cannot navigate`() {
        val ambiguous = catalog + CanonicalEntitySummary(CanonicalEntityRef.Aspect("o"), "Wrong class")
        assertThat(bigPictureDestination(projectBigPicture(listOf(p("p", "o")), ambiguous).single())).isNull()
        assertThat(bigPictureDestination(projectBigPicture(listOf(p("p", "missing")), catalog).single())).isNull()
        assertThat(bigPictureDestination(projectBigPicture(listOf(p("p", "o")), catalog + catalog.first()).single())).isNull()
    }

    @Test fun `invalid topology fails closed using canonical validator`() {
        listOf(
            listOf(p("orphan", "o", "missing")),
            listOf(p("duplicate", "o"), p("duplicate", "a")),
            listOf(p("one", "o", "two"), p("two", "a", "one")),
            listOf(p("one", "o"), p("two", "o")),
        ).forEach { invalid -> assertThrows(IllegalArgumentException::class.java) { projectBigPicture(invalid, catalog) } }
    }

    @Test fun `focus shows only roots or direct children with occurrence specific breadcrumbs`() {
        val nodes = projectBigPicture(listOf(
            p("primary", "o"), p("child", "a", "primary"),
            p("link", "o", order = 1, kind = PlacementKind.LINK), p("work", "w", "link", workspace = true),
        ), catalog)
        val roots = focusBigPicture(nodes, null)
        assertThat(roots.activeNode).isNull()
        assertThat(roots.breadcrumbs).isEmpty()
        assertThat(roots.children.map { it.placement.id.value }).containsExactly("primary", "link").inOrder()
        val primary = focusBigPicture(nodes, "primary")
        assertThat(primary.children.map { it.placement.id.value }).containsExactly("child")
        val link = focusBigPicture(nodes, "link")
        assertThat(link.children.map { it.placement.id.value }).containsExactly("work")
        val leaf = focusBigPicture(nodes, "work")
        assertThat(leaf.activeNode?.placement?.id?.value).isEqualTo("work")
        assertThat(leaf.children).isEmpty()
        assertThat(leaf.breadcrumbs.map { it.placement.id.value }).containsExactly("link", "work").inOrder()
    }

    @Test fun `grandchildren never appear in root or parent child lists`() {
        val nodes = projectBigPicture(listOf(p("root", "o"), p("child", "a", "root"), p("grandchild", "w", "child", workspace = true)), catalog)
        assertThat(focusBigPicture(nodes, null).children.map { it.placement.id.value }).containsExactly("root")
        assertThat(focusBigPicture(nodes, "root").children.map { it.placement.id.value }).containsExactly("child")
        assertThat(focusBigPicture(nodes, "grandchild").breadcrumbs.map { it.placement.id.value })
            .containsExactly("root", "child", "grandchild").inOrder()
        assertThat(focusBigPicture(nodes, "removed").children.map { it.placement.id.value }).containsExactly("root")
    }

    @Test fun `existing inventory does not populate an empty Big Picture`() {
        assertThat(projectBigPicture(emptyList(), catalog)).isEmpty()
    }

    private fun p(
        id: String, target: String, parent: String? = null, order: Long = 0,
        workspace: Boolean = false, kind: PlacementKind = PlacementKind.PRIMARY,
    ) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(if (workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, target),
        parent?.let(::PlacementId), kind, order, 1, 1, null, false, 1,
    )
}
