package com.romankozak.forwardappmobile.ui.screens.commanddeck.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.recent.RecentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardBottomBar(
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToProjectSearch: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showRecentSheet by remember { mutableStateOf(false) }

    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()

    if (showMoreBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreBottomSheet = false },
            sheetState = modalSheetState,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        ) {
            MoreBottomSheetContent(onNavigateToReminders = {
                coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                    if (!modalSheetState.isVisible) {
                        showMoreBottomSheet = false
                    }
                    onNavigateToReminders()
                }
            }, onNavigateToProjectSearch = {
                coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
                    if (!modalSheetState.isVisible) {
                        showMoreBottomSheet = false
                    }
                    onNavigateToProjectSearch()
                }
            })
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
            onPinClick = { item -> recentViewModel.onPinClick(item) }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarButton(
            icon = Icons.Outlined.Inbox,
            label = "Inbox",
            onClick = onNavigateToInbox
        )

        BarButton(
            icon = Icons.Outlined.Analytics,
            label = "Tracker",
            onClick = onNavigateToTracker
        )

        BarButton(
            icon = Icons.Outlined.AccountTree,
            onClick = onNavigateToProjectHierarchy,
            label = "Projects",
        )
        BarButton(
            icon = Icons.Outlined.History,
            label = "Recent",
            onClick = { showRecentSheet = true }
        )
        BarButton(
            icon = Icons.Outlined.MoreHoriz,
            label = "More",
            onClick = { showMoreBottomSheet = true }
        )
    }
}

@Composable
private fun MoreBottomSheetContent(onNavigateToReminders: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Text("More Options", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToProjectSearch)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search in projects")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Search in projects")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToReminders)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Reminders")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Reminders")
            }
        }
    }
}


@Composable
private fun BarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.10f))
                .border(
                    width = 1.dp,
                    color = primary.copy(alpha = 0.22f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = primary.copy(alpha = 0.9f))
        }
    }
}

@Preview
@Composable
fun DashboardBottomBarPreview() {
    DashboardBottomBar(
        onNavigateToProjectHierarchy = {},
        onNavigateToTracker = {},
        onNavigateToInbox = {},
        onNavigateToReminders = {},
        onNavigateToRecentItem = {},
        recentViewModel = hiltViewModel()
    )
}
