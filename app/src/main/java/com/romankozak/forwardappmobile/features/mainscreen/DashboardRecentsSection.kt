package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.RecentItemCard

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
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                expanded = !expanded
                commandDeckViewModel.setCategoryExpanded(RECENTS_EXPANDED_KEY, expanded)
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recents", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Згорнути Recents" else "Розгорнути Recents",
            )
        }
        if (expanded) {
            if (recentItems.isEmpty()) {
                Text("Історія порожня.", style = MaterialTheme.typography.bodyMedium)
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val columns = ((maxWidth + 8.dp) / 158.dp).toInt().coerceAtLeast(1)
                    val visibleItems = recentItems.distinctBy { it.type to it.target }.take(columns * 2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        visibleItems.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { item ->
                                    Box(Modifier.weight(1f)) {
                                        RecentItemCard(
                                            item = item,
                                            onClick = { onItemClick(item) },
                                            onPinClick = { recentViewModel.onPinClick(item) },
                                        )
                                    }
                                }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
