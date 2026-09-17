package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation

fun String?.normalizedParentId(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

fun Context.displayParentId(projectsById: Map<String, Context>): String? =
    resolveDisplayParentId(
        id = id,
        parentId = parentId,
        containsProject = projectsById::containsKey,
        parentIdFor = { projectId -> projectsById[projectId]?.parentId },
    )

fun ContextPresentation.displayParentId(
    projectsById: Map<String, ContextPresentation>,
): String? =
    resolveDisplayParentId(
        id = id,
        parentId = parentId,
        containsProject = projectsById::containsKey,
        parentIdFor = { projectId -> projectsById[projectId]?.parentId },
    )

private fun resolveDisplayParentId(
    id: String,
    parentId: String?,
    containsProject: (String) -> Boolean,
    parentIdFor: (String) -> String?,
): String? {
    val directParentId = parentId.normalizedParentId() ?: return null
    if (directParentId == id) return null
    if (!containsProject(directParentId)) return null

    var currentParentId = directParentId
    val visited = mutableSetOf(id)

    while (true) {
        if (!visited.add(currentParentId)) return null
        val nextParentId =
            parentIdFor(currentParentId)
                .normalizedParentId()
                ?: return directParentId
        if (nextParentId == currentParentId) return null
        currentParentId = nextParentId
    }
}
