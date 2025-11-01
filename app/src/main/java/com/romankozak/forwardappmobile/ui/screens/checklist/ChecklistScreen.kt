package com.romankozak.forwardappmobile.ui.screens.checklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.romankozak.forwardappmobile.R

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.ReorderableCollectionItemScope


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    navController: NavController,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.onMoveItem(from.index, to.index)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.pendingFocusItemId, uiState.items) {
        val targetId = uiState.pendingFocusItemId ?: return@LaunchedEffect
        val targetIndex = uiState.items.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Scaffold(
        topBar = {
            ChecklistTopBar(
                title = uiState.title,
                onTitleChange = viewModel::onTitleChange,
                onBack = { navController.popBackStack() },
                showCheckboxes = uiState.showCheckboxes,
                onToggleCheckboxes = viewModel::onToggleCheckboxVisibility,
                onClearCompleted = viewModel::onClearCompleted,
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.errorMessage == null) {
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
    reorderState: sh.calvin.reorderable.ReorderableLazyListState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onContentChange: (String, String) -> Unit,
    onCheckedChange: (String, Boolean) -> Unit,
    onAddBelow: (String?) -> Unit,
    onDelete: (String) -> Unit,
    onFocusConsumed: () -> Unit,
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
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.items, key = { it.id }) { item ->
                ReorderableItem(reorderState, key = item.id) { isDragging ->
                    ChecklistItemRow(
                        item = item,
                        reorderableScope = this,

                        showCheckbox = uiState.showCheckboxes,
                        isDragging = isDragging,
                        shouldRequestFocus = item.id == uiState.pendingFocusItemId,
                        onFocusConsumed = onFocusConsumed,
                        onContentChange = { onContentChange(item.id, it) },
                        onCheckedChange = { onCheckedChange(item.id, it) },
                        onAddBelow = { onAddBelow(item.id) },
                        onDelete = { onDelete(item.id) },
                    )
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
    onClearCompleted: () -> Unit,
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.checklist_title_placeholder)) },
                singleLine = true,
colors = OutlinedTextFieldDefaults.colors(
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
            IconButton(onClick = onToggleCheckboxes) {
                val icon =
                    if (showCheckboxes) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.checklist_toggle_checkboxes),
                )
            }
            IconButton(onClick = onClearCompleted) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = stringResource(R.string.checklist_clear_completed),
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
    isDragging: Boolean,
    shouldRequestFocus: Boolean,
    onFocusConsumed: () -> Unit,
    onContentChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onAddBelow: () -> Unit,
    onDelete: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val hapticFeedback = LocalHapticFeedback.current


    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            focusRequester.requestFocus()
            onFocusConsumed()
        }
    }

        Surface(

            modifier = Modifier.fillMaxWidth(),

            shape = MaterialTheme.shapes.large,

            tonalElevation = if (isDragging) 4.dp else 1.dp,

            color = MaterialTheme.colorScheme.surface,

        ) {

            Row(

                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),

                verticalAlignment = Alignment.CenterVertically,

            ) {

                if (showCheckbox) {

                    IconToggleButton(

                        checked = item.isChecked,

                        onCheckedChange = onCheckedChange,

                        modifier = Modifier.size(40.dp)

                    ) {

                        Surface(

                            shape = androidx.compose.foundation.shape.CircleShape,

                            color = if (item.isChecked) 

                                MaterialTheme.colorScheme.primary 

                            else 

                                Color.Transparent,

                            border = if (!item.isChecked) 

                                androidx.compose.foundation.BorderStroke(

                                    2.dp, 

                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

                                ) 

                            else null,

                            modifier = Modifier.size(20.dp)

                        ) {

                            Box(

                                contentAlignment = Alignment.Center,

                                modifier = Modifier.fillMaxSize()

                            ) {

                                if (item.isChecked) {

                                    Icon(

                                        imageVector = Icons.Filled.Check,

                                        contentDescription = "Checkbox",

                                        tint = MaterialTheme.colorScheme.onPrimary,

                                        modifier = Modifier.size(14.dp)

                                    )

                                }

                            }

                        }

                    }

                    Spacer(modifier = Modifier.width(4.dp))

                }

                OutlinedTextField(

                    value = item.content,

                    onValueChange = onContentChange,

                    modifier =

                        Modifier

                            .weight(1f)

                            .focusRequester(focusRequester)

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

                            style = MaterialTheme.typography.bodyLarge

                        ) 

                    },

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),

                    keyboardActions = KeyboardActions(onNext = { onAddBelow() }),

                    colors = OutlinedTextFieldDefaults.colors(

                        focusedBorderColor = Color.Transparent,

                        unfocusedBorderColor = Color.Transparent,

                        disabledBorderColor = Color.Transparent,

                        focusedContainerColor = Color.Transparent,

                        unfocusedContainerColor = Color.Transparent,

                    ),

                    textStyle = MaterialTheme.typography.bodyLarge,

                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(

                    onClick = onDelete,

                    modifier = Modifier.size(40.dp)

                ) {

                    Icon(

                        imageVector = Icons.Outlined.Delete,

                        contentDescription = stringResource(R.string.checklist_delete_item),

                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),

                    )

                }

                IconButton(

                    onClick = { /* No action on click for drag handle */ },

                    modifier = with(reorderableScope) {

                        Modifier.draggableHandle()

                            .size(40.dp)

                    }

                ) {

                    Icon(

                        imageVector = Icons.Rounded.DragHandle,

                        contentDescription = null,

                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),

                        modifier = Modifier.size(24.dp),

                    )

                }

            }

        }
}
