package com.romankozak.forwardappmobile.features.mainscreen

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.StrategicArcBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.TacticsBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.TodayBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel
import com.romankozak.forwardappmobile.features.recent.RecentViewModel

@Composable
internal fun MainScreenBottomBar(
    currentRoute: String,
    onOpenStrategicArtifact: (String) -> Unit,
    contextHierarchyViewModel: ContextHierarchyScreenViewModel,
    onNavigateToProjectHierarchy: () -> Unit,
    onShowContextMarkersSheet: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToQuickCatch: () -> Unit,
    onShowCreateActions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFileRequest: (Uri) -> Unit,
    onSelectiveImportFromFileRequest: (Uri) -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    onWifiPush: (String) -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onNavigateToEntityLibrary: () -> Unit,
    onShowAbout: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel,
    commandDeckViewModel: CommandDeckViewModel,
    dayManagementViewModel: DayManagementViewModel,
    dayManagementRuntimeViewModel: DayManagementRuntimeViewModel,
    dayManagementRuntimeUiState: DayManagementRuntimeUiState,
    defaultTodayTab: DayManagementTab,
    dayPlanViewModel: DayPlanViewModel,
    activityTrackerViewModel: ActivityTrackerViewModel,
    dayFocusesViewModel: DayFocusesViewModel,
    tacticalMissionViewModel: TacticalMissionViewModel,
) {
    val contextUiState by contextHierarchyViewModel.uiState.collectAsStateWithLifecycle()
    val dayManagementUiState by dayManagementViewModel.uiState.collectAsStateWithLifecycle()
    val tacticalActiveIteration by tacticalMissionViewModel.activeIteration.collectAsStateWithLifecycle()
    val isTacticalExecution =
        currentRoute == MAIN_SCREEN_TACTICS_ROUTE &&
            tacticalActiveIteration?.status == TacticalIterationStatus.ACTIVE
    val globalActions =
        BottomPanelGlobalActions(
            onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
            onShowContextMarkersSheet = onShowContextMarkersSheet,
            onNavigateToPresets = onNavigateToPresets,
            onNavigateToGlobalSearch = onNavigateToGlobalSearch,
            onNavigateToQuickCatch = onNavigateToQuickCatch,
            onShowCreateActions = onShowCreateActions,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToInbox = onNavigateToInbox,
            onNavigateToTracker = onNavigateToTracker,
            onNavigateToReminders = onNavigateToReminders,
            onNavigateToAiChat = onNavigateToAiChat,
            onNavigateToAiInsights = onNavigateToAiInsights,
            onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
            onExportToFile = onExportToFile,
            onImportFromFileRequest = onImportFromFileRequest,
            onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
            onExportAttachments = onExportAttachments,
            onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
            onWifiPush = onWifiPush,
            onShowWifiServer = onShowWifiServer,
            onShowWifiImport = onShowWifiImport,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToScripts = onNavigateToScripts,
            onNavigateToEntityLibrary = onNavigateToEntityLibrary,
            onShowAbout = onShowAbout,
            featureToggles = contextUiState.featureToggles,
            onNavigateToRecentItem = onNavigateToRecentItem,
        )

    MainScreenDock(currentRoute, globalActions, recentViewModel) {
      when (currentRoute) {
        MAIN_SCREEN_TODAY_ROUTE ->
            TodayBottomPanel(
                selectedDate = dayManagementUiState.selectedDate,
                onDateSelected = dayManagementViewModel::navigateToDate,
                currentTab =
                    dayManagementUiState.selectedTab
                        .takeIf { it in DayManagementTab.todaySubTabs() }
                        ?: defaultTodayTab,
                onSelectTodayTab = { tab ->
                    dayManagementViewModel.selectTab(tab)
                    commandDeckViewModel.setSelectedTodayTab(tab)
                },
                runtimeUiState = dayManagementRuntimeUiState,
                onWakeUp = dayManagementRuntimeViewModel::wakeUp,
                onFinalizeThemes = dayManagementRuntimeViewModel::finalizeThemes,
                onFinalizeFocus = dayManagementRuntimeViewModel::finalizeFocus,
                onFinalizePlan = dayManagementRuntimeViewModel::finalizePlan,
                onStartImplementation = dayManagementRuntimeViewModel::startImplementation,
                onStartFinalization = dayManagementRuntimeViewModel::startFinalization,
                onSleep = dayManagementRuntimeViewModel::sleep,
                dayPlanViewModel = dayPlanViewModel,
                activityTrackerViewModel = activityTrackerViewModel,
                dayFocusesViewModel = dayFocusesViewModel,
            )
        MAIN_SCREEN_TACTICS_ROUTE -> TacticsBottomPanel(
            viewModel = tacticalMissionViewModel, execution = isTacticalExecution,
        )
        MAIN_SCREEN_STRATEGIC_ARC_ROUTE ->
            StrategicArcBottomPanel(onOpenArtifact = onOpenStrategicArtifact)
        else -> Unit
      }
    }
}
