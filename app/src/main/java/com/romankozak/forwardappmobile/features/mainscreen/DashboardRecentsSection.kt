package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalConfiguration
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
) {
    var expanded by remember {
        mutableStateOf(commandDeckViewModel.isCategoryExpanded(RECENTS_EXPANDED_KEY, defaultValue = true))
    }
    var showSheet by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()
    val itemLimit = 4

    Column {
        DashboardSectionHeader(
            title = stringResource(R.string.deck_recent),
            action = { TextButton(onClick = { showSheet = true }) { Text(stringResource(R.string.deck_all)) } },
            isExpanded = expanded,
            onClick = {
                expanded = !expanded
                commandDeckViewModel.setCategoryExpanded(RECENTS_EXPANDED_KEY, expanded)
            },
        )
        if (expanded) {
            if (recentItems.isEmpty()) {
                Text(
                    stringResource(R.string.deck_empty_recent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                recentItems.distinctBy { it.type to it.target }.take(itemLimit).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) }
                            .heightIn(min = 52.dp).padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = item.displayName.ifBlank { item.target },
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
