// File: com/romankozak/forwardappmobile/ui/screens/mainscreen/GoalListEvents.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.net.Uri
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList

// UI Events
sealed class GoalListUiEvent {
    data class NavigateToSyncScreenWithData(val json: String) : GoalListUiEvent()
    data class NavigateToDetails(val listId: String) : GoalListUiEvent()
    data class NavigateToGlobalSearch(val query: String) : GoalListUiEvent()
    object NavigateToSettings : GoalListUiEvent()
    data class ShowToast(val message: String) : GoalListUiEvent()
    data class ScrollToIndex(val index: Int) : GoalListUiEvent()
    object FocusSearchField : GoalListUiEvent()
    data class NavigateToEditListScreen(val listId: String) : GoalListUiEvent()
    data class Navigate(val route: String) : GoalListUiEvent()
    data class NavigateToDayPlan(val date: Long) : GoalListUiEvent()
}

// Search Related
data class SearchResult(
    val list: GoalList,
    val path: List<BreadcrumbItem>
)

// Planning Modes
sealed class PlanningMode {
    object All : PlanningMode()
    object Daily : PlanningMode()
    object Medium : PlanningMode()
    object Long : PlanningMode()
}

// Statistics
data class AppStatistics(
    val listCount: Int = 0,
    val goalCount: Int = 0
)

// Dialog States
sealed class DialogState {
    object Hidden : DialogState()
    data class AddList(val parentId: String?) : DialogState()
    data class ContextMenu(val list: GoalList) : DialogState()
    data class ConfirmDelete(val list: GoalList) : DialogState()
    data class EditList(val list: GoalList) : DialogState()
    object AboutApp : DialogState()
    data class ConfirmFullImport(val uri: Uri) : DialogState()
}

// Planning Settings
data class PlanningSettingsState(
    val showModes: Boolean = false,
    val dailyTag: String = "daily",
    val mediumTag: String = "medium",
    val longTag: String = "long",
)

// Navigation
data class BreadcrumbItem(
    val id: String,
    val name: String,
    val level: Int
)

// Hierarchy Display
data class HierarchyDisplaySettings(
    val maxCollapsibleLevels: Int = 3,
    val useBreadcrumbsAfter: Int = 2,
    val maxIndentation: Dp = 120.dp
)

// Drag and Drop
enum class DropPosition { BEFORE, AFTER }

// Filter State
internal data class FilterState(
    val flatList: List<GoalList>,
    val query: String,
    val searchActive: Boolean,
    val mode: PlanningMode,
    val settings: PlanningSettingsState,
)