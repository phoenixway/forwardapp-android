package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Assert.assertThrows
import org.junit.Test

class BigPictureScopesTest {
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("group"), "My group"),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("beacon"), "My beacon"),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
    )

    @Test fun `authored groups contain exact roots and NoBeacon contains only Workspace roots`() {
        val structure = BigPictureStructure(listOf(
            p("b1", "beacon"),
            p("b2", "beacon").copy(placementKind = PlacementKind.LINK),
            p("nested", "w", "b1", true),
            p("free", "w", workspace = true).copy(placementKind = PlacementKind.LINK),
        ), mapOf(PlacementId("b1") to "group", PlacementId("b2") to "group"), mapOf("group" to 2))
        val nodes = projectBigPicture(structure.placements, catalog)
        val scopes = bigPictureScopes(structure, nodes, catalog)
        assertThat(scopes.map { it.title }).containsExactly("My group", "NoBeacon").inOrder()
        assertThat(scopes.first().rootPlacementIds.map { it.value }).containsExactly("b1", "b2")
        assertThat(scopes.last().rootPlacementIds.map { it.value }).containsExactly("free")
        assertThat(focusBigPicture(nodes, "b1").children.map { it.placement.id.value }).containsExactly("nested")
        assertThat(bigPictureDestination(nodes.single { it.placement.id.value == "nested" }))
            .isEqualTo(NavTarget.Workspace("w"))
        assertThat(bigPictureDestination(nodes.single { it.placement.id.value == "b1" }))
            .isEqualTo(NavTarget.SemanticEntityEditor("beacon"))
    }

    @Test fun `group order and empty groups survive without authoring placements`() {
        val structure = BigPictureStructure(emptyList(), groupOrder = mapOf("group" to 2, "earlier" to 1))
        val groups = bigPictureScopes(structure, emptyList(), catalog +
            CanonicalEntitySummary(CanonicalEntityRef.Orientation("earlier"), "Earlier"))
        assertThat(groups.map { it.title }).containsExactly("Earlier", "My group").inOrder()
        assertThat(groups.flatMap { it.rootPlacementIds }).isEmpty()
    }

    @Test fun `NoGroup uses explicit provenance not semantic membership inference`() {
        val structure = BigPictureStructure(listOf(p("b", "beacon")), mapOf(PlacementId("b") to null))
        val scopes = bigPictureScopes(structure, projectBigPicture(structure.placements, catalog), catalog)
        assertThat(scopes.single().id).isEqualTo(CANONICAL_V2_NO_GROUP_SCOPE_ID)
    }

    @Test fun `unscoped roots stay visible without inferred Group or NoGroup membership`() {
        val structure = BigPictureStructure(listOf(p("b", "beacon")))
        val nodes = projectBigPicture(structure.placements, catalog)
        val scopes = bigPictureScopes(structure, nodes, catalog)
        assertThat(scopes).isEmpty()
        assertThat(bigPictureRootOccurrences(nodes, scopes).map { it.placement.id.value }).containsExactly("b")
    }

    @Test fun `historical provenance cannot be reinterpreted as generic Workspace membership`() {
        val structure = BigPictureStructure(listOf(p("w", "w", workspace = true)), mapOf(PlacementId("w") to "group"))
        assertThrows(IllegalArgumentException::class.java) {
            bigPictureScopes(structure, projectBigPicture(structure.placements, catalog), catalog)
        }
    }

    @Test fun `compatibility groups are distinguished from presentation only containers`() {
        val structure = BigPictureStructure(listOf(p("b", "beacon")), mapOf(PlacementId("b") to "group"))
        val scopes = bigPictureScopes(structure, projectBigPicture(structure.placements, catalog), catalog)
        assertThat(scopes.single().origin).isEqualTo(BigPictureContainerOrigin.HISTORICAL_GROUP)
        assertThat(scopes.single().id).isEqualTo("group:group")
    }

    private fun p(id: String, target: String, parent: String? = null, workspace: Boolean = false) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(if (workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, target),
        parent?.let(::PlacementId), PlacementKind.PRIMARY, 0, 1, 1, null, false, 1,
    )
}
