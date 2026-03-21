package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.shouldUseHierarchyFocusMode

@Composable
internal fun rememberHierarchyFocusMode(
    breadcrumbs: List<BreadcrumbItem>,
    hasFocusedProject: Boolean,
): Boolean {
    return remember(breadcrumbs, hasFocusedProject) {
        shouldUseHierarchyFocusMode(
            breadcrumbs = breadcrumbs,
            hasFocusedProject = hasFocusedProject,
        )
    }
}
