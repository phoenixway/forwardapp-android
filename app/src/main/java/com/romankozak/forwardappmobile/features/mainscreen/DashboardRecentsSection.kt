package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var showSheet by remember { mutableStateOf(false) }
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val itemLimit = if (configuration.screenWidthDp >= 600) 6 else 4

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                commandDeckViewModel.setCategoryExpanded(RECENTS_EXPANDED_KEY, expanded)
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Recents",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Згорнути Recents" else "Розгорнути Recents",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            if (recentItems.isEmpty()) {
                Text(
                    "Історія порожня.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                recentItems.distinctBy { it.type to it.target }.take(itemLimit).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) }
                            .padding(horizontal = 4.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = item.displayName.ifBlank { item.target },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                TextButton(onClick = { showSheet = true }, modifier = Modifier.align(Alignment.End)) {
                    Text("More")
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
