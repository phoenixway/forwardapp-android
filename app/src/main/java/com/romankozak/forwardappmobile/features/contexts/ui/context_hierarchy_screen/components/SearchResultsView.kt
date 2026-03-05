package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultFilter
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResultSort
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchResultsView(
    results: List<SearchResult>,
    query: String,
    selectedFilter: SearchResultFilter,
    selectedSort: SearchResultSort,
    onFilterChange: (SearchResultFilter) -> Unit,
    onSortChange: (SearchResultSort) -> Unit,
    onRevealClick: (String) -> Unit,
    onOpenClick: (String) -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
) {
    val visibleResults =
        remember(results, query, selectedFilter, selectedSort) {
            results
                .asSequence()
                .filter { result ->
                    when (selectedFilter) {
                        SearchResultFilter.All -> true
                        SearchResultFilter.WithPath -> result.parentPath.size > 1
                        SearchResultFilter.RootOnly -> result.parentPath.size <= 1
                    }
                }.sortedWith(
                    when (selectedSort) {
                        SearchResultSort.Relevance -> compareByDescending<SearchResult> { relevanceScore(it.projectName, query) }.thenBy { it.projectName }
                        SearchResultSort.Alphabetical -> compareBy<SearchResult> { it.projectName.lowercase(Locale.getDefault()) }
                        SearchResultSort.HierarchyDepth -> compareByDescending<SearchResult> { it.parentPath.size }.thenBy { it.projectName }
                    },
                ).toList()
        }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${results.size} results",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchResultFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filterLabel(filter)) },
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchResultSort.entries.forEach { sort ->
                        FilterChip(
                            selected = selectedSort == sort,
                            onClick = { onSortChange(sort) },
                            label = { Text(sortLabel(sort)) },
                        )
                    }
                }
            }
        }

        if (results.isEmpty()) {
            item {
                SearchNoResultsState(
                    query = query,
                    onPerformGlobalSearch = onPerformGlobalSearch,
                )
            }
            return@LazyColumn
        }

        if (visibleResults.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = "No results for the selected filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            return@LazyColumn
        }

        items(visibleResults, key = { it.projectId }) { result ->
            val pathText = result.parentPath.joinToString(" > ")
            ListItem(
                headlineContent = {
                    Text(
                        text = highlightedText(result.projectName, query),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "Context",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }

                        if (pathText.isNotBlank()) {
                            Text(
                                text = highlightedText(pathText, query),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onRevealClick(result.projectId) }) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "Show in hierarchy",
                            )
                        }
                        IconButton(onClick = { onOpenClick(result.projectId) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Open project",
                            )
                        }
                    }
                },
                modifier = Modifier.clickable { onOpenClick(result.projectId) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

private fun filterLabel(filter: SearchResultFilter): String =
    when (filter) {
        SearchResultFilter.All -> "All"
        SearchResultFilter.WithPath -> "With path"
        SearchResultFilter.RootOnly -> "Root"
    }

private fun sortLabel(sort: SearchResultSort): String =
    when (sort) {
        SearchResultSort.Relevance -> "Relevance"
        SearchResultSort.Alphabetical -> "A-Z"
        SearchResultSort.HierarchyDepth -> "Depth"
    }

@Composable
private fun SearchNoResultsState(
    query: String,
    onPerformGlobalSearch: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "No local matches",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = "Try a shorter phrase, another keyword, or search globally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (query.isNotBlank()) {
                FilterChip(
                    selected = false,
                    onClick = { onPerformGlobalSearch(query) },
                    label = { Text("Search globally: \"$query\"") },
                )
            }
        }
    }
}

@Composable
private fun highlightedText(
    text: String,
    query: String,
) =
    remember(text, query) {
        if (query.isBlank()) return@remember buildAnnotatedString { append(text) }

        val lowerText = text.lowercase(Locale.getDefault())
        val lowerQuery = query.lowercase(Locale.getDefault())
        val matchStart = lowerText.indexOf(lowerQuery)

        if (matchStart < 0) {
            buildAnnotatedString { append(text) }
        } else {
            val matchEnd = matchStart + query.length
            buildAnnotatedString {
                append(text.substring(0, matchStart))
                withStyle(
                    style =
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.Unspecified,
                        ),
                ) {
                    append(text.substring(matchStart, matchEnd))
                }
                append(text.substring(matchEnd))
            }
        }
    }

private fun relevanceScore(
    value: String,
    query: String,
): Int {
    if (query.isBlank()) return 0

    val lowerValue = value.lowercase(Locale.getDefault())
    val lowerQuery = query.lowercase(Locale.getDefault())

    return when {
        lowerValue == lowerQuery -> 4
        lowerValue.startsWith(lowerQuery) -> 3
        lowerValue.contains(lowerQuery) -> 2
        else -> 1
    }
}
