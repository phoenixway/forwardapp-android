package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData

/**
 * Returns the visible presentation path to [targetId].
 *
 * Traversal follows the already-normalized presentation tree rather than the
 * raw parentId field, so missing-parent/cycle behavior remains owned by the
 * presentation tree builder.
 */
fun buildPresentationPathToProject(
    targetId: String,
    hierarchy: HierarchyPresentationData,
): List<HierarchyContextPresentationNode> {
    val path = mutableListOf<HierarchyContextPresentationNode>()
    val visited = mutableSetOf<String>()

    fun findPath(projects: List<HierarchyContextPresentationNode>): Boolean {
        for (project in projects.sortedBy { it.order }) {
            if (!visited.add(project.id)) continue

            path += project
            if (project.id == targetId) {
                return true
            }

            if (findPath(hierarchy.childMap[project.id].orEmpty())) {
                return true
            }

            path.removeLastOrNull()
        }
        return false
    }

    return if (findPath(hierarchy.topLevelProjects)) {
        path.toList()
    } else {
        emptyList()
    }
}
