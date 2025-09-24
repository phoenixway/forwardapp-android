package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.dialogs.UiContext


data class MainScreenUiState(
    
    val subStateStack: List<MainSubState> = listOf(MainSubState.Hierarchy),
    
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchHistory: List<String> = emptyList(),
    
    val projectHierarchy: ListHierarchyData = ListHierarchyData(),
    val currentBreadcrumbs: List<BreadcrumbItem> = emptyList(),
    val areAnyProjectsExpanded: Boolean = false,
    
    val planningMode: PlanningMode = PlanningMode.All,
    val planningSettings: PlanningSettingsState = PlanningSettingsState(),
    
    val dialogState: DialogState = DialogState.Hidden,
    val showRecentListsSheet: Boolean = false,
    val isBottomNavExpanded: Boolean = false,
    
    val recentItems: List<com.romankozak.forwardappmobile.data.database.models.RecentItem> = emptyList(),
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
    val showSearchDialog: Boolean = false,
    val searchResults: List<SearchResult> = emptyList(),
    val recordForReminderDialog: com.romankozak.forwardappmobile.data.database.models.ActivityRecord? = null,
) {
    val currentSubState: MainSubState
        get() = subStateStack.last()
}
