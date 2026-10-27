package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Test

class BigPictureCardActivationTest {
    private fun node(ref: CanonicalEntityRef, kind: PlacementKind = PlacementKind.PRIMARY) = BigPictureNode(
        HierarchyPlacement(PlacementId("exact-appearance"), HierarchyId.GENERAL,
            HierarchyTargetRef(if (ref is CanonicalEntityRef.Workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, ref.id),
            null, kind, 0, 1, 1, null, false, 1),
        CanonicalEntitySummary(ref, "Title"), 0, 0,
    )

    @Test fun `leaf Orientation Aspect and Workspace open exact canonical destination without focus`() {
        listOf(CanonicalEntityRef.Orientation("o"), CanonicalEntityRef.Aspect("a"), CanonicalEntityRef.Workspace("w")).forEach { ref ->
            val node = node(ref)
            var opened: BigPictureNode? = null
            activateBigPictureOccurrence(node, 0, { error("Leaf must not focus") }, { opened = it })
            assertThat(opened).isEqualTo(node)
            assertThat(bigPictureDestination(opened!!)).isEqualTo(
                if (ref is CanonicalEntityRef.Workspace) NavTarget.Workspace(ref.id) else NavTarget.SemanticEntityEditor(ref.id))
        }
    }

    @Test fun `mixed child count keeps focus even when occurrence child count is zero`() {
        val node = node(CanonicalEntityRef.Workspace("w"))
        var focus: String? = null
        activateBigPictureOccurrence(node, 1, { focus = it }, { error("Parent must focus") })
        assertThat(focus).isEqualTo("exact-appearance")
    }

    @Test fun `leaf LINK preserves exact occurrence and adaptive primary destination`() {
        val node = node(CanonicalEntityRef.Aspect("a"), PlacementKind.LINK)
            .copy(operationalEmbodiment = CanonicalEntityRef.Workspace("embodiment"))
        activateBigPictureOccurrence(node, 0, { error("Leaf must not focus") }, {
            assertThat(it).isEqualTo(node)
            assertThat(bigPictureDestination(it)).isEqualTo(NavTarget.Workspace("embodiment"))
            assertThat(bigPictureSemanticDestination(it)).isEqualTo(NavTarget.SemanticEntityEditor("a"))
        })
    }
}
