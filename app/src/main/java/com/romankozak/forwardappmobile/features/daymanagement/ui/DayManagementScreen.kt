package com.romankozak.forwardappmobile.features.daymanagement.ui

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayanalitics.DayAnalyticsScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.daydashboard.DayDashboardScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanScreenNavigator
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckFabDefaults

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DayManagementScreen(
    mainNavController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    viewModel: DayManagementViewModel = hiltViewModel(),
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    startTab: String? = null,
    currentDayManagementTab: DayManagementTab? = null,
    showFabMenu: Boolean = true,
) {
    val screenLogTag = "NAV_DEBUG"

    val uiState by viewModel.uiState.collectAsState()
    val tabs = DayManagementTab.entries.toTypedArray()
    val initialPage =
        remember(startTab) { tabs.indexOfFirst { it.name == startTab }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage) { tabs.size }
    val snackbarHostState = remember { SnackbarHostState() }
    var addTaskTrigger by remember { mutableStateOf(0) }

    var isFabMenuExpanded by remember { mutableStateOf(false) }

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
            if (showFabMenu) {
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
                dayPlanViewModel = dayPlanViewModel,
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

@Composable
private fun DayManagementNavigationEffects(
    viewModel: DayManagementViewModel,
    uiState: DayManagementState,
    navigationManager: EnhancedNavigationManager?,
    mainNavController: NavController,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DayManagementUiEvent.NavigateToProject -> {
                    navigationManager.navigateOrFallback(
                        navController = mainNavController,
                        target = NavTarget.ContextDetail(contextId = event.projectId),
                        recordInHistory = true,
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            val result =
                snackbarHostState.showSnackbar(
                    message = error,
                    actionLabel = "Спробувати знову",
                    duration = SnackbarDuration.Long,
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLoading()
            }
        }
    }
}

@Composable
private fun DayManagementFabMenu(
    isFabMenuExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDismiss: () -> Unit,
    dayPlanViewModel: DayPlanViewModel,
) {
    Box(modifier = Modifier.padding(bottom = CommandDeckFabDefaults.BottomPadding)) {
        FloatingActionButton(onClick = onToggleExpanded) {
            Icon(Icons.Default.Menu, contentDescription = "Меню дій дня")
        }
        DayManagementActionsMenu(
            expanded = isFabMenuExpanded,
            onDismissRequest = onDismiss,
            dayPlanViewModel = dayPlanViewModel,
        )
    }
}

@Composable
fun DayManagementActionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    dayPlanViewModel: DayPlanViewModel,
) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier =
                Modifier.background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            DropdownMenuItem(
                text = { Text("Додати задачу") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {
                    onDismissRequest()
                    dayPlanViewModel.openAddTaskDialog()
                },
            )
            DropdownMenuItem(
                text = { Text("Показати зв'язки") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                onClick = {
                    onDismissRequest()
                    dayPlanViewModel.toggleScopeLinksSheet()
                },
            )
            DropdownMenuItem(
                text = { Text("Назад") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                },
                onClick = {
                    onDismissRequest()
                    dayPlanViewModel.navigateToPreviousDay()
                },
            )
            DropdownMenuItem(
                text = { Text("Вперед") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                },
                onClick = {
                    onDismissRequest()
                    dayPlanViewModel.navigateToNextDay()
                },
            )
        }
}

@Composable
private fun DayManagementScreenContent(
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
                        dayPlanViewModel = args.dayPlanViewModel,
                        addTaskTrigger = args.addTaskTrigger,
                        screenLogTag = args.screenLogTag,
                    )
                DayManagementPagerContent(
                    args = pagerArgs,
                )
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

private data class DayManagementScreenContentArgs(
    val uiState: DayManagementState,
    val pagerState: androidx.compose.foundation.pager.PagerState,
    val tabs: Array<DayManagementTab>,
    val mainNavController: NavController,
    val navigationManager: EnhancedNavigationManager?,
    val dayPlanViewModel: DayPlanViewModel,
    val addTaskTrigger: Int,
    val screenLogTag: String,
)

private data class DayManagementPagerArgs(
    val planId: String,
    val pagerState: androidx.compose.foundation.pager.PagerState,
    val tabs: Array<DayManagementTab>,
    val mainNavController: NavController,
    val navigationManager: EnhancedNavigationManager?,
    val dayPlanViewModel: DayPlanViewModel,
    val addTaskTrigger: Int,
    val screenLogTag: String,
)

@Composable
private fun DayManagementPagerContent(
    args: DayManagementPagerArgs,
) {
    HorizontalPager(
        state = args.pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
    ) { page ->
        when (args.tabs[page]) {
            DayManagementTab.TRACK -> ActivityTrackerScreen(navController = args.mainNavController)
            DayManagementTab.PLAN ->
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
fun NeonTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val backgroundColor = color.copy(alpha = 0.1f)
    val textColor = color

    Box(
        modifier =
            modifier
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp), ambientColor = color)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = textColor,
        )
    }
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
