package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class CommandDeckItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandDeckScreen(
    navController: NavController,
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToDayManagement: () -> Unit,
    onNavigateToStrategicManagement: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val deckItems = remember {
        listOf(
            CommandDeckItem(
                title = "Проєкти",
                subtitle = "Ієрархія цілей",
                icon = Icons.Outlined.AccountTree,
                route = "projects"
            ),
            CommandDeckItem(
                title = "День",
                subtitle = "План на сьогодні",
                icon = Icons.Outlined.Today,
                route = "day"
            ),
            CommandDeckItem(
                title = "Стратегія",
                subtitle = "Довгострокові цілі",
                icon = Icons.Outlined.Flag,
                route = "strategy"
            ),
            CommandDeckItem(
                title = "Пошук",
                subtitle = "Глобальний пошук",
                icon = Icons.Outlined.Search,
                route = "search"
            ),
            CommandDeckItem(
                title = "Налаштування",
                subtitle = "Параметри додатку",
                icon = Icons.Outlined.Settings,
                route = "settings"
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Command Deck",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deckItems) { item ->
                CommandDeckCard(
                    item = item,
                    onClick = {
                        when (item.route) {
                            "projects" -> onNavigateToProjectHierarchy()
                            "day" -> onNavigateToDayManagement()
                            "strategy" -> onNavigateToStrategicManagement()
                            "search" -> onNavigateToGlobalSearch()
                            "settings" -> onNavigateToSettings()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CommandDeckCard(
    item: CommandDeckItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
