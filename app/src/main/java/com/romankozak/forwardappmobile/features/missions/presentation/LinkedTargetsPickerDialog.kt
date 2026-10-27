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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.hierarchy.ChooserHierarchyItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LinkPickerTab {
    CONTEXTS,
    ATTACHMENTS,
}

enum class PickerCreateAction {
    CONTEXT,
    NOTE,
    MUSIC_NOTE,
    CHECKLIST,
    WEB_LINK,
    OBSIDIAN,
}

sealed interface NewDocumentDraft {
    data class Note(val name: String) : NewDocumentDraft


    data class MusicNote(val name: String) : NewDocumentDraft

    data class Checklist(val name: String) : NewDocumentDraft

    data class WebLink(val url: String, val name: String) : NewDocumentDraft

    data class Obsidian(
        val noteName: String,
        val displayName: String,
        val vault: String? = null,
    ) : NewDocumentDraft
}

private data class LinkedTargetsPickerDialogState(
    val query: String,
    val selectedTab: LinkPickerTab,
    val expandedIds: Set<String>,
    val showDescendants: Boolean,
    val showAddContextDialog: Boolean,
    val newContextName: String,
    val documentsMenuExpanded: Boolean,
    val pendingDocumentType: DocumentCreationType?,
    val documentName: String,
    val documentTarget: String,
    val documentVault: String,
)

