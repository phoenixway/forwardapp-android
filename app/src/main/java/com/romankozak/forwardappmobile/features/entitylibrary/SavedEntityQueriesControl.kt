package com.romankozak.forwardappmobile.features.entitylibrary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.entitybrowser.SavedEntityQuery
import com.romankozak.forwardappmobile.features.entitybrowser.*

@Composable
internal fun SavedEntityQueriesControl(
    current: () -> CanonicalEntityQuery,
    model: SavedEntityQueriesViewModel,
    onApply: (CanonicalEntityQuery) -> Unit,
) {
    val saved by model.queries.collectAsStateWithLifecycle()
    val editing by model.editing.collectAsStateWithLifecycle()
    var showList by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf<SavedEntityQuery?>(null) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { showList = true }, enabled = !editing.busy) { Text("Збережені (${saved.items.size})") }
        TextButton(onClick = { model.beginSave(current()) }, enabled = !editing.busy && saved.issue == null) { Text("Зберегти запит…") }
    }
    saved.issue?.let { Text(it, Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.error) }
    if (showList) AlertDialog(onDismissRequest = { showList = false }, title = { Text("Збережені запити") }, text = {
        Column {
            if (saved.items.isEmpty()) Text("Збережених запитів поки немає")
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(saved.items, key = { it.id }) { item ->
                    val decoded = remember(item.specification) { item.decode() }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(Modifier.weight(1f).clickable(enabled = decoded.query != null && !editing.busy) {
                            decoded.query?.let(onApply); showList = false
                        }.padding(vertical = 10.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall)
                            Text(decoded.issue ?: canonicalQueryPreview(requireNotNull(decoded.query)), style = MaterialTheme.typography.bodySmall)
                        }
                        var menu by remember(item.id) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menu = true }, enabled = !editing.busy) { Icon(Icons.Default.MoreVert, "Дії із запитом ${item.name}") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Перейменувати") }, onClick = { menu = false; showList = false; model.beginRename(item) })
                                DropdownMenuItem(text = { Text("Видалити запит") }, onClick = { menu = false; delete = item })
                            }
                        }
                    }
                }
            }
            editing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = { showList = false }) { Text("Закрити") } })
    editing.draft?.let { draft ->
        AlertDialog(onDismissRequest = model::cancel, title = { Text(if (draft.id == null) "Зберегти запит" else "Перейменувати запит") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = draft.name, onValueChange = model::changeName, enabled = !editing.busy,
                    label = { Text("Назва") }, singleLine = true)
                draft.query?.let { Text(canonicalQueryPreview(it), style = MaterialTheme.typography.bodySmall) }
                editing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = model::submit, enabled = !editing.busy && draft.name.trim().length in 1..120) { Text("Зберегти") } },
            dismissButton = { TextButton(onClick = model::cancel, enabled = !editing.busy) { Text("Скасувати") } })
    }
    delete?.let { item ->
        AlertDialog(onDismissRequest = { delete = null }, title = { Text("Видалити запит «${item.name}»?") },
            text = { Text("Видаляється лише збережений запит. Сутності, їхні дані та поточні фільтри не зміняться.") },
            confirmButton = { TextButton(onClick = { model.delete(item.id); delete = null }, enabled = !editing.busy) { Text("Видалити запит") } },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("Скасувати") } })
    }
}

internal fun canonicalQueryPreview(query: CanonicalEntityQuery): String = buildList {
    if (query.text.isNotBlank()) add("Пошук: ${query.text}")
    add(query.entityTypes.sortedBy { it.name }.joinToString { it.displayLabel() }.ifEmpty { "Жодного типу" })
    if (query.tags.isNotEmpty()) add(query.tags.sorted().joinToString(" ") { "#$it" })
    query.scoreFilters.entries.sortedBy { it.key.code }.forEach { (dimension, range) -> add("${dimension.displayLabel()} ${range.displayLabel()}") }
    if (query.unplaced) add("Не розміщені")
    val field = when (query.sort.key) {
        CanonicalEntitySortKey.DEFAULT -> "за замовчуванням"
        CanonicalEntitySortKey.TITLE -> "назва"
        CanonicalEntitySortKey.CREATED -> "створено"
        CanonicalEntitySortKey.UPDATED -> "оновлено"
        CanonicalEntitySortKey.SCORE -> requireNotNull(query.sort.dimension).displayLabel()
    }
    add("Сортування: $field ${if (query.sort.direction == CanonicalSortDirection.ASC) "↑" else "↓"}")
}.joinToString("\n")
