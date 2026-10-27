package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.planning.PlanningWorkspaceRelationRow
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Test

class WorkspaceRelationsProjectionTest {
    private val orientation = CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Health")
    private val aspect = CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Family")
    private fun binding(kind: String = "EMBODIES", subject: String = "o", deleted: Boolean = false, workspace: String = "w") =
        WorkspaceBindingEntity("$workspace:$subject:$kind", workspace, subject, kind, false, 0, 1, 1, null, deleted, 1)
    private fun project(bindings: List<WorkspaceBindingEntity> = emptyList(), planning: List<PlanningWorkspaceRelationRow> = emptyList(),
        catalog: List<CanonicalEntitySummary> = listOf(orientation, aspect)) = projectWorkspaceRelations("w", bindings, catalog, planning).relations
    private fun plan(kind: String = "DAY_TASK", id: String = "task", role: String = "SOURCE", title: String = "Tests") =
        PlanningWorkspaceRelationRow("$kind:$id:$role", kind, id, role, title)

    @Test fun `empty is valid`() { assertThat(project()).isEmpty() }
    @Test fun `EMBODIES appears exact title and type`() {
        val row = project(listOf(binding())).single()
        assertThat(row.title).isEqualTo("Health"); assertThat(row.targetType).isEqualTo("ORIENTATION")
        assertThat(row.kinds).containsExactly("EMBODIES")
    }
    @Test fun `REALIZES appears`() { assertThat(project(listOf(binding("REALIZES"))).single().kinds).containsExactly("REALIZES") }
    @Test fun `SUPPORTS appears`() { assertThat(project(listOf(binding("SUPPORTS"))).single().kinds).containsExactly("SUPPORTS") }
    @Test fun `MONITORS appears`() { assertThat(project(listOf(binding("MONITORS"))).single().kinds).containsExactly("MONITORS") }
    @Test fun `Aspect admitted and typed`() {
        assertThat(project(listOf(binding(subject = "a"))).single().targetType).isEqualTo("ASPECT")
    }
    @Test fun `multiple kinds merge once deterministically`() {
        val row = project(listOf(binding("SUPPORTS"), binding("MONITORS"))).single()
        assertThat(row.kinds).containsExactly("MONITORS", "SUPPORTS").inOrder()
    }
    @Test fun `SOURCE inverse reference direction retained`() {
        val row = project(planning = listOf(plan())).single()
        assertThat(row.direction).isEqualTo(WorkspaceRelationDirection.ENTITY_TO_WORKSPACE)
        assertThat(row.description()).contains("сутність → SOURCE → Workspace")
    }
    @Test fun `RELATED and SOURCE merge per typed owner`() {
        assertThat(project(planning = listOf(plan(), plan(role = "RELATED"))).single().kinds).containsExactly("RELATED", "SOURCE").inOrder()
    }
    @Test fun `binding direction retained`() {
        assertThat(project(listOf(binding())).single().description()).contains("Workspace → EMBODIES → сутність")
    }
    @Test fun `tombstoned relations and missing semantic targets excluded`() {
        assertThat(project(listOf(binding(deleted = true)))).isEmpty()
        assertThat(project(listOf(binding()), catalog = emptyList())).isEmpty()
    }
    @Test fun `wrong workspace and unknown kinds excluded`() {
        assertThat(project(listOf(binding(workspace = "other"), binding("WRONG")))).isEmpty()
        assertThat(project(planning = listOf(plan(role = "WRONG"), plan(kind = "CONTEXT")))).isEmpty()
    }
    @Test fun `same raw IDs do not collapse families or Planning types`() {
        val result = project(listOf(binding()), listOf(plan(id = "o"), plan("SCOPE", "o")))
        assertThat(result.map { it.key }.toSet()).hasSize(3)
    }
    @Test fun `ambiguous semantic class collision fails closed rather than inferring type`() {
        assertThat(project(listOf(binding()), catalog = listOf(orientation, aspect.copy(ref = CanonicalEntityRef.Aspect("o"))))).isEmpty()
    }
    @Test fun `semantic exact navigation`() {
        assertThat(project(listOf(binding())).single().navigation).isEqualTo(NavTarget.SemanticEntityEditor("o"))
        assertThat(project(listOf(binding(subject = "a"))).single().navigation).isEqualTo(NavTarget.SemanticEntityEditor("a"))
    }
    @Test fun `Day task exact navigation and unsupported Planning destinations disabled`() {
        assertThat(project(planning = listOf(plan())).single().navigation).isEqualTo(NavTarget.EditTask("task"))
        listOf("SCOPE", "TACTICAL_MISSION", "ARC_QUEST", "DAY_FOCUS").forEach {
            assertThat(project(planning = listOf(plan(it))).single().navigation).isNull()
        }
    }
    @Test fun `deterministic grouping and title order`() {
        val result = project(listOf(binding(subject = "a"), binding()), listOf(plan(title = "A")))
        assertThat(result.map { it.title }).containsExactly("Family", "Health", "A").inOrder()
    }
    @Test fun `catalog alone tags and H1 independent metadata do not create relations`() {
        assertThat(project(catalog = listOf(orientation.copy(tags = setOf("w")), aspect))).isEmpty()
    }
}
