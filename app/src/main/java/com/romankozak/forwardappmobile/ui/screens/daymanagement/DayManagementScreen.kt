package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayanalitics.DayAnalyticsScreen
import com.romankozak.forwardappmobile.ui.screens.daymanagement.daydashboard.DayDashboardScreen
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayPlanScreen
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components.DayManagementBottomNav
import kotlinx.coroutines.launch

enum class DayManagementTab(
    val title: String,
    val icon: ImageVector,
    val description: String,
) {
    PLAN("План", Icons.AutoMirrored.Filled.ListAlt, "Створити та керувати завданнями"),
    DASHBOARD("Дашборд", Icons.Default.Dashboard, "Переглянути прогрес дня"),
    ANALYTICS("Аналітика", Icons.Default.Assessment, "Статистика та аналіз продуктивності"),
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DayManagementScreen(
    mainNavController: NavController,
    viewModel: DayManagementViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = DayManagementTab.entries.toTypedArray()
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var addTaskTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Спробувати знову",
                duration = SnackbarDuration.Long,
            ).let { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.retryLoading()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Керування днем") },
                navigationIcon = {
                    IconButton(onClick = { mainNavController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    if (uiState.dayPlanId != null) {
                        IconButton(
                            onClick = { viewModel.retryLoading() },
                            enabled = !uiState.isLoading,
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Оновити",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.dayPlanId != null) {
                DayManagementBottomNav(
                    currentTab = tabs[pagerState.currentPage],
                    onTabSelected = { tab ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(tab.ordinal)
                        }
                    },
                    onHomeClick = { mainNavController.popBackStack() }
                )
            }
        },
        floatingActionButton = {
            if (pagerState.currentPage == DayManagementTab.PLAN.ordinal && uiState.dayPlanId != null) {
                FloatingActionButton(onClick = { addTaskTrigger++ }) {
                    Icon(Icons.Default.Add, contentDescription = "Додати завдання")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }

                uiState.error != null && uiState.dayPlanId == null -> {
                    ErrorContent(
                        error = uiState.error!!,
                        onRetry = { viewModel.retryLoading() },
                    )
                }

                else -> {
                    val planId = uiState.dayPlanId!!
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (tabs[page]) {
                            DayManagementTab.PLAN -> DayPlanScreen(
                                dayPlanId = planId,
                                onNavigateToProject = { projectId ->
                                    mainNavController.navigate("goal_detail_screen/$projectId")
                                },
                                onNavigateToBacklog = { task ->
                                    task.projectId?.let { id ->
                                        mainNavController.navigate("goal_detail_screen/$id")
                                    }
                                },
                                addTaskTrigger = addTaskTrigger
                            )
                            DayManagementTab.DASHBOARD -> DayDashboardScreen(
                                dayPlanId = planId
                            )
                            DayManagementTab.ANALYTICS -> DayAnalyticsScreen()
                        }
                    }
                }
            }

            if (uiState.isLoading && uiState.dayPlanId != null) {
                LinearProgressIndicator(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
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
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .padding(24.dp),
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
