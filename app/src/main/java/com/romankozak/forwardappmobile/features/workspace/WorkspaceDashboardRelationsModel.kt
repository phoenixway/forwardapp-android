package com.romankozak.forwardappmobile.features.workspace

internal data class WorkspaceDashboardRelationGroup(
    val title: String,
    val targetType: String,
    val count: Int,
    val firstTitle: String,
    val moreCount: Int,
)

internal data class WorkspaceDashboardRelations(
    val source: WorkspaceRelationItem?,
    val groups: List<WorkspaceDashboardRelationGroup>,
    val totalGroups: Int,
)

/** A bounded disclosure preview, not a new query or a persisted relation list. */
internal fun WorkspaceRelationsProjection.dashboardRelations(): WorkspaceDashboardRelations {
    val panel = relationshipPanel()
    return WorkspaceDashboardRelations(panel.source, panel.groups.take(3).map { group ->
        WorkspaceDashboardRelationGroup(group.label, group.rows.first().targetType, group.rows.size,
            group.rows.first().title, group.rows.size - 1)
    }, panel.groups.size)
}
