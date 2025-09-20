package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.dialogs.UiContext

/**
 * Єдина модель, що представляє весь стан головного екрану.
 */
data class MainScreenUiState(
    // Стек підстанів головного екрану
    val subStateStack: List<MainSubState> = listOf(MainSubState.Hierarchy),

    // Стан пошуку
    val searchQuery: TextFieldValue = TextFieldValue(""),
    val searchHistory: List<String> = emptyList(),

    // Стан ієрархії
    val projectHierarchy: ListHierarchyData = ListHierarchyData(),
    val currentBreadcrumbs: List<BreadcrumbItem> = emptyList(),
    val areAnyProjectsExpanded: Boolean = false,

    // Стан режимів планування
    val planningMode: PlanningMode = PlanningMode.All,
    val planningSettings: PlanningSettingsState = PlanningSettingsState(),

    // Стан діалогів та нижніх панелей
    val dialogState: DialogState = DialogState.Hidden,
    val showRecentListsSheet: Boolean = false,
    val isBottomNavExpanded: Boolean = false,

    // Дані для діалогів та панелей
    val recentProjects: List<Project> = emptyList(),
    val allContexts: List<UiContext> = emptyList(),
    val listChooserFinalExpandedIds: Set<String> = emptySet(),
    val filteredListHierarchyForDialog: ListHierarchyData = ListHierarchyData(),

    // Стан кастомної історії навігації
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val showNavigationMenu: Boolean = false,

    // Інший стан UI
    val isProcessingReveal: Boolean = false,
    val isReadyForFiltering: Boolean = false,

    val obsidianVaultName: String = "",
    val appStatistics: AppStatistics = AppStatistics(),
    val showWifiServerDialog: Boolean = false,
    val wifiServerAddress: String? = null,
    val showWifiImportDialog: Boolean = false,
    val desktopAddress: String = "",
    val showSearchDialog: Boolean = false
) {
    val currentSubState: MainSubState
        get() = subStateStack.last()
}
