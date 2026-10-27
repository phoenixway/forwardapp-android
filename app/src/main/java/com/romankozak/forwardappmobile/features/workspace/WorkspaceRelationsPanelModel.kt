package com.romankozak.forwardappmobile.features.workspace

/** Visual grouping only: uses exactly the existing admitted rows and navigation identities. */
internal data class WorkspaceRelationsPanelModel(
    val source: WorkspaceRelationItem?,
    val structureLabel: String,
    val groups: List<WorkspaceEntityHubGroup>,
)

internal fun WorkspaceRelationsProjection.relationshipPanel(): WorkspaceRelationsPanelModel {
    val sourceId = when {
        embodiedAspect != null && embodiedOrientation == null -> embodiedAspect.aspectId
        embodiedOrientation != null && embodiedAspect == null -> embodiedOrientation.orientationId
        else -> null
    }
    val source = relations.singleOrNull {
        it.family == WorkspaceRelationFamily.SEMANTIC && "EMBODIES" in it.kinds && it.targetId == sourceId
    }
    val groups = entityHubGroups().mapNotNull { group ->
        val rows = group.rows.filterNot { it == source }
        if (rows.isEmpty()) return@mapNotNull null
        val label = when {
            rows.all { "PARENT_ASPECT" in it.kinds && it.direction == WorkspaceRelationDirection.ENTITY_TO_ASPECT } -> "Sub-aspects"
            rows.all { "PARENT_ASPECT" in it.kinds } -> "Parent Aspect"
            rows.all { it.family == WorkspaceRelationFamily.PLANNING } -> group.label
            rows.all { it.targetType == "ORIENTATION" } -> "Orientations"
            rows.all { it.targetType == "ASPECT" } -> "Aspects"
            rows.all { it.targetType == "WORKSPACE" } -> "Workspaces"
            else -> group.label
        }
        group.copy(label = label, rows = rows)
    }
    return WorkspaceRelationsPanelModel(source, when {
        source == null -> "Workspace relations"
        embodiedAspect != null -> "Aspect structure"
        else -> "Orientation relations"
    }, groups)
}

internal fun WorkspaceRelationItem.panelRelationLabel(): String {
    val arrow = when (direction) {
        WorkspaceRelationDirection.ENTITY_TO_WORKSPACE, WorkspaceRelationDirection.ENTITY_TO_ASPECT,
        WorkspaceRelationDirection.ENTITY_TO_ORIENTATION -> "←"
        WorkspaceRelationDirection.ORIENTATION_MUTUAL -> "↔"
        else -> "→"
    }
    return "$arrow ${kinds.joinToString(" · ") { it.lowercase(java.util.Locale.ROOT).replace('_', ' ') }}"
}
