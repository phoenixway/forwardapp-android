package com.romankozak.forwardappmobile.features.mainscreen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBottomBar(
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToProjectSearch: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    // New lambdas for Import/Export actions
    onExportToFile: () -> Unit,
    onImportFromFileRequest: () -> Unit,
    onSelectiveImportFromFileRequest: () -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: () -> Unit,
    onWifiPush: (String) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showRecentSheet by remember { mutableStateOf(false) }
    var showImportExportSheet by remember { mutableStateOf(false) }

    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImportFromFileRequest() }
    }

    val selectiveImportLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onSelectiveImportFromFileRequest() }
    }

    val importAttachmentsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onImportAttachmentsFromFileRequest() }
    }

    if (showMoreBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreBottomSheet = false },
            sheetState = modalSheetState,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        ) {
            MoreBottomSheetContent(
                onNavigateToReminders = {
                    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                        if (!modalSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                        onNavigateToReminders()
                    }
                },
                onNavigateToProjectSearch = {
                    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                        if (!modalSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                        onNavigateToProjectSearch()
                    }
                },
                onNavigateToPresets = {
                    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                        if (!modalSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                        onNavigateToPresets()
                    }
                },
                onNavigateToAiInsights = {
                    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                        if (!modalSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                        onNavigateToAiInsights()
                    }
                },
                onShowImportExportSheet = {
                    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                        if (!modalSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                        showImportExportSheet = true
                    }
                },
                onNavigateToSettings = {
                    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                        if (!modalSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                        onNavigateToSettings()
                    }
                },
            )
        }
    }

    if (showImportExportSheet) {
        ModalBottomSheet(onDismissRequest = { showImportExportSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Імпорт / Експорт",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item {
                        ImportExportTile(
                            icon = Icons.Default.CloudUpload,
                            title = "Експорт бекапу",
                            subtitle = "Зберегти JSON у файлі",
                            onClick = {
                                showImportExportSheet = false
                                onExportToFile()
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.CloudDownload,
                            title = "Повний імпорт",
                            subtitle = "Замінити поточні дані",
                            onClick = {
                                showImportExportSheet = false
                                importLauncher.launch("application/json")
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.FolderOpen,
                            title = "Вибірковий імпорт",
                            subtitle = "Обрати сутності",
                            onClick = {
                                showImportExportSheet = false
                                selectiveImportLauncher.launch("application/json")
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.Description,
                            title = "Експорт вкладень",
                            subtitle = "JSON вкладень",
                            onClick = {
                                showImportExportSheet = false
                                onExportAttachments()
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.FolderOpen,
                            title = "Імпорт вкладень",
                            subtitle = "Додати вкладення",
                            onClick = {
                                showImportExportSheet = false
                                importAttachmentsLauncher.launch("application/json")
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.CloudUpload,
                            title = "Push змін по Wi‑Fi",
                            subtitle = "Надіслати несинхронізоване",
                            onClick = {
                                showImportExportSheet = false
                                onWifiPush("localhost:8080")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showRecentSheet) {
        NewRecentListsSheet(
            showSheet = showRecentSheet,
            recentItems = recentItems,
            onDismiss = { showRecentSheet = false },
            onItemClick = { item ->
                coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                    showRecentSheet = false
                    onNavigateToRecentItem(item)
                }
            },
            onPinClick = { item -> recentViewModel.onPinClick(item) },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(
            icon = Icons.Outlined.MoveToInbox,
            label = "Inbox",
            onClick = onNavigateToInbox,
        )

        BarButton(
            icon = Icons.Outlined.Radar,
            label = "Tracker",
            onClick = onNavigateToTracker,
        )

        BarButton(
            icon = Icons.Outlined.AlternateEmail,
            onClick = onNavigateToProjectHierarchy,
            label = "Contexts",
        )
        BarButton(
            icon = Icons.Outlined.History,
            label = "Recent",
            onClick = { showRecentSheet = true },
        )
        BarButton(
            icon = Icons.Outlined.MoreHoriz,
            label = "More",
            onClick = { showMoreBottomSheet = true },
        )
    }
}

@Composable
private fun MoreBottomSheetContent(
    onNavigateToReminders: () -> Unit,
    onNavigateToProjectSearch: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onShowImportExportSheet: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
    ) {
        Column {
            Text("More Options", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToProjectSearch)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search in projects")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Search in projects")
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToReminders)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Reminders")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Reminders")
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToPresets)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.DashboardCustomize, contentDescription = "Presets")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Structure presets")
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToAiInsights)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI Insights")
                Spacer(modifier = Modifier.width(16.dp))
                Text("AI Insights")
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShowImportExportSheet)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.SwapVert, contentDescription = "Import/Export")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Import/Export")
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToSettings)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Settings")
            }
        }
    }
}

@Composable
fun TodayBottomBar(
    dayPlanViewModel: DayPlanViewModel,
    onNavigateToSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(
            icon = Icons.Outlined.Add,
            label = "Add Task",
            onClick = { dayPlanViewModel.openAddTaskDialog() },
        )
        BarButton(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            onClick = onNavigateToSettings,
        )
    }
}

@Composable
fun StrategicArcBottomBar(viewModel: StrategicArcViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: Add buttons based on StrategicArcViewModel logic
    }
}

@Composable
fun CoreBottomBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: Add buttons for Core screen
    }
}

@Composable
fun TacticsBottomBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO: Add buttons for Tactics screen
    }
}


@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClick() }
                .padding(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.10f))
                    .border(
                        width = 1.dp,
                        color = primary.copy(alpha = 0.22f),
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = primary.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun ImportExportTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}