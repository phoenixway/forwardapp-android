package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.resolveOperationalEmbodiments

/** Semantic-first composition, action-first navigation. No structural or lifecycle writes. */
internal fun withOperationalEmbodiments(
    nodes: List<BigPictureNode>,
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
): List<BigPictureNode> {
    val resolved = resolveOperationalEmbodiments(bindings, catalog)
    return nodes.map { node -> node.copy(operationalEmbodiment = resolved[node.entity?.ref]) }
}
