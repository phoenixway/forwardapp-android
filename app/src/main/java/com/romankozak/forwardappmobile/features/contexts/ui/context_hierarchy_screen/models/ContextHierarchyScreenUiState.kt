package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.features.sync.WifiSyncStatus
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker

typealias MainScreenUiState = ProjectHierarchyScreenUiState

data class ContextRoleOption(
    val code: String,
    val label: String,
)

data class ProjectHierarchyScreenUiState(
    val subStateStack: List<MainSubState> = listOf(ProjectHierarchyScreenSubState.Hierarchy),
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchHistory: List<String> = emptyList(),
    val projectHierarchy: ContextHierarchyData = ContextHierarchyData(),
    val flattenedHierarchy: List<FlatHierarchyItem> = emptyList(),
    val beaconRootedHierarchy: List<BeaconRootedHierarchyItem> = emptyList(),
    val longDescendantsMap: Map<String, Boolean> = emptyMap(),
    val currentBreadcrumbs: List<BreadcrumbItem> = emptyList(),
    val planningSettings: PlanningSettingsState = PlanningSettingsState(),
    val dialogState: DialogState = DialogState.Hidden,
    val showRecentListsSheet: Boolean = false,
    val isBottomNavExpanded: Boolean = false,
    val recentItems: List<RecentItem> = emptyList(),
    val allContextMarkers: List<UiContextMarker> = emptyList(),
    val listChooserFinalExpandedIds: Set<String> = emptySet(),
    val filteredListHierarchyForDialog: ContextHierarchyData = ContextHierarchyData(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showNavigationMenu: Boolean = false,
    val isProcessingReveal: Boolean = false,
    val isReadyForFiltering: Boolean = false,
    val obsidianVaultName: String = "",
    val appStatistics: AppStatistics = AppStatistics(),
    val showWifiServerDialog: Boolean = false,
    val wifiServerAddress: String? = null,
    val showWifiImportDialog: Boolean = false,
    val desktopAddress: String = "",
    val syncStatus: WifiSyncStatus = WifiSyncStatus.Disabled,
    val showSearchDialog: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val searchResultFilter: SearchResultFilter = SearchResultFilter.All,
    val searchResultSort: SearchResultSort = SearchResultSort.Relevance,
    val recordForReminderDialog: ActivityRecord? = null,
    val contextMarkerToEmojiMap: Map<String, String> = emptyMap(),
    val availableContextRoles: List<ContextRoleOption> = emptyList(),
    val featureToggles: Map<FeatureFlag, Boolean> = emptyMap(),
    val selectedContextIds: Set<String> = emptySet(),
    val clipboardContextIds: Set<String> = emptySet(),
    val clipboardOperation: ContextClipboardOperationUi? = null,
) {
    val currentSubState: MainSubState
        get() = subStateStack.last()

    val isSelectionMode: Boolean
        get() = selectedContextIds.isNotEmpty()
}

enum class ContextClipboardOperationUi {
    COPY,
    CUT,
}
