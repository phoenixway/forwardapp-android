package com.romankozak.forwardappmobile.features.entitybrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.semantic.displayLabel

@Composable
fun CanonicalEntityBrowserContent(
    items: List<CanonicalEntitySummary>,
    searchText: String,
    availableClasses: Set<CanonicalEntityClass>,
    allowedClasses: Set<CanonicalEntityClass>,
    selectedRefs: Set<CanonicalEntityRef>,
    multiSelect: Boolean,
    onSearchChange: (String) -> Unit,
    onToggleClass: (CanonicalEntityClass) -> Unit,
    onSelect: (CanonicalEntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        CanonicalEntityBrowserSearch(searchText = searchText, onSearchChange = onSearchChange)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CanonicalEntityClass.entries.filter(availableClasses::contains).forEach { entityClass ->
                FilterChip(
                    selected = entityClass in allowedClasses,
                    onClick = { onToggleClass(entityClass) },
                    label = { Text(entityClass.displayLabel()) },
                )
            }
        }
        CanonicalEntityBrowserList(
            items = items,
            emptyMessage = if (searchText.isBlank()) "Немає доступних сутностей" else "Нічого не знайдено",
            selectedRefs = selectedRefs,
            multiSelect = multiSelect,
            showSelectionControl = true,
            onSelect = onSelect,
        )
    }
}

@Composable
fun CanonicalEntityBrowserSearch(
    searchText: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchChange,
        label = { Text("Пошук") },
        singleLine = true,
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Очистити пошук")
                }
            }
        },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun CanonicalEntityBrowserList(
    items: List<CanonicalEntitySummary>,
    emptyMessage: String,
    onSelect: (CanonicalEntityRef) -> Unit,
    modifier: Modifier = Modifier,
    selectedRefs: Set<CanonicalEntityRef> = emptySet(),
    multiSelect: Boolean = false,
    showSelectionControl: Boolean = false,
) {
    if (items.isEmpty()) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.padding(24.dp),
        )
    } else {
        LazyColumn(modifier = modifier.fillMaxWidth()) {
            items(items, key = { "${it.ref.entityClass}:${it.ref.id}" }) { item ->
                CanonicalEntityRow(
                    item = item,
                    selected = item.ref in selectedRefs,
                    multiSelect = multiSelect,
                    showSelectionControl = showSelectionControl,
                    onClick = { onSelect(item.ref) },
                )
            }
        }
    }
}

@Composable
fun CanonicalEntityRow(
    item: CanonicalEntitySummary,
    onClick: () -> Unit,
    selected: Boolean = false,
    multiSelect: Boolean = false,
    showSelectionControl: Boolean = false,
) {
    val metadata =
        when (item.ref.entityClass) {
            CanonicalEntityClass.ORIENTATION -> item.orientationKind?.displayLabel()
            CanonicalEntityClass.ASPECT -> item.ref.entityClass.displayLabel()
            CanonicalEntityClass.WORKSPACE -> item.ref.entityClass.displayLabel()
        }
    ListItem(
        headlineContent = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                listOfNotNull(metadata, item.subtitle).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            if (showSelectionControl && multiSelect) {
                Checkbox(checked = selected, onCheckedChange = null)
            } else if (showSelectionControl) {
                RadioButton(selected = selected, onClick = null)
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}
