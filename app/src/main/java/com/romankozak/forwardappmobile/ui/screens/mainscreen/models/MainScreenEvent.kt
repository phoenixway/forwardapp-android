package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.dialogs.UiContext

/**
 * Усі можливі дії користувача на головному екрані, що надсилаються з UI у ViewModel.
 */
sealed interface MainScreenEvent {
    // Події пошуку
    data class SearchQueryChanged(val query: TextFieldValue) : MainScreenEvent
    data class SearchFromHistory(val query: String) : MainScreenEvent
    data class GlobalSearchPerform(val query: String) : MainScreenEvent
    data class SearchResultClick(val projectId: String) : MainScreenEvent

    // Події ієрархії
    data class ProjectClick(val projectId: String) : MainScreenEvent
    data class ProjectMenuRequest(val project: Project) : MainScreenEvent
    data class ToggleProjectExpanded(val project: Project) : MainScreenEvent
    data class ProjectReorder(val fromId: String, val toId: String, val position: DropPosition) : MainScreenEvent

    // Події навігації в ієрархії (хлібні крихти)
    data class BreadcrumbNavigation(val breadcrumb: BreadcrumbItem) : MainScreenEvent
    data object ClearBreadcrumbNavigation : MainScreenEvent

    // Події режимів планування
    data class PlanningModeChange(val mode: PlanningMode) : MainScreenEvent

    // Події діалогів
    data object DismissDialog : MainScreenEvent
    data object AddNewProjectRequest : MainScreenEvent
    data class DeleteRequest(val project: Project) : MainScreenEvent
    data class MoveRequest(val project: Project) : MainScreenEvent
    data class DeleteConfirm(val project: Project) : MainScreenEvent
    data class MoveConfirm(val newParentId: String?) : MainScreenEvent
    data class FullImportConfirm(val uri: Uri) : MainScreenEvent
    data object ShowAboutDialog : MainScreenEvent
    data class ImportFromFileRequest(val uri: Uri) : MainScreenEvent

    // Події головної навігації (браузерної)
    data object HomeClick : MainScreenEvent
    data object BackClick : MainScreenEvent
    data object ForwardClick : MainScreenEvent
    data object HistoryClick : MainScreenEvent
    data object HideHistory : MainScreenEvent

    // Події нижньої панелі та модальних вікон
    data class BottomNavExpandedChange(val isExpanded: Boolean) : MainScreenEvent
    data object ShowRecentLists : MainScreenEvent
    data object DismissRecentLists : MainScreenEvent
    data class RecentProjectSelected(val projectId: String) : MainScreenEvent
    data object DayPlanClick : MainScreenEvent
    data class ContextSelected(val name: String) : MainScreenEvent

    // Загальні дії
    data class EditRequest(val project: Project): MainScreenEvent
    data object GoToSettings: MainScreenEvent
    data object ShowSearchDialog: MainScreenEvent
    data object DismissSearchDialog: MainScreenEvent

    // Дії з меню
    data object ShowWifiServerDialog: MainScreenEvent
    data object ShowWifiImportDialog: MainScreenEvent
    data object ExportToFile: MainScreenEvent

    object NavigateToChat : MainScreenEvent
    object NavigateToActivityTracker : MainScreenEvent
    object NavigateToAiInsights : MainScreenEvent

    data class SaveSettings(
        val show: Boolean,
        val daily: String,
        val medium: String,
        val long: String,
        val vaultName: String
    ) : MainScreenEvent

    data class SaveAllContexts(val updatedContexts: List<UiContext>) : MainScreenEvent

    data object DismissWifiServerDialog : MainScreenEvent
    data object DismissWifiImportDialog : MainScreenEvent
    data class DesktopAddressChange(val address: String) : MainScreenEvent
    data class PerformWifiImport(val address: String) : MainScreenEvent

    data class AddProjectConfirm(val name: String, val parentId: String?) : MainScreenEvent
    data class AddSubprojectRequest(val parentProject: Project) : MainScreenEvent

    data object CloseSearch : MainScreenEvent

    data class NavigateToProject(val projectId: String) : MainScreenEvent
    data object CollapseAll : MainScreenEvent


}
