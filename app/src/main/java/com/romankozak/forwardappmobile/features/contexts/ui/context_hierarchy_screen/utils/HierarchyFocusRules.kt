package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem

private const val FOCUS_SEGMENT_THRESHOLD = 3
private const val FOCUS_BREADCRUMB_CHAR_THRESHOLD = 30

internal fun shouldUseHierarchyFocusMode(
    breadcrumbs: List<BreadcrumbItem>,
    hasFocusedProject: Boolean,
): Boolean =
    shouldUseHierarchyFocusModeForBreadcrumbNames(
        breadcrumbNames = breadcrumbs.map(BreadcrumbItem::name),
        hasFocusedProject = hasFocusedProject,
    )

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
