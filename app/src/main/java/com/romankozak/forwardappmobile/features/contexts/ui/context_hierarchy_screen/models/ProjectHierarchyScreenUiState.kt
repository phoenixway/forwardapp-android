package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.features.sync.WifiSyncStatus


typealias MainScreenUiState = ProjectHierarchyScreenUiState

data class ProjectHierarchyScreenUiState(

    val subStateStack: List<MainSubState> = listOf(ProjectHierarchyScreenSubState.Hierarchy),

    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchHistory: List<String> = emptyList(),

    val projectHierarchy: ListHierarchyData = ListHierarchyData(),
    val flattenedHierarchy: List<FlatHierarchyItem> = emptyList(),
    val currentBreadcrumbs: List<BreadcrumbItem> = emptyList(),
    val areAnyProjectsExpanded: Boolean = false,

    val planningMode: PlanningMode = PlanningMode.All,
    val planningSettings: PlanningSettingsState = PlanningSettingsState(),

    val dialogState: DialogState = DialogState.Hidden,
    val showRecentListsSheet: Boolean = false,
    val isBottomNavExpanded: Boolean = false,

    val recentItems: List<RecentItem> = emptyList(),
    val allContexts: List<UiContext> = emptyList(),
    val listChooserFinalExpandedIds: Set<String> = emptySet(),
    val filteredListHierarchyForDialog: ListHierarchyData = ListHierarchyData(),

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
    val recordForReminderDialog: ActivityRecord? = null,
    val contextMarkerToEmojiMap: Map<String, String> = emptyMap(),
    val featureToggles: Map<FeatureFlag, Boolean> = emptyMap(),
) {
    val currentSubState: MainSubState
        get() = subStateStack.last()
}
