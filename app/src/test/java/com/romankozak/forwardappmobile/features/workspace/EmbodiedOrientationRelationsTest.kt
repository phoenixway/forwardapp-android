package com.romankozak.forwardappmobile.features.workspace

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.*
import org.junit.Test

class EmbodiedOrientationRelationsTest {
    private val catalog = listOf(
        CanonicalEntitySummary(CanonicalEntityRef.Workspace("w"), "Workspace"),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("o"), "Treatment"),
        CanonicalEntitySummary(CanonicalEntityRef.Orientation("other"), "Other Orientation"),
        CanonicalEntitySummary(CanonicalEntityRef.Aspect("a"), "Health"))
    private fun binding(kind: String = "EMBODIES", subject: String = "o") = WorkspaceBindingEntity(
        "b:$subject", "w", subject, kind, false, 0, 1, 1, null, false, 1)
    private fun membership(kind: String = "BELONGS_TO") = AspectOrientationRefEntity(
        "ref:$kind", "a", "o", kind, false, 0, 1, 1, null, false, 1)
    private fun edge(from: String = "o", to: String = "other", kind: String = "SUPPORTS") = OrientationRelationEntity(
        "$from:$to:$kind", from, to, kind, null, 1, 1, null, false, 1)

    @Test fun `only a unique live typed EMBODIES supplies semantic context`() {
        assertThat(embodiedSemanticRef("w", listOf(binding()), catalog)).isEqualTo(CanonicalEntityRef.Orientation("o"))
        for (kind in listOf("REALIZES", "SUPPORTS", "MONITORS"))
            assertThat(embodiedSemanticRef("w", listOf(binding(kind)), catalog)).isNull()
        assertThat(embodiedSemanticRef("w", listOf(binding().copy(isDeleted = true)), catalog)).isNull()
        assertThat(embodiedSemanticRef("w", listOf(binding(), binding(subject = "a")), catalog)).isNull()
        assertThat(embodiedSemanticRef("missing", listOf(binding()), catalog)).isNull()
    }

    @Test fun `ambiguous and missing semantic identities never become a fake context`() {
        assertThat(embodiedSemanticRef("w", listOf(binding()), catalog.filterNot { it.ref == CanonicalEntityRef.Orientation("o") })).isNull()
        assertThat(embodiedSemanticRef("w", listOf(binding()), catalog + CanonicalEntitySummary(CanonicalEntityRef.Aspect("o"), "Collision"))).isNull()
    }

    @Test fun `memberships preserve exact Aspect and relation kinds separately from Workspace hierarchy`() {
        val context = projectEmbodiedOrientationRelations("o", catalog, listOf(membership(), membership("RELEVANT_TO")), emptyList())!!
        val row = context.relations.single()
        assertThat(row.navigation).isEqualTo(NavTarget.SemanticEntityEditor("a"))
        assertThat(row.kinds).containsExactly("BELONGS_TO", "RELEVANT_TO")
        assertThat(row.direction).isEqualTo(WorkspaceRelationDirection.ORIENTATION_TO_ENTITY)
        assertThat(row.description()).startsWith("Аспект · Orientation")
    }

    @Test fun `incoming outgoing and mutual graph relations retain direction and distinct identity`() {
        val rows = projectEmbodiedOrientationRelations("o", catalog, emptyList(), listOf(
            edge(), edge("other", "o"), edge(kind = "CONFLICTS_WITH")))!!.relations
        assertThat(rows.map { it.direction }).containsExactly(WorkspaceRelationDirection.ORIENTATION_TO_ENTITY,
            WorkspaceRelationDirection.ENTITY_TO_ORIENTATION, WorkspaceRelationDirection.ORIENTATION_MUTUAL)
        assertThat(rows.map { it.key }.toSet()).hasSize(3)
        assertThat(rows.single { it.direction == WorkspaceRelationDirection.ENTITY_TO_ORIENTATION }.compactDescription()).contains("← SUPPORTS")
        assertThat(rows.single { it.direction == WorkspaceRelationDirection.ORIENTATION_MUTUAL }.compactDescription()).contains("↔ CONFLICTS_WITH")
        assertThat(rows.all { it.navigation == NavTarget.SemanticEntityEditor("other") }).isTrue()
    }

    @Test fun `missing deleted malformed and unrelated edges are excluded without crashing`() {
        val rows = projectEmbodiedOrientationRelations("o", catalog, listOf(membership().copy(isDeleted = true), membership("UNKNOWN")),
            listOf(edge().copy(isDeleted = true), edge(kind = "UNKNOWN"), edge("unrelated", "other"), edge(to = "missing"), edge(to = "o")))!!.relations
        assertThat(rows).isEmpty()
        assertThat(projectEmbodiedOrientationRelations("missing", catalog, emptyList(), emptyList())).isNull()
    }

    @Test fun `typed same raw id resolves an Aspect membership independently of Orientation relation`() {
        val entities = catalog + CanonicalEntitySummary(CanonicalEntityRef.Aspect("other"), "Other Aspect")
        val context = projectEmbodiedOrientationRelations("o", entities, listOf(membership().copy(aspectId = "other")), listOf(edge()))!!
        assertThat(context.relations.map { it.targetType }).containsExactly("ASPECT", "ORIENTATION")
        assertThat(context.relations.map { it.key }.toSet()).hasSize(2)
    }

    @Test fun `source editing delegates Aspect taxonomy and Orientation membership to exact semantic editor`() {
        val aspect = WorkspaceRelationsProjection("w", emptyList(), EmbodiedAspectRelations("a", "Health", emptyList()))
        assertThat(aspect.semanticContextEditAction()!!.destination).isEqualTo(NavTarget.SemanticEntityEditor("a"))
        assertThat(aspect.semanticContextEditAction()!!.label).contains("таксономію")
        val orientation = aspect.copy(embodiedAspect = null, embodiedOrientation = EmbodiedOrientationRelations("o", "Treatment", emptyList()))
        assertThat(orientation.semanticContextEditAction()!!.destination).isEqualTo(NavTarget.SemanticEntityEditor("o"))
        assertThat(orientation.semanticContextEditAction()!!.label).contains("належність")
        assertThat(orientation.semanticContextEditAction()!!.explanation).contains("BELONGS_TO")
        assertThat(orientation.copy(embodiedAspect = aspect.embodiedAspect).semanticContextEditAction()).isNull()
        assertThat(WorkspaceRelationsProjection("w", emptyList()).semanticContextEditAction()).isNull()
    }

    @Test fun `dashboard and full view use the same read context with no Aspect taxonomy invented for Orientation`() {
        val source = projectEmbodiedOrientationRelations("o", catalog, listOf(membership()), listOf(edge()))!!
        val projection = WorkspaceRelationsProjection("w", emptyList(), embodiedOrientation = source)
        val groups = projection.entityHubGroups()
        assertThat(groups.flatMap { it.rows }).containsExactlyElementsIn(source.relations)
        assertThat(groups.map { it.label }.joinToString()).doesNotContain("Батьківський")
        assertThat(groups.map { it.label }.joinToString()).contains("Належність до Aspects")
        assertThat(groups.entityHubPreview().flatMap { it.rows }).containsExactlyElementsIn(source.relations)
    }
}
