package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspect
import com.romankozak.forwardappmobile.data.planning.PlanningWorkspaceRelationRow
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Test

class EmbodiedAspectRelationsTest {
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Current Workspace"),
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("other"), "Other Workspace"),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Health"),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("parent"), "Parent"),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("child"), "Child"),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Treatment"),
    )
    private fun binding(kind: String = "EMBODIES", workspace: String = "w", subject: String = "a") =
        WorkspaceBindingEntity("$workspace:$kind", workspace, subject, kind, false, 0, 1, 1, null, false, 1)
    private fun aspect(id: String = "a", parent: String? = null, deleted: Boolean = false) = CanonicalAspect(
        ManagedSubjectEntity(id, "ASPECT", id, null, 1, 1, null, deleted, 1), AspectEntity(id, parent, 0, false))
    private fun ref(kind: String = "BELONGS_TO", deleted: Boolean = false, owner: String = "a") =
        AspectOrientationRefEntity(kind, owner, "o", kind, false, 0, 1, 1, null, deleted, 1)
    private fun project(refs: List<AspectOrientationRefEntity> = emptyList(), bindings: List<WorkspaceBindingEntity> = emptyList(),
        aspects: List<CanonicalAspect> = listOf(aspect()), entities: List<CanonicalEntitySummary> = catalog,
        plans: List<PlanningWorkspaceRelationRow> = emptyList()) =
        projectEmbodiedAspectRelations("w", "a", entities, aspects, refs, bindings, plans)

    @Test fun `only unique live EMBODIES Aspect admits expansion`() {
        assertThat(embodiedAspectId("w", listOf(binding()), catalog)).isEqualTo("a")
        for (kind in listOf("SUPPORTS", "REALIZES", "MONITORS"))
            assertThat(embodiedAspectId("w", listOf(binding(kind)), catalog)).isNull()
        assertThat(embodiedAspectId("w", listOf(binding().copy(isDeleted = true)), catalog)).isNull()
        assertThat(embodiedAspectId("w", listOf(binding(subject = "o")), catalog)).isNull()
    }

    @Test fun `missing ambiguous and wrongly typed embodiment fails closed`() {
        assertThat(embodiedAspectId("w", listOf(binding(), binding(subject = "parent")), catalog)).isNull()
        assertThat(embodiedAspectId("w", listOf(binding()), catalog.filterNot { it.ref == CanonicalEntityRef.Aspect("a") })).isNull()
        assertThat(embodiedAspectId("w", listOf(binding()), catalog + CanonicalEntitySummary(CanonicalEntityRef.Orientation("a"), "Same ID"))).isNull()
        assertThat(embodiedAspectId("missing", listOf(binding()), catalog)).isNull()
    }

    @Test fun `Orientation associations preserve direction merge kinds and navigate exact identity`() {
        val rows = project(refs = listOf(ref(), ref("RELEVANT_TO"), ref(owner = "unrelated")))!!.relations
        assertThat(rows).hasSize(1)
        assertThat(rows.single().kinds).containsExactly("BELONGS_TO", "RELEVANT_TO")
        assertThat(rows.single().direction).isEqualTo(WorkspaceRelationDirection.ENTITY_TO_ASPECT)
        assertThat(rows.single().navigation).isEqualTo(NavTarget.SemanticEntityEditor("o"))
        assertThat(rows.single().description()).endsWith("→ Aspect")
    }

    @Test fun `taxonomy includes immediate parent and children but never grandchildren`() {
        val rows = project(aspects = listOf(aspect(parent = "parent"), aspect("parent"), aspect("child", "a"), aspect("grandchild", "child")))!!.relations
        assertThat(rows.map { it.targetId }).containsExactly("parent", "child")
        assertThat(rows.single { it.targetId == "parent" }.direction).isEqualTo(WorkspaceRelationDirection.ASPECT_TO_ENTITY)
        assertThat(rows.single { it.targetId == "child" }.navigation).isEqualTo(NavTarget.SemanticEntityEditor("child"))
    }

    @Test fun `other bound Workspaces combine kinds without repeating current Workspace`() {
        val rows = project(bindings = listOf(binding(), binding("SUPPORTS", "other"), binding("MONITORS", "other")))!!.relations
        assertThat(rows).hasSize(1)
        assertThat(rows.single().navigation).isEqualTo(NavTarget.Workspace("other"))
        assertThat(rows.single().kinds).containsExactly("MONITORS", "SUPPORTS")
    }

    @Test fun `Planning rows retain Aspect direction and existing exact task navigation`() {
        val plans = listOf(PlanningWorkspaceRelationRow("r", "DAY_TASK", "task", "RELATED", "Task"),
            PlanningWorkspaceRelationRow("s", "DAY_TASK", "task", "SOURCE", "Task"),
            PlanningWorkspaceRelationRow("q", "ARC_QUEST", "quest", "RELATED", "Quest"))
        val rows = project(plans = plans)!!.relations
        assertThat(rows.single { it.targetId == "task" }.navigation).isEqualTo(NavTarget.EditTask("task"))
        assertThat(rows.single { it.targetId == "quest" }.navigation).isNull()
        assertThat(rows.all { it.direction == WorkspaceRelationDirection.ENTITY_TO_ASPECT }).isTrue()
    }

    @Test fun `deleted unavailable or unknown semantic relations never become live rows`() {
        assertThat(project(aspects = listOf(aspect(deleted = true)))).isNull()
        assertThat(project(entities = emptyList())).isNull()
        assertThat(project(refs = listOf(ref(deleted = true), ref("UNKNOWN")))!!.relations).isEmpty()
        assertThat(project(refs = listOf(ref()), entities = catalog.filterNot { it.ref == CanonicalEntityRef.Orientation("o") })!!.relations).isEmpty()
    }

    @Test fun `typed targets stay separate even with matching raw IDs`() {
        val rows = project(refs = listOf(ref()), bindings = listOf(binding("SUPPORTS", "o")),
            entities = catalog + CanonicalEntitySummary(CanonicalEntityRef.Workspace("o"), "Operational"))!!.relations
        assertThat(rows.map { it.key }.toSet()).hasSize(2)
        assertThat(rows.map { it.navigation }).containsExactly(NavTarget.Workspace("o"), NavTarget.SemanticEntityEditor("o"))
    }

    @Test fun `renames update titles without changing canonical targets or direct Workspace relations`() {
        val renamed = catalog.map { if (it.ref == CanonicalEntityRef.Orientation("o")) it.copy(title = "New title") else it }
        val inherited = project(refs = listOf(ref()), entities = renamed)!!
        assertThat(inherited.relations.single().title).isEqualTo("New title")
        val direct = projectWorkspaceRelations("w", listOf(binding()), renamed, emptyList())
        assertThat(direct.copy(embodiedAspect = inherited).relations).isEqualTo(direct.relations)
        assertThat(inherited.aspectId).isEqualTo("a")
    }
}
