package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode

internal fun findOrientationHierarchyItem(
    items: List<OrientationHierarchyItem>,
    nodeId: String,
    placementId: String? = null,
): OrientationHierarchyItem? =
    items.firstOrNull { item ->
        if (placementId != null) {
            item.node.id == nodeId && item.node.placementId?.value == placementId
        } else {
            item.node.id == nodeId
        }
    }

internal fun findOrientationHierarchyItemIndex(
    items: List<OrientationHierarchyItem>,
    nodeId: String,
    placementId: String? = null,
): Int =
    items.indexOfFirst { item ->
        if (placementId != null) {
            item.node.id == nodeId && item.node.placementId?.value == placementId
        } else {
            item.node.id == nodeId
        }
    }

internal fun buildOrientationBreadcrumbs(
    items: List<OrientationHierarchyItem>,
    nodeId: String,
    placementId: String? = null,
): List<BreadcrumbItem> {
    val nodeIndex =
        findOrientationHierarchyItemIndex(
            items = items,
            nodeId = nodeId,
            placementId = placementId,
        )
    if (nodeIndex == -1) return emptyList()
    return buildDisplayedOrientationBreadcrumbs(items, nodeIndex)
}

internal fun buildOrientationBreadcrumbsToContext(
    items: List<OrientationHierarchyItem>,
    contextId: String,
    placementId: String? = null,
): List<BreadcrumbItem> {
    val nodeIndex =
        findOrientationHierarchyItemIndex(
            items = items,
            nodeId = contextId,
            placementId = placementId,
        ).takeIf { index ->
            index >= 0 && items[index].node is OrientationHierarchyNode.ProjectLike
        } ?: -1
    if (nodeIndex == -1) return emptyList()
    return buildDisplayedOrientationBreadcrumbs(items, nodeIndex)
}

private fun buildDisplayedOrientationBreadcrumbs(
    items: List<OrientationHierarchyItem>,
    nodeIndex: Int,
): List<BreadcrumbItem> {
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
        val projectNode = item.node as? OrientationHierarchyNode.ProjectLike
        BreadcrumbItem(
            id = projectNode?.id ?: item.node.id,
            name = item.node.title,
            level = index,
            target =
                if (projectNode == null) {
                    BreadcrumbTarget.OrientationNode
                } else {
                    BreadcrumbTarget.Context
                },
            placementId = item.node.placementId?.value,
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
            result.getOrPut(parent.node.structuralKey) { mutableListOf() } += item
        }
        stack.addLast(item)
    }
    return result
}
