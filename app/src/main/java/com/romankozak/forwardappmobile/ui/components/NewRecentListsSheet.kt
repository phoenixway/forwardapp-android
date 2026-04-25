package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Note
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import kotlinx.coroutines.launch

private const val TEXT_CONTRAST_THRESHOLD = 4.5f
private const val BORDER_CONTRAST_THRESHOLD = 2.2f
private const val CARD_ASPECT_RATIO = 1.3f
private const val CONTRAST_LUMINANCE_OFFSET = 0.05f
private const val SOFTEN_BLEND_FACTOR = 0.30f

private data class RecentItemCardColors(
    val textColor: Color,
    val borderColor: Color,
    val typeIconColor: Color,
    val cardBackground: Color,
)

private val recentItemsComparator =
    compareByDescending<RecentItem> { it.lastAccessed }
        .thenBy { it.id }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NewRecentListsSheet(
    showSheet: Boolean,
    recentItems: List<RecentItem>,
    onDismiss: () -> Unit,
    onItemClick: (RecentItem) -> Unit,
    onPinClick: (RecentItem) -> Unit,
) {
    if (!showSheet) return

    val stableRecentItems =
        remember(recentItems) {
            recentItems.sortedWith(recentItemsComparator)
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        RecentItemsSheetContent(
            stableRecentItems = stableRecentItems,
            onItemClick = onItemClick,
            onPinClick = { item ->
                onPinClick(item)
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentItemsSheetContent(
    stableRecentItems: List<RecentItem>,
    onItemClick: (RecentItem) -> Unit,
    onPinClick: (RecentItem) -> Unit,
) {
    val tabs = listOf("Недавні", "Закріплені")
    val pagerState = rememberPagerState { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.navigationBarsPadding()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(text = title) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            RecentItemsGrid(
                items = itemsForRecentItemsPage(page = page, stableRecentItems = stableRecentItems),
                onItemClick = onItemClick,
                onPinClick = onPinClick,
            )
        }
    }
}

private fun itemsForRecentItemsPage(
    page: Int,
    stableRecentItems: List<RecentItem>,
): List<RecentItem> = if (page == 0) stableRecentItems else stableRecentItems.filter { it.isPinned }

@Composable
private fun RecentItemsGrid(
    items: List<RecentItem>,
    onItemClick: (RecentItem) -> Unit,
    onPinClick: (RecentItem) -> Unit,
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
    onPinClick: () -> Unit,
) {
    val colors = rememberRecentItemCardColors(item.type)

    Card(
        modifier =
            Modifier
                .aspectRatio(CARD_ASPECT_RATIO)
                .clickable(onClick = onClick)
                .border(
                    width = 1.dp,
                    color = colors.borderColor,
                    shape = MaterialTheme.shapes.medium,
                ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                ItemTypeIcon(
                    type = item.type,
                    tint = colors.typeIconColor,
                )
                IconButton(onClick = onPinClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = colors.typeIconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = item.displayName.ifBlank { fallbackRecentItemTitle(item) },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = colors.textColor,
                maxLines = 3,
            )
        }
    }
}

@Composable
private fun ItemTypeIcon(
    type: RecentItemType,
    tint: Color,
) {
    Icon(
        imageVector =
            when (type) {
                RecentItemType.PROJECT -> Icons.Outlined.Folder
                RecentItemType.NOTE -> Icons.AutoMirrored.Outlined.Note
                RecentItemType.NOTE_DOCUMENT -> Icons.AutoMirrored.Outlined.List
                RecentItemType.CHECKLIST -> Icons.Outlined.Checklist
                RecentItemType.MUSIC_NOTE -> Icons.Outlined.MusicNote
                RecentItemType.OBSIDIAN_LINK -> Icons.Outlined.Link
            },
        contentDescription = null,
        modifier = Modifier.size(36.dp),
        tint = tint,
    )
}

@Composable
private fun rememberRecentItemCardColors(type: RecentItemType): RecentItemCardColors {
    val accentColor = getColorsForType(type)
    val cardBackground = MaterialTheme.colorScheme.surfaceContainerHigh
    val baseTextColor =
        if (contrastRatio(accentColor, cardBackground) >= TEXT_CONTRAST_THRESHOLD) {
            accentColor
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val baseBorderColor =
        if (contrastRatio(accentColor, cardBackground) >= BORDER_CONTRAST_THRESHOLD) {
            accentColor
        } else {
            MaterialTheme.colorScheme.outline
        }
    val isChecklist = type == RecentItemType.CHECKLIST
    val textColor = if (isChecklist) softenAgainstBackground(baseTextColor, cardBackground) else baseTextColor
    val borderColor = if (isChecklist) softenAgainstBackground(baseBorderColor, cardBackground) else baseBorderColor
    val typeIconColor = if (isChecklist) textColor else accentColor
    return RecentItemCardColors(
        textColor = textColor,
        borderColor = borderColor,
        typeIconColor = typeIconColor,
        cardBackground = cardBackground,
    )
}

@Composable
private fun getColorsForType(type: RecentItemType): Color {
    return when (type) {
        RecentItemType.PROJECT -> MaterialTheme.colorScheme.primary
        RecentItemType.NOTE -> MaterialTheme.colorScheme.secondary
        RecentItemType.NOTE_DOCUMENT -> MaterialTheme.colorScheme.tertiary
        RecentItemType.CHECKLIST -> MaterialTheme.colorScheme.tertiaryContainer
        RecentItemType.MUSIC_NOTE -> MaterialTheme.colorScheme.secondaryContainer
        RecentItemType.OBSIDIAN_LINK -> MaterialTheme.colorScheme.primaryContainer
    }
}

private fun fallbackRecentItemTitle(item: RecentItem): String =
    when (item.type) {
        RecentItemType.PROJECT -> "Контекст"
        RecentItemType.NOTE -> "Нотатка"
        RecentItemType.NOTE_DOCUMENT -> "Документ"
        RecentItemType.CHECKLIST -> "Чекліст"
        RecentItemType.MUSIC_NOTE -> "Музичні ноти"
        RecentItemType.OBSIDIAN_LINK -> item.target.ifBlank { "Посилання" }
    }

private fun contrastRatio(
    foreground: Color,
    background: Color,
): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + CONTRAST_LUMINANCE_OFFSET) / (darker + CONTRAST_LUMINANCE_OFFSET)
}

private fun softenAgainstBackground(
    color: Color,
    background: Color,
): Color = lerp(color, background, SOFTEN_BLEND_FACTOR)