private data class LinkedTargetsPickerDerivedState(
    val contextsEnabled: Boolean,
    val attachmentsEnabled: Boolean,
    val hasContextsTab: Boolean,
    val hasAttachmentsTab: Boolean,
    val hasBothTabs: Boolean,
    val contextNodes: List<LinkedPickerNode>,
    val childMap: Map<String, List<LinkedPickerNode>>,
    val topLevelContexts: List<LinkedPickerNode>,
    val visibleContextKeys: Set<String>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun LinkedTargetsPickerDialog(
    contextOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    preselectedContextIds: Set<String>,
    preselectedAttachmentIds: Set<String>,
    initialTab: LinkPickerTab,
    allowedTabs: Set<LinkPickerTab> = setOf(LinkPickerTab.CONTEXTS, LinkPickerTab.ATTACHMENTS),
    initialCreateAction: PickerCreateAction? = null,
    onDismiss: () -> Unit,
    onContextSelected: (String) -> Unit,
    onAttachmentSelected: (String) -> Unit,
    onCreateRootContext: (suspend (String) -> String?)? = null,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)? = null,
) {
    LinkedTargetsPickerDialogRoute(
        contextOptions = contextOptions,
        attachmentOptions = attachmentOptions,
        preselectedContextIds = preselectedContextIds,
        preselectedAttachmentIds = preselectedAttachmentIds,
        initialTab = initialTab,
        allowedTabs = allowedTabs,
        initialCreateAction = initialCreateAction,
        onDismiss = onDismiss,
        onContextSelected = onContextSelected,
        onAttachmentSelected = onAttachmentSelected,
        onCreateRootContext = onCreateRootContext,
        onCreateDocument = onCreateDocument,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkedTargetsPickerDialogRoute(
    contextOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    preselectedContextIds: Set<String>,
    preselectedAttachmentIds: Set<String>,
    initialTab: LinkPickerTab,
    allowedTabs: Set<LinkPickerTab>,
    initialCreateAction: PickerCreateAction?,
    onDismiss: () -> Unit,
    onContextSelected: (String) -> Unit,
    onAttachmentSelected: (String) -> Unit,
    onCreateRootContext: (suspend (String) -> String?)?,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)?,
) {
    val scope = rememberCoroutineScope()
    val hierarchyViewModel: LinkedTargetsPickerHierarchyViewModel = hiltViewModel()
    val canonicalOccurrences by hierarchyViewModel.occurrences.collectAsStateWithLifecycle()
    val contextsEnabled = remember(allowedTabs) { LinkPickerTab.CONTEXTS in allowedTabs }
    val attachmentsEnabled = remember(allowedTabs) { LinkPickerTab.ATTACHMENTS in allowedTabs }
    var selectedTab by remember(initialTab, allowedTabs) {
        mutableStateOf(
            when {
                initialTab in allowedTabs -> initialTab
                contextsEnabled -> LinkPickerTab.CONTEXTS
                else -> LinkPickerTab.ATTACHMENTS
            },
        )
    }
    var query by remember { mutableStateOf("") }
    var expandedIds by remember { mutableStateOf(setOf<String>()) }
    var showDescendants by remember { mutableStateOf(false) }
    var showAddContextDialog by remember { mutableStateOf(false) }
    var newContextName by remember { mutableStateOf("") }
    var documentsMenuExpanded by remember { mutableStateOf(false) }
    var pendingDocumentType by remember { mutableStateOf<DocumentCreationType?>(null) }
    var documentName by remember { mutableStateOf("") }
    var documentTarget by remember { mutableStateOf("") }
    var documentVault by remember { mutableStateOf("") }
    val derivedState =
        rememberLinkedTargetsPickerDerivedState(
            contextOptions = contextOptions,
            canonicalOccurrences = canonicalOccurrences,
            attachmentOptions = attachmentOptions,
            allowedTabs = allowedTabs,
            query = query,
            showDescendants = showDescendants,
        )
    val dialogState =
        LinkedTargetsPickerDialogState(
            query = query,
            selectedTab = selectedTab,
            expandedIds = expandedIds,
            showDescendants = showDescendants,
            showAddContextDialog = showAddContextDialog,
            newContextName = newContextName,
            documentsMenuExpanded = documentsMenuExpanded,
            pendingDocumentType = pendingDocumentType,
            documentName = documentName,
            documentTarget = documentTarget,
            documentVault = documentVault,
        )
    val directCreateAction = initialCreateAction?.takeIf {
        if (it == PickerCreateAction.CONTEXT) derivedState.contextsEnabled && onCreateRootContext != null
        else derivedState.attachmentsEnabled && onCreateDocument != null
    }

    SyncExpandedIdsWithQuery(
        query = query,
        contextNodes = derivedState.contextNodes,
        onExpandedIdsChange = { expandedIds = it },
    )
    HandleInitialPickerCreateAction(
        initialCreateAction = directCreateAction,
        hasContextsTab = derivedState.hasContextsTab,
        hasAttachmentsTab = derivedState.attachmentsEnabled,
        onCreateRootContext = onCreateRootContext,
        onCreateDocument = onCreateDocument,
        onSelectTab = { selectedTab = it },
        onOpenContextCreation = {
            newContextName = ""
            showAddContextDialog = true
        },
        onOpenDocumentCreation = { type ->
            pendingDocumentType = type
            documentName = ""
            documentTarget = ""
            documentVault = ""
        },
    )

    if (showLinkedTargetsPickerResults(directCreateAction, showAddContextDialog, pendingDocumentType != null)) {
        LinkedTargetsPickerDialogShell(
            dialogState = dialogState,
            derivedState = derivedState,
            attachmentOptions = attachmentOptions,
            preselectedContextIds = preselectedContextIds,
            preselectedAttachmentIds = preselectedAttachmentIds,
            onDismiss = onDismiss,
            onQueryChange = { query = it },
            onTabSelected = { selectedTab = it },
            onExpandedIdsChange = { expandedIds = it },
            onToggleDescendants = { showDescendants = !showDescendants },
            onOpenContextCreation = {
                newContextName = ""
                showAddContextDialog = true
            },
            onDocumentsMenuExpandedChange = { documentsMenuExpanded = it },
            onOpenDocumentCreation = { type ->
                pendingDocumentType = type
                documentName = ""
                documentTarget = ""
                documentVault = ""
            },
            onContextSelected = onContextSelected,
            onAttachmentSelected = onAttachmentSelected,
        )
    }

    ContextCreationOverlay(
        isVisible = showAddContextDialog,
        newContextName = newContextName,
        onNameChange = { newContextName = it },
        onDismiss = {
            showAddContextDialog = false
            if (directCreateAction != null) onDismiss()
        },
        onCreateRootContext = onCreateRootContext,
        onContextSelected = onContextSelected,
        onPickerDismiss = onDismiss,
        scope = scope,
    )
    DocumentCreationOverlay(
        type = pendingDocumentType,
        name = documentName,
        target = documentTarget,
        vault = documentVault,
        onNameChange = { documentName = it },
        onTargetChange = { documentTarget = it },
        onVaultChange = { documentVault = it },
        onDismiss = {
            pendingDocumentType = null
            documentVault = ""
            if (directCreateAction != null) onDismiss()
        },
        onCreateDocument = onCreateDocument,
        onAttachmentSelected = onAttachmentSelected,
        onPickerDismiss = onDismiss,
        scope = scope,
    )
}

@Composable
private fun rememberLinkedTargetsPickerDerivedState(
    contextOptions: List<ProjectOption>,
    canonicalOccurrences: List<ChooserHierarchyItem>?,
    attachmentOptions: List<AttachmentOption>,
    allowedTabs: Set<LinkPickerTab>,
    query: String,
    showDescendants: Boolean,
): LinkedTargetsPickerDerivedState {
    val contextNodes =
        remember(contextOptions, canonicalOccurrences) {
            // Before the first canonical read, options are flat target-only entries.
            // No V1 parentId can establish picker topology.
            buildLinkedPickerNodes(
                options = contextOptions,
                occurrences = canonicalOccurrences.orEmpty(),
            )
        }
    val tree =
        remember(contextNodes, query, showDescendants) {
            buildLinkedPickerTree(
                nodes = contextNodes,
                query = query,
                showDescendants = showDescendants,
            )
        }

    return LinkedTargetsPickerDerivedState(
        contextsEnabled = LinkPickerTab.CONTEXTS in allowedTabs,
        attachmentsEnabled = LinkPickerTab.ATTACHMENTS in allowedTabs,
        hasContextsTab = LinkPickerTab.CONTEXTS in allowedTabs,
        hasAttachmentsTab = LinkPickerTab.ATTACHMENTS in allowedTabs && attachmentOptions.isNotEmpty(),
        hasBothTabs =
            LinkPickerTab.CONTEXTS in allowedTabs &&
                LinkPickerTab.ATTACHMENTS in allowedTabs &&
                attachmentOptions.isNotEmpty(),
        contextNodes = contextNodes,
        childMap = tree.childrenByKey,
        topLevelContexts = tree.roots,
        visibleContextKeys = tree.visibleKeys,
    )
}

@Composable
private fun SyncExpandedIdsWithQuery(
    query: String,
    contextNodes: List<LinkedPickerNode>,
    onExpandedIdsChange: (Set<String>) -> Unit,
) {
    LaunchedEffect(query, contextNodes) {
        onExpandedIdsChange(linkedPickerExpandedKeysForQuery(contextNodes, query))
    }
}

@Composable
private fun HandleInitialPickerCreateAction(
    initialCreateAction: PickerCreateAction?,
    hasContextsTab: Boolean,
    hasAttachmentsTab: Boolean,
    onCreateRootContext: (suspend (String) -> String?)?,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)?,
    onSelectTab: (LinkPickerTab) -> Unit,
    onOpenContextCreation: () -> Unit,
    onOpenDocumentCreation: (DocumentCreationType) -> Unit,
) {
    LaunchedEffect(initialCreateAction) {
        when (initialCreateAction) {
            PickerCreateAction.CONTEXT -> {
                if (hasContextsTab && onCreateRootContext != null) {
                    onSelectTab(LinkPickerTab.CONTEXTS)
                    onOpenContextCreation()
                }
            }
            PickerCreateAction.NOTE -> {
                if (hasAttachmentsTab && onCreateDocument != null) {
                    onSelectTab(LinkPickerTab.ATTACHMENTS)
                    onOpenDocumentCreation(DocumentCreationType.NOTE)
                }
            }
            PickerCreateAction.CHECKLIST -> {
                if (hasAttachmentsTab && onCreateDocument != null) {
                    onSelectTab(LinkPickerTab.ATTACHMENTS)
                    onOpenDocumentCreation(DocumentCreationType.CHECKLIST)
                }
            }
            PickerCreateAction.MUSIC_NOTE -> {
                if (hasAttachmentsTab && onCreateDocument != null) {
                    onSelectTab(LinkPickerTab.ATTACHMENTS)
                    onOpenDocumentCreation(DocumentCreationType.MUSIC_NOTE)
                }
            }
            PickerCreateAction.WEB_LINK -> {
                if (hasAttachmentsTab && onCreateDocument != null) {
                    onSelectTab(LinkPickerTab.ATTACHMENTS)
                    onOpenDocumentCreation(DocumentCreationType.WEB_LINK)
                }
            }
            PickerCreateAction.OBSIDIAN -> {
                if (hasAttachmentsTab && onCreateDocument != null) {
                    onSelectTab(LinkPickerTab.ATTACHMENTS)
                    onOpenDocumentCreation(DocumentCreationType.OBSIDIAN)
                }
            }
            null -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkedTargetsPickerDialogShell(
    dialogState: LinkedTargetsPickerDialogState,
    derivedState: LinkedTargetsPickerDerivedState,
    attachmentOptions: List<AttachmentOption>,
    preselectedContextIds: Set<String>,
    preselectedAttachmentIds: Set<String>,
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTabSelected: (LinkPickerTab) -> Unit,
    onExpandedIdsChange: (Set<String>) -> Unit,
    onToggleDescendants: () -> Unit,
    onOpenContextCreation: () -> Unit,
    onDocumentsMenuExpandedChange: (Boolean) -> Unit,
    onOpenDocumentCreation: (DocumentCreationType) -> Unit,
    onContextSelected: (String) -> Unit,
    onAttachmentSelected: (String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().imePadding(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                floatingActionButton = {
                    LinkedTargetsPickerFab(
                        selectedTab = dialogState.selectedTab,
                        hasContextsTab = derivedState.hasContextsTab,
                        hasAttachmentsTab = derivedState.hasAttachmentsTab,
                        documentsMenuExpanded = dialogState.documentsMenuExpanded,
                        canCreateContext = derivedState.contextsEnabled,
                        canCreateDocument = derivedState.attachmentsEnabled,
                        onOpenContextCreation = onOpenContextCreation,
                        onDocumentsMenuExpandedChange = onDocumentsMenuExpandedChange,
                        onOpenDocumentCreation = onOpenDocumentCreation,
                    )
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
                            .pickerDragGesture(
                                hasBothTabs = derivedState.hasBothTabs,
                                selectedTab = dialogState.selectedTab,
                                onTabSelected = onTabSelected,
                            ),
                ) {
                    LinkedTargetsPickerHeader(
                        selectedTab = dialogState.selectedTab,
                        query = dialogState.query,
                        showDescendants = dialogState.showDescendants,
                        hasBothTabs = derivedState.hasBothTabs,
                        onQueryChange = onQueryChange,
                        onTabSelected = onTabSelected,
                        onToggleDescendants = onToggleDescendants,
                    )

                    if (dialogState.selectedTab == LinkPickerTab.CONTEXTS) {
                        ContextPickerList(
                            topLevelContexts = derivedState.topLevelContexts,
                            childMap = derivedState.childMap,
                            visibleIds = derivedState.visibleContextKeys,
                            expandedIds = dialogState.expandedIds,
                            onToggleExpanded = { id ->
                                onExpandedIdsChange(
                                    if (id in dialogState.expandedIds) {
                                        dialogState.expandedIds - id
                                    } else {
                                        dialogState.expandedIds + id
                                    },
                                )
                            },
                            preselectedIds = preselectedContextIds,
                            onSelect = onContextSelected,
                        )
                    } else {
                        AttachmentPickerList(
                            options = attachmentOptions,
                            query = dialogState.query,
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
private fun LinkedTargetsPickerHeader(
    selectedTab: LinkPickerTab,
    query: String,
    showDescendants: Boolean,
    hasBothTabs: Boolean,
    onQueryChange: (String) -> Unit,
    onTabSelected: (LinkPickerTab) -> Unit,
    onToggleDescendants: () -> Unit,
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
                CompactTypeTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                    onToggle = onToggleDescendants,
                )
            }
        }
    }
}

@Composable
private fun LinkedTargetsPickerFab(
    selectedTab: LinkPickerTab,
    hasContextsTab: Boolean,
    hasAttachmentsTab: Boolean,
    documentsMenuExpanded: Boolean,
    canCreateContext: Boolean,
    canCreateDocument: Boolean,
    onOpenContextCreation: () -> Unit,
    onDocumentsMenuExpandedChange: (Boolean) -> Unit,
    onOpenDocumentCreation: (DocumentCreationType) -> Unit,
) {
    if (selectedTab == LinkPickerTab.CONTEXTS && hasContextsTab && canCreateContext) {
        FloatingActionButton(onClick = onOpenContextCreation) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
        return
    }

    if (selectedTab == LinkPickerTab.ATTACHMENTS && hasAttachmentsTab && canCreateDocument) {
        Box {
            FloatingActionButton(onClick = { onDocumentsMenuExpandedChange(true) }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
            DropdownMenu(
                expanded = documentsMenuExpanded,
                onDismissRequest = { onDocumentsMenuExpandedChange(false) },
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                DocumentCreationType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(documentCreationMenuLabel(type)) },
                        onClick = {
                            onDocumentsMenuExpandedChange(false)
                            onOpenDocumentCreation(type)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun documentCreationMenuLabel(type: DocumentCreationType): String =
    when (type) {
        DocumentCreationType.NOTE -> stringResource(R.string.attachment_type_notes)
        DocumentCreationType.MUSIC_NOTE -> stringResource(R.string.attachment_type_music_notes)
        DocumentCreationType.CHECKLIST -> stringResource(R.string.attachment_type_checklist)
        DocumentCreationType.WEB_LINK -> stringResource(R.string.attachment_type_web_link)
        DocumentCreationType.OBSIDIAN -> stringResource(R.string.attachment_type_obsidian)
    }

private fun Modifier.pickerDragGesture(
    hasBothTabs: Boolean,
    selectedTab: LinkPickerTab,
    onTabSelected: (LinkPickerTab) -> Unit,
): Modifier =
    pointerInput(hasBothTabs, selectedTab) {
        if (!hasBothTabs) return@pointerInput
        var horizontalDragAccum = 0f
        detectHorizontalDragGestures(
            onDragStart = { horizontalDragAccum = 0f },
            onHorizontalDrag = { _, dragAmount ->
                horizontalDragAccum += dragAmount
            },
            onDragEnd = {
                if (abs(horizontalDragAccum) >= 60f) {
                    onTabSelected(
                        if (horizontalDragAccum < 0f) {
                            LinkPickerTab.ATTACHMENTS
                        } else {
                            LinkPickerTab.CONTEXTS
                        },
                    )
                }
                horizontalDragAccum = 0f
            },
            onDragCancel = { horizontalDragAccum = 0f },
        )
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
