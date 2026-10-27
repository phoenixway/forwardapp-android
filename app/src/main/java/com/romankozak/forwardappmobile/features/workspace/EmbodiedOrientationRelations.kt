package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import java.util.Locale

data class EmbodiedOrientationRelations(val orientationId: String, val title: String, val relations: List<WorkspaceRelationItem>)

/** One-hop source context. Orientation membership is not Workspace or H1 parentage. */
internal fun projectEmbodiedOrientationRelations(
    orientationId: String,
    catalog: List<CanonicalEntitySummary>,
    memberships: List<AspectOrientationRefEntity>,
    graph: List<OrientationRelationEntity>,
): EmbodiedOrientationRelations? {
    val byRef = catalog.groupBy { it.ref }.mapNotNull { (ref, rows) -> rows.singleOrNull()?.let { ref to it } }.toMap()
    val source = byRef[CanonicalEntityRef.Orientation(orientationId)] ?: return null
    fun item(ref: CanonicalEntityRef, kinds: List<String>, direction: WorkspaceRelationDirection): WorkspaceRelationItem? =
        byRef[ref]?.let { target -> WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, ref.entityClass.name, ref.id,
            target.title, kinds.distinct().sorted(), direction, NavTarget.SemanticEntityEditor(ref.id), ref.entityClass.displayLabel()) }
    val aspects = memberships.filter { !it.isDeleted && it.orientationId == orientationId &&
        it.relationType in AspectOrientationRelationType.entries.map { kind -> kind.name }
    }.groupBy { it.aspectId }.mapNotNull { (id, rows) ->
        item(CanonicalEntityRef.Aspect(id), rows.map { it.relationType }, WorkspaceRelationDirection.ORIENTATION_TO_ENTITY)
    }
    val edges = graph.filter { !it.isDeleted && it.fromOrientationId != it.toOrientationId &&
        (it.fromOrientationId == orientationId || it.toOrientationId == orientationId) &&
        it.relationType in OrientationRelationType.entries.map { kind -> kind.name }
    }.groupBy { edge ->
        val outgoing = edge.fromOrientationId == orientationId
        val other = if (outgoing) edge.toOrientationId else edge.fromOrientationId
        other to when {
            edge.relationType == OrientationRelationType.CONFLICTS_WITH.name -> WorkspaceRelationDirection.ORIENTATION_MUTUAL
            outgoing -> WorkspaceRelationDirection.ORIENTATION_TO_ENTITY
            else -> WorkspaceRelationDirection.ENTITY_TO_ORIENTATION
        }
    }.mapNotNull { (endpoint, rows) -> item(CanonicalEntityRef.Orientation(endpoint.first), rows.map { it.relationType }, endpoint.second) }
    return EmbodiedOrientationRelations(orientationId, source.title, (aspects + edges).sortedWith(
        compareBy({ it.targetType }, { it.title.lowercase(Locale.ROOT) }, { it.targetId }, { it.direction.ordinal })))
}
