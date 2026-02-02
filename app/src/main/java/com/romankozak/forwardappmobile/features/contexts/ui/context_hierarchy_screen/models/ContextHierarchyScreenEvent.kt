package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.theme.ThemeMode
import com.romankozak.forwardappmobile.core.theme.ThemeName
import com.romankozak.forwardappmobile.features.settings.settings.models.PlanningSettings
import com.romankozak.forwardappmobile.ui.dialogs.UiContext

sealed interface ContextHierarchyScreenEvent {
    data class SearchQueryChanged(val query: TextFieldValue) : ContextHierarchyScreenEvent

    data class SearchFromHistory(val query: String) : ContextHierarchyScreenEvent

    data class GlobalSearchPerform(val query: String) : ContextHierarchyScreenEvent

    data class SearchResultClick(val projectId: String) : ContextHierarchyScreenEvent

    data class ContextClick(val projectId: String) : ContextHierarchyScreenEvent

    data class ContextMenuRequest(val project: Context) : ContextHierarchyScreenEvent

    data class ToggleContextExpanded(val project: Context) : ContextHierarchyScreenEvent

    data class ContextReorder(val fromId: String, val toId: String, val position: DropPosition) : ContextHierarchyScreenEvent

    data class BreadcrumbNavigation(val breadcrumb: BreadcrumbItem) : ContextHierarchyScreenEvent

    data object ClearBreadcrumbNavigation : ContextHierarchyScreenEvent

    data class PlanningModeChange(val mode: PlanningMode) : ContextHierarchyScreenEvent

    data object DismissDialog : ContextHierarchyScreenEvent

    data object AddNewContextRequest : ContextHierarchyScreenEvent

    data object AddNoteDocumentRequest : ContextHierarchyScreenEvent

    data object AddChecklistRequest : ContextHierarchyScreenEvent

    data class ListChooserResult(val projectId: String?) : ContextHierarchyScreenEvent

    data class DeleteRequest(val project: Context) : ContextHierarchyScreenEvent

    data class MoveRequest(val project: Context) : ContextHierarchyScreenEvent

    data class DeleteConfirm(val project: Context) : ContextHierarchyScreenEvent

    data class MoveConfirm(val newParentId: String?) : ContextHierarchyScreenEvent

    data class FullImportConfirm(val uri: Uri) : ContextHierarchyScreenEvent

    data class FullImportConfirmV2(val uri: Uri) : ContextHierarchyScreenEvent

    data object ShowAboutDialog : ContextHierarchyScreenEvent

    data class ImportFromFileRequest(val uri: Uri) : ContextHierarchyScreenEvent

    data class SelectiveImportFromFileRequest(val uri: Uri) : ContextHierarchyScreenEvent

    data object HomeClick : ContextHierarchyScreenEvent

    data object BackClick : ContextHierarchyScreenEvent

    data object ForwardClick : ContextHierarchyScreenEvent

    data object HistoryClick : ContextHierarchyScreenEvent

    data object HideHistory : ContextHierarchyScreenEvent

    data class BottomNavExpandedChange(val isExpanded: Boolean) : ContextHierarchyScreenEvent

    data object ShowRecentLists : ContextHierarchyScreenEvent

    data object DismissRecentLists : ContextHierarchyScreenEvent

    data class RecentItemSelected(val item: RecentItem) : ContextHierarchyScreenEvent

    data class RecentItemPinClick(val item: RecentItem) : ContextHierarchyScreenEvent

    data object DayPlanClick : ContextHierarchyScreenEvent

    data class ContextSelected(val name: String) : ContextHierarchyScreenEvent

    data object CommandDeckClick : ContextHierarchyScreenEvent

    data class EditRequest(val project: Context) : ContextHierarchyScreenEvent

    data class AddToDayPlanRequest(val project: Context) : ContextHierarchyScreenEvent

    data class SetReminderRequest(val project: Context) : ContextHierarchyScreenEvent

    data class FocusContext(val project: Context) : ContextHierarchyScreenEvent

    data object GoToSettings : ContextHierarchyScreenEvent

    data object ShowSearchDialog : ContextHierarchyScreenEvent

    data object DismissSearchDialog : ContextHierarchyScreenEvent

    data object ShowWifiServerDialog : ContextHierarchyScreenEvent

    data object ShowWifiImportDialog : ContextHierarchyScreenEvent

    data class WifiPush(val address: String) : ContextHierarchyScreenEvent

    data object ExportToFile : ContextHierarchyScreenEvent

    data object ExportToFileV2 : ContextHierarchyScreenEvent

    data object ExportAttachments : ContextHierarchyScreenEvent

    data class ImportAttachmentsFromFile(val uri: Uri) : ContextHierarchyScreenEvent

    object NavigateToChat : ContextHierarchyScreenEvent

    object NavigateToActivityTracker : ContextHierarchyScreenEvent

    object NavigateToAiInsights : ContextHierarchyScreenEvent

    object NavigateToLifeState : ContextHierarchyScreenEvent

    object NavigateToStrategicManagement : ContextHierarchyScreenEvent

    data class SaveSettings(val settings: PlanningSettings) : ContextHierarchyScreenEvent

    data class SaveAllContexts(val updatedContexts: List<UiContext>) : ContextHierarchyScreenEvent

    data object DismissWifiServerDialog : ContextHierarchyScreenEvent

    data object DismissWifiImportDialog : ContextHierarchyScreenEvent

    data class DesktopAddressChange(val address: String) : ContextHierarchyScreenEvent

    data class PerformWifiImport(val address: String) : ContextHierarchyScreenEvent

    data class AddContextConfirm(val name: String, val parentId: String?) : ContextHierarchyScreenEvent

    data class AddSubprojectRequest(val parentProject: Context) : ContextHierarchyScreenEvent

    data object CloseSearch : ContextHierarchyScreenEvent

    data class NavigateToContext(val projectId: String) : ContextHierarchyScreenEvent

    data object CollapseAll : ContextHierarchyScreenEvent

    data class UpdateLightTheme(val themeName: ThemeName) : ContextHierarchyScreenEvent

    data class UpdateDarkTheme(val themeName: ThemeName) : ContextHierarchyScreenEvent

    data class UpdateThemeMode(val themeMode: ThemeMode) : ContextHierarchyScreenEvent

    data object GoToReminders : ContextHierarchyScreenEvent

    data class RevealContextInHierarchy(val projectId: String) : ContextHierarchyScreenEvent

    object OpenInboxContext : ContextHierarchyScreenEvent

    object NavigateToActivityTrackerScreen : ContextHierarchyScreenEvent

    object OpenAttachmentsLibrary : ContextHierarchyScreenEvent

    object OpenScriptsLibrary : ContextHierarchyScreenEvent

    object AddScriptRequest : ContextHierarchyScreenEvent

    object NavigateToTacticsScreen : ContextHierarchyScreenEvent

    object NavigateToContextLab : ContextHierarchyScreenEvent
}
