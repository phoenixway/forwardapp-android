// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goallist/GoalListScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.goallist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.Role
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

    val listChooserFinalExpandedIds by viewModel.listChooserFinalExpandedIds.collectAsState()
    val listChooserFilterText by viewModel.listChooserFilterText.collectAsState()
    val filteredListHierarchyForDialog by viewModel.filteredListHierarchyForDialog.collectAsState()

    var showPlanningModeSheet by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }

    // Новий стан, що містить і теги, і емодзі
    val contextSettings by viewModel.reservedContextsState.collectAsState()

    if (showPlanningModeSheet) {
        ModalBottomSheet(onDismissRequest = { showPlanningModeSheet = false }) {
            Column(Modifier.navigationBarsPadding()) {
                Text("Обрати режим", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                ListItem(
                    headlineContent = { Text("Всі") },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "Всі") },
                    modifier = Modifier.clickable {
                        viewModel.onPlanningModeChange(PlanningMode.All)
                        showPlanningModeSheet = false
                    }
                )
                if (planningSettings.showModes) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(headlineContent = { Text("Денний") }, leadingContent = { Icon(Icons.Outlined.Today, contentDescription = "Денний") }, modifier = Modifier.clickable { viewModel.onPlanningModeChange(PlanningMode.Daily); showPlanningModeSheet = false })
                    ListItem(headlineContent = { Text("Середньостроковий") }, leadingContent = { Icon(Icons.Outlined.QueryStats, contentDescription = "Середньостроковий") }, modifier = Modifier.clickable { viewModel.onPlanningModeChange(PlanningMode.Medium); showPlanningModeSheet = false })
                    ListItem(headlineContent = { Text("Довгостроковий") }, leadingContent = { Icon(Icons.Outlined.TrackChanges, contentDescription = "Довгостроковий") }, modifier = Modifier.clickable { viewModel.onPlanningModeChange(PlanningMode.Long); showPlanningModeSheet = false })
                }
            }
        }
    }

    if (showContextSheet) {
        ModalBottomSheet(onDismissRequest = { showContextSheet = false }) {
            Column(Modifier.navigationBarsPadding()) {
                Text("Обрати контекст", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                if (contextSettings.isEmpty()) {
                    Text("Немає налаштованих контекстів.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn {
                        items(contextSettings.entries.toList().sortedBy { it.key }) { (contextName, settings) ->
                            val (_, emoji) = settings
                            ListItem(
                                headlineContent = { Text(contextName.replaceFirstChar { it.uppercase() }) },
                                leadingContent = {
                                    if (emoji.isNotBlank()) {
                                        Text(emoji, fontSize = 24.sp) // Показуємо емодзі
                                    } else {
                                        Icon(Icons.Outlined.Label, contentDescription = contextName) // Запасний варіант
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.onContextSelected(contextName)
                                    showContextSheet = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    BackHandler(enabled = isSearchActive) { viewModel.onToggleSearch(false) }
    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent(), onResult = { uri -> if (uri != null) viewModel.importFromFile(uri) })
    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is GoalListUiEvent.NavigateToSyncScreenWithData -> { syncDataViewModel.jsonString = event.json; navController.navigate("sync_screen") }
                is GoalListUiEvent.NavigateToDetails -> navController.navigate("goal_detail_screen/${event.listId}")
                is GoalListUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is GoalListUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding(),
        topBar = { GoalListTopAppBar(isSearchActive, searchQuery, viewModel::onSearchQueryChanged, { viewModel.onToggleSearch(!isSearchActive) }, { viewModel.onAddNewListRequest() }, { viewModel.onShowSearchDialog() }, viewModel, { importLauncher.launch("application/json") }) },
        bottomBar = { GoalListBottomNav(navController, isSearchActive, viewModel::onToggleSearch, planningMode, { showPlanningModeSheet = true }, { showContextSheet = true }) }
    ) { paddingValues ->
        val isListEmpty = hierarchy.topLevelLists.isEmpty() && hierarchy.childMap.isEmpty()
        if (isListEmpty) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), contentAlignment = Alignment.Center) {
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
                LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    renderGoalList(hierarchy.topLevelLists, hierarchy.childMap, 0, dragAndDropState, viewModel, hierarchy.allLists, isSearchActive, planningMode)
                }
            }
        }
    }

    HandleDialogs(dialogState, viewModel, listChooserFilterText, listChooserFinalExpandedIds, filteredListHierarchyForDialog)
}

@Composable
private fun GoalListTopAppBar(isSearchActive: Boolean, searchQuery: String, onQueryChange: (String) -> Unit, onToggleSearch: () -> Unit, onAddNewList: () -> Unit, onShowGlobalSearch: () -> Unit, viewModel: GoalListViewModel, onImportFromFile: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    TopAppBar(
        title = { if (!isSearchActive) Text("Backlogs") },
        actions = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp).focusRequester(focusRequester),
                    placeholder = { Text("Filter lists...") },
                    shape = CircleShape,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, "Search Icon") },
                    trailingIcon = { IconButton(onClick = onToggleSearch) { Icon(Icons.Default.Close, "Close filter") } },
                    colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            } else {
                IconButton(onClick = onAddNewList) { Icon(Icons.Default.Add, "Add new list") }
                IconButton(onClick = onShowGlobalSearch) { Icon(Icons.Default.Search, "Global search") }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Run Wi-Fi Server") }, onClick = { viewModel.onShowWifiServerDialog(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Import from Wi-Fi") }, onClick = { viewModel.onShowWifiImportDialog(); menuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Export to file") }, onClick = { viewModel.exportToFile(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Import from file") }, onClick = { onImportFromFile(); menuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { viewModel.onShowSettingsDialog(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("About") }, onClick = { viewModel.onShowAboutDialog(); menuExpanded = false })
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
    currentMode: PlanningMode,
    onModeSelectorClick: () -> Unit,
    onContextsClick: () -> Unit
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
            selected = false,
            onClick = onContextsClick,
            icon = { Icon(Icons.Outlined.Style, "Contexts") },
            label = { Text("Contexts") }
        )

        val (currentIcon, currentLabel) = when (currentMode) {
            is PlanningMode.Daily -> Icons.Outlined.Today to "Daily"
            is PlanningMode.Medium -> Icons.Outlined.QueryStats to "Medium"
            is PlanningMode.Long -> Icons.Outlined.TrackChanges to "Long"
            else -> Icons.AutoMirrored.Outlined.List to "All"
        }
        val isSelected = !isSearchActive && (currentMode !is PlanningMode.All)

        Box(
            modifier = Modifier
                .weight(1.0f)
                .height(80.dp)
                .selectable(
                    selected = isSelected,
                    onClick = { onModeSelectorClick() },
                    role = Role.Tab,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val iconColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

                // --- ВИПРАВЛЕНО ТУТ ---
                val indicatorColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    label = "Indicator Color Animation"
                )

                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(64.dp)
                        .background(indicatorColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentIcon,
                        contentDescription = "Change planning mode",
                        tint = iconColor
                    )
                }
                Text(
                    text = currentLabel,
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun LazyListScope.renderGoalList(lists: List<GoalList>, childMap: Map<String, List<GoalList>>, level: Int, dragAndDropState: DragAndDropState<GoalList>, viewModel: GoalListViewModel, allListsFlat: List<GoalList>, isSearchActive: Boolean, planningMode: PlanningMode) {
    lists.forEach { list ->
        item(key = list.id) {
            DraggableItem(state = dragAndDropState, key = list.id, data = list, dragAfterLongPress = true, modifier = Modifier.fillMaxWidth().dropTarget(state = dragAndDropState, key = list.id, onDrop = { draggedItemState -> viewModel.onListMoved(draggedItemState.data.id, list.id) })) {
                val isDragging = this.isDragging
                val draggedItemIndex = allListsFlat.indexOfFirst { it.id == dragAndDropState.draggedItem?.data?.id }
                val currentItemIndex = allListsFlat.indexOfFirst { it.id == list.id }
                val isHovered = dragAndDropState.hoveredDropTargetKey == list.id
                val isDraggingDown = draggedItemIndex != -1 && draggedItemIndex < currentItemIndex
                GoalListRow(list, level, childMap.containsKey(list.id), { viewModel.onListClicked(it) }, { viewModel.onToggleExpanded(it) }, { viewModel.onMenuRequested(it) }, isDragging, isHovered, isDraggingDown)
            }
        }
        if (list.isExpanded) {
            val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
            if (children.isNotEmpty()) {
                renderGoalList(children, childMap, level + 1, dragAndDropState, viewModel, allListsFlat, isSearchActive, planningMode)
            }
        }
    }
}

private fun getDescendantIds(listId: String, childMap: Map<String, List<GoalList>>): Set<String> {
    val descendants = mutableSetOf<String>()
    val queue = ArrayDeque<String>(); queue.add(listId)
    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        childMap[currentId]?.forEach { child -> descendants.add(child.id); queue.add(child.id) }
    }
    return descendants
}

