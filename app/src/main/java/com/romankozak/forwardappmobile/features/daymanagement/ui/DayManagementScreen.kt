package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayThemesViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DayManagementScreen(
    mainNavController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: DayManagementViewModel = hiltViewModel(),
    runtimeViewModel: DayManagementRuntimeViewModel = hiltViewModel(),
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
    activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel(),
    dayFocusesViewModel: DayFocusesViewModel = hiltViewModel(),
    dayThemesViewModel: DayThemesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    startTab: String? = null,
    currentDayManagementTab: DayManagementTab? = null,
    showFabMenu: Boolean = true,
) {
    val screenLogTag = "NAV_DEBUG"

    val uiState by viewModel.uiState.collectAsState()
    val runtimeUiState by runtimeViewModel.uiState.collectAsState()
    val runtimeDefaultTodayTab = defaultTodayTabForRuntimeState(runtimeUiState.runtimeState)
    val requestedStartTab =
        remember(startTab, currentDayManagementTab, runtimeDefaultTodayTab) {
            currentDayManagementTab ?: DayManagementTab.fromRouteValue(startTab) ?: runtimeDefaultTodayTab
        }
    val tabs =
        remember(requestedStartTab) {
            if (requestedStartTab in DayManagementTab.todaySubTabs()) {
                DayManagementTab.todaySubTabs().toTypedArray()
            } else {
                DayManagementTab.entries.toTypedArray()
            }
        }
    val initialPage =
        remember(requestedStartTab) { tabs.indexOfFirst { it == requestedStartTab }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
    val snackbarHostState = remember { SnackbarHostState() }
    var addTaskTrigger by remember { mutableStateOf(0) }

    var isFabMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(requestedStartTab) {
        viewModel.selectTab(requestedStartTab)
    }

    LaunchedEffect(uiState.selectedTab, pagerState) {
        val pageIndex = tabs.indexOf(uiState.selectedTab)
        if (pageIndex >= 0 && pagerState.currentPage != pageIndex) {
            pagerState.scrollToPage(pageIndex)
        }
    }

    DayManagementNavigationEffects(
        viewModel = viewModel,
        uiState = uiState,
        navigationManager = navigationManager,
        mainNavController = mainNavController,
        snackbarHostState = snackbarHostState,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (showFabMenu && uiState.selectedTab != DayManagementTab.DAY_THEMES) {
                DayManagementFabMenu(
                    isFabMenuExpanded = isFabMenuExpanded,
                    onToggleExpanded = { isFabMenuExpanded = !isFabMenuExpanded },
                    onDismiss = { isFabMenuExpanded = false },
                    dayPlanViewModel = dayPlanViewModel,
                )
            }
        },
    ) { innerPadding ->
        val contentArgs =
            DayManagementScreenContentArgs(
                uiState = uiState,
                pagerState = pagerState,
                tabs = tabs,
                mainNavController = mainNavController,
                navigationManager = navigationManager,
                runtimeViewModel = runtimeViewModel,
                dayPlanViewModel = dayPlanViewModel,
                activityTrackerViewModel = activityTrackerViewModel,
                dayFocusesViewModel = dayFocusesViewModel,
                dayThemesViewModel = dayThemesViewModel,
                addTaskTrigger = addTaskTrigger,
                screenLogTag = screenLogTag,
            )
        DayManagementScreenContent(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            args = contentArgs,
            onRetry = viewModel::retryLoading,
        )
    }
}
