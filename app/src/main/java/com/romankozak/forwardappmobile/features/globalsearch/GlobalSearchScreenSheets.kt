package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun TypeSelectionList(
    options: List<GlobalSearchType>,
    draft: Set<GlobalSearchType>,
    onSelectAll: () -> Unit,
    onToggleType: (GlobalSearchType) -> Unit,
) {
    ListItem(
        headlineContent = { Text("Усі типи") },
        leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingContent = {
            if (draft.size == options.size) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelectAll),
    )
    options.forEach { type ->
        ListItem(
            headlineContent = { Text(type.label) },
            leadingContent = { Icon(type.icon, contentDescription = null) },
            trailingContent = {
                if (type in draft) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { onToggleType(type) },
        )
    }
}

@Composable
internal fun TypeBottomSheetActions(
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text("Скасувати")
        }
        Button(onClick = onApply, modifier = Modifier.weight(1f)) {
            Text("Застосувати")
        }
    }
}
