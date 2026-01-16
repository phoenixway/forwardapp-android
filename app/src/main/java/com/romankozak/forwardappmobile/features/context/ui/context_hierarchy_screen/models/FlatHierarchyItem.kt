package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.data.database.models.Project

/**
 * Represents a single project inside the flattened hierarchy list along with its depth level.
 */
data class FlatHierarchyItem(
    val project: Project,
    val level: Int,
)
