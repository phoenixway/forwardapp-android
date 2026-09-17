package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.normalizedParentId

/**
 * Builds the read-only hierarchy presentation tree without depending on Room
 * Context objects. It intentionally has no mutation or persistence surface.
 */
class HierarchyPresentationTreeBuilder {
    fun build(nodes: List<HierarchyContextPresentationNode>): HierarchyPresentationData {
        if (nodes.isEmpty()) {
            return HierarchyPresentationData(allProjects = nodes)
        }

        val nodesById = nodes.associateBy { it.id }
        val topLevel = mutableListOf<HierarchyContextPresentationNode>()
        val childMap = mutableMapOf<String, MutableList<HierarchyContextPresentationNode>>()

        nodes.forEach { node ->
            val parentId = node.displayParentId(nodesById)
            if (parentId == null) {
                topLevel += node
            } else {
                childMap.getOrPut(parentId) { mutableListOf() } += node
            }
        }

        return HierarchyPresentationData(
            allProjects = nodes,
            topLevelProjects = topLevel.sortedBy { it.order },
            childMap = childMap.mapValues { (_, children) -> children.sortedBy { it.order } },
        )
    }
}

private fun HierarchyContextPresentationNode.displayParentId(
    nodesById: Map<String, HierarchyContextPresentationNode>,
): String? {
    val directParentId = parentId.normalizedParentId() ?: return null
    if (directParentId == id || !nodesById.containsKey(directParentId)) return null

    var currentParentId = directParentId
    val visited = mutableSetOf(id)
    while (true) {
        if (!visited.add(currentParentId)) return null
        val parent = nodesById[currentParentId] ?: return directParentId
        val nextParentId = parent.parentId.normalizedParentId() ?: return directParentId
        if (nextParentId == currentParentId) return null
        currentParentId = nextParentId
    }
}
