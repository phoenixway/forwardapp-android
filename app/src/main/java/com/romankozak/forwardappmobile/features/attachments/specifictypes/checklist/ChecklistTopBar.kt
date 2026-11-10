package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChecklistTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    showCheckboxes: Boolean,
    onToggleCheckboxes: () -> Unit,
    onClearCompleted: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImportFromClipboard: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkAllCompleted: () -> Unit,
    onMarkAllIncomplete: () -> Unit,
    isSelectionMode: Boolean,
    selectedCount: Int,
    canPasteChecklistItems: Boolean,
    onToggleSelectionMode: () -> Unit,
    onClearSelection: () -> Unit,
    onSelectAllForSelection: () -> Unit,
    onCopySelected: () -> Unit,
    onCutSelected: () -> Unit,
    onPasteChecklistItems: () -> Unit,
    onSearchInList: () -> Unit,
    onShowUsages: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.checklist_title_placeholder)) },
                singleLine = true,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                    ),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.checklist_usages)) },
                    onClick = { menuExpanded = false; onShowUsages() },
                )
                DropdownMenuItem(
                    text = { Text("Search in list") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onSearchInList()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text =
                                if (showCheckboxes) {
                                    "Hide checkboxes"
                                } else {
                                    "Show checkboxes"
                                },
                        )
                    },
                    leadingIcon = {
                        val icon =
                            if (showCheckboxes) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank
                        Icon(imageVector = icon, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleCheckboxes()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Clear completed") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onClearCompleted()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Copy as Markdown") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onExportMarkdown()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Import from clipboard") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        menuExpanded = false
                        onImportFromClipboard()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Select all") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.CheckBox, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onSelectAll()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Mark all done") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Check, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onMarkAllCompleted()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Mark all not done") },
                    leadingIcon = { Icon(imageVector = Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onMarkAllIncomplete()
                    },
                )
                DropdownMenuItem(
                    text = { Text(if (isSelectionMode) "Exit multi-select" else "Multi-select mode") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.CheckBox, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onToggleSelectionMode()
                    },
                )
                if (isSelectionMode) {
                    DropdownMenuItem(
                        text = { Text("Select all items") },
                        leadingIcon = { Icon(imageVector = Icons.Filled.CheckBox, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSelectAllForSelection()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Copy selected ($selectedCount)") },
                        leadingIcon = { Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCopySelected()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Cut selected ($selectedCount)") },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.ContentCut, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onCutSelected()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Clear selection") },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClearSelection()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Paste checklist items") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onPasteChecklistItems()
                    },
                    enabled = canPasteChecklistItems,
                )
            }
        },
    )
}

