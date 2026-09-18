package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode

/**
 * Chooses a read navigation destination without turning presentation-only nodes
 * into persistable Contexts. Exact shell-free System identities return to the
 * hierarchy's read route; ordinary and Context-backed nodes retain ContextDetail.
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
    hasLegacyBacking: Boolean,
): HierarchyProjectNavigation? {
    val title = presentation?.name ?: return null

    if (hasLegacyBacking || !SystemContexts.isSystem(ContextId(projectId))) {
        return HierarchyProjectNavigation.ContextDetail(
            projectId = projectId,
            title = title,
        )
    }

    return if (SystemContexts.isSystem(ContextId(projectId))) {
        HierarchyProjectNavigation.HierarchyRead(projectId, title)
    } else {
        null
    }
}
