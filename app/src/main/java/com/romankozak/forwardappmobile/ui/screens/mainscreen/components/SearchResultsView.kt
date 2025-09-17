package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.mainscreen.SearchResult

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchResultsView(
    results: List<SearchResult>,
    onResultClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(results, key = { it.list.id }) { result ->
            ListItem(
                headlineContent = {
                    Text(
                        text = result.list.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    // --- ЗМІНЕНО ТУТ: Використовуємо FlowRow для повного шляху ---
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(4.dp), // <-- Correct parameter and value
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Видалено логіку скорочення, тепер завжди показуємо повний шлях
                        result.path.forEachIndexed { index, breadcrumb ->
                            Text(
                                text = breadcrumb.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (index < result.path.size - 1) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    // --- КІНЕЦЬ ЗМІН ---
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Перейти",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                   // .animateItemPlacement()
                    .clickable { onResultClick(result.list.id) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}