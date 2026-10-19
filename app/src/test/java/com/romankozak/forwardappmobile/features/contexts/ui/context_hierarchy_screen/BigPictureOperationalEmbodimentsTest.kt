package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import org.junit.Test

class BigPictureOperationalEmbodimentsTest {
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Meaning")
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Facet")
    private val workspace = CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Operational")
    private val catalog = listOf(orientation, aspect, workspace)

    private fun placement(id: String, target: String = "o", parent: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY, workspace: Boolean = false) = HierarchyPlacement(
        PlacementId(id), HierarchyId.GENERAL,
        HierarchyTargetRef(if (workspace) HierarchyTargetType.WORKSPACE else HierarchyTargetType.MANAGED_SUBJECT, target),
        parent?.let(::PlacementId), kind, 7, 1, 1, null, false, 1,
    )

    private fun binding(subject: String = "o", workspace: String = "w", type: String = "EMBODIES") =
        WorkspaceBindingEntity("$subject-$workspace-$type", workspace, subject, type, true, 0, 1, 1, null, false, 1)

    private fun project(bindings: List<WorkspaceBindingEntity>, entities: List<CanonicalEntitySummary> = catalog) =
        withOperationalEmbodiments(projectBigPicture(listOf(placement("p")), entities), bindings, entities).single()

    @Test fun `unbound semantic keeps normal presentation and exact editor route`() {
        val node = project(emptyList())
        assertThat(node.operationalEmbodiment).isNull()
        assertThat(bigPictureDestination(node)).isEqualTo(NavTarget.SemanticEntityEditor("o"))
    }

    @Test fun `unique embodiment changes primary open but not semantic title class or secondary route`() {
        val node = project(listOf(binding()))
        assertThat(node.entity).isEqualTo(orientation)
        assertThat(node.operationalEmbodiment).isEqualTo(CanonicalEntityRef.Workspace("w"))
        assertThat(bigPictureDestination(node)).isEqualTo(NavTarget.Workspace("w"))
        assertThat(bigPictureSemanticDestination(node)).isEqualTo(NavTarget.SemanticEntityEditor("o"))
    }

    @Test fun `Aspect embodiment uses same contract`() {
        val node = withOperationalEmbodiments(projectBigPicture(listOf(placement("p", "a")), catalog),
            listOf(binding("a")), catalog).single()
        assertThat(bigPictureDestination(node)).isEqualTo(NavTarget.Workspace("w"))
        assertThat(bigPictureSemanticDestination(node)).isEqualTo(NavTarget.SemanticEntityEditor("a"))
    }

    @Test fun `non EMBODIES and tombstoned bindings never redirect`() {
        listOf("REALIZES", "SUPPORTS", "MONITORS").forEach { type ->
            assertThat(project(listOf(binding(type = type))).operationalEmbodiment).isNull()
        }
        assertThat(project(listOf(binding().copy(isDeleted = true))).operationalEmbodiment).isNull()
    }

    @Test fun `unique EMBODIES uses relation kind not optional primary flag`() {
        // The validator enforces EMBODIES uniqueness regardless of isPrimary.
        assertThat(bigPictureDestination(project(listOf(binding().copy(isPrimary = false)))))
            .isEqualTo(NavTarget.Workspace("w"))
    }

    @Test fun `multiple or duplicate embodiments fail closed instead of picking a winner`() {
        val other = CanonicalEntitySummary(CanonicalEntityRef.Workspace("w2"), "Other")
        assertThat(project(listOf(binding(), binding(workspace = "w2")), catalog + other).operationalEmbodiment).isNull()
        assertThat(project(listOf(binding(), binding())).operationalEmbodiment).isNull()
        assertThat(project(listOf(binding(), binding("a"))).operationalEmbodiment).isNull()
    }

    @Test fun `missing ambiguous or unavailable catalog targets fail closed`() {
        assertThat(project(listOf(binding()), listOf(orientation)).operationalEmbodiment).isNull()
        assertThat(project(listOf(binding()), catalog + workspace).operationalEmbodiment).isNull()
        assertThat(project(listOf(binding()), catalog + CanonicalEntitySummary(CanonicalEntityRef.Aspect("o"), "Collision"))
            .operationalEmbodiment).isNull()
    }

    @Test fun `raw ID or title equality does not confer embodiment and Workspace occurrences stay Workspace`() {
        val entities = listOf(orientation, CanonicalEntitySummary(CanonicalEntityRef.Workspace("o"), "Meaning"))
        val nodes = withOperationalEmbodiments(projectBigPicture(listOf(placement("p"), placement("w", "o", workspace = true)), entities),
            listOf(binding(subject = "missing", workspace = "o")), entities)
        assertThat(nodes.map { it.operationalEmbodiment }).containsExactly(null, null)
        assertThat(nodes.map(::bigPictureDestination)).containsExactly(NavTarget.SemanticEntityEditor("o"), NavTarget.Workspace("o")).inOrder()
    }

    @Test fun `PRIMARY LINK ancestry order and exact occurrence focus survive decoration`() {
        val original = projectBigPicture(listOf(placement("primary"), placement("link", kind = PlacementKind.LINK), placement("child", "a", "link")), catalog)
        val decorated = withOperationalEmbodiments(original, listOf(binding()), catalog)
        assertThat(decorated.map { it.copy(operationalEmbodiment = null) }).isEqualTo(original)
        assertThat(decorated.filter { it.entity?.ref == orientation.ref }.map(::bigPictureDestination))
            .containsExactly(NavTarget.Workspace("w"), NavTarget.Workspace("w"))
        assertThat(focusBigPicture(decorated, null).children.map { it.placement.id.value }).containsExactly("primary", "link")
        assertThat(focusBigPicture(decorated, "link").children.single().placement.id.value).isEqualTo("child")
        assertThat(focusBigPicture(decorated, "child").breadcrumbs.map { it.placement.id.value }).containsExactly("link", "child").inOrder()
    }

    @Test fun `source deletion resolver remains semantic while primary open is operational`() {
        val node = project(listOf(binding()))
        val semantic = BigPictureSourceDeleteTarget(orientation.ref, "Meaning", 1)
        val operational = BigPictureSourceDeleteTarget(workspace.ref, "Operational", 1)
        assertThat(bigPictureDestination(node)).isEqualTo(NavTarget.Workspace("w"))
        assertThat(sourceDeleteTarget(node, mapOf(orientation.ref to semantic, workspace.ref to operational))).isEqualTo(semantic)
    }
}
