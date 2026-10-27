package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspect
import com.romankozak.forwardappmobile.data.planning.PlanningWorkspaceRelationRow
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import java.util.Locale

data class EmbodiedAspectRelations(val aspectId: String, val title: String, val relations: List<WorkspaceRelationItem>)

/** EMBODIES is the only traversal, and only a unique live typed Aspect is admitted. */
internal fun embodiedAspectId(workspaceId: String, bindings: List<WorkspaceBindingEntity>, catalog: List<CanonicalEntitySummary>): String? {
    return (embodiedSemanticRef(workspaceId, bindings, catalog) as? CanonicalEntityRef.Aspect)?.id
}

/** WorkspaceBinding owns the hop. Never guess a subject type from a raw ID collision. */
internal fun embodiedSemanticRef(workspaceId: String, bindings: List<WorkspaceBindingEntity>, catalog: List<CanonicalEntitySummary>): CanonicalEntityRef? {
    if (catalog.none { it.ref == CanonicalEntityRef.Workspace(workspaceId) }) return null
    val link = bindings.filter { !it.isDeleted && it.workspaceId == workspaceId && it.bindingType == WorkspaceBindingType.EMBODIES.name }
        .singleOrNull() ?: return null
    val subjects = catalog.filter { it.ref !is CanonicalEntityRef.Workspace && it.ref.id == link.subjectId }
    return subjects.singleOrNull()?.ref
}

/** Direct edges of this Aspect only; never copies them into Workspace ownership. */
internal fun projectEmbodiedAspectRelations(
    workspaceId: String,
    aspectId: String,
    catalog: List<CanonicalEntitySummary>,
    aspects: List<CanonicalAspect>,
    associations: List<AspectOrientationRefEntity>,
    bindings: List<WorkspaceBindingEntity>,
    planning: List<PlanningWorkspaceRelationRow>,
): EmbodiedAspectRelations? {
    val byRef = catalog.groupBy { it.ref }.mapNotNull { (key, values) -> values.singleOrNull()?.let { key to it } }.toMap()
    val aspect = aspects.singleOrNull { it.subject.id == aspectId && !it.subject.isDeleted } ?: return null
    val title = byRef[CanonicalEntityRef.Aspect(aspectId)]?.title ?: return null
    fun item(ref: CanonicalEntityRef, kinds: List<String>, direction: WorkspaceRelationDirection): WorkspaceRelationItem? {
        val target = byRef[ref] ?: return null
        val navigation = when (ref) {
            is CanonicalEntityRef.Workspace -> NavTarget.Workspace(ref.id)
            else -> NavTarget.SemanticEntityEditor(ref.id)
        }
        return WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, ref.entityClass.name, ref.id, target.title,
            kinds.distinct().sorted(), direction, navigation, ref.entityClass.displayLabel())
    }
    val orientations = associations.filter {
        !it.isDeleted && it.aspectId == aspectId && it.relationType in AspectOrientationRelationType.entries.map { type -> type.name }
    }.groupBy { it.orientationId }.mapNotNull { (id, rows) ->
        item(CanonicalEntityRef.Orientation(id), rows.map { it.relationType }, WorkspaceRelationDirection.ENTITY_TO_ASPECT)
    }
    val parent = aspect.node.parentAspectId?.let { item(CanonicalEntityRef.Aspect(it), listOf("PARENT_ASPECT"), WorkspaceRelationDirection.ASPECT_TO_ENTITY) }
    val children = aspects.filter { !it.subject.isDeleted && it.node.parentAspectId == aspectId }.mapNotNull {
        item(CanonicalEntityRef.Aspect(it.subject.id), listOf("PARENT_ASPECT"), WorkspaceRelationDirection.ENTITY_TO_ASPECT)
    }
    val workspaces = bindings.filter {
        !it.isDeleted && it.subjectId == aspectId && it.workspaceId != workspaceId &&
            it.bindingType in WorkspaceBindingType.entries.map { type -> type.name }
    }.groupBy { it.workspaceId }.mapNotNull { (id, rows) ->
        item(CanonicalEntityRef.Workspace(id), rows.map { it.bindingType }, WorkspaceRelationDirection.ENTITY_TO_ASPECT)
    }
    val rows = orientations + listOfNotNull(parent) + children + workspaces +
        projectPlanningRelationItems(planning, WorkspaceRelationDirection.ENTITY_TO_ASPECT)
    return EmbodiedAspectRelations(aspectId, title, rows.sortedWith(
        compareBy({ it.family.ordinal }, { it.title.lowercase(Locale.ROOT) }, { it.targetType }, { it.targetId })))
}
