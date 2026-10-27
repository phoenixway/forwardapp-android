package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType

@Composable
internal fun ContextPickerList(
    topLevelContexts: List<LinkedPickerNode>,
    childMap: Map<String, List<LinkedPickerNode>>,
    visibleIds: Set<String>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val visibleTopLevel =
        topLevelContexts.filter { it.key in visibleIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (visibleTopLevel.isEmpty()) {
            item {
                PickerEmptyState(text = stringResource(R.string.picker_no_contexts_found))
            }
        } else {
            items(visibleTopLevel, key = { it.key }) { node ->
                ContextRow(
                    node = node,
                    level = 0,
                    childMap = childMap,
                    expandedIds = expandedIds,
                    onToggleExpanded = onToggleExpanded,
                    visibleIds = visibleIds,
                    preselectedIds = preselectedIds,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun ContextRow(
    node: LinkedPickerNode,
    level: Int,
    childMap: Map<String, List<LinkedPickerNode>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    visibleIds: Set<String>,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val isSelected = node.targetId in preselectedIds
    val children = childMap[node.key].orEmpty()
    val isExpanded = node.key in expandedIds
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "caret")

    val visibleChildren =
        children.filter { it.key in visibleIds }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ElevatedCard(
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(node.targetId) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width((level * 14).dp))

                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleExpanded(node.key) },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.rotate(rotation),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(26.dp))
                }

                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = node.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }

        if (isExpanded && visibleChildren.isNotEmpty()) {
            visibleChildren.forEach { child ->
                ContextRow(
                    node = child,
                    level = level + 1,
                    childMap = childMap,
                    expandedIds = expandedIds,
                    onToggleExpanded = onToggleExpanded,
                    visibleIds = visibleIds,
                    preselectedIds = preselectedIds,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
internal fun AttachmentPickerList(
    options: List<AttachmentOption>,
    query: String,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val filtered =
        remember(options, query) {
            options
                .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                .sortedBy { it.name.lowercase() }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (filtered.isEmpty()) {
            item {
                PickerEmptyState(text = stringResource(R.string.picker_no_documents_found))
            }
        } else {
            items(filtered, key = { it.id }) { option ->
                val isSelected = option.id in preselectedIds
                ElevatedCard(
                    colors =
                        CardDefaults.elevatedCardColors(
                            containerColor =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                        ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = attachmentIcon(option.linkType),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
                            Box(
                                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondary).padding(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerEmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun attachmentIcon(type: LinkType?) =
    when (type) {
        LinkType.URL -> Icons.Default.Link
        LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Notes
        LinkType.CONTEXT -> Icons.Default.Folder
        LinkType.NOTE_DOCUMENT,
        LinkType.CHECKLIST,
        LinkType.MUSIC_NOTE,
        null -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
