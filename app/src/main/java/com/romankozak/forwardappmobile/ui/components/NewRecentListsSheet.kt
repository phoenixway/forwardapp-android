package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.database.models.RecentItem
import com.romankozak.forwardappmobile.core.database.models.RecentItemType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewRecentListsSheet(
    showSheet: Boolean,
    recentItems: List<RecentItem>,
    onDismiss: () -> Unit,
    onItemClick: (RecentItem) -> Unit,
    onPinClick: (RecentItem) -> Unit,
) {
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            val tabs = listOf("Недавні", "Закріплені")
            val pagerState = rememberPagerState { tabs.size }
            val coroutineScope = rememberCoroutineScope()

            Column(Modifier.navigationBarsPadding()) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(text = title) }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    page ->
                    val items = if (page == 0) recentItems else recentItems.filter { it.isPinned }
                    RecentItemsGrid(items = items, onItemClick = onItemClick, onPinClick = onPinClick)
                }
            }
        }
    }
}

@Composable
private fun RecentItemsGrid(
    items: List<RecentItem>,
    onItemClick: (RecentItem) -> Unit,
    onPinClick: (RecentItem) -> Unit
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Історія порожня.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { item ->
                RecentItemCard(item = item, onClick = { onItemClick(item) }, onPinClick = { onPinClick(item) })
            }
        }
    }
}

@Composable
private fun RecentItemCard(
    item: RecentItem,
    onClick: () -> Unit,
    onPinClick: () -> Unit
) {
    val color = getColorsForType(item.type)
    Card(
        modifier = Modifier
            .aspectRatio(1.3f)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = color,
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = when (item.type) {
                        RecentItemType.PROJECT -> Icons.Outlined.Folder
                        RecentItemType.NOTE -> Icons.AutoMirrored.Outlined.Note
                        RecentItemType.NOTE_DOCUMENT -> Icons.AutoMirrored.Outlined.List
                        RecentItemType.CHECKLIST -> Icons.Outlined.Checklist
                        RecentItemType.OBSIDIAN_LINK -> Icons.Outlined.Link
                    },
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = color
                )
                IconButton(onClick = onPinClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = item.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun getColorsForType(type: RecentItemType): Color {
    return when (type) {
        RecentItemType.PROJECT -> MaterialTheme.colorScheme.primary
        RecentItemType.NOTE -> MaterialTheme.colorScheme.secondary
        RecentItemType.NOTE_DOCUMENT -> MaterialTheme.colorScheme.tertiary
        RecentItemType.CHECKLIST -> MaterialTheme.colorScheme.tertiaryContainer
        RecentItemType.OBSIDIAN_LINK -> MaterialTheme.colorScheme.primaryContainer
    }
}
