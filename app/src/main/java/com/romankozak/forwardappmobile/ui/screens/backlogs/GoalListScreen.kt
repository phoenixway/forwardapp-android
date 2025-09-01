@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.backlogs

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import com.romankozak.forwardappmobile.ui.screens.backlogs.components.GoalListBottomNav
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

    val allContexts by viewModel.allContextsForDialog.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

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

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is GoalListUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navController.navigate("sync_screen")
                }
                is GoalListUiEvent.NavigateToDetails -> {
                    navController.navigate("goal_detail_screen/${event.listId}")
                }
                is GoalListUiEvent.ShowToast -> {
                    Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                }
                is GoalListUiEvent.NavigateToGlobalSearch -> {
                    navController.navigate("global_search_screen/${event.query}")
                }
                is GoalListUiEvent.ScrollToIndex -> {
                    coroutineScope.launch {
                        Log.d("REVEAL_DEBUG", "GoalListScreen: Команда отримана. Скролю до індексу ${event.index}")
                        listState.animateScrollToItem(event.index)
                    }
                }
                GoalListUiEvent.FocusSearchField -> {
                    coroutineScope.launch {
                        delay(100)
                        focusRequester.requestFocus()
                    }
                }
                GoalListUiEvent.NavigateToSettings -> {
                    navController.navigate("settings_screen")
                }
                is GoalListUiEvent.NavigateToEditListScreen -> {
                    navController.navigate("edit_list_screen/${event.listId}")
                }
            }
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
                if (allContexts.isEmpty()) {
                    Text(
                        text = "Немає налаштованих контекстів.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(allContexts, key = { it.name }) { context ->
                            ListItem(
                                headlineContent = { Text(context.name.replaceFirstChar { it.uppercase() }) },
                                leadingContent = {
                                    if (context.emoji.isNotBlank()) {
                                        Text(context.emoji, fontSize = 24.sp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = context.name)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.onContextSelected(context.name)
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
        // Викликаємо новий метод, який показує діалог, а не імпортує напряму
        uri?.let { viewModel.onImportFromFileRequested(it) }
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
                onRecentsClick = { viewModel.onShowRecentLists() }
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
                            viewModel.onShowSettingsScreen()
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