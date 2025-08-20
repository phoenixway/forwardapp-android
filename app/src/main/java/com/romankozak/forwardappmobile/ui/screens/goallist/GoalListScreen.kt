// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goallist/GoalListScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goallist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.dialogs.*
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.UUID


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
    val highlightedListId by viewModel.highlightedListId.collectAsState()

    val listChooserFinalExpandedIds by viewModel.listChooserFinalExpandedIds.collectAsState()
    val listChooserFilterText by viewModel.listChooserFilterText.collectAsState()
    val filteredListHierarchyForDialog by viewModel.filteredListHierarchyForDialog.collectAsState()

    var showPlanningModeSheet by remember { mutableStateOf(value = false) }
    var showContextSheet by remember { mutableStateOf(value = false) }

    val contextSettings by viewModel.reservedContextsState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // ✨ ДОДАНО: Отримуємо стан з ViewModel для шторки недавніх
    val showRecentSheet by viewModel.showRecentListsSheet.collectAsState()
    val recentLists by viewModel.recentLists.collectAsState()


    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("list_to_reveal", null)
            ?.filterNotNull()
            ?.collect { listId ->
                viewModel.processRevealRequest(listId)
                savedStateHandle["list_to_reveal"] = null
            }
    }


    if (showPlanningModeSheet) {
        ModalBottomSheet(onDismissRequest = { showPlanningModeSheet = false }) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Обрати режим",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ListItem(
                    headlineContent = { Text("Всі") },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Всі") },
                    modifier = Modifier.clickable {
                        viewModel.onPlanningModeChange(PlanningMode.All)
                        showPlanningModeSheet = false
                    },
                )
                if (planningSettings.showModes) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Денний") },
                        leadingContent = { Icon(Icons.Outlined.Today, contentDescription = "Денний") },
                        modifier = Modifier.clickable {
                            viewModel.onPlanningModeChange(PlanningMode.Daily)
                            showPlanningModeSheet = false
                        },
                    )
                    ListItem(
                        headlineContent = { Text("Середньостроковий") },
                        leadingContent = { Icon(Icons.Outlined.QueryStats, contentDescription = "Середньостроковий") },
                        modifier = Modifier.clickable {
                            viewModel.onPlanningModeChange(PlanningMode.Medium)
                            showPlanningModeSheet = false
                        },
                    )
                    ListItem(
                        headlineContent = { Text("Довгостроковий") },
                        leadingContent = { Icon(Icons.Outlined.TrackChanges, contentDescription = "Довгостроковий") },
                        modifier = Modifier.clickable {
                            viewModel.onPlanningModeChange(PlanningMode.Long)
                            showPlanningModeSheet = false
                        },
                    )
                }
            }
        }
    }

    if (showContextSheet) {
        ModalBottomSheet(onDismissRequest = { showContextSheet = false }) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Обрати контекст",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (contextSettings.isEmpty()) {
                    Text(
                        text = "Немає налаштованих контекстів.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(contextSettings.entries.toList().sortedBy { it.key }) { (contextName, settings) ->
                            val (_, emoji) = settings
                            ListItem(
                                headlineContent = { Text(contextName.replaceFirstChar { it.uppercase() }) },
                                leadingContent = {
                                    if (emoji.isNotBlank()) {
                                        Text(emoji, fontSize = 24.sp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = contextName)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.onContextSelected(contextName)
                                    showContextSheet = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = isSearchActive) { viewModel.onToggleSearch(isActive = false) }
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importFromFile(it) }
    }
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

                is GoalListUiEvent.ScrollToList -> {
                    coroutineScope.launch {
                        fun flattenHierarchy(
                            topLevel: List<GoalList>,
                            children: Map<String, List<GoalList>>
                        ): List<GoalList> {
                            val result = mutableListOf<GoalList>()
                            fun traverse(lists: List<GoalList>) {
                                lists.forEach { list ->
                                    result.add(list)
                                    if (list.isExpanded) {
                                        children[list.id]?.sortedBy { it.order }?.let(::traverse)
                                    }
                                }
                            }
                            traverse(topLevel)
                            return result
                        }

                        val displayedLists = flattenHierarchy(hierarchy.topLevelLists, hierarchy.childMap)
                        val index = displayedLists.indexOfFirst { it.id == event.listId }
                        if (index != -1) {
                            listState.animateScrollToItem(index)
                        }
                    }
                }
                GoalListUiEvent.FocusSearchField -> {
                    coroutineScope.launch {
                        delay(100)
                        focusRequester.requestFocus()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        topBar = {
            GoalListTopAppBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                onToggleSearch = { viewModel.onToggleSearch(!isSearchActive) },
                onAddNewList = { viewModel.onAddNewListRequest() },
                viewModel = viewModel,
                onImportFromFile = { importLauncher.launch("application/json") },
                focusRequester = focusRequester
            )
        },
        bottomBar = {
            GoalListBottomNav(
                navController = navController,
                isSearchActive = isSearchActive,
                onToggleSearch = viewModel::onToggleSearch,
                onGlobalSearchClick = { viewModel.onShowSearchDialog() },
                currentMode = planningMode,
                onModeSelectorClick = { showPlanningModeSheet = true },
                onContextsClick = { showContextSheet = true },
                onRecentsClick = { viewModel.onShowRecentLists() } // ✨ ДОДАНО
            )
        },
    ) { paddingValues ->
        val isListEmpty = hierarchy.topLevelLists.isEmpty() && hierarchy.childMap.isEmpty()
        if (isListEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
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
            DragAndDropContainer(state = dragAndDropState, enabled = !isSearchActive) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    renderGoalList(
                        lists = hierarchy.topLevelLists,
                        childMap = hierarchy.childMap,
                        level = 0,
                        dragAndDropState = dragAndDropState,
                        viewModel = viewModel,
                        allListsFlat = hierarchy.allLists,
                        isSearchActive = isSearchActive,
                        planningMode = planningMode,
                        highlightedListId = highlightedListId,
                    )
                }
            }
        }
    }

    // ✨ ДОДАНО: Відображаємо Bottom Sheet
    RecentListsSheet(
        showSheet = showRecentSheet,
        recentLists = recentLists,
        onDismiss = { viewModel.onDismissRecentLists() },
        onListClick = { listId -> viewModel.onRecentListSelected(listId) }
    )

    HandleDialogs(
        dialogState = dialogState,
        viewModel = viewModel,
        listChooserFilterText = listChooserFilterText,
        listChooserExpandedIds = listChooserFinalExpandedIds,
        filteredListHierarchyForDialog = filteredListHierarchyForDialog,
    )
}