@Composable
private fun HandleDialogs(dialogState: DialogState, viewModel: GoalListViewModel, listChooserFilterText: String, listChooserExpandedIds: Set<String>, filteredListHierarchyForDialog: ListHierarchyData) {
    val stats by viewModel.appStatistics.collectAsState()
    val planningSettings by viewModel.planningSettingsState.collectAsState()
    val vaultName by viewModel.obsidianVaultName.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    // Новий стан для діалогу налаштувань
    val contextSettings by viewModel.reservedContextsState.collectAsState()

    when (val state = dialogState) {
        DialogState.Hidden -> {}
        is DialogState.AddList -> { AddListDialog(if (state.parentId == null) "Create new list" else "Create sublist", { viewModel.dismissDialog() }, { name -> viewModel.onAddList(name, state.parentId) }) }
        is DialogState.ContextMenu -> { ContextMenuDialog(state.list, { viewModel.dismissDialog() }, { viewModel.onMoveListRequest(it) }, { viewModel.onAddSublistRequest(it) }, { viewModel.onDeleteRequest(it) }, { viewModel.onEditRequest(it) }) }
        is DialogState.MoveList -> {
            val disabledIds = remember(state.list.id, filteredListHierarchyForDialog.childMap) { getDescendantIds(state.list.id, filteredListHierarchyForDialog.childMap) + state.list.id }
            FilterableListChooser("Перемістити '${state.list.name}'", listChooserFilterText, viewModel::onListChooserFilterChanged, filteredListHierarchyForDialog.topLevelLists, filteredListHierarchyForDialog.childMap, listChooserExpandedIds, viewModel::onListChooserToggleExpanded, { viewModel.dismissDialog() }, { newParentId -> viewModel.onMoveListConfirmed(newParentId) }, state.list.parentId, disabledIds)
        }
        is DialogState.ConfirmDelete -> { AlertDialog({ viewModel.dismissDialog() }, title = { Text("Delete list?") }, text = { Text("Are you sure you want to delete '${state.list.name}' and all its sublists and goals? This action cannot be undone.") }, confirmButton = { Button(onClick = { viewModel.onDeleteListConfirmed(state.list) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } }) }
        is DialogState.EditList -> { EditListDialog(state.list, { viewModel.dismissDialog() }, { newName, newTags -> viewModel.onEditListConfirmed(state.list, newName, newTags) }) }
        DialogState.AppSettings -> { SettingsDialog(planningSettings, vaultName, { viewModel.onManageContextsRequest() }, { viewModel.dismissDialog() }, { showModes, dailyTag, mediumTag, longTag, newVaultName -> viewModel.saveSettings(showModes, dailyTag, mediumTag, longTag, newVaultName) }) }
        DialogState.ReservedContextsSettings -> {
            ReservedContextsDialog(
                initialContexts = contextSettings,
                onDismiss = { viewModel.dismissDialog() },
                onSave = { newContexts -> viewModel.saveContextSettings(newContexts) }
            )
        }
        DialogState.AboutApp -> { AboutAppDialog(stats, { viewModel.dismissDialog() }) }
    }

    if (showWifiServerDialog) { WifiServerDialog(wifiServerAddress) { viewModel.onDismissWifiServerDialog() } }
    if (showWifiImportDialog) {
        val desktopAddress by viewModel.desktopAddress.collectAsState()
        WifiImportDialog(desktopAddress, { viewModel.onDesktopAddressChange(it) }, { viewModel.onDismissWifiImportDialog() }, { address -> viewModel.performWifiImport(address) })
    }
    if (showSearchDialog) { GlobalSearchDialog({ viewModel.onDismissSearchDialog() }, { query -> viewModel.onPerformGlobalSearch(query) }) }
}