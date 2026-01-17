package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppStatistics(
    val projectCount: Int = 0,
    val goalCount: Int = 0,
)

data class BreadcrumbItem(
    val id: String,
    val name: String,
    val level: Int,
)

data class HierarchyDisplaySettings(
    
    val maxCollapsibleLevels: Int = 3,
    val useBreadcrumbsAfter: Int = 2,
    val maxIndentation: Dp = 120.dp,
    
    val showCompletedProjects: Boolean = true,
    val showProjectTags: Boolean = true,
    val showProjectProgress: Boolean = false,
    val compactMode: Boolean = false,
)

enum class DropPosition { BEFORE, AFTER }
