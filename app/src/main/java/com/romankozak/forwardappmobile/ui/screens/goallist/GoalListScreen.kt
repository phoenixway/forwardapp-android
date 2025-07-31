package com.romankozak.forwardappmobile.ui.screens.goallist

import android.content.pm.PackageInfo
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.components.GoalListRow
import com.romankozak.forwardappmobile.ui.dialogs.*
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: GoalListViewModel = hiltViewModel()
) {
    val hierarchy by viewModel.listHierarchy.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    // Єдиний стан, що нам потрібен для drag-and-drop
    val dragAndDropState = rememberDragAndDropState<GoalList>()

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is GoalListUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navController.navigate("sync_screen")
                }
                is GoalListUiEvent.NavigateToDetails -> navController.navigate("goal_detail_screen/${event.listId}")
                is GoalListUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is GoalListUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backlogs") },
                actions = {
                    IconButton(onClick = { viewModel.onAddNewListRequest() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add new list")
                    }
                    IconButton(onClick = { viewModel.onShowSearchDialog() }) {
                        Icon(Icons.Default.Search, contentDescription = "Global search")
                    }
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Run Wi-Fi Server") }, onClick = {
                            viewModel.onShowWifiServerDialog()
                            menuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("Import from Wi-Fi") }, onClick = {
                            viewModel.onShowWifiImportDialog()
                            menuExpanded = false
                        })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Settings") }, onClick = {
                            viewModel.onShowSettingsDialog()
                            menuExpanded = false
                        })
                        DropdownMenuItem(text = { Text("About") }, onClick = {
                            viewModel.onShowAboutDialog()
                            menuExpanded = false
                        })
                    }
                }
            )
        }
    ) { paddingValues ->
        if (hierarchy.allLists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Create your first list")
            }
        } else {
            DragAndDropContainer(state = dragAndDropState) {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    lateinit var renderList: LazyListScope.(GoalList, Int) -> Unit
                    renderList = { list, level ->
                        item(key = list.id) {
                            DraggableItem(
                                state = dragAndDropState,
                                key = list.id,
                                data = list,
                                // ✅ ГОЛОВНА ЗМІНА: Використовуємо вбудований параметр
                                dragAfterLongPress = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .dropTarget(
                                        state = dragAndDropState,
                                        key = list.id,
                                        onDrop = { draggedItemState ->
                                            val draggedList = draggedItemState.data
                                            val targetListId = list.id
                                            viewModel.onListMoved(draggedList.id, targetListId)
                                        }
                                    )
                            ) {
                                val isCurrentlyDragging = this.isDragging
                                val allListsFlat = hierarchy.allLists
                                val draggedItemIndex = allListsFlat.indexOfFirst { it.id == dragAndDropState.draggedItem?.data?.id }
                                val currentItemIndex = allListsFlat.indexOfFirst { it.id == list.id }
                                val isHovered = dragAndDropState.hoveredDropTargetKey == list.id
                                val isDraggingDown = draggedItemIndex != -1 && draggedItemIndex < currentItemIndex

                                GoalListRow(
                                    list = list,
                                    level = level,
                                    hasChildren = hierarchy.childMap.containsKey(list.id),
                                    onListClick = { viewModel.onListClicked(it) },
                                    onToggleExpanded = { viewModel.onToggleExpanded(it) },
                                    onMenuRequested = { viewModel.onMenuRequested(it) },
                                    isCurrentlyDragging = isCurrentlyDragging,
                                    isHovered = isHovered,
                                    isDraggingDown = isDraggingDown,
                                )
                            }
                        }

                        if (list.isExpanded) {
                            val children = hierarchy.childMap[list.id]?.sortedBy { it.order } ?: emptyList()
                            children.forEach { child ->
                                renderList(child, level + 1)
                            }
                        }
                    }

                    hierarchy.topLevelLists.forEach { list ->
                        renderList(list, 0)
                    }
                }
            }
        }
    }

    HandleDialogs(
        dialogState = dialogState,
        hierarchy = hierarchy,
        viewModel = viewModel,
        showWifiServerDialog = showWifiServerDialog,
        wifiServerAddress = wifiServerAddress,
        showWifiImportDialog = showWifiImportDialog,
        showSearchDialog = showSearchDialog
    )
}

// Кастомний детектор більше не потрібен
// private fun Modifier.longPressForDragDetector(...) { ... }


@Composable
private fun HandleDialogs(
    dialogState: DialogState,
    hierarchy: ListHierarchyData,
    viewModel: GoalListViewModel,
    showWifiServerDialog: Boolean,
    wifiServerAddress: String?,
    showWifiImportDialog: Boolean,
    showSearchDialog: Boolean
) {
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()

    when (val state = dialogState) {
        is DialogState.Hidden -> {}
        is DialogState.AddList -> {
            val title = if (state.parentId == null) "Create new list" else "Create sublist"
            AddListDialog(
                title = title,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name -> viewModel.onAddList(name, state.parentId) }
            )
        }
        is DialogState.ContextMenu -> {
            ContextMenuDialog(
                list = state.list,
                onDismissRequest = { viewModel.dismissDialog() },
                onMoveRequest = { viewModel.onMoveListRequest(it) },
                onAddSublistRequest = { viewModel.onAddSublistRequest(it) },
                onDeleteRequest = { viewModel.onDeleteRequest(it) },
                onRenameRequest = { viewModel.onRenameRequest(it) }
            )
        }
        is DialogState.MoveList -> {
            MoveListDialog(
                listToMove = state.list,
                allListsFlat = hierarchy.allLists,
                topLevelLists = hierarchy.topLevelLists,
                childMap = hierarchy.childMap,
                onDismiss = { viewModel.dismissDialog() },
                onConfirmMove = { newParentId ->
                    viewModel.onMoveListConfirmed(
                        state.list,
                        newParentId
                    )
                }
            )
        }
        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete list?") },
                text = { Text("Are you sure you want to delete '${state.list.name}' and all its sublists and goals? This action cannot be undone.") },
                confirmButton = { Button(onClick = { viewModel.onDeleteListConfirmed(state.list) }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } }
            )
        }
        is DialogState.RenameList -> {
            RenameListDialog(
                currentName = state.list.name,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName -> viewModel.onRenameListConfirmed(state.list, newName) }
            )
        }
        is DialogState.AppSettings -> {
            SettingsDialog(
                initialVaultName = obsidianVaultName,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { newName -> viewModel.onSaveSettings(newName) }
            )
        }
        is DialogState.AboutApp -> {
            AboutAppDialog(onDismiss = { viewModel.dismissDialog() })
        }
    }

    if (showWifiServerDialog) {
        WifiServerDialog(
            address = wifiServerAddress,
            onDismiss = { viewModel.onDismissWifiServerDialog() })
    }

    if (showWifiImportDialog) {
        val desktopAddress by viewModel.desktopAddress.collectAsState()
        WifiImportDialog(
            desktopAddress = desktopAddress,
            onAddressChange = { viewModel.onDesktopAddressChange(it) },
            onDismiss = { viewModel.onDismissWifiImportDialog() },
            onConfirm = { address -> viewModel.performWifiImport(address) }
        )
    }

    if (showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { viewModel.onDismissSearchDialog() },
            onConfirm = { query -> viewModel.onPerformGlobalSearch(query) }
        )
    }
}

@Composable
private fun AboutAppDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val packageInfo: PackageInfo? = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About Forward App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Version: ${packageInfo?.versionName ?: "N/A"}")
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode ?: -1
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toLong() ?: -1
                }
                if (versionCode != -1L) {
                    Text("Build: $versionCode")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}