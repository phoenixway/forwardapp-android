package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import org.junit.Test

class WorkspaceRelationsPanelModelTest {
    private fun row(type: String, id: String, kinds: List<String>, direction: WorkspaceRelationDirection = WorkspaceRelationDirection.ENTITY_TO_ASPECT) =
        WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, type, id, "Title $id", kinds, direction,
            if (type == "WORKSPACE") NavTarget.Workspace(id) else NavTarget.SemanticEntityEditor(id), type)
    private val source = row("ASPECT", "source", listOf("EMBODIES"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)

    @Test fun `Aspect panel presents exact source and counted semantic groups without replacing navigation identities`() {
        val orientation = row("ORIENTATION", "orientation", listOf("BELONGS_TO"))
        val child = row("ASPECT", "child", listOf("PARENT_ASPECT"))
        val parent = row("ASPECT", "parent", listOf("PARENT_ASPECT"), WorkspaceRelationDirection.ASPECT_TO_ENTITY)
        val workspace = row("WORKSPACE", "supporter", listOf("SUPPORTS"))
        val projection = WorkspaceRelationsProjection("workspace", listOf(source), EmbodiedAspectRelations("source", "Domain", listOf(orientation, child, parent, workspace)))
        val model = projection.relationshipPanel()
        assertThat(model.source).isEqualTo(source)
        assertThat(model.structureLabel).isEqualTo("Aspect structure")
        assertThat(model.groups.map { it.label }).containsAtLeast("Orientations", "Sub-aspects", "Parent Aspect", "Workspaces")
        assertThat(model.groups.flatMap { it.rows }).containsExactly(orientation, child, parent, workspace)
        assertThat(model.groups.flatMap { it.rows }.map { it.navigation }).containsExactlyElementsIn(listOf(orientation, child, parent, workspace).map { it.navigation })
    }
    @Test fun `Orientation source uses the same panel without inventing an Aspect tree`() {
        val source = row("ORIENTATION", "meaning", listOf("EMBODIES"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)
        val membership = row("ASPECT", "aspect", listOf("BELONGS_TO"), WorkspaceRelationDirection.ORIENTATION_TO_ENTITY)
        val projection = WorkspaceRelationsProjection("workspace", listOf(source), embodiedOrientation = EmbodiedOrientationRelations("meaning", "Meaning", listOf(membership)))
        val model = projection.relationshipPanel()
        assertThat(model.source).isEqualTo(source); assertThat(model.structureLabel).isEqualTo("Orientation relations")
        assertThat(model.groups.single().label).isEqualTo("Aspects")
    }
    @Test fun `no verified embodiment does not synthesize a source and preserves every direct relation`() {
        val direct = row("ORIENTATION", "meaning", listOf("SUPPORTS"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)
        val model = WorkspaceRelationsProjection("workspace", listOf(direct)).relationshipPanel()
        assertThat(model.source).isNull(); assertThat(model.structureLabel).isEqualTo("Workspace relations")
        assertThat(model.groups.flatMap { it.rows }).containsExactly(direct)
        assertThat(WorkspaceRelationsProjection("workspace", emptyList()).relationshipPanel().groups).isEmpty()
    }
    @Test fun `same raw ID across types and multiple relation kinds remain distinct and directional`() {
        val orientation = row("ORIENTATION", "same", listOf("BELONGS_TO", "RELEVANT_TO"))
        val workspace = row("WORKSPACE", "same", listOf("SUPPORTS", "MONITORS"))
        val projection = WorkspaceRelationsProjection("workspace", listOf(source), EmbodiedAspectRelations("source", "Domain", listOf(orientation, workspace)))
        assertThat(projection.relationshipPanel().groups.flatMap { it.rows }).containsExactly(orientation, workspace)
        assertThat(orientation.panelRelationLabel()).isEqualTo("← belongs to · relevant to")
        assertThat(workspace.panelRelationLabel()).isEqualTo("← supports · monitors")
    }
    @Test fun `unavailable Planning row survives as non-navigable without fake destination`() {
        val planning = WorkspaceRelationItem(WorkspaceRelationFamily.PLANNING, "TACTICAL_MISSION", "mission", "Mission", listOf("SOURCE"),
            WorkspaceRelationDirection.ENTITY_TO_WORKSPACE, null, "Mission")
        val rows = WorkspaceRelationsProjection("workspace", listOf(planning)).relationshipPanel().groups.flatMap { it.rows }
        assertThat(rows).containsExactly(planning); assertThat(rows.single().navigation).isNull()
        assertThat(rows.single().panelRelationLabel()).isEqualTo("← source")
    }

    @Test fun `Orientation relations explain both directions relative to exact source title`() {
        val labels = mapOf(
            "PART_OF" to ("Містить" to "Частина"),
            "SUPPORTS" to ("Отримує підтримку від" to "Підтримує"),
            "REALIZES" to ("Реалізується через" to "Реалізує"),
            "DEPENDS_ON" to ("Потрібний для" to "Залежить від"),
            "PRECEDES" to ("Після" to "Передує"),
            "REFINES" to ("Уточнюється через" to "Уточнює"),
            "DERIVED_FROM" to ("Основа для" to "Походить від"),
        )
        labels.forEach { (kind, verbs) ->
            val outgoing = row("ORIENTATION", "other", listOf(kind), WorkspaceRelationDirection.ORIENTATION_TO_ENTITY)
            val incoming = outgoing.copy(direction = WorkspaceRelationDirection.ENTITY_TO_ORIENTATION)
            assertThat(outgoing.panelRelationLabel("Meaning")).isEqualTo("${verbs.first} «Meaning»")
            assertThat(incoming.panelRelationLabel("Meaning")).isEqualTo("${verbs.second} «Meaning»")
        }
    }

    @Test fun `mutual relation and multiple kinds stay concise without direction arrows`() {
        val mutual = row("ORIENTATION", "other", listOf("CONFLICTS_WITH"), WorkspaceRelationDirection.ORIENTATION_MUTUAL)
        assertThat(mutual.panelRelationLabel("Meaning")).isEqualTo("Конфліктує з «Meaning»")
        val multiple = mutual.copy(kinds = listOf("SUPPORTS", "REFINES"), direction = WorkspaceRelationDirection.ENTITY_TO_ORIENTATION)
        assertThat(multiple.panelRelationLabel("Meaning")).isEqualTo("Підтримує «Meaning» · Уточнює «Meaning»")
    }

    @Test fun `Aspect membership names the embodied Orientation instead of ambiguous relevant to arrow`() {
        val aspect = row("ASPECT", "aspect", listOf("BELONGS_TO", "RELEVANT_TO"), WorkspaceRelationDirection.ORIENTATION_TO_ENTITY)
        assertThat(aspect.panelRelationLabel("Meaning")).isEqualTo(
            "«Meaning» належить до цього аспекту · «Meaning» стосується цього аспекту")
    }

    @Test fun `presentation wording preserves exact navigation identity and unknown vocabulary`() {
        val relation = row("ORIENTATION", "id", listOf("FUTURE_KIND"), WorkspaceRelationDirection.ORIENTATION_TO_ENTITY)
        assertThat(relation.panelRelationLabel("Meaning")).isEqualTo("FUTURE_KIND")
        assertThat(relation.navigation).isEqualTo(NavTarget.SemanticEntityEditor("id"))
        assertThat(relation.kinds).containsExactly("FUTURE_KIND")
    }
}
