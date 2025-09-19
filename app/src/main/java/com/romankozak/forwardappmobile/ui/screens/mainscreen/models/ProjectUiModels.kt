package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Project

sealed class ProjectUiEvent {
    data class NavigateToSyncScreenWithData(val json: String) : ProjectUiEvent()
    data class NavigateToDetails(val projectId: String) : ProjectUiEvent()
    data class NavigateToGlobalSearch(val query: String) : ProjectUiEvent()
    object NavigateToSettings : ProjectUiEvent()
    data class ShowToast(val message: String) : ProjectUiEvent()
    data class ScrollToIndex(val index: Int) : ProjectUiEvent()
    object FocusSearchField : ProjectUiEvent()
    data class NavigateToEditProjectScreen(val projectId: String) : ProjectUiEvent()
    data class Navigate(val route: String) : ProjectUiEvent()
    data class NavigateToDayPlan(val date: Long) : ProjectUiEvent()
}

data class SearchResult(
    val project: Project,
    val path: List<BreadcrumbItem>
)

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

sealed class DialogState {
    object Hidden : DialogState()
    data class AddProject(val parentId: String?) : DialogState()
    data class ContextMenu(val project: Project) : DialogState()
    data class ConfirmDelete(val project: Project) : DialogState()
    data class EditProject(val project: Project) : DialogState()
    object AboutApp : DialogState()
    data class ConfirmFullImport(val uri: Uri) : DialogState()
}

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
    val maxCollapsibleLevels: Int = 3,
    val useBreadcrumbsAfter: Int = 2,
    val maxIndentation: Dp = 120.dp
)

enum class DropPosition { BEFORE, AFTER }

