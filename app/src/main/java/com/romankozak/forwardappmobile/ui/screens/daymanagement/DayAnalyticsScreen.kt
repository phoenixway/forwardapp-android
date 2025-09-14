// DayAnalyticsScreen.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TrendingUp
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
import kotlinx.coroutines.launch

private enum class AnalyticsTab(
    val title: String,
    val icon: ImageVector
) {
    OVERVIEW("Огляд", Icons.Default.PieChart),
    TRENDS("Тренди", Icons.Default.TrendingUp),
    DETAILS("Деталі", Icons.Default.BarChart)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayAnalyticsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: DayAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = AnalyticsTab.values()
    val pagerState = rememberPagerState(initialPage = 0) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

// File: DayAnalyticsScreen.kt

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                // ВИПРАВЛЕНО: Замінено застарілий Indicator та pagerTabIndicatorOffset
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    height = 3.dp,
                    color = MaterialTheme.colorScheme.primary
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
                    text = {
                        Text(
                            tab.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    icon = {
                        Icon(
                            tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (tabs[page]) {
                AnalyticsTab.OVERVIEW -> AnalyticsOverviewScreen(uiState)
                AnalyticsTab.TRENDS -> AnalyticsTrendsScreen(uiState)
                AnalyticsTab.DETAILS -> AnalyticsDetailsScreen(uiState)
            }
        }
    }
}

@Composable
fun AnalyticsOverviewScreen(uiState: DayAnalyticsUiState) {
    // Implementation for analytics overview
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Аналітика продуктивності - огляд")
    }
}

@Composable
fun AnalyticsTrendsScreen(uiState: DayAnalyticsUiState) {
    // Implementation for trends
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Аналітика продуктивності - тренди")
    }
}

@Composable
fun AnalyticsDetailsScreen(uiState: DayAnalyticsUiState) {
    // Implementation for detailed analytics
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Аналітика продуктивності - деталі")
    }
}