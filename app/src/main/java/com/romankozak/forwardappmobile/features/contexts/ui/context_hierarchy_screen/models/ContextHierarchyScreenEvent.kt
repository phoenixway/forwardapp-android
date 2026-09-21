package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.theme.ThemeMode
import com.romankozak.forwardappmobile.core.theme.ThemeName
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogPasteMode
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.features.settings.settings.models.PlanningSettings
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker

sealed interface ContextHierarchyScreenEvent {
    data class SearchQueryChanged(val query: TextFieldValue) : ContextHierarchyScreenEvent

    data class SearchFromHistory(val query: String) : ContextHierarchyScreenEvent

    data class RemoveSearchHistoryEntry(val query: String) : ContextHierarchyScreenEvent

    data object ClearSearchHistory : ContextHierarchyScreenEvent

    data class SearchFilterChanged(val filter: SearchResultFilter) : ContextHierarchyScreenEvent

    data class SearchSortChanged(val sort: SearchResultSort) : ContextHierarchyScreenEvent

    data class GlobalSearchPerform(val query: String) : ContextHierarchyScreenEvent

    data class SearchResultClick(
        val projectId: String,
        val placementId: String? = null,
    ) : ContextHierarchyScreenEvent

    data class ContextClick(val projectId: String) : ContextHierarchyScreenEvent

    /** Read-side stable-ID expansion for Context/Workspace presentation nodes. */

    /**
     * Session-only collapse state for structural Orientation containers
     * (Group, Beacon, NoGroup, NoBeacon). It never writes Context.isExpanded.
     */

    data class OrientationNodeClick(
        val nodeId: String,
        val placementId: String? = null,
    ) : ContextHierarchyScreenEvent

    data class ContextMenuRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class MigrateRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class MigrationChoiceSelected(val choice: ContextMigrationChoice) : ContextHierarchyScreenEvent

    data class MigrationOrientationKindSelected(val kind: OrientationKind) : ContextHierarchyScreenEvent

    data class MigrationExistingAspectSelected(val id: String) : ContextHierarchyScreenEvent

    data class MigrationExistingOrientationSelected(val id: String) : ContextHierarchyScreenEvent

    data object MigrationConfirmationRequested : ContextHierarchyScreenEvent

    data object MigrationExecute : ContextHierarchyScreenEvent

    data class ContextReorder(
        val fromId: String,
        val toId: String,
        val position: DropPosition,
    ) : ContextHierarchyScreenEvent

    data object ToggleSiblingReorderMode : ContextHierarchyScreenEvent

    data class ReorderContextSiblings(
        val parentContextId: String?,
        val orderedContextIds: List<String>,
    ) : ContextHierarchyScreenEvent

    data class ReorderOrientationBeaconSiblings(
        val parentNodeId: String,
        val orderedBeaconIds: List<String>,
    ) : ContextHierarchyScreenEvent

    data class ReorderOrientationGroups(
        val orderedGroupIds: List<String>,
    ) : ContextHierarchyScreenEvent

    /** Focus a read-side hierarchy project by stable id, including shell-free Workspaces. */
    data class FocusHierarchyProject(
        val projectId: String,
        val placementId: String? = null,
    ) : ContextHierarchyScreenEvent

    data class BreadcrumbNavigation(val breadcrumb: BreadcrumbItem) : ContextHierarchyScreenEvent

    data object ClearBreadcrumbNavigation : ContextHierarchyScreenEvent

    data object DismissDialog : ContextHierarchyScreenEvent

    data object AddNewContextRequest : ContextHierarchyScreenEvent

    data object AddNoteDocumentRequest : ContextHierarchyScreenEvent

    data object AddChecklistRequest : ContextHierarchyScreenEvent

    data class AddNoteDocumentToContextRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class AddChecklistToContextRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class ListChooserResult(val projectId: String?) : ContextHierarchyScreenEvent

    data class DeleteRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class MoveRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class DeleteConfirm(val projectId: String) : ContextHierarchyScreenEvent

    data class MoveConfirm(val newParentId: String?) : ContextHierarchyScreenEvent

    data class RestoreConfirm(val uri: Uri) : ContextHierarchyScreenEvent

    data class MergeConfirm(val uri: Uri) : ContextHierarchyScreenEvent

    data object ShowAboutDialog : ContextHierarchyScreenEvent

    data class ImportFromFileRequest(val uri: Uri) : ContextHierarchyScreenEvent

    data class RestoreImportRequest(val uri: Uri) : ContextHierarchyScreenEvent

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

    data class EditRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class OpenContextRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class AddToDayPlanRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class AddToDayFocusRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class SetReminderRequest(val projectId: String) : ContextHierarchyScreenEvent

    data class ToggleUserFocusContext(val projectId: String) : ContextHierarchyScreenEvent

    data class CopyWorkspace(val projectId: String) : ContextHierarchyScreenEvent

    data class CutWorkspace(val projectId: String) : ContextHierarchyScreenEvent

    data class PasteWorkspace(val projectId: String) : ContextHierarchyScreenEvent

    data class CopyContextLink(val projectId: String) : ContextHierarchyScreenEvent

    data class CutContextLink(val projectId: String) : ContextHierarchyScreenEvent

    data class PasteContextLink(
        val projectId: String,
        val mode: BacklogPasteMode = BacklogPasteMode.AS_LINK,
    ) : ContextHierarchyScreenEvent

    data class PasteContextLinksIntoBeacon(val beaconNodeId: String) : ContextHierarchyScreenEvent

    data object PasteContextLinksIntoNoBeacon : ContextHierarchyScreenEvent

    data class CopyBeacon(val beaconNodeId: String) : ContextHierarchyScreenEvent

    data class CopyBeaconAsLink(val beaconNodeId: String) : ContextHierarchyScreenEvent

    data class CutBeacon(val beaconNodeId: String) : ContextHierarchyScreenEvent

    data class PasteBeaconIntoBeacon(val beaconNodeId: String) : ContextHierarchyScreenEvent

    data class PasteBeaconIntoGroup(val groupNodeId: String?) : ContextHierarchyScreenEvent

    data class AddContextAppearanceHere(val parentProjectId: String) : ContextHierarchyScreenEvent

    data class ToggleContextSelection(val projectId: String) : ContextHierarchyScreenEvent

    data class StartContextSelection(val projectId: String) : ContextHierarchyScreenEvent

    data object ClearContextSelection : ContextHierarchyScreenEvent

    data object CopySelectedContexts : ContextHierarchyScreenEvent

    data object CutSelectedContexts : ContextHierarchyScreenEvent

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

    data class SaveAllContextMarkers(val updatedContextMarkers: List<UiContextMarker>) : ContextHierarchyScreenEvent

    data object DismissWifiServerDialog : ContextHierarchyScreenEvent

    data object DismissWifiImportDialog : ContextHierarchyScreenEvent

    data class DesktopAddressChange(val address: String) : ContextHierarchyScreenEvent

    data class PerformWifiImport(val address: String) : ContextHierarchyScreenEvent

    data class AddContextConfirm(
        val name: String,
        val parentId: String?,
        val roleCode: String? = null,
    ) : ContextHierarchyScreenEvent

    data class AddSubprojectRequest(val parentProjectId: String) : ContextHierarchyScreenEvent

    data object CloseSearch : ContextHierarchyScreenEvent

    data class NavigateToContext(val projectId: String) : ContextHierarchyScreenEvent

    data class UpdateLightTheme(val themeName: ThemeName) : ContextHierarchyScreenEvent

    data class UpdateDarkTheme(val themeName: ThemeName) : ContextHierarchyScreenEvent

    data class UpdateThemeMode(val themeMode: ThemeMode) : ContextHierarchyScreenEvent

    data object GoToReminders : ContextHierarchyScreenEvent

    data class RevealContextInHierarchy(
        val projectId: String,
    ) : ContextHierarchyScreenEvent

    object OpenInboxContext : ContextHierarchyScreenEvent

    object NavigateToActivityTrackerScreen : ContextHierarchyScreenEvent

    object OpenAttachmentsLibrary : ContextHierarchyScreenEvent

    object OpenScriptsLibrary : ContextHierarchyScreenEvent

    object AddScriptRequest : ContextHierarchyScreenEvent

    object NavigateToTacticsScreen : ContextHierarchyScreenEvent
}
