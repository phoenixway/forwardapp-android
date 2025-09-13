@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.GoalListBottomNav
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
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

    var showContextSheet by remember { mutableStateOf(value = false) }

    val allContexts by viewModel.allContextsForDialog.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val showRecentSheet by viewModel.showRecentListsSheet.collectAsState()
    val recentLists by viewModel.recentLists.collectAsState()
    val areAnyListsExpanded by viewModel.areAnyListsExpanded.collectAsState()

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        delay(100)
        viewModel.enableFiltering()
    }


    LaunchedEffect(savedStateHandle) {
        savedStateHandle
            ?.getStateFlow<String?>("list_to_reveal", null)
            ?.filterNotNull()
            ?.collect { listId ->
                viewModel.processRevealRequest(listId)
                savedStateHandle["list_to_reveal"] = null
            }
    }

    DisposableEffect(savedStateHandle, lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val result = savedStateHandle?.remove<String?>("list_chooser_result")
                    if (result != null) {
                        Log.d("MOVE_DEBUG", "[Screen] Resumed and processed result: '$result'")
                        viewModel.onListChooserResult(result)
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                is GoalListUiEvent.Navigate -> {
                    navController.navigate(event.route)
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
                                modifier =
                                    Modifier.clickable {
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

    BackHandler(enabled = !isSearchActive && areAnyListsExpanded) {
        viewModel.collapseAllLists()
    }

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.onImportFromFileRequested(it) }
        }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding(),
        topBar = {
            GoalListTopAppBar(
                isSearchActive = isSearchActive,
                onAddNewList = { viewModel.onAddNewListRequest() },
                viewModel = viewModel,
                onImportFromFile = { importLauncher.launch("application/json") },
            )
        },
        bottomBar = {
            if (isSearchActive) {
                SearchBottomBar(
                    searchQuery = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onCloseSearch = { viewModel.onToggleSearch(false) },
                    onPerformGlobalSearch = viewModel::onPerformGlobalSearch,
                    focusRequester = focusRequester,
                )
            } else {
                GoalListBottomNav(
                    navController = navController,
                    isSearchActive = isSearchActive,
                    onToggleSearch = viewModel::onToggleSearch,
                    onGlobalSearchClick = { viewModel.onShowSearchDialog() },
                    currentMode = planningMode,
                    onPlanningModeChange = viewModel::onPlanningModeChange,
                    onContextsClick = { showContextSheet = true },
                    onRecentsClick = { viewModel.onShowRecentLists() },
                )
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            val isListEmpty = hierarchy.topLevelLists.isEmpty() && hierarchy.childMap.isEmpty()
            if (isListEmpty) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val emptyText =
                        when {
                            isSearchActive -> "No projects found for your query."
                            planningMode is PlanningMode.Daily -> "No projects with tag '#${planningSettings.dailyTag}'"
                            planningMode is PlanningMode.Medium -> "No projects with tag '#${planningSettings.mediumTag}'"
                            planningMode is PlanningMode.Long -> "No projects with tag '#${planningSettings.longTag}'"
                            else -> "Create your first project"
                        }
                    Text(emptyText, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                DragAndDropContainer(
                    state = dragAndDropState,
                    enabled = !isSearchActive,
                    modifier = Modifier.weight(1f),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
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
                            searchQuery = searchQuery.text,
                        )
                    }
                }
            }
        }
    }

    RecentListsSheet(
        showSheet = showRecentSheet,
        recentLists = recentLists,
        onDismiss = { viewModel.onDismissRecentLists() },
        onListClick = { listId -> viewModel.onRecentListSelected(listId) },
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
    onAddNewList: () -> Unit,
    viewModel: GoalListViewModel,
    onImportFromFile: () -> Unit,
) {
    TopAppBar(
        title = { Text("Projects") },
        actions = {
            if (!isSearchActive) {
                IconButton(onClick = onAddNewList) { Icon(Icons.Default.Add, "Add new project") }
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

@Composable
private fun SearchEverywhereButton(
    searchQuery: String,
    onPerformGlobalSearch: (String) -> Unit,
) {
    AnimatedVisibility(
        visible = searchQuery.isNotBlank(),
        enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPerformGlobalSearch(searchQuery) },
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Perform global search",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search everywhere for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RowScope.SearchTextField(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = textFieldValue,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        singleLine = true,
        textStyle =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    if (textFieldValue.text.isNotBlank()) {
                        onPerformGlobalSearch(textFieldValue.text)
                    }
                    focusManager.clearFocus()
                },
            ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isFocused) 0.6f else 0.3f),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(24.dp),
                        )
                        .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "Search projects...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.semantics { contentDescription = "Search placeholder" },
                        )
                    }
                    innerTextField()
                }
                AnimatedVisibility(
                    visible = textFieldValue.text.isNotBlank(),
                    enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f),
                ) {
                    IconButton(
                        onClick = { onValueChange(TextFieldValue("")) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear search input",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SearchBottomBar(
    searchQuery: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onCloseSearch: () -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            SearchEverywhereButton(
                searchQuery = searchQuery.text,
                onPerformGlobalSearch = onPerformGlobalSearch
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowBack,
                        contentDescription = "Close search",
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                SearchTextField(
                    textFieldValue = searchQuery,
                    onValueChange = onQueryChange,
                    onPerformGlobalSearch = onPerformGlobalSearch,
                    focusRequester = focusRequester
                )
            }
        }
    }
}