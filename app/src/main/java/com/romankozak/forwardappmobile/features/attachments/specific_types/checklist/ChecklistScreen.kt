package com.romankozak.forwardappmobile.features.attachments.specific_types.checklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.awaitFrame
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.net.URLEncoder

private data class ChecklistTypedWikiLink(
    val type: String,
    val targetId: String,
)

private fun parseChecklistTypedWikiLink(raw: String): ChecklistTypedWikiLink? {
    val match = Regex("""^(doc|ctx|music|checklist):([^|]+)(?:\|(.+))?$""", RegexOption.IGNORE_CASE).matchEntire(raw.trim()) ?: return null
    return ChecklistTypedWikiLink(
        type = match.groupValues[1].lowercase(),
        targetId = match.groupValues[2].trim(),
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    navController: NavController,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            viewModel.onMoveItem(from.index, to.index)
        }
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val linkSuggestions by viewModel.linkSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.showUndoSnackbar) {
        if (uiState.showUndoSnackbar) {
            val result =
                snackbarHostState.showSnackbar(
                    message = "Item deleted",
                    actionLabel = "Undo",
                )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onUndoDelete()
            } else {
                viewModel.onConfirmDelete()
            }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<ChecklistItemUiModel?>(null) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showContextPicker by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.pendingFocusItemId, uiState.items) {
        val targetId = uiState.pendingFocusItemId ?: return@LaunchedEffect
        val targetIndex = uiState.items.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Delete") },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier =
                        Modifier.clickable {
                            showBottomSheet = false
                            selectedItem?.let { viewModel.onDeleteItem(it.id) }
                        },
                )
                ListItem(
                    headlineContent = { Text("Додати посилання на вкладення") },
                    leadingContent = {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Вкладення",
                        )
                    },
                    modifier =
                        Modifier.clickable {
                            showBottomSheet = false
                            showAttachmentPicker = true
                        },
                )
                ListItem(
                    headlineContent = { Text("Додати посилання на контекст") },
                    leadingContent = {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = "Контекст",
                        )
                    },
                    modifier =
                        Modifier.clickable {
                            showBottomSheet = false
                            showContextPicker = true
                        },
                )
            }
        }
    }

    if (showAttachmentPicker) {
        ChecklistLinkPickerDialog(
            title = "Виберіть вкладення",
            suggestions = linkSuggestions.filterNot { it.startsWith("ctx:", ignoreCase = true) },
            onDismiss = { showAttachmentPicker = false },
            onSelect = { token ->
                selectedItem?.let { item -> viewModel.insertLinkIntoItem(item.id, token) }
                showAttachmentPicker = false
            },
        )
    }

    if (showContextPicker) {
        ChecklistLinkPickerDialog(
            title = "Виберіть контекст",
            suggestions = linkSuggestions.filter { it.startsWith("ctx:", ignoreCase = true) },
            onDismiss = { showContextPicker = false },
            onSelect = { token ->
                selectedItem?.let { item -> viewModel.insertLinkIntoItem(item.id, token) }
                showContextPicker = false
            },
        )
    }

    Scaffold(
        topBar = {
            ChecklistTopBar(
                title = uiState.title,
                onTitleChange = viewModel::onTitleChange,
                onBack = { navController.popBackStack() },
                showCheckboxes = uiState.showCheckboxes,
                onToggleCheckboxes = viewModel::onToggleCheckboxVisibility,
                isEditing = uiState.isEditing,
                onToggleEditing = viewModel::onToggleEditingMode,
                onClearCompleted = viewModel::onClearCompleted,
                onExportMarkdown = {
                    val markdown = viewModel.buildMarkdownExport()
                    clipboardManager.setText(AnnotatedString(markdown))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Checklist copied to clipboard")
                    }
                },
                onImportFromClipboard = {
                    val clipboardText = clipboardManager.getText()?.text?.toString().orEmpty()
                    if (clipboardText.isBlank()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Clipboard is empty")
                        }
                    } else {
                        viewModel.importMarkdown(clipboardText) { success ->
                            coroutineScope.launch {
                                val message =
                                    if (success) {
                                        "Checklist imported from clipboard"
                                    } else {
                                        "No checklist items detected in clipboard"
                                    }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                },
                onSelectAll = viewModel::onSelectAllItems,
                onMarkAllCompleted = viewModel::onMarkAllCompleted,
                onMarkAllIncomplete = viewModel::onMarkAllIncomplete,
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.errorMessage == null && uiState.isEditing) {
                FloatingActionButton(
                    onClick = { viewModel.onAddItem(uiState.items.lastOrNull()?.id) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.checklist_add_item),
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                }
            }
            else -> {
                ChecklistContent(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    uiState = uiState,
                    reorderState = reorderState,
                    listState = listState,
                    onContentChange = viewModel::onItemContentChange,
                    onCheckedChange = viewModel::onToggleItemChecked,
                    onAddBelow = viewModel::onAddItem,
                    onDelete = viewModel::onDeleteItem,
                    onFocusConsumed = viewModel::onPendingFocusConsumed,
                    isEditing = uiState.isEditing,
                    onWikiLinkClick = { link ->
                        coroutineScope.launch {
                            when {
                                link.startsWith("#") || link.startsWith("@") -> {
                                    val encoded = URLEncoder.encode(link, "UTF-8")
                                    val target = NavTarget.GlobalSearch(query = encoded)
                                    navController.navigate(NavTargetRouter.routeOf(target))
                                }
                                else -> {
                                    val typed = parseChecklistTypedWikiLink(link)
                                    if (typed != null) {
                                        when (typed.type) {
                                            "doc" -> navController.navigate(NavTargetRouter.routeOf(NavTarget.NoteDocument(id = typed.targetId, startEdit = false)))
                                            "ctx" -> navController.navigate(NavTargetRouter.routeOf(NavTarget.ContextDetail(contextId = typed.targetId)))
                                            "music" -> navController.navigate(NavTargetRouter.routeOf(NavTarget.MusicNote(id = typed.targetId, startEdit = false)))
                                            "checklist" -> navController.navigate(NavTargetRouter.routeOf(NavTarget.Checklist(id = typed.targetId, contextId = null)))
                                        }
                                        return@launch
                                    }
                                    val targetId = viewModel.findDocumentIdByName(link)
                                    if (targetId != null) {
                                        val target = NavTarget.NoteDocument(id = targetId, startEdit = false)
                                        navController.navigate(NavTargetRouter.routeOf(target))
                                    } else {
                                        snackbarHostState.showSnackbar("Не зміг відкрити вкладення \"$link\"")
                                    }
                                }
                            }
                        }
                    },
                    onShowItemActions = { item ->
                        selectedItem = item
                        showBottomSheet = true
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistContent(
    modifier: Modifier,
    uiState: ChecklistUiState,
    reorderState: ReorderableLazyListState,
    listState: LazyListState,
    onContentChange: (String, String) -> Unit,
    onCheckedChange: (String, Boolean) -> Unit,
    onAddBelow: (String?) -> Unit,
    onDelete: (String) -> Unit,
    isEditing: Boolean,
    onWikiLinkClick: (String) -> Unit,
    onFocusConsumed: () -> Unit,
    onShowItemActions: (ChecklistItemUiModel) -> Unit,
) {
    AnimatedVisibility(
        visible = uiState.items.isEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Column(
            modifier = modifier.padding(horizontal = 24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.checklist_add_item),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.checklist_item_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }

    val focusManager = LocalFocusManager.current
    AnimatedVisibility(
        visible = uiState.items.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LazyColumn(
            state = listState,
            modifier = modifier.imePadding(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.items, key = { it.id }) { item ->
                ReorderableItem(reorderState, key = item.id) { isDragging ->
                    val reorderableScope = this
                    val clipboardManager = LocalClipboardManager.current
                    val dismissState =
                        rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                when (it) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        onDelete(item.id)
                                        true
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        clipboardManager.setText(AnnotatedString(item.content))
                                        false
                                    }
                                    else -> false
                                }
                            },
                        )

                    val content: @Composable () -> Unit = {
                        ChecklistItemRow(
                            item = item,
                            reorderableScope = reorderableScope,
                            showCheckbox = uiState.showCheckboxes,
                            isEditing = isEditing,
                            isDragging = isDragging,
                            shouldRequestFocus = item.id == uiState.pendingFocusItemId,
                            onFocusConsumed = onFocusConsumed,
                            onContentChange = { onContentChange(item.id, it) },
                            onCheckedChange = { onCheckedChange(item.id, it) },
                            onAddBelow = { onAddBelow(item.id) },
                            onWikiLinkClick = onWikiLinkClick,
                            onShowItemActions = { onShowItemActions(item) },
                        )
                    }

                    if (isEditing) {
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color =
                                    when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                        else -> Color.Transparent
                                    }
                                val icon =
                                    when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete
                                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.ContentCopy
                                        else -> null
                                    }
                                val alignment =
                                    when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        else -> Alignment.Center
                                    }
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(color)
                                            .padding(horizontal = 16.dp),
                                    contentAlignment = alignment,
                                ) {
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            },
                        ) {
                            content()
                        }
                    } else {
                        content()
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistTopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onBack: () -> Unit,
    showCheckboxes: Boolean,
    onToggleCheckboxes: () -> Unit,
    isEditing: Boolean,
    onToggleEditing: () -> Unit,
    onClearCompleted: () -> Unit,
    onExportMarkdown: () -> Unit,
    onImportFromClipboard: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkAllCompleted: () -> Unit,
    onMarkAllIncomplete: () -> Unit,
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
            IconButton(onClick = onToggleEditing) {
                Icon(
                    imageVector = if (isEditing) Icons.Filled.Visibility else Icons.Filled.Edit,
                    contentDescription = if (isEditing) "Switch to read mode" else "Switch to edit mode",
                )
            }
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
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChecklistItemRow(
    item: ChecklistItemUiModel,
    reorderableScope: ReorderableCollectionItemScope,
    showCheckbox: Boolean,
    isEditing: Boolean,
    isDragging: Boolean,
    shouldRequestFocus: Boolean,
    onFocusConsumed: () -> Unit,
    onContentChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onAddBelow: () -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onShowItemActions: (ChecklistItemUiModel) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var hasInputFocus by remember(item.id) { mutableStateOf(false) }
    var wasFocusedAtLeastOnce by remember(item.id) { mutableStateOf(false) }

    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            hasInputFocus = true
            wasFocusedAtLeastOnce = false
            onFocusConsumed()
        }
    }

    LaunchedEffect(hasInputFocus, isEditing) {
        if (hasInputFocus && isEditing) {
            awaitFrame()
            runCatching { focusRequester.requestFocus() }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (isDragging) 4.dp else 1.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (showCheckbox) {
                IconToggleButton(
                    checked = item.isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.padding(top = 16.dp).size(32.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color =
                            if (item.isChecked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                        border =
                            if (!item.isChecked) {
                                BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            } else {
                                null
                            },
                        modifier = Modifier.size(18.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (item.isChecked) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Checkbox",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(1.dp))
            }

            if (isEditing) {
                Column(modifier = Modifier.weight(1f)) {
                    if (hasInputFocus) {
                        OutlinedTextField(
                            value = item.content,
                            onValueChange = onContentChange,
                            modifier =

                                Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { state ->
                                        if (state.isFocused) {
                                            wasFocusedAtLeastOnce = true
                                            hasInputFocus = true
                                        } else if (wasFocusedAtLeastOnce) {
                                            hasInputFocus = false
                                        }
                                    }
                                    .onKeyEvent { event ->

                                        if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                            onAddBelow()

                                            true
                                        } else if (event.type == KeyEventType.KeyUp && event.key == Key.Tab) {
                                            focusManager.clearFocus()

                                            false
                                        } else {
                                            false
                                        }
                                    },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.checklist_item_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            minLines = 1,
                            maxLines = 10,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { onAddBelow() }),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    disabledBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                        )
                    } else if (item.content.isBlank()) {
                        Text(
                            text = stringResource(R.string.checklist_item_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hasInputFocus = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    } else {
                        ChecklistReadOnlyText(
                            text = item.content,
                            onWikiLinkClick = onWikiLinkClick,
                            onPlainTextClick = {
                                hasInputFocus = true
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
            } else {
                ChecklistReadOnlyText(
                    text = item.content,
                    onWikiLinkClick = onWikiLinkClick,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            if (isEditing) {
                IconButton(
                    onClick = { onShowItemActions(item) },
                    modifier =
                        with(reorderableScope) {
                            Modifier
                                .draggableHandle()
                                .padding(top = 16.dp)
                                .size(40.dp)
                        },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistReadOnlyText(
    text: String,
    onWikiLinkClick: (String) -> Unit,
    onPlainTextClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val wikiLinkRegex = Regex("\\[\\[([^\\[\\]]+)\\]\\]")

    val annotatedText =
        remember(text, accentColor, textColor) {
            buildAnnotatedString {
                var cursor = 0
                for (match in wikiLinkRegex.findAll(text)) {
                    if (match.range.first > cursor) {
                        withStyle(SpanStyle(color = textColor)) {
                            append(text.substring(cursor, match.range.first))
                        }
                    }
                    val linkValue = match.groupValues[1]
                    val start = length
                    append(extractChecklistWikiDisplay(linkValue))
                    val end = length
                    addStyle(
                        style = SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline),
                        start = start,
                        end = end,
                    )
                    addStringAnnotation(
                        tag = "wikilink",
                        annotation = linkValue,
                        start = start,
                        end = end,
                    )
                    cursor = match.range.last + 1
                }
                if (cursor < text.length) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(text.substring(cursor))
                    }
                }
            }
        }

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        onClick = { offset ->
            val link = annotatedText.getStringAnnotations("wikilink", offset, offset).firstOrNull()
            if (link != null) onWikiLinkClick(link.item) else onPlainTextClick()
        },
    )
}

@Composable
private fun ChecklistLinkPickerDialog(
    title: String,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered =
        remember(query, suggestions) {
            if (query.isBlank()) {
                suggestions.take(24)
            } else {
                suggestions.filter {
                    extractChecklistWikiDisplay(it).contains(query, ignoreCase = true) || it.contains(query, ignoreCase = true)
                }.take(24)
            }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Пошук...") },
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    filtered.forEach { token ->
                        Text(
                            text = extractChecklistWikiDisplay(token),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(token) }
                                    .padding(horizontal = 6.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрити") } },
    )
}

private fun extractChecklistWikiDisplay(raw: String): String {
    val trimmed = raw.trim()
    val parts = trimmed.split("|", limit = 2)
    if (parts.size == 2 && parts[1].isNotBlank()) return parts[1]
    val typed = Regex("""^(doc|ctx|music|checklist):(.+)$""", RegexOption.IGNORE_CASE).matchEntire(trimmed)
    return typed?.groupValues?.get(2)?.takeIf { it.isNotBlank() } ?: trimmed
}
