package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import org.junit.Test

class WorkspaceEntityHubTest {
    private fun row(id: String, type: String = "ORIENTATION", kinds: List<String> = listOf("BELONGS_TO"),
        direction: WorkspaceRelationDirection = WorkspaceRelationDirection.ENTITY_TO_ASPECT) =
        WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, type, id, "Title $id", kinds, direction,
            if (type == "WORKSPACE") NavTarget.Workspace(id) else NavTarget.SemanticEntityEditor(id), type)
    private val source = row("a", "ASPECT", listOf("EMBODIES"), WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)
    private fun projection(rows: List<WorkspaceRelationItem>) = WorkspaceRelationsProjection("w", listOf(source),
        EmbodiedAspectRelations("a", "Health", rows))

    @Test fun `semantic source and its relations stay separate from Workspace owned edges`() {
        val related = row("o")
        val direct = row("d", kinds = listOf("SUPPORTS"), direction = WorkspaceRelationDirection.WORKSPACE_TO_ENTITY)
        val groups = projection(listOf(related)).copy(relations = listOf(source, direct)).entityHubGroups()
        assertThat(groups.first().rows).containsExactly(source)
        assertThat(groups.single { related in it.rows }.label).contains("через Health")
        assertThat(groups.last().rows).containsExactly(direct)
        assertThat(groups.flatMap { it.rows }).containsExactly(source, related, direct)
    }

    @Test fun `taxonomy is labelled Aspect parent and children never Workspace taxonomy`() {
        val parent = row("parent", "ASPECT", listOf("PARENT_ASPECT"), WorkspaceRelationDirection.ASPECT_TO_ENTITY)
        val child = row("child", "ASPECT", listOf("PARENT_ASPECT"))
        val groups = projection(listOf(parent, child)).entityHubGroups()
        assertThat(groups.single { parent in it.rows }.label).startsWith("Батьківський Aspect")
        assertThat(groups.single { child in it.rows }.label).startsWith("Дочірні Aspects")
    }

    @Test fun `supporting Workspaces retain actual kinds and exact operational destination`() {
        val workspace = row("supporter", "WORKSPACE", listOf("MONITORS", "SUPPORTS"))
        val group = projection(listOf(workspace)).entityHubGroups().single { workspace in it.rows }
        assertThat(group.label).isEqualTo("Workspaces · пов’язані з Health")
        assertThat(group.rows.single().navigation).isEqualTo(NavTarget.Workspace("supporter"))
        assertThat(group.rows.single().kinds).containsExactly("MONITORS", "SUPPORTS")
    }

    @Test fun `preview bounded globally and per section without mutating full model`() {
        val full = projection((1..20).map { row("o$it") } + (1..10).map { row("w$it", "WORKSPACE") }).entityHubGroups()
        val before = full.toList()
        val preview = full.entityHubPreview(4)
        assertThat(preview.sumOf { it.rows.size }).isEqualTo(4)
        assertThat(preview.all { it.rows.size <= 2 }).isTrue()
        assertThat(full).isEqualTo(before)
        assertThat(full.sumOf { it.rows.size }).isEqualTo(31)
        assertThat(full.entityHubPreview(0)).isEmpty()
    }

    @Test fun `same raw ID stays separate across typed destinations`() {
        val semantic = row("same")
        val operational = row("same", "WORKSPACE", listOf("SUPPORTS"))
        val rows = projection(listOf(semantic, operational)).entityHubGroups().flatMap { it.rows }
        assertThat(rows.map { it.navigation }).containsAtLeast(NavTarget.SemanticEntityEditor("same"), NavTarget.Workspace("same"))
    }

    @Test fun `empty and missing embodiment are normal and no semantic source is invented`() {
        assertThat(WorkspaceRelationsProjection("w", emptyList()).entityHubGroups()).isEmpty()
        val support = row("o", kinds = listOf("SUPPORTS"))
        val groups = WorkspaceRelationsProjection("w", listOf(support)).entityHubGroups()
        assertThat(groups.single().label).isEqualTo("Інші прямі зв’язки Workspace")
        assertThat(groups.single().rows).containsExactly(support)
    }

    @Test fun `unsupported Planning navigation remains unavailable and kind remains visible`() {
        val plan = row("q").copy(family = WorkspaceRelationFamily.PLANNING, targetType = "ARC_QUEST", navigation = null,
            kinds = listOf("SOURCE"), direction = WorkspaceRelationDirection.ENTITY_TO_WORKSPACE)
        val group = WorkspaceRelationsProjection("w", listOf(plan)).entityHubGroups().single()
        assertThat(group.rows.single().navigation).isNull()
        assertThat(group.rows.single().description()).contains("SOURCE")
    }
}
