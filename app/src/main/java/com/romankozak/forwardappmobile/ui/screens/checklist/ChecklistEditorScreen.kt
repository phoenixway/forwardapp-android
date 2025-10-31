package com.romankozak.forwardappmobile.ui.screens.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.detectReorderAfterLongPress
import sh.calvin.reorderable.draggableHandle
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.reorderable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistEditorScreen(
    navController: NavController,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChecklistEvent.NavigateBack -> {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_needed", true)
                    navController.popBackStack()
                }
                is ChecklistEvent.ShowError -> {
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
            }
        }
    }

    val topBarScrollState = rememberTopAppBarState()
    val listState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.onMoveItem(from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        placeholder = { Text(stringResource(R.string.checklist_placeholder_name)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onSave,
                        enabled = !uiState.isSaving,
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarScrollState),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddItem(uiState.items.lastIndex) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.checklist_add_item))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .reorderable(reorderableState)
                        .detectReorderAfterLongPress(reorderableState),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(uiState.items, key = { _, item -> item.localId }) { index, item ->
                    ReorderableItem(reorderableState, key = item.localId) { isDragging ->
                        ChecklistRow(
                            index = index,
                            item = item,
                            isDragging = isDragging,
                            onContentChange = { viewModel.onItemContentChange(index, it) },
                            onToggle = { viewModel.onItemToggle(index, it) },
                            onRemove = { viewModel.onRemoveItem(index) },
                            reorderableScope = this,
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    index: Int,
    item: ChecklistItemUi,
    isDragging: Boolean,
    onContentChange: (String) -> Unit,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    reorderableScope: ReorderableCollectionItemScope? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = "Перетягнути",
            tint = MaterialTheme.colorScheme.outline,
            modifier =
                Modifier
                    .size(24.dp)
                    .let { base ->
                        if (reorderableScope != null) {
                            with(reorderableScope) { base.draggableHandle() }
                        } else {
                            base
                        }
                    },
        )
        Spacer(modifier = Modifier.size(8.dp))
        androidx.compose.material3.Checkbox(
            checked = item.isChecked,
            onCheckedChange = onToggle,
        )
        Spacer(modifier = Modifier.size(12.dp))
            TextField(
                value = item.content,
                onValueChange = onContentChange,
                placeholder = { Text(stringResource(R.string.checklist_item_placeholder)) },
            modifier = Modifier.weight(1f),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            singleLine = false,
        )
        Spacer(modifier = Modifier.size(8.dp))
        IconButton(
            onClick = onRemove,
            enabled = true,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Видалити пункт",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
