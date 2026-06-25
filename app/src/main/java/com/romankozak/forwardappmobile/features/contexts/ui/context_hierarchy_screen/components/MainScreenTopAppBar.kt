package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun ProjectHierarchyScreenTopAppBar(
    onBackClick: () -> Unit,
    showHierarchyBack: Boolean,
    onHierarchyBackClick: () -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    canPasteToFocusedContext: Boolean,
    onCopySelection: () -> Unit,
    onCutSelection: () -> Unit,
    onPasteToFocusedContext: () -> Unit,
    isSiblingReorderMode: Boolean,
    onToggleSiblingReorderMode: () -> Unit,
    onSearchClick: () -> Unit,
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showHierarchyBack) {
            IconButton(onClick = onHierarchyBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Повернутися в ієрархії",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = if (isSelectionMode) "Вибрано: $selectedCount" else "Orientation Hierarchy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isSelectionMode) {
            IconButton(onClick = onCopySelection, enabled = selectedCount > 0) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Копіювати вибране",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onCutSelection, enabled = selectedCount > 0) {
                Icon(
                    imageVector = Icons.Default.ContentCut,
                    contentDescription = "Вирізати вибране",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else if (canPasteToFocusedContext) {
            IconButton(onClick = onPasteToFocusedContext) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = "Вставити у вибраний контекст",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (!isSelectionMode) {
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    text = { Text("Search") },
                    onClick = {
                        showMoreMenu = false
                        onSearchClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (isSiblingReorderMode) "Done" else "Reorder siblings") },
                    onClick = {
                        showMoreMenu = false
                        onToggleSiblingReorderMode()
                    },
                )
            }
        }
    }
}
