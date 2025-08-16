// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goallist/GoalListScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goallist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.components.FilterableListChooser
import com.romankozak.forwardappmobile.ui.components.GoalListRow
import com.romankozak.forwardappmobile.ui.dialogs.*
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel

@Composable
fun GoalListScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: GoalListViewModel = hiltViewModel(),
) {
    val hierarchy by viewModel.listHierarchy.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val planningMode by viewModel.planningMode.collectAsState()
    val planningSettings by viewModel.planningSettingsState.collectAsState()
    val dragAndDropState = rememberDragAndDropState<GoalList>()

    // ✨ КРОК 1: Використовуємо правильну змінну зі стану ViewModel
    val listChooserFinalExpandedIds by viewModel.listChooserFinalExpandedIds.collectAsState()
    val listChooserFilterText by viewModel.listChooserFilterText.collectAsState()
    val filteredListHierarchyForDialog by viewModel.filteredListHierarchyForDialog.collectAsState()

    BackHandler(enabled = isSearchActive) {
        viewModel.onToggleSearch(false)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> if (uri != null) viewModel.importFromFile(uri) }
    )

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
            GoalListTopAppBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onToggleSearch = { viewModel.onToggleSearch(!isSearchActive) },
                onAddNewList = { viewModel.onAddNewListRequest() },
                onShowGlobalSearch = { viewModel.onShowSearchDialog() },
                viewModel = viewModel,
                onImportFromFile = { importLauncher.launch("application/json") }
            )
        },
        bottomBar = {
            GoalListBottomNav(
                navController = navController,
                isSearchActive = isSearchActive,
                onToggleSearch = viewModel::onToggleSearch,
                showPlanningModes = planningSettings.showModes,
                currentMode = planningMode,
                onModeChange = viewModel::onPlanningModeChange
            )
        }
    ) { paddingValues ->
        val isListEmpty = hierarchy.topLevelLists.isEmpty() && hierarchy.childMap.isEmpty()

        if (isListEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val emptyText = when {
                    isSearchActive -> "No lists found for your query."
                    planningMode is PlanningMode.Daily -> "No lists with tag '#${planningSettings.dailyTag}'"
                    planningMode is PlanningMode.Medium -> "No lists with tag '#${planningSettings.mediumTag}'"
                    planningMode is PlanningMode.Long -> "No lists with tag '#${planningSettings.longTag}'"
                    else -> "Create your first list"
                }
                Text(emptyText, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            DragAndDropContainer(
                state = dragAndDropState,
                enabled = !isSearchActive
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                ) {
                    renderGoalList(
                        lists = hierarchy.topLevelLists,
                        childMap = hierarchy.childMap,
                        level = 0,
                        dragAndDropState = dragAndDropState,
                        viewModel = viewModel,
                        allListsFlat = hierarchy.allLists,
                        isSearchActive = isSearchActive,
                        planningMode = planningMode
                    )
                }
            }
        }
    }

    HandleDialogs(
        dialogState = dialogState,
        viewModel = viewModel,
        listChooserFilterText = listChooserFilterText,
        // ✨ КРОК 2: Передаємо правильну змінну в `HandleDialogs`
        listChooserExpandedIds = listChooserFinalExpandedIds,
        filteredListHierarchyForDialog = filteredListHierarchyForDialog
    )
}

@Composable
private fun GoalListTopAppBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddNewList: () -> Unit,
    onShowGlobalSearch: () -> Unit,
    viewModel: GoalListViewModel,
    onImportFromFile: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    TopAppBar(
        title = { if (!isSearchActive) Text("Backlogs") },
        actions = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Filter lists...") },
                    shape = CircleShape,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        IconButton(onClick = onToggleSearch) {
                            Icon(Icons.Default.Close, contentDescription = "Close filter")
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                IconButton(onClick = onAddNewList) { Icon(Icons.Default.Add, "Add new list") }
                IconButton(onClick = onShowGlobalSearch) { Icon(Icons.Default.Search, "Global search") }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menu") }
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
                    DropdownMenuItem(text = { Text("Export to file") }, onClick = {
                        viewModel.exportToFile()
                        menuExpanded = false
                    })
                    DropdownMenuItem(text = { Text("Import from file") }, onClick = {
                        onImportFromFile()
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
        }
    )
}


@Composable
private fun GoalListBottomNav(
    navController: NavController,
    isSearchActive: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    showPlanningModes: Boolean,
    currentMode: PlanningMode,
    onModeChange: (PlanningMode) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("activity_tracker_screen") },
            icon = { Icon(Icons.Outlined.Timeline, "Activity Tracker") },
            label = { Text("Track") }
        )
        NavigationBarItem(
            selected = isSearchActive,
            onClick = { onToggleSearch(true) },
            icon = { Icon(Icons.Outlined.FilterList, "Filter lists") },
            label = { Text("Filter") }
        )
        NavigationBarItem(
            selected = !isSearchActive && currentMode is PlanningMode.All,
            onClick = { onModeChange(PlanningMode.All) },
            icon = { Icon(Icons.AutoMirrored.Outlined.List, "All lists") },
            label = { Text("All") }
        )

        if (showPlanningModes) {
            NavigationBarItem(
                selected = !isSearchActive && currentMode is PlanningMode.Daily,
                onClick = { onModeChange(PlanningMode.Daily) },
                icon = { Icon(Icons.Outlined.Today, "Daily") },
                label = { Text("Daily") }
            )
            NavigationBarItem(
                selected = !isSearchActive && currentMode is PlanningMode.Medium,
                onClick = { onModeChange(PlanningMode.Medium) },
                icon = { Icon(Icons.Outlined.QueryStats, "Medium-term") },
                label = { Text("Medium") }
            )
            NavigationBarItem(
                selected = !isSearchActive && currentMode is PlanningMode.Long,
                onClick = { onModeChange(PlanningMode.Long) },
                icon = { Icon(Icons.Outlined.TrackChanges, "Long-term") },
                label = { Text("Long") }
            )
        }
    }
}

