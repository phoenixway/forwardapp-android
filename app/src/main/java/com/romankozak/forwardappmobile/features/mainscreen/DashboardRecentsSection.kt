package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet

private const val RECENTS_EXPANDED_KEY = "dashboard_recents_expanded"

@Composable
internal fun DashboardRecentsSection(
    onItemClick: (RecentItem) -> Unit,
    commandDeckViewModel: CommandDeckViewModel,
    recentViewModel: RecentViewModel = hiltViewModel(),
    expandedOverride: Boolean? = null,
    onToggleOverride: (() -> Unit)? = null,
    header: (@Composable (Boolean, () -> Unit, () -> Unit) -> Unit)? = null,
) {
    var expanded by remember {
        mutableStateOf(commandDeckViewModel.isCategoryExpanded(RECENTS_EXPANDED_KEY, defaultValue = true))
    }
    var showSheet by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()
    val itemLimit = 4
    val isExpanded = expandedOverride ?: expanded
    val toggle = onToggleOverride ?: {
        expanded = !expanded
        commandDeckViewModel.setCategoryExpanded(RECENTS_EXPANDED_KEY, expanded)
    }

    Column {
        if (header != null) {
            header(isExpanded, toggle) { showSheet = true }
        } else {
            DashboardSectionHeader(
                title = stringResource(R.string.deck_recent),
                action = { TextButton(onClick = { showSheet = true }) { Text(stringResource(R.string.deck_all)) } },
                isExpanded = isExpanded,
                onClick = toggle,
            )
        }
        if (isExpanded) {
            if (recentItems.isEmpty()) {
                Text(
                    stringResource(R.string.deck_empty_recent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val visibleItems = recentItems.distinctBy { it.type to it.target }.take(itemLimit)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
                ) {
                    Column {
                        visibleItems.forEachIndexed { index, item ->
                            Box(Modifier.fillMaxWidth().clickable { onItemClick(item) }
                                .testTag("deck-recent-${item.id}")) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.History, null, Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = item.displayName.ifBlank { item.target },
                                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (index < visibleItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(start = 42.dp, end = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }
    }
    NewRecentListsSheet(
        showSheet = showSheet,
        recentItems = recentItems,
        onDismiss = { showSheet = false },
        onItemClick = { item ->
            showSheet = false
            onItemClick(item)
        },
        onPinClick = recentViewModel::onPinClick,
    )
}