@Composable
private fun GoalListTopAppBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onAddNewList: () -> Unit,
    viewModel: GoalListViewModel,
    onImportFromFile: () -> Unit,
    focusRequester: FocusRequester
) {
    val focusManager = LocalFocusManager.current
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
                    leadingIcon = { Icon(Icons.Default.Search, "Search Icon") },
                    trailingIcon = { IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Close, "Close filter") } },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )
            } else {
                IconButton(onClick = onAddNewList) { Icon(Icons.Default.Add, "Add new list") }
                var menuExpanded by remember { mutableStateOf(value = false) }
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Run Wi-Fi Server") },
                        onClick = {
                            viewModel.onShowWifiServerDialog()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import from Wi-Fi") },
                        onClick = {
                            viewModel.onShowWifiImportDialog()
                            menuExpanded = false
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Export to file") },
                        onClick = {
                            viewModel.exportToFile()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import from file") },
                        onClick = {
                            onImportFromFile()
                            menuExpanded = false
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            viewModel.onShowSettingsDialog()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            viewModel.onShowAboutDialog()
                            menuExpanded = false
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun GoalListBottomNav(
    navController: NavController,
    isSearchActive: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    onGlobalSearchClick: () -> Unit,
    currentMode: PlanningMode,
    onModeSelectorClick: () -> Unit,
    onContextsClick: () -> Unit,
    onRecentsClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("activity_tracker_screen") },
            icon = { Icon(Icons.Outlined.Timeline, "Activity Tracker") },
            // ✨ ЗМІНЕНО: Додано налаштування шрифту
            label = {
                Text(
                    text = "Track",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
        NavigationBarItem(
            selected = false,
            onClick = onGlobalSearchClick,
            icon = { Icon(Icons.Outlined.Search, "Global Search") },
            // ✨ ЗМІНЕНО: Додано налаштування шрифту
            label = {
                Text(
                    text = "Search",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
        NavigationBarItem(
            selected = isSearchActive,
            onClick = { onToggleSearch(true) },
            icon = { Icon(Icons.Outlined.FilterList, "Filter") },
            // ✨ ЗМІНЕНО: Додано налаштування шрифту
            label = {
                Text(
                    text = "Filter",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )
        NavigationBarItem(
            selected = false,
            onClick = onContextsClick,
            icon = { Icon(Icons.Outlined.Style, "Contexts") },
            // ✨ ЗМІНЕНО: Додано налаштування шрифту
            label = {
                Text(
                    text = "Contexts",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )

        NavigationBarItem(
            selected = false,
            onClick = onRecentsClick,
            icon = { Icon(Icons.Outlined.History, "Recent Lists") },
            // ✨ ЗМІНЕНО: Текст "Недавні" на "Recent" та додано налаштування шрифту
            label = {
                Text(
                    text = "Recent",
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
        )

        val (currentIcon, currentLabel) = when (currentMode) {
            is PlanningMode.Daily -> Icons.Outlined.Today to "Daily"
            is PlanningMode.Medium -> Icons.Outlined.QueryStats to "Medium"
            is PlanningMode.Long -> Icons.Outlined.TrackChanges to "Long"
            else -> Icons.AutoMirrored.Outlined.List to "All"
        }
        val isSelected = !isSearchActive && (currentMode !is PlanningMode.All)

        NavigationBarItem(
            selected = isSelected,
            onClick = onModeSelectorClick,
            icon = {
                Icon(
                    imageVector = currentIcon,
                    contentDescription = "Change planning mode"
                )
            },
            // ✨ ЗМІНЕНО: Додано налаштування шрифту
            label = {
                Text(
                    text = currentLabel,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
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
    planningMode: PlanningMode,
    highlightedListId: String?,
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
                    ) { draggedItemState ->
                        viewModel.onListMoved(
                            draggedItemState.data.id,
                            list.id,
                        )
                    },
            ) {
                val isDragging = this.isDragging
                val draggedItemIndex = allListsFlat.indexOfFirst { it.id == dragAndDropState.draggedItem?.data?.id }
                val currentItemIndex = allListsFlat.indexOfFirst { it.id == list.id }
                val isHovered = dragAndDropState.hoveredDropTargetKey == list.id
                val isDraggingDown = (draggedItemIndex != -1) && (draggedItemIndex < currentItemIndex)
                val isHighlighted = list.id == highlightedListId

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
                    isHighlighted = isHighlighted,
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
                    planningMode = planningMode,
                    highlightedListId = highlightedListId,
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
    filteredListHierarchyForDialog: ListHierarchyData,
) {
    val stats by viewModel.appStatistics.collectAsState()
    val planningSettings by viewModel.planningSettingsState.collectAsState()
    val vaultName by viewModel.obsidianVaultName.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    val contextSettings by viewModel.reservedContextsState.collectAsState()

    when (val state = dialogState) {
        DialogState.Hidden -> {}
        is DialogState.AddList -> {
            AddListDialog(
                title = if (state.parentId == null) "Create new list" else "Create sublist",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name ->
                    val newId = UUID.randomUUID().toString()
                    viewModel.addNewList(newId, state.parentId, name)
                    viewModel.dismissDialog()
                },
            )
        }

        is DialogState.ContextMenu -> {
            ContextMenuDialog(
                list = state.list,
                onDismissRequest = { viewModel.dismissDialog() },
                onMoveRequest = { viewModel.onMoveListRequest(it) },
                onAddSublistRequest = { viewModel.onAddSublistRequest(it) },
                onDeleteRequest = { viewModel.onDeleteRequest(it) },
                onEditRequest = { viewModel.onEditRequest(it) },
            )
        }
        is DialogState.MoveList -> {
            val disabledIds = remember(state.list.id, filteredListHierarchyForDialog.childMap) {
                getDescendantIds(
                    state.list.id,
                    filteredListHierarchyForDialog.childMap,
                ) + state.list.id
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
                disabledIds = disabledIds,
                onAddNewList = viewModel::addNewList,
            )
        }

        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete list?") },
                text = { Text("Are you sure you want to delete '${state.list.name}' and all its sublists and goals? This action cannot be undone.") },
                confirmButton = { Button(onClick = { viewModel.onDeleteListConfirmed(state.list) }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } },
            )
        }
        is DialogState.EditList -> {
            EditListDialog(
                list = state.list,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newName, newTags ->
                    viewModel.onEditListConfirmed(
                        state.list,
                        newName,
                        newTags,
                    )
                },
            )
        }
        DialogState.AppSettings -> {
            SettingsDialog(
                planningSettings = planningSettings,
                initialVaultName = vaultName,
                onManageContextsClick = { viewModel.onManageContextsRequest() },
                onDismiss = { viewModel.dismissDialog() },
            ) { showModes, dailyTag, mediumTag, longTag, newVaultName ->
                viewModel.saveSettings(
                    showModes,
                    dailyTag,
                    mediumTag,
                    longTag,
                    newVaultName,
                )
            }
        }
        DialogState.ReservedContextsSettings -> {
            ReservedContextsDialog(
                initialContexts = contextSettings,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { newContexts -> viewModel.saveContextSettings(newContexts) },
            )
        }
        is DialogState.AboutApp -> {
            AboutAppDialog(stats) { viewModel.dismissDialog() }
        }
    }

    if (showWifiServerDialog) {
        WifiServerDialog(wifiServerAddress) { viewModel.onDismissWifiServerDialog() }
    }
    if (showWifiImportDialog) {
        val desktopAddress by viewModel.desktopAddress.collectAsState()
        WifiImportDialog(
            desktopAddress = desktopAddress,
            onAddressChange = { viewModel.onDesktopAddressChange(it) },
            onDismiss = { viewModel.onDismissWifiImportDialog() },
            onConfirm = { address -> viewModel.performWifiImport(address) },
        )
    }
    if (showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { viewModel.onDismissSearchDialog() },
            onConfirm = { query -> viewModel.onPerformGlobalSearch(query) },
        )
    }
}