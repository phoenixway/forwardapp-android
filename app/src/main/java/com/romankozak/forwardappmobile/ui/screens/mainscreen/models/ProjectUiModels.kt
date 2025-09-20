package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Project


/*data class SearchResult(
    val project: Project,
    val path: List<BreadcrumbItem>
)*/

sealed class PlanningMode {
    object All : PlanningMode()
    object Daily : PlanningMode()
    object Medium : PlanningMode()
    object Long : PlanningMode()
}

data class AppStatistics(
    val projectCount: Int = 0,
    val goalCount: Int = 0
)

data class PlanningSettingsState(
    val showModes: Boolean = false,
    val dailyTag: String = "daily",
    val mediumTag: String = "medium",
    val longTag: String = "long",
)

data class BreadcrumbItem(
    val id: String,
    val name: String,
    val level: Int
)

data class HierarchyDisplaySettings(
    // Properties for structure and layout (from ProjectUiModels.kt)
    val maxCollapsibleLevels: Int = 3,
    val useBreadcrumbsAfter: Int = 2,
    val maxIndentation: Dp = 120.dp,

    // Properties for content visibility (from MainSubState.kt)
    val showCompletedProjects: Boolean = true,
    val showProjectTags: Boolean = true,
    val showProjectProgress: Boolean = false,
    val compactMode: Boolean = false
)


enum class DropPosition { BEFORE, AFTER }

