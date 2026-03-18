package com.romankozak.forwardappmobile.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.dialogs.UiContextMarker
import java.text.BreakIterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageContextMarkersScreen(
    initialContextMarkers: List<UiContextMarker>,
    onBack: () -> Unit,
    onSave: (updatedContextMarkers: List<UiContextMarker>) -> Unit,
) {
    val contextMarkers =
        remember {
            mutableStateListOf<UiContextMarker>().apply {
                addAll(initialContextMarkers.map { it.copy() })
            }
        }

    val hasNameErrors by remember {
        derivedStateOf {
            val names =
                contextMarkers
                    .filter { !it.isReserved }
                    .map { it.name.lowercase().trim() }
                    .filter { it.isNotEmpty() }
            names.size != names.distinct().size
        }
    }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Manage Context Markers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = !hasNameErrors,
                        onClick = { onSave(contextMarkers.toList()) },
                    ) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        val reserved = contextMarkers.filter { it.isReserved }
        val custom = contextMarkers.filter { !it.isReserved }

        LazyColumn(
            modifier =
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (reserved.isNotEmpty()) {
                item { SectionHeader("Reserved") }
                items(reserved, key = { it.id }) { contextMarker ->
                    val index = contextMarkers.indexOf(contextMarker)
                    ContextMarkerEditorItem(
                        contextMarker = contextMarker,
                        contextMarkersList = contextMarkers,
                        onValueChange = { if (index != -1) contextMarkers[index] = it },
                        onDelete = {},
                    )
                }
            }

            item { SectionHeader("Custom") }

            if (custom.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Text(
                            text = "No custom context markers yet.\nTap the button below to add one.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(custom, key = { it.id }) { contextMarker ->
                    val index = contextMarkers.indexOf(contextMarker)
                    ContextMarkerEditorItem(
                        contextMarker = contextMarker,
                        contextMarkersList = contextMarkers,
                        onValueChange = { if (index != -1) contextMarkers[index] = it },
                        onDelete = { contextMarkers.remove(contextMarker) },
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        val newContextMarker = UiContextMarker(name = "", tag = "", emoji = "", isReserved = false)
                        contextMarkers.add(newContextMarker)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Context Marker")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add Custom Context Marker")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun ContextMarkerEditorItem(
    contextMarker: UiContextMarker,
    contextMarkersList: SnapshotStateList<UiContextMarker>,
    onValueChange: (UiContextMarker) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNameError =
        remember(contextMarker.name, contextMarkersList.size) {
            !contextMarker.isReserved && contextMarker.name.isNotBlank() &&
                contextMarkersList.any {
                    (it.id != contextMarker.id) && it.name.equals(contextMarker.name, ignoreCase = true)
                }
        }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = contextMarker.name,
                    onValueChange = {
                        onValueChange(
                            contextMarker.copy(
                                name = it.filter { char -> char.isLetterOrDigit() || (char == '-') },
                            ),
                        )
                    },
                    label = { Text("Name") },
                    modifier = Modifier.weight(1f),
                    readOnly = contextMarker.isReserved,
                    enabled = !contextMarker.isReserved,
                    singleLine = true,
                    isError = isNameError,
                    supportingText = { if (isNameError) Text("Name must be unique") },
                )
                if (!contextMarker.isReserved) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Context Marker",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = contextMarker.emoji,
                    onValueChange = { newText ->
                        if (newText.isNotEmpty()) {
                            val breakIterator = BreakIterator.getCharacterInstance()
                            breakIterator.setText(newText)
                            val firstCharacterEnd = breakIterator.next()
                            onValueChange(contextMarker.copy(emoji = newText.substring(0, firstCharacterEnd)))
                        } else {
                            onValueChange(contextMarker.copy(emoji = ""))
                        }
                    },
                    label = { Text("Emoji") },
                    modifier = Modifier.width(90.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = contextMarker.tag,
                    onValueChange = { onValueChange(contextMarker.copy(tag = it)) },
                    label = { Text("Corresponding Project Tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
    }
}