private fun LazyListScope.renderGoalList(
    lists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    dragAndDropState: DragAndDropState<GoalList>,
    viewModel: GoalListViewModel,
    allListsFlat: List<GoalList>,
    isSearchActive: Boolean,
    planningMode: PlanningMode
) {
    lists.forEach { list ->
        item(key = list.id) {
            DraggableItem(
                state = dragAndDropState,
                key = list.id,
                data = list,
                dragAfterLongPress = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .dropTarget(
                        state = dragAndDropState,
                        key = list.id,
                        onDrop = { draggedItemState ->
                            val draggedList = draggedItemState.data
                            viewModel.onListMoved(draggedList.id, list.id)
                        }
                    )
            ) {
                val isDragging = this.isDragging
                val draggedItemIndex = allListsFlat.indexOfFirst { it.id == dragAndDropState.draggedItem?.data?.id }
                val currentItemIndex = allListsFlat.indexOfFirst { it.id == list.id }
                val isHovered = dragAndDropState.hoveredDropTargetKey == list.id
                val isDraggingDown = draggedItemIndex != -1 && draggedItemIndex < currentItemIndex

                GoalListRow(
                    list = list,
                    level = level,
                    hasChildren = childMap.containsKey(list.id),
                    onListClick = { viewModel.onListClicked(it) },
                    onToggleExpanded = { viewModel.onToggleExpanded(it) },
                    onMenuRequested = { viewModel.onMenuRequested(it) },
                    isCurrentlyDragging = isDragging,
                    isHovered = isHovered,
                    isDraggingDown = isDraggingDown,
                )
            }
        }

        if (list.isExpanded) {
            val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
            if (children.isNotEmpty()) {
                renderGoalList(
                    lists = children,
                    childMap = childMap,
                    level = level + 1,
                    dragAndDropState = dragAndDropState,
                    viewModel = viewModel,
                    allListsFlat = allListsFlat,
                    isSearchActive = isSearchActive,
                    planningMode = planningMode
                )
            }
        }
    }
}

private fun getDescendantIds(listId: String, childMap: Map<String, List<GoalList>>): Set<String> {
    val descendants = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(listId)
    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        childMap[currentId]?.forEach { child ->
            descendants.add(child.id)
            queue.add(child.id)
        }
    }
    return descendants
}


@Composable
private fun HandleDialogs(
    dialogState: DialogState,
    viewModel: GoalListViewModel,
    listChooserFilterText: String,
    listChooserExpandedIds: Set<String>,
    filteredListHierarchyForDialog: ListHierarchyData
) {
    val stats by viewModel.appStatistics.collectAsState()
    val planningSettings by viewModel.planningSettingsState.collectAsState()
    val vaultName by viewModel.obsidianVaultName.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    val contextTags by viewModel.contextTagsState.collectAsState()

    when (val state = dialogState) {
        DialogState.Hidden -> {}
        is DialogState.AddList -> {
            AddListDialog(
                title = if (state.parentId == null) "Create new list" else "Create sublist",
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
                onEditRequest = { viewModel.onEditRequest(it) }
            )
        }
        is DialogState.MoveList -> {
            val disabledIds = remember(state.list.id, filteredListHierarchyForDialog.childMap) {
                getDescendantIds(state.list.id, filteredListHierarchyForDialog.childMap) + state.list.id
            }

            FilterableListChooser(
                title = "Перемістити '${state.list.name}'",
                filterText = listChooserFilterText,
                onFilterTextChanged = viewModel::onListChooserFilterChanged,
                topLevelLists = filteredListHierarchyForDialog.topLevelLists,
                childMap = filteredListHierarchyForDialog.childMap,
                expandedIds = listChooserExpandedIds,
                onToggleExpanded = viewModel::onListChooserToggleExpanded,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newParentId -> viewModel.onMoveListConfirmed(newParentId) },
                currentParentId = state.list.parentId,
                disabledIds = disabledIds
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
        is DialogState.EditList -> {
            EditListDialog(
                list = state.list,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName, newTags ->
                    viewModel.onEditListConfirmed(state.list, newName, newTags)
                }
            )
        }

        DialogState.AppSettings -> {
            SettingsDialog(
                planningSettings = planningSettings,
                initialVaultName = vaultName,
                onManageContextsClick = { viewModel.onManageContextsRequest() },
                onDismiss = { viewModel.dismissDialog() },
                onSave = { showModes, dailyTag, mediumTag, longTag, newVaultName ->
                    viewModel.saveSettings(showModes, dailyTag, mediumTag, longTag, newVaultName)
                },
            )
        }

        DialogState.ReservedContextsSettings -> {
            ReservedContextsDialog(
                initialContextTags = contextTags,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { newTags -> viewModel.saveContextSettings(newTags) }
            )
        }

        DialogState.AboutApp -> {
            AboutAppDialog(
                stats = stats,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
    }

    if (showWifiServerDialog) {
        WifiServerDialog(
            address = wifiServerAddress,
            onDismiss = { viewModel.onDismissWifiServerDialog() }
        )
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