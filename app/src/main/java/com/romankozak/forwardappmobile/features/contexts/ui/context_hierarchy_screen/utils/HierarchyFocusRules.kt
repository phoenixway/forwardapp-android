package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem

private const val FOCUS_SEGMENT_THRESHOLD = 3
private const val FOCUS_BREADCRUMB_CHAR_THRESHOLD = 30
private const val FOCUS_DESCENDANT_DEPTH_THRESHOLD = 3
private const val FOCUS_ROW_BASE_UNITS = 14
private const val FOCUS_ROW_INDENT_UNITS = 7
private const val FOCUS_ROW_SCREEN_UNITS = 44

internal fun shouldUseHierarchyFocusMode(
    breadcrumbs: List<BreadcrumbItem>,
    hasFocusedProject: Boolean,
): Boolean = shouldUseHierarchyFocusModeForBreadcrumbNames(breadcrumbs.map(BreadcrumbItem::name), hasFocusedProject)

internal fun shouldUseHierarchyFocusModeForBreadcrumbNames(
    breadcrumbNames: List<String>,
    hasFocusedProject: Boolean,
): Boolean {
    if (hasFocusedProject) return true
    if (breadcrumbNames.isEmpty()) return false

    val estimatedBreadcrumbChars =
        breadcrumbNames.sumOf { it.length } +
            (breadcrumbNames.size * 4) +
            3

    return breadcrumbNames.size >= FOCUS_SEGMENT_THRESHOLD ||
        estimatedBreadcrumbChars >= FOCUS_BREADCRUMB_CHAR_THRESHOLD
}

internal fun shouldShowHierarchyFocusButton(
    hasChildren: Boolean,
    level: Int,
    hasOverflowingDescendants: Boolean,
): Boolean = hasChildren && (hasOverflowingDescendants || level >= FOCUS_DESCENDANT_DEPTH_THRESHOLD)

internal fun createHierarchyDescendantOverflowMap(hierarchy: ContextHierarchyData): Map<String, Boolean> {
    if (hierarchy.allProjects.isEmpty()) return emptyMap()

    val visibleProjects =
        buildSet {
            hierarchy.topLevelProjects.forEach { add(it.id) }
            hierarchy.childMap.forEach { (parentId, children) ->
                add(parentId)
                children.forEach { add(it.id) }
            }
        }

    if (visibleProjects.isEmpty()) return emptyMap()

    val memo = mutableMapOf<String, Int>()

    fun estimateRowWidthUnits(
        name: String,
        relativeLevel: Int,
    ): Int {
        val clampedNameUnits = name.trim().length.coerceIn(6, 32)
        return FOCUS_ROW_BASE_UNITS + (relativeLevel * FOCUS_ROW_INDENT_UNITS) + clampedNameUnits
    }

    fun maxVisibleDescendantWidthUnits(
        projectId: String,
        relativeLevel: Int,
        visited: Set<String>,
    ): Int {
        if (!visibleProjects.contains(projectId) || projectId in visited) {
            return 0
        }

        val nextVisited = visited + projectId

        if (relativeLevel == 1) {
            memo[projectId]?.let { return it }
        }

        val children = hierarchy.childMap[projectId].orEmpty()
        val maxWidth =
            children.maxOfOrNull { child ->
                val childWidth = estimateRowWidthUnits(child.name, relativeLevel)
                val descendantWidth = maxVisibleDescendantWidthUnits(child.id, relativeLevel + 1, nextVisited)
                maxOf(childWidth, descendantWidth)
            } ?: 0

        if (relativeLevel == 1) {
            memo[projectId] = maxWidth
        }

        return maxWidth
    }

    return visibleProjects.associateWith { projectId ->
        maxVisibleDescendantWidthUnits(
            projectId = projectId,
            relativeLevel = 1,
            visited = mutableSetOf(),
        ) >= FOCUS_ROW_SCREEN_UNITS
    }
}
