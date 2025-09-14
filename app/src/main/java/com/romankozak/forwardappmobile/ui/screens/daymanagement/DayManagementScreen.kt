package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.screens.daydashboard.DayDashboardScreen
import kotlinx.coroutines.launch

private enum class DayManagementTab(
    val title: String,
    val icon: ImageVector
) {
    PLAN("План", Icons.Default.ListAlt),
    DASHBOARD("Дашборд", Icons.Default.Dashboard),
    ANALYTICS("Аналітика", Icons.Default.Assessment)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayManagementScreen(
    mainNavController: NavController,
    viewModel: DayManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = DayManagementTab.values()
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // UI ОНОВЛЕННЯ: Використовуємо Scaffold для загальної структури,
    // але навігація тепер реалізована через TabRow та HorizontalPager.
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        "Помилка завантаження плану: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.dayPlanId != null -> {
                    val planId = uiState.dayPlanId!!
                    Column {
                        // UI ОНОВЛЕННЯ: TabRow для візуального представлення вкладок.
                        TabRow(selectedTabIndex = pagerState.currentPage) {
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

                        // UX ОНОВЛЕННЯ: HorizontalPager для свайпання між екранами.
                        // Це зберігає стан кожного екрану при перемиканні.
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (tabs[page]) {
                                DayManagementTab.PLAN -> DayPlanScreen(dayPlanId = planId)
                                DayManagementTab.DASHBOARD -> DayDashboardScreen(dayPlanId = planId)
                                DayManagementTab.ANALYTICS -> DayAnalyticsScreen(navController = mainNavController)
                            }
                        }
                    }
                }
            }
        }
    }
}