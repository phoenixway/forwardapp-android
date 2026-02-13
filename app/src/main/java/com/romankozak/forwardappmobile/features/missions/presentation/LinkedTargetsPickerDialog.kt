package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LinkPickerTab {
    CONTEXTS,
    ATTACHMENTS,
}

sealed interface NewDocumentDraft {
    data class Note(val name: String) : NewDocumentDraft

    data class Checklist(val name: String) : NewDocumentDraft

    data class WebLink(val url: String, val name: String) : NewDocumentDraft

    data class Obsidian(val noteName: String, val displayName: String) : NewDocumentDraft
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
    onCreateRootContext: (suspend (String) -> String?)? = null,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)? = null,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var showDescendants by remember { mutableStateOf(false) }
    var showAddContextDialog by remember { mutableStateOf(false) }
    var newContextName by remember { mutableStateOf("") }
    var documentsMenuExpanded by remember { mutableStateOf(false) }
    var pendingDocumentType by remember { mutableStateOf<DocumentCreationType?>(null) }
    var documentName by remember { mutableStateOf("") }
    var documentTarget by remember { mutableStateOf("") }
    var horizontalDragAccum by remember { mutableStateOf(0f) }

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
    val contextById = remember(contextNodes) { contextNodes.associateBy { it.id } }
    val visibleContextIds =
        remember(contextNodes, childMap, query, showDescendants) {
            if (query.isBlank()) {
                contextNodes.map { it.id }.toSet()
            } else {
                val matchingIds = contextNodes.filter { it.title.contains(query, ignoreCase = true) }.map { it.id }.toSet()

                val ancestorIds = mutableSetOf<String>()
                matchingIds.forEach { id ->
                    var parentId = contextById[id]?.parentId
                    while (parentId != null) {
                        ancestorIds += parentId
                        parentId = contextById[parentId]?.parentId
                    }
                }

                val descendantIds = mutableSetOf<String>()
                if (showDescendants) {
                    val queue = ArrayDeque(matchingIds.toList())
                    while (queue.isNotEmpty()) {
                        val current = queue.removeFirst()
                        val children = childMap[current].orEmpty()
                        children.forEach { child ->
                            if (descendantIds.add(child.id)) {
                                queue.add(child.id)
                            }
                        }
                    }
                }

                matchingIds + ancestorIds + descendantIds
            }
        }

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
                floatingActionButton = {
                    if (selectedTab == LinkPickerTab.CONTEXTS && onCreateRootContext != null) {
                        FloatingActionButton(
                            onClick = {
                                newContextName = ""
                                showAddContextDialog = true
                            },
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    } else if (selectedTab == LinkPickerTab.ATTACHMENTS && onCreateDocument != null) {
                        Box {
                            FloatingActionButton(onClick = { documentsMenuExpanded = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = documentsMenuExpanded,
                                onDismissRequest = { documentsMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attachment_type_notes)) },
                                    onClick = {
                                        documentsMenuExpanded = false
                                        pendingDocumentType = DocumentCreationType.NOTE
                                        documentName = ""
                                        documentTarget = ""
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attachment_type_checklist)) },
                                    onClick = {
                                        documentsMenuExpanded = false
                                        pendingDocumentType = DocumentCreationType.CHECKLIST
                                        documentName = ""
                                        documentTarget = ""
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attachment_type_web_link)) },
                                    onClick = {
                                        documentsMenuExpanded = false
                                        pendingDocumentType = DocumentCreationType.WEB_LINK
                                        documentName = ""
                                        documentTarget = ""
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.attachment_type_obsidian)) },
                                    onClick = {
                                        documentsMenuExpanded = false
                                        pendingDocumentType = DocumentCreationType.OBSIDIAN
                                        documentName = ""
                                        documentTarget = ""
                                    },
                                )
                            }
                        }
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.picker_add_connections_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .pointerInput(hasBothTabs, selectedTab) {
                                if (!hasBothTabs) return@pointerInput
                                detectHorizontalDragGestures(
                                    onDragStart = { horizontalDragAccum = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        horizontalDragAccum += dragAmount
                                    },
                                    onDragEnd = {
                                        if (abs(horizontalDragAccum) >= 60f) {
                                            selectedTab =
                                                if (horizontalDragAccum < 0f) {
                                                    LinkPickerTab.ATTACHMENTS
                                                } else {
                                                    LinkPickerTab.CONTEXTS
                                                }
                                        }
                                        horizontalDragAccum = 0f
                                    },
                                    onDragCancel = { horizontalDragAccum = 0f },
                                )
                            },
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
                                CompactTypeTabs(
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                )
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
                                            stringResource(R.string.picker_search_contexts)
                                        } else {
                                            stringResource(R.string.picker_search_documents)
                                        },
                                    )
                                },
                            )

                            if (selectedTab == LinkPickerTab.CONTEXTS && query.isNotBlank()) {
                                CompactDescendantsToggle(
                                    enabled = showDescendants,
                                    onToggle = { showDescendants = !showDescendants },
                                )
                            }
                        }
                    }

                    if (selectedTab == LinkPickerTab.CONTEXTS) {
                        ContextPickerList(
                            topLevelContexts = topLevelContexts,
                            childMap = childMap,
                            visibleIds = visibleContextIds,
                            expandedIds = expandedIds,
                            onToggleExpanded = { id ->
                                expandedIds =
                                    if (id in expandedIds) {
                                        expandedIds - id
                                    } else {
                                        expandedIds + id
                                    }
                            },
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

    if (showAddContextDialog && onCreateRootContext != null) {
        AlertDialog(
            onDismissRequest = { showAddContextDialog = false },
            title = { Text(stringResource(R.string.add_action_project)) },
            text = {
                OutlinedTextField(
                    value = newContextName,
                    onValueChange = { newContextName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.picker_context_name_label)) },
                    placeholder = { Text(stringResource(R.string.picker_context_name_placeholder)) },
                )
            },
            confirmButton = {
                Button(
                    enabled = newContextName.isNotBlank(),
                    onClick = {
                        val name = newContextName.trim()
                        scope.launch {
                            val id = onCreateRootContext(name)
                            if (!id.isNullOrBlank()) {
                                onContextSelected(id)
                                onDismiss()
                            }
                            showAddContextDialog = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContextDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingDocumentType?.let { type ->
        DocumentCreationDialog(
            type = type,
            name = documentName,
            target = documentTarget,
            onNameChange = { documentName = it },
            onTargetChange = { documentTarget = it },
            onDismiss = { pendingDocumentType = null },
            onConfirm = {
                val request =
                    when (type) {
                        DocumentCreationType.NOTE -> NewDocumentDraft.Note(name = documentName.trim().ifBlank { "New note" })
                        DocumentCreationType.CHECKLIST -> NewDocumentDraft.Checklist(name = documentName.trim().ifBlank { "New checklist" })
                        DocumentCreationType.WEB_LINK -> NewDocumentDraft.WebLink(url = documentTarget.trim(), name = documentName.trim())
                        DocumentCreationType.OBSIDIAN ->
                            NewDocumentDraft.Obsidian(
                                noteName = documentTarget.trim(),
                                displayName = documentName.trim(),
                            )
                    }
                scope.launch {
                    val id = onCreateDocument?.invoke(request)
                    if (!id.isNullOrBlank()) {
                        onAttachmentSelected(id)
                        onDismiss()
                    }
                    pendingDocumentType = null
                }
            },
        )
    }
}

@Composable
private fun ContextPickerList(
    topLevelContexts: List<PickerNode>,
    childMap: Map<String, List<PickerNode>>,
    visibleIds: Set<String>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val visibleTopLevel =
        topLevelContexts.filter { it.id in visibleIds }

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
            items(visibleTopLevel, key = { it.id }) { node ->
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
    node: PickerNode,
    level: Int,
    childMap: Map<String, List<PickerNode>>,
    expandedIds: Set<String>,
    onToggleExpanded: (String) -> Unit,
    visibleIds: Set<String>,
    preselectedIds: Set<String>,
    onSelect: (String) -> Unit,
) {
    val isSelected = node.id in preselectedIds
    val children = childMap[node.id].orEmpty()
    val isExpanded = node.id in expandedIds
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "caret")

    val visibleChildren =
        children.filter { it.id in visibleIds }

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
                    visibleIds = visibleIds,
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

@Composable
private fun CompactTypeTabs(
    selectedTab: LinkPickerTab,
    onTabSelected: (LinkPickerTab) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CompactTypeTabButton(
                selected = selectedTab == LinkPickerTab.CONTEXTS,
                icon = Icons.Default.Folder,
                contentDescription = stringResource(R.string.picker_contexts),
                onClick = { onTabSelected(LinkPickerTab.CONTEXTS) },
                modifier = Modifier.weight(1f),
            )
            CompactTypeTabButton(
                selected = selectedTab == LinkPickerTab.ATTACHMENTS,
                icon = Icons.Default.AttachFile,
                contentDescription = stringResource(R.string.picker_documents),
                onClick = { onTabSelected(LinkPickerTab.ATTACHMENTS) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompactTypeTabButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun CompactDescendantsToggle(
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle),
        color =
            if (enabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (enabled) Icons.Default.Check else Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.picker_show_nested_contexts),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun attachmentIcon(type: LinkType?) =
    when (type) {
        LinkType.URL -> Icons.Default.Link
        LinkType.OBSIDIAN -> Icons.Default.Notes
        LinkType.CONTEXT -> Icons.Default.Folder
        null -> Icons.Default.InsertDriveFile
    }

private enum class DocumentCreationType {
    NOTE,
    CHECKLIST,
    WEB_LINK,
    OBSIDIAN,
}

@Composable
private fun DocumentCreationDialog(
    type: DocumentCreationType,
    name: String,
    target: String,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title =
        when (type) {
            DocumentCreationType.NOTE -> stringResource(R.string.attachment_type_notes)
            DocumentCreationType.CHECKLIST -> stringResource(R.string.attachment_type_checklist)
            DocumentCreationType.WEB_LINK -> stringResource(R.string.attachment_type_web_link)
            DocumentCreationType.OBSIDIAN -> stringResource(R.string.attachment_type_obsidian)
        }
    val targetLabel =
        when (type) {
            DocumentCreationType.NOTE,
            DocumentCreationType.CHECKLIST,
            -> null
            DocumentCreationType.WEB_LINK -> "URL"
            DocumentCreationType.OBSIDIAN -> stringResource(R.string.note_name)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (targetLabel != null) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = onTargetChange,
                        label = { Text(targetLabel) },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.display_name_optional)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled =
                    when (type) {
                        DocumentCreationType.NOTE,
                        DocumentCreationType.CHECKLIST,
                        -> true
                        DocumentCreationType.WEB_LINK,
                        DocumentCreationType.OBSIDIAN,
                        -> target.isNotBlank()
                    },
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
