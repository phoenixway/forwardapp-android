package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.entitybrowser.*

internal data class EntityLibraryQueryActions(
    val apply: (Set<CanonicalEntityClass>, Set<String>, Map<CanonicalScoreDimension, CanonicalScoreRange>) -> Unit,
    val unplaced: (Boolean) -> Unit,
    val sort: (EntityLibrarySort) -> Unit,
    val scoreSort: (CanonicalScoreDimension, Boolean) -> Unit,
    val clear: () -> Unit,
)

@Composable
internal fun EntityLibraryQueryControls(state: EntityLibraryUiState.Ready, actions: EntityLibraryQueryActions) {
    var filters by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(false) }
    val query = state.query
    val hasTypes = query.entityTypes != CanonicalEntityClass.entries.toSet()
    val hasFilters = hasTypes || query.tags.isNotEmpty() || query.scoreFilters.isNotEmpty() || query.unplaced
    if (filters) EntityLibraryQueryDialog(query, state.availableTags,
        onDismiss = { filters = false }, onApply = { types, tags, scores -> actions.apply(types, tags, scores); filters = false })
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { filters = true }) { Text("Фільтри") }
        Box {
            val sortLabel = if (query.sort.key == CanonicalEntitySortKey.SCORE)
                "${query.sort.dimension!!.displayLabel()}: ${if (query.sort.direction == CanonicalSortDirection.ASC) "↑" else "↓"}"
                else state.sort.label
            TextButton(onClick = { sort = true }) { Text(sortLabel) }
            DropdownMenu(expanded = sort, onDismissRequest = { sort = false }) {
                EntityLibrarySort.entries.filterNot { it == EntityLibrarySort.SCORE_ASC || it == EntityLibrarySort.SCORE_DESC }.forEach { value ->
                    DropdownMenuItem(text = { Text(value.label) }, onClick = { actions.sort(value); sort = false })
                }
                CanonicalScoreDimension.entries.forEach { dimension ->
                    listOf(false, true).forEach { descending ->
                        DropdownMenuItem(text = { Text("${dimension.displayLabel()}: ${if (descending) "за спаданням" else "за зростанням"}") },
                            onClick = { actions.scoreSort(dimension, descending); sort = false })
                    }
                }
            }
        }
        FilterChip(selected = query.unplaced, onClick = { actions.unplaced(!query.unplaced) }, label = { Text("Не розміщені") })
    }
    if (hasFilters) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasTypes) query.entityTypes.sortedBy { it.name }.forEach { type ->
                RemovableQueryChip(type.displayLabel()) {
                    actions.apply((query.entityTypes - type).ifEmpty { CanonicalEntityClass.entries.toSet() }, query.tags, query.scoreFilters)
                }
            }
            if (query.entityTypes.isEmpty()) RemovableQueryChip("Жодного типу") {
                actions.apply(CanonicalEntityClass.entries.toSet(), query.tags, query.scoreFilters)
            }
            query.tags.sorted().forEach { tag -> RemovableQueryChip("#$tag") { actions.apply(query.entityTypes, query.tags - tag, query.scoreFilters) } }
            query.scoreFilters.forEach { (dimension, range) ->
                RemovableQueryChip("${dimension.displayLabel()} ${range.displayLabel()}") {
                    actions.apply(query.entityTypes, query.tags, query.scoreFilters - dimension)
                }
            }
            TextButton(onClick = actions.clear) { Text("Скинути фільтри") }
        }
    }
}

@Composable
private fun RemovableQueryChip(label: String, remove: () -> Unit) {
    InputChip(selected = true, onClick = remove, label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Прибрати фільтр: $label", modifier = Modifier.size(16.dp)) })
}

internal fun CanonicalScoreDimension.displayLabel(): String = when (this) {
    CanonicalScoreDimension.IMPORTANCE -> "Важливість"
    CanonicalScoreDimension.IMPACT -> "Вплив"
    CanonicalScoreDimension.EFFORT -> "Зусилля"
    CanonicalScoreDimension.COST -> "Вартість"
    CanonicalScoreDimension.RISK -> "Ризик"
    CanonicalScoreDimension.SCORE -> "Підсумкова оцінка"
}

internal fun Double.queryLabel() = if (this % 1.0 == 0.0 && this >= Long.MIN_VALUE.toDouble() && this < Long.MAX_VALUE.toDouble()) toLong().toString() else toString()
internal fun CanonicalScoreRange.displayLabel() = listOfNotNull(min?.let { "≥ ${it.queryLabel()}" }, max?.let { "≤ ${it.queryLabel()}" }).joinToString("; ")
