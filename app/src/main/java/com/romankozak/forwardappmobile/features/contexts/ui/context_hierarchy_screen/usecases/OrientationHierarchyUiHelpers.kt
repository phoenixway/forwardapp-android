package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode

internal fun buildOrientationBreadcrumbs(
    items: List<OrientationHierarchyItem>,
    nodeId: String,
): List<BreadcrumbItem> {
    val nodeIndex = items.indexOfFirst { it.node.id == nodeId }
    if (nodeIndex == -1) return emptyList()
    val targetItem = items[nodeIndex]
    val ancestors = ArrayDeque<OrientationHierarchyItem>()
    var expectedLevel = targetItem.level - 1
    for (index in nodeIndex - 1 downTo 0) {
        val item = items[index]
        if (item.level == expectedLevel) {
            ancestors.addFirst(item)
            expectedLevel--
        }
        if (expectedLevel < 0) break
    }
    return (ancestors + targetItem).mapIndexed { index, item ->
        BreadcrumbItem(
            id = item.node.id,
            name = item.node.title,
            level = index,
            target = BreadcrumbTarget.OrientationNode,
        )
    }
}

internal fun buildDirectChildrenByOrientationNodeId(
    items: List<OrientationHierarchyItem>,
): Map<String, List<OrientationHierarchyItem>> {
    val result = linkedMapOf<String, MutableList<OrientationHierarchyItem>>()
    val stack = ArrayDeque<OrientationHierarchyItem>()
    items.forEach { item ->
        while (stack.isNotEmpty() && stack.last().level >= item.level) {
            stack.removeLast()
        }
        stack.lastOrNull()?.let { parent ->
            result.getOrPut(parent.node.id) { mutableListOf() } += item
        }
        stack.addLast(item)
    }
    return result
}

internal fun buildOrientationDisplayChildMap(
    canonicalChildMap: Map<String, List<Context>>,
    orientationHierarchy: List<OrientationHierarchyItem>,
    directChildrenByNodeId: Map<String, List<OrientationHierarchyItem>>,
): Map<String, List<Context>> {
    val result = canonicalChildMap.mapValues { (_, children) -> children.toMutableList() }.toMutableMap()
    orientationHierarchy
        .filter { it.node is OrientationHierarchyNode.ContextNode }
        .forEach { parentItem ->
            val parentContext = (parentItem.node as OrientationHierarchyNode.ContextNode).context
            val children =
                directChildrenByNodeId[parentContext.id].orEmpty().mapNotNull { childItem ->
                    (childItem.node as? OrientationHierarchyNode.ContextNode)?.context
                }
            if (children.isNotEmpty()) {
                val mutableChildren = result.getOrPut(parentContext.id) { mutableListOf() }
                children.forEach { child ->
                    if (mutableChildren.none { it.id == child.id }) {
                        mutableChildren += child
                    }
                }
            }
        }
    return result
}
