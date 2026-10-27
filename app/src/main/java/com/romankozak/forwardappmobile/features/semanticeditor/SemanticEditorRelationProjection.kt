package com.romankozak.forwardappmobile.features.semanticeditor

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType

internal fun SemanticEntityEditorModel.withRelations(
    catalog: List<CanonicalEntitySummary>,
    refs: List<AspectOrientationRefEntity>,
    bindings: List<WorkspaceBindingEntity>,
    graphRelations: List<OrientationRelationEntity>,
): SemanticEntityEditorModel {
    val byRef = catalog.associateBy(CanonicalEntitySummary::ref)
    return copy(
        aspectTitles = if (entityClass == SemanticEntityClass.ASPECT) catalog
            .filter { it.ref is CanonicalEntityRef.Aspect }.associate { it.ref.id to it.title } else emptyMap(),
        orientationRelations = graphRelations.map { relation ->
            val otherId =
                when (id) {
                    relation.fromOrientationId -> relation.toOrientationId
                    relation.toOrientationId -> relation.fromOrientationId
                    else -> error("Orientation relation does not include editor subject")
                }
            val title = requireNotNull(byRef[CanonicalEntityRef.Orientation(otherId)]) {
                "Orientation relation points to an unavailable Orientation"
            }.title
            OrientationGraphRelationItem(
                relationId = relation.id,
                otherOrientationId = otherId,
                title = title,
                relationType = OrientationRelationType.valueOf(relation.relationType),
                direction = if (relation.fromOrientationId == id) {
                    OrientationRelationEditDirection.OUTGOING
                } else {
                    OrientationRelationEditDirection.INCOMING
                },
                version = relation.version,
            )
        },
        aspectAssociations = refs.filter { entityClass == SemanticEntityClass.ORIENTATION && !it.isDeleted }.map { ref ->
            val title = requireNotNull(byRef[CanonicalEntityRef.Aspect(ref.aspectId)]) {
                "Aspect association points to an unavailable Aspect"
            }.title
            AspectAssociationItem(
                ref.id, ref.aspectId, title,
                AspectOrientationRelationType.valueOf(ref.relationType), ref.isPrimary,
            )
        },
        orientationAssociations = refs.filter { entityClass == SemanticEntityClass.ASPECT && !it.isDeleted }.map { ref ->
            val title = requireNotNull(byRef[CanonicalEntityRef.Orientation(ref.orientationId)]) {
                "Aspect association points to an unavailable Orientation"
            }.title
            OrientationAssociationItem(ref.id, ref.orientationId, title,
                AspectOrientationRelationType.valueOf(ref.relationType), ref.isPrimary)
        },
        workspaceBindings = bindings.map { binding ->
            val title = requireNotNull(byRef[CanonicalEntityRef.Workspace(binding.workspaceId)]) {
                "Workspace binding points to an unavailable Workspace"
            }.title
            WorkspaceBindingItem(
                binding.id, binding.workspaceId, title,
                WorkspaceBindingType.valueOf(binding.bindingType), binding.isPrimary,
            )
        },
    )
}
