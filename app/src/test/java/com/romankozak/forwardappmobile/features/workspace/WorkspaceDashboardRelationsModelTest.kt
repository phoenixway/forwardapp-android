package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import org.junit.Test

class WorkspaceDashboardRelationsModelTest {
    private fun row(type: String, id: String, kinds: List<String>, direction: WorkspaceRelationDirection = WorkspaceRelationDirection.ENTITY_TO_ASPECT) =
        WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, type, id, "Title $id", kinds, direction,
            if (type == "WORKSPACE") NavTarget.Workspace(id) else NavTarget.SemanticEntityEditor(id), type)
    private val source = row("ASPECT", "aspect", listOf("EMBODIES"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)

    @Test fun `preview matches reference with exact linked source and first name plus remaining counts`() {
        val orientations = (1..3).map { row("ORIENTATION", "orientation-$it", listOf("BELONGS_TO")) }
        val child = row("ASPECT", "child", listOf("PARENT_ASPECT"))
        val model = WorkspaceRelationsProjection("workspace", listOf(source), EmbodiedAspectRelations("aspect", "Domain", orientations + child)).dashboardRelations()
        assertThat(model.source).isEqualTo(source)
        assertThat(model.source!!.navigation).isEqualTo(NavTarget.SemanticEntityEditor("aspect"))
        assertThat(model.groups.map { it.title }).containsExactly("Orientations", "Sub-aspects").inOrder()
        assertThat(model.groups.first().count).isEqualTo(3)
        assertThat(model.groups.first().firstTitle).isEqualTo("Title orientation-1")
        assertThat(model.groups.first().moreCount).isEqualTo(2)
        assertThat(model.groups.last().moreCount).isEqualTo(0)
    }
    @Test fun `many groups stay bounded without changing the full projection`() {
        val rows = listOf(row("ORIENTATION", "meaning", listOf("BELONGS_TO")),
            row("ASPECT", "parent", listOf("PARENT_ASPECT"), WorkspaceRelationDirection.ASPECT_TO_ENTITY),
            row("ASPECT", "child", listOf("PARENT_ASPECT")), row("WORKSPACE", "supporter", listOf("SUPPORTS")))
        val projection = WorkspaceRelationsProjection("workspace", listOf(source), EmbodiedAspectRelations("aspect", "Domain", rows))
        val before = projection.relationshipPanel()
        val summary = projection.dashboardRelations()
        assertThat(summary.groups).hasSize(3); assertThat(summary.totalGroups).isEqualTo(4)
        assertThat(projection.relationshipPanel()).isEqualTo(before)
        assertThat(projection.embodiedAspect!!.relations).containsExactlyElementsIn(rows)
    }
    @Test fun `no source or empty workspace never fabricates embodiment`() {
        assertThat(WorkspaceRelationsProjection("workspace", emptyList()).dashboardRelations().source).isNull()
        val related = row("ORIENTATION", "meaning", listOf("SUPPORTS"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)
        val model = WorkspaceRelationsProjection("workspace", listOf(related)).dashboardRelations()
        assertThat(model.source).isNull(); assertThat(model.groups.single().count).isEqualTo(1)
    }
    @Test fun `rename and changed membership are reflected by derived summary`() {
        val child = row("ASPECT", "child", listOf("PARENT_ASPECT"))
        val projection = WorkspaceRelationsProjection("workspace", listOf(source), EmbodiedAspectRelations("aspect", "Domain", listOf(child)))
        assertThat(projection.dashboardRelations().groups.single().firstTitle).isEqualTo("Title child")
        val renamed = projection.copy(embodiedAspect = projection.embodiedAspect!!.copy(relations = listOf(child.copy(title = "Renamed"))))
        assertThat(renamed.dashboardRelations().groups.single().firstTitle).isEqualTo("Renamed")
        assertThat(renamed.copy(embodiedAspect = renamed.embodiedAspect!!.copy(relations = emptyList())).dashboardRelations().groups).isEmpty()
    }
}
