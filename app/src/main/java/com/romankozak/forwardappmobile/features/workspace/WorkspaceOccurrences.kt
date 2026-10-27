package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructure
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import java.util.Locale

data class WorkspaceOccurrence(
    val placementId: String,
    val path: List<String>,
    val kind: PlacementKind,
    val destination: NavTarget.ContextHierarchy,
)

/** Uses the same admitted H1 graph/breadcrumbs as Big Picture; no Context or semantic inference. */
fun projectWorkspaceOccurrences(
    workspaceId: String,
    structure: BigPictureStructure,
    catalog: List<CanonicalEntitySummary>,
): List<WorkspaceOccurrence> {
    if (workspaceId.isBlank()) return emptyList()
    val read = BigPictureReadComposition(projectBigPicture(structure.placements, catalog), emptyList(), structure.presentation)
    val index = BigPictureStructuralIndex(read)
    return read.nodes.filter { it.entity?.ref == CanonicalEntityRef.Workspace(workspaceId) }.map { node ->
        val id = node.placement.id.value
        WorkspaceOccurrence(
            placementId = id,
            path = listOf("Корінь") + index.breadcrumbs(H1StructuralParent.Node(H1TypedNode.Occurrence(id))).map { it.name },
            kind = node.placement.placementKind,
            destination = NavTarget.ContextHierarchy(projectIdToReveal = workspaceId, placementIdToReveal = id),
        )
    }.sortedWith(compareBy<WorkspaceOccurrence> { it.path.joinToString("\u0000").lowercase(Locale.ROOT) }.thenBy { it.placementId })
}
