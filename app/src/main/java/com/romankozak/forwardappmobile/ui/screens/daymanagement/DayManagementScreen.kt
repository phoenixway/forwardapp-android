// DayManagementScreen.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.screens.daydashboard.DayDashboardScreen
import kotlinx.coroutines.launch

private enum class DayManagementTab(
    val title: String,
    val icon: ImageVector
) {
    PLAN("План", Icons.AutoMirrored.Filled.ListAlt),
    DASHBOARD("Дашборд", Icons.Default.Dashboard),
    ANALYTICS("Аналітика", Icons.Default.Assessment)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DayManagementScreen(
    mainNavController: NavController,
    viewModel: DayManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = DayManagementTab.entries
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Керування днем") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Помилка завантаження: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.retryLoading() }) {
                            Text("Спробувати знову")
                        }
                    }
                }
                uiState.dayPlanId != null -> {
                    val planId = uiState.dayPlanId!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            indicator = { tabPositions ->
                                TabRowDefaults.PrimaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) }
                                )
                            }
                        }

// File: DayManagementScreen.kt

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (tabs[page]) {
                                DayManagementTab.PLAN -> DayPlanScreen(
                                    dayPlanId = planId,
                                    onNavigateBack = { mainNavController.navigateUp() }
                                )
                                DayManagementTab.DASHBOARD -> DayDashboardScreen(
                                    dayPlanId = planId,
                                )
                                DayManagementTab.ANALYTICS -> DayAnalyticsScreen(
                                    // ВИПРАВЛЕНО: Видалено параметр dayPlanId, оскільки DayAnalyticsScreen його не приймає
                                    navController = mainNavController,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}