package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType

enum class LinkPickerTab {
    CONTEXTS,
    ATTACHMENTS,
}

private data class PickerNode(
    val id: String,
    val title: String,
    val parentId: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedTargetsPickerDialog(
    contextOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    preselectedContextIds: Set<String>,
    preselectedAttachmentIds: Set<String>,
    initialTab: LinkPickerTab,
    onDismiss: () -> Unit,
    onContextSelected: (String) -> Unit,
    onAttachmentSelected: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }

    val contextNodes =
        remember(contextOptions) {
            contextOptions
                .map { PickerNode(id = it.id, title = it.name, parentId = it.parentId) }
                .distinctBy { it.id }
        }

    val childMap =
        remember(contextNodes) {
            contextNodes
                .filter { !it.parentId.isNullOrBlank() }
                .groupBy { it.parentId!! }
                .mapValues { (_, value) -> value.sortedBy { it.title.lowercase() } }
        }

    val topLevelContexts =
        remember(contextNodes) {
            contextNodes
                .filter { it.parentId.isNullOrBlank() }
                .sortedBy { it.title.lowercase() }
        }

    val hasBothTabs = contextOptions.isNotEmpty() && attachmentOptions.isNotEmpty()

    LaunchedEffect(query, childMap, contextNodes) {
        if (query.isBlank()) {
            expandedIds = emptySet()
            return@LaunchedEffect
        }
        val byId = contextNodes.associateBy { it.id }
        val matched = contextNodes.filter { it.title.contains(query, ignoreCase = true) }
        val nextExpanded = mutableSetOf<String>()
        matched.forEach { node ->
            var parentId = node.parentId
            while (parentId != null) {
                nextExpanded += parentId
                parentId = byId[parentId]?.parentId
            }
        }
        expandedIds = nextExpanded
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Link Targets",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    brush =
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                                Color.Transparent,
                                            ),
                                        ),
                                ).padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (hasBothTabs) {
                                TabRow(selectedTabIndex = if (selectedTab == LinkPickerTab.CONTEXTS) 0 else 1) {
                                    Tab(
                                        selected = selectedTab == LinkPickerTab.CONTEXTS,
                                        onClick = { selectedTab = LinkPickerTab.CONTEXTS },
                                        text = { Text("Contexts") },
                                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                    )
                                    Tab(
                                        selected = selectedTab == LinkPickerTab.ATTACHMENTS,
                                        onClick = { selectedTab = LinkPickerTab.ATTACHMENTS },
                                        text = { Text("Attachments") },
                                        icon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                placeholder = {
                                    Text(
                                        if (selectedTab == LinkPickerTab.CONTEXTS) {
                                            "Search contexts"
                                        } else {
                                            "Search attachments"
                                        },
                                    )
                                },
                            )
                        }
                    }

                    if (selectedTab == LinkPickerTab.CONTEXTS) {
                        ContextPickerList(
                            topLevelContexts = topLevelContexts,
                            childMap = childMap,
                            expandedIds = expandedIds,
                            onToggleExpanded = { id ->
                                expandedIds =
                                    if (id in expandedIds) {
                                        expandedIds - id
                                    } else {
                                        expandedIds + id
                                    }
                            },
                            query = query,
                            preselectedIds = preselectedContextIds,
                            onSelect = onContextSelected,
                        )
                    } else {
                        AttachmentPickerList(
                            options = attachmentOptions,
                            query = query,
                            preselectedIds = preselectedAttachmentIds,
                            onSelect = onAttachmentSelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextPickerList(
    topLevelContexts: List<PickerNode>,
    childMap: Map<String, List<PickerNode>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    query: String,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val visibleTopLevel =
        if (query.isBlank()) {
            topLevelContexts
        } else {
            topLevelContexts.filter { it.title.contains(query, ignoreCase = true) || hasMatchedDescendant(it.id, query, childMap) }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (visibleTopLevel.isEmpty()) {
            item {
                PickerEmptyState(text = "No contexts found")
            }
        } else {
            items(visibleTopLevel, key = { it.id }) { node ->
                ContextRow(
                    node = node,
                    level = 0,
                    childMap = childMap,
                    expandedIds = expandedIds,
                    onToggleExpanded = onToggleExpanded,
                    query = query,
                    preselectedIds = preselectedIds,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun ContextRow(
    node: PickerNode,
    level: Int,
    childMap: Map<String, List<PickerNode>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    query: String,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val isSelected = node.id in preselectedIds
    val children = childMap[node.id].orEmpty()
    val isExpanded = node.id in expandedIds
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "caret")

    val visibleChildren =
        if (query.isBlank()) {
            children
        } else {
            children.filter { it.title.contains(query, ignoreCase = true) || hasMatchedDescendant(it.id, query, childMap) }
        }

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
                        .clickable { onSelect(node.id) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width((level * 14).dp))

                if (children.isNotEmpty()) {
                    IconButton(
                        onClick = { onToggleExpanded(node.id) },
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
                    query = query,
                    preselectedIds = preselectedIds,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun AttachmentPickerList(
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
                PickerEmptyState(text = "No attachments found")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            option.linkType?.let {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = it.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
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

private fun hasMatchedDescendant(
    id: String,
    query: String,
    childMap: Map<String, List<PickerNode>>,
): Boolean {
    val children = childMap[id].orEmpty()
    if (children.isEmpty()) return false
    return children.any { child ->
        child.title.contains(query, ignoreCase = true) || hasMatchedDescendant(child.id, query, childMap)
    }
}

private fun attachmentIcon(type: LinkType?) =
    when (type) {
        LinkType.URL -> Icons.Default.Link
        LinkType.OBSIDIAN -> Icons.Default.Notes
        null -> Icons.Default.InsertDriveFile
    }
