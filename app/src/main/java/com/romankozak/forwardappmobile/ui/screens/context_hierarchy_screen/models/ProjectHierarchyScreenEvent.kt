package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.dialogs.UiContext


import com.romankozak.forwardappmobile.ui.screens.settings.models.PlanningSettings

sealed interface ProjectHierarchyScreenEvent {
    
    data class SearchQueryChanged(val query: TextFieldValue) : ProjectHierarchyScreenEvent

    data class SearchFromHistory(val query: String) : ProjectHierarchyScreenEvent

    data class GlobalSearchPerform(val query: String) : ProjectHierarchyScreenEvent

    data class SearchResultClick(val projectId: String) : ProjectHierarchyScreenEvent

    
    data class ProjectClick(val projectId: String) : ProjectHierarchyScreenEvent

    data class ProjectMenuRequest(val project: Project) : ProjectHierarchyScreenEvent

    data class ToggleProjectExpanded(val project: Project) : ProjectHierarchyScreenEvent

    data class ProjectReorder(val fromId: String, val toId: String, val position: DropPosition) : ProjectHierarchyScreenEvent

    
    data class BreadcrumbNavigation(val breadcrumb: BreadcrumbItem) : ProjectHierarchyScreenEvent

    data object ClearBreadcrumbNavigation : ProjectHierarchyScreenEvent

    
    data class PlanningModeChange(val mode: PlanningMode) : ProjectHierarchyScreenEvent


    data object DismissDialog : ProjectHierarchyScreenEvent

    data object AddNewProjectRequest : ProjectHierarchyScreenEvent
    data object AddNoteDocumentRequest : ProjectHierarchyScreenEvent
    data object AddChecklistRequest : ProjectHierarchyScreenEvent
    data class ListChooserResult(val projectId: String?) : ProjectHierarchyScreenEvent

    data class DeleteRequest(val project: Project) : ProjectHierarchyScreenEvent

    data class MoveRequest(val project: Project) : ProjectHierarchyScreenEvent

    data class DeleteConfirm(val project: Project) : ProjectHierarchyScreenEvent

    data class MoveConfirm(val newParentId: String?) : ProjectHierarchyScreenEvent

    data class FullImportConfirm(val uri: Uri) : ProjectHierarchyScreenEvent

    data object ShowAboutDialog : ProjectHierarchyScreenEvent

    data class ImportFromFileRequest(val uri: Uri) : ProjectHierarchyScreenEvent
    data class SelectiveImportFromFileRequest(val uri: Uri) : ProjectHierarchyScreenEvent

    
    data object HomeClick : ProjectHierarchyScreenEvent

    data object BackClick : ProjectHierarchyScreenEvent

    data object ForwardClick : ProjectHierarchyScreenEvent

    data object HistoryClick : ProjectHierarchyScreenEvent

    data object HideHistory : ProjectHierarchyScreenEvent

    
    data class BottomNavExpandedChange(val isExpanded: Boolean) : ProjectHierarchyScreenEvent

    data object ShowRecentLists : ProjectHierarchyScreenEvent

    data object DismissRecentLists : ProjectHierarchyScreenEvent

    data class RecentItemSelected(val item: com.romankozak.forwardappmobile.data.database.models.RecentItem) : ProjectHierarchyScreenEvent

    data class RecentItemPinClick(val item: com.romankozak.forwardappmobile.data.database.models.RecentItem) : ProjectHierarchyScreenEvent

    data object DayPlanClick : ProjectHierarchyScreenEvent

    data class ContextSelected(val name: String) : ProjectHierarchyScreenEvent

    data object CommandDeckClick : ProjectHierarchyScreenEvent

    
    data class EditRequest(val project: Project) : ProjectHierarchyScreenEvent
    data class AddToDayPlanRequest(val project: Project) : ProjectHierarchyScreenEvent
    data class SetReminderRequest(val project: Project) : ProjectHierarchyScreenEvent
    data class FocusProject(val project: Project) : ProjectHierarchyScreenEvent

    data object GoToSettings : ProjectHierarchyScreenEvent

    data object ShowSearchDialog : ProjectHierarchyScreenEvent

    data object DismissSearchDialog : ProjectHierarchyScreenEvent


    data object ShowWifiServerDialog : ProjectHierarchyScreenEvent

    data object ShowWifiImportDialog : ProjectHierarchyScreenEvent
    data class WifiPush(val address: String) : ProjectHierarchyScreenEvent

    data object ExportToFile : ProjectHierarchyScreenEvent
    data object ExportAttachments : ProjectHierarchyScreenEvent
    data class ImportAttachmentsFromFile(val uri: Uri) : ProjectHierarchyScreenEvent

    object NavigateToChat : ProjectHierarchyScreenEvent

    object NavigateToActivityTracker : ProjectHierarchyScreenEvent

    object NavigateToAiInsights : ProjectHierarchyScreenEvent
    object NavigateToLifeState : ProjectHierarchyScreenEvent

    object NavigateToStrategicManagement : ProjectHierarchyScreenEvent

    data class SaveSettings(val settings: PlanningSettings) : ProjectHierarchyScreenEvent

    data class SaveAllContexts(val updatedContexts: List<UiContext>) : ProjectHierarchyScreenEvent

    data object DismissWifiServerDialog : ProjectHierarchyScreenEvent

    data object DismissWifiImportDialog : ProjectHierarchyScreenEvent

    data class DesktopAddressChange(val address: String) : ProjectHierarchyScreenEvent

    data class PerformWifiImport(val address: String) : ProjectHierarchyScreenEvent

    data class AddProjectConfirm(val name: String, val parentId: String?) : ProjectHierarchyScreenEvent

    data class AddSubprojectRequest(val parentProject: Project) : ProjectHierarchyScreenEvent

    data object CloseSearch : ProjectHierarchyScreenEvent

    data class NavigateToProject(val projectId: String) : ProjectHierarchyScreenEvent

    data object CollapseAll : ProjectHierarchyScreenEvent

    data class UpdateLightTheme(val themeName: com.romankozak.forwardappmobile.ui.theme.ThemeName) : ProjectHierarchyScreenEvent
    data class UpdateDarkTheme(val themeName: com.romankozak.forwardappmobile.ui.theme.ThemeName) : ProjectHierarchyScreenEvent
    data class UpdateThemeMode(val themeMode: com.romankozak.forwardappmobile.ui.theme.ThemeMode) : ProjectHierarchyScreenEvent
    data object GoToReminders : ProjectHierarchyScreenEvent
    data class RevealProjectInHierarchy(val projectId: String) : ProjectHierarchyScreenEvent
    object OpenInboxProject : ProjectHierarchyScreenEvent
    object NavigateToActivityTrackerScreen : ProjectHierarchyScreenEvent
    object OpenAttachmentsLibrary : ProjectHierarchyScreenEvent
    object OpenScriptsLibrary : ProjectHierarchyScreenEvent
    object AddScriptRequest : ProjectHierarchyScreenEvent
    object NavigateToTacticsScreen : ProjectHierarchyScreenEvent
}
