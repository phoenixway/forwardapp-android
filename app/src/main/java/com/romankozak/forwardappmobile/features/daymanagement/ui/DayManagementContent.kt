package com.romankozak.forwardappmobile.features.daymanagement.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayanalitics.DayAnalyticsScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.daystart.DayStartScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.daydashboard.DayDashboardScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanScreenNavigator
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel

internal data class DayManagementScreenContentArgs(
    val uiState: DayManagementState,
    val pagerState: androidx.compose.foundation.pager.PagerState,
    val tabs: Array<DayManagementTab>,
    val mainNavController: NavController,
    val navigationManager: EnhancedNavigationManager?,
    val runtimeViewModel: DayManagementRuntimeViewModel,
    val dayPlanViewModel: DayPlanViewModel,
    val activityTrackerViewModel: ActivityTrackerViewModel,
    val dayFocusesViewModel: DayFocusesViewModel,
    val addTaskTrigger: Int,
    val screenLogTag: String,
)

private data class DayManagementPagerArgs(
    val planId: String,
    val pagerState: androidx.compose.foundation.pager.PagerState,
    val tabs: Array<DayManagementTab>,
    val mainNavController: NavController,
    val navigationManager: EnhancedNavigationManager?,
    val runtimeViewModel: DayManagementRuntimeViewModel,
    val dayPlanViewModel: DayPlanViewModel,
    val activityTrackerViewModel: ActivityTrackerViewModel,
    val dayFocusesViewModel: DayFocusesViewModel,
    val addTaskTrigger: Int,
    val screenLogTag: String,
)

@Composable
internal fun DayManagementScreenContent(
    args: DayManagementScreenContentArgs,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when {
            args.uiState.isLoading -> LoadingContent()
            args.uiState.error != null && args.uiState.dayPlanId == null -> {
                ErrorContent(
                    error = requireNotNull(args.uiState.error),
                    onRetry = onRetry,
                )
            }
            else -> {
                val pagerArgs =
                    DayManagementPagerArgs(
                        planId = requireNotNull(args.uiState.dayPlanId),
                        pagerState = args.pagerState,
                        tabs = args.tabs,
                        mainNavController = args.mainNavController,
                        navigationManager = args.navigationManager,
                        runtimeViewModel = args.runtimeViewModel,
                        dayPlanViewModel = args.dayPlanViewModel,
                        activityTrackerViewModel = args.activityTrackerViewModel,
                        dayFocusesViewModel = args.dayFocusesViewModel,
                        addTaskTrigger = args.addTaskTrigger,
                        screenLogTag = args.screenLogTag,
                    )
                DayManagementPagerContent(args = pagerArgs)
            }
        }

        if (args.uiState.isLoading && args.uiState.dayPlanId != null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayManagementPagerContent(
    args: DayManagementPagerArgs,
) {
    val runtimeUiState by args.runtimeViewModel.uiState.collectAsState()
    val dayPlanUiState by args.dayPlanViewModel.uiState.collectAsState()
    LaunchedEffect(args.planId) {
        args.dayPlanViewModel.loadDataForPlan(args.planId)
    }
    HorizontalPager(
        state = args.pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
    ) { page ->
        when (args.tabs[page]) {
            DayManagementTab.DAY_START ->
                DayStartScreen(
                    runtimeState = runtimeUiState.runtimeState,
                    predictedDurationMinutes = dayPlanUiState.dayPlan?.predictedDurationMinutes,
                    onWakeUp = args.runtimeViewModel::wakeUp,
                    onSleep = args.runtimeViewModel::sleep,
                )
            DayManagementTab.DAY_FOCUSES ->
                DayFocusesScreen(
                    initialDayPlanId = args.planId,
                    navController = args.mainNavController,
                    predictedDayDurationMinutes = dayPlanUiState.dayPlan?.predictedDurationMinutes,
                    viewModel = args.dayFocusesViewModel,
                )
            DayManagementTab.DAY_PLAN ->
                DayPlanScreen(
                    initialDayPlanId = args.planId,
                    navigator =
                        DayPlanScreenNavigator(
                            navController = args.mainNavController,
                            navigationManager = args.navigationManager,
                            onNavigateToBacklog =
                                createBacklogNavigator(
                                    screenLogTag = args.screenLogTag,
                                    mainNavController = args.mainNavController,
                                    navigationManager = args.navigationManager,
                                ),
                        ),
                    addTaskTrigger = args.addTaskTrigger,
                    viewModel = args.dayPlanViewModel,
                )
            DayManagementTab.JOURNAL ->
                ActivityTrackerScreen(
                    navController = args.mainNavController,
                    viewModel = args.activityTrackerViewModel,
                    showTopBar = false,
                    showInputBar = false,
                )
            DayManagementTab.FINALIZATION ->
                TodayPlaceholderScreen(
                    title = "Finalization",
                    description = "Під-екран завершення дня ще не реалізований.",
                )
            DayManagementTab.DASHBOARD -> DayDashboardScreen(dayPlanId = args.planId)
            DayManagementTab.ANALYTICS -> DayAnalyticsScreen()
        }
    }
}

private fun createBacklogNavigator(
    screenLogTag: String,
    mainNavController: NavController,
    navigationManager: EnhancedNavigationManager?,
): (DayTask) -> Unit =
    { task ->
        Log.d(screenLogTag, "2. НАВІГАЦІЯ: Отримано task для переходу в беклог.")
        task.projectId?.let { projectId ->
            Log.d(
                screenLogTag,
                "3. УМОВА ВИКОНАНА: projectId не є null. Значення: $projectId",
            )
            val goalIdToHighlight = task.goalId ?: task.id
            Log.d(screenLogTag, "   - Формую маршрут з goalId: $goalIdToHighlight")
            navigationManager.navigateOrFallback(
                navController = mainNavController,
                target =
                    NavTarget.ContextDetail(
                        contextId = projectId,
                        goalId = goalIdToHighlight,
                    ),
                recordInHistory = true,
            )
        } ?: Log.e(
            screenLogTag,
            "3. УМОВА НЕ ВИКОНАНА: task.projectId є null! Навігація неможлива.",
        )
    }

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Завантаження плану дня...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Помилка завантаження",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Спробувати знову")
        }
    }
}
