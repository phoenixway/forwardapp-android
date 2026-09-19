package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode

/**
 * Chooses a read navigation destination without turning presentation-only nodes
 * into persistable Contexts. Exact System identities return to the hierarchy's
 * read route; ordinary ProjectLike owners use the operational detail route.
 */
internal sealed interface HierarchyProjectNavigation {
    data class ContextDetail(
        val projectId: String,
        val title: String,
    ) : HierarchyProjectNavigation

    data class HierarchyRead(
        val projectId: String,
        val title: String,
    ) : HierarchyProjectNavigation
}

internal fun resolveHierarchyProjectNavigation(
    projectId: String,
    presentation: HierarchyContextPresentationNode?,
): HierarchyProjectNavigation? {
    val title = presentation?.name ?: return null

    if (!SystemContexts.isSystem(ContextId(projectId))) {
        return HierarchyProjectNavigation.ContextDetail(
            projectId = projectId,
            title = title,
        )
    }

    return HierarchyProjectNavigation.HierarchyRead(projectId, title)
}
