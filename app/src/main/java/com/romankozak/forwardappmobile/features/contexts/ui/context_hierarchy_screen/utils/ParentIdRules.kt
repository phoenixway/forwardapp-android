package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.core.data.models.entities.Context

fun String?.normalizedParentId(): String? =
    this
        ?.trim()
        ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

fun Context.displayParentId(projectsById: Map<String, Context>): String? {
    val directParentId = parentId.normalizedParentId() ?: return null
    if (directParentId == id) return null
    if (!projectsById.containsKey(directParentId)) return null

    var currentParentId = directParentId
    val visited = mutableSetOf(id)

    while (true) {
        if (!visited.add(currentParentId)) return null
        val parentProject = projectsById[currentParentId] ?: return directParentId
        val nextParentId = parentProject.parentId.normalizedParentId() ?: return directParentId
        if (nextParentId == currentParentId) return null
        currentParentId = nextParentId
    }
}
