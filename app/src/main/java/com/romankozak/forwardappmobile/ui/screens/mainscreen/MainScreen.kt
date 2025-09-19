// File: MainScreen.kt

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.routes.navigateToDayManagement
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.navigation.NavigationHistoryMenu
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ExpandingBottomNav
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.SearchResultsView
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.BreadcrumbNavigation
import com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy.ProjectHierarchyView
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.HandleDialogs
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    // region State Collection
    val hierarchy by viewModel.projectHierarchy.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val planningMode by viewModel.planningMode.collectAsState()
    val planningSettings by viewModel.planningSettingsState.collectAsState()
    val currentBreadcrumbs by viewModel.currentBreadcrumbs.collectAsState()
    val focusedProjectId by viewModel.focusedProjectId.collectAsState()
    val areAnyProjectsExpanded by viewModel.areAnyProjectsExpanded.collectAsState()
    val isBottomNavExpanded by viewModel.isBottomNavExpanded.collectAsStateWithLifecycle()
    val listChooserFinalExpandedIds by viewModel.listChooserFinalExpandedIds.collectAsState()
    val filteredListHierarchyForDialog by viewModel.filteredListHierarchyForDialog.collectAsState()
    val allContexts by viewModel.allContextsForDialog.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val showRecentSheet by viewModel.showRecentListsSheet.collectAsState()
    val recentProjects by viewModel.recentProjects.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val showHistoryMenu by viewModel.showNavigationMenu.collectAsStateWithLifecycle()
    val scrollTargetIndex by viewModel.scrollTargetIndex.collectAsState()
    val searchQueryText by remember { derivedStateOf { searchQuery.text } }

    var showContextSheet by remember { mutableStateOf(false) }
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    // endregion

    // region Side Effects
    LaunchedEffect(Unit) {
        delay(100)
        viewModel.enableFiltering()

        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is ProjectUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navController.navigate("sync_screen")
                }
                is ProjectUiEvent.NavigateToDetails -> navController.navigate("goal_detail_screen/${event.projectId}")
                is ProjectUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is ProjectUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
                is ProjectUiEvent.ScrollToIndex -> viewModel.setScrollTarget(event.index)
                ProjectUiEvent.FocusSearchField -> {
                    coroutineScope.launch {
                        delay(100)
                        focusRequester.requestFocus()
                    }
                }
                ProjectUiEvent.NavigateToSettings -> navController.navigate("settings_screen")
                is ProjectUiEvent.NavigateToEditProjectScreen -> navController.navigate("edit_list_screen/${event.projectId}")
                is ProjectUiEvent.Navigate -> navController.navigate(event.route)
                is ProjectUiEvent.NavigateToDayPlan -> navController.navigateToDayManagement(event.date)
            }
        }
    }

    LaunchedEffect(savedStateHandle) {
        savedStateHandle
            ?.getStateFlow<String?>("project_to_reveal", null)
            ?.filterNotNull()
            ?.collect { projectId ->
                viewModel.onSearchResultClick(projectId)
                savedStateHandle["project_to_reveal"] = null
            }
    }

    DisposableEffect(savedStateHandle, lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                savedStateHandle?.remove<String?>("list_chooser_result")?.let { result ->
                    Log.d("MOVE_DEBUG", "[Screen] Resumed and processed result: '$result'")
                    viewModel.onListChooserResult(result)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel.projectHierarchy, scrollTargetIndex) {
        scrollTargetIndex?.let { targetIndex ->
            if (targetIndex >= 0) {
                snapshotFlow { listState.layoutInfo.totalItemsCount }
                    .filter { it > targetIndex }
                    .first()
                Log.d("UI_SCROLL", "Scrolling to index: $targetIndex after hierarchy update")
                delay(50)
                listState.animateScrollToItem(targetIndex)
                viewModel.clearScrollTarget()
            }
        }
    }

    val backHandlerEnabled by remember(isSearchActive, focusedProjectId, canGoBack, areAnyProjectsExpanded) {
        derivedStateOf { isSearchActive || focusedProjectId != null || canGoBack || areAnyProjectsExpanded }
    }

    BackHandler(enabled = backHandlerEnabled) {
        when {
            isSearchActive -> viewModel.onToggleSearch(isActive = false)
            focusedProjectId != null -> viewModel.clearNavigation()
            canGoBack -> viewModel.enhancedNavigationManager.goBack()
            areAnyProjectsExpanded -> viewModel.collapseAllProjects()
        }
    }
    // endregion

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            MainScreenTopAppBar(
                isSearchActive = isSearchActive,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onGoBack = { viewModel.enhancedNavigationManager.goBack() },
                onGoForward = { viewModel.enhancedNavigationManager.goForward() },
                onShowHistory = { viewModel.onShowNavigationHistory() },
                onAddNewProject = { viewModel.onAddNewProjectRequest() },
                onShowWifiServer = { viewModel.onShowWifiServerDialog() },
                onShowWifiImport = { viewModel.onShowWifiImportDialog() },
                onExportToFile = { viewModel.exportToFile() },
                onShowSettings = { viewModel.onShowSettingsScreen() },
                onShowAbout = { viewModel.onShowAboutDialog() }
            )
        },
        bottomBar = {
            if (isSearchActive) {
                SearchBottomBar(
                    searchQuery = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onCloseSearch = { viewModel.onToggleSearch(isActive = false) },
                    onPerformGlobalSearch = { viewModel.onPerformGlobalSearch(it) },
                    onShowSearchHistory = { showSearchHistorySheet = true },
                    focusRequester = focusRequester,
                )
            } else {
                ExpandingBottomNav(
                    navController = navController,
                    isSearchActive = isSearchActive,
                    onToggleSearch = viewModel::onToggleSearch,
                    onGlobalSearchClick = { viewModel.onShowSearchDialog() },
                    currentMode = planningMode,
                    onPlanningModeChange = viewModel::onPlanningModeChange,
                    onContextsClick = { showContextSheet = true },
                    onRecentsClick = { viewModel.onShowRecentLists() },
                    onDayPlanClick = viewModel::onDayPlanClicked,
                    onHomeClick = viewModel::onHomeClicked,
                    isExpanded = isBottomNavExpanded,
                    onExpandedChange = viewModel::onBottomNavExpandedChange
                )
            }
        },
    ) { paddingValues ->
        MainScreenContent(
            modifier = Modifier.padding(paddingValues),
            isSearchActive = isSearchActive,
            searchQueryText = searchQueryText,
            searchResults = viewModel.searchResults.collectAsState().value,
            onRevealClick = viewModel::onSearchResultClick,
            onOpenClick = viewModel::onProjectClicked,
            currentBreadcrumbs = currentBreadcrumbs,
            hierarchy = hierarchy,
            onNavigateToBreadcrumb = viewModel::navigateToBreadcrumb,
            onClearNavigation = viewModel::clearNavigation,
            onFocusedListMenuClick = { projectId ->
                hierarchy.allProjects.find { it.id == projectId }?.let(viewModel::onMenuRequested)
            },
            planningMode = planningMode,
            planningSettings = planningSettings,
            listState = listState,
            viewModel = viewModel
        )
    }

    // region Modals and Dialogs
    ContextBottomSheet(
        showSheet = showContextSheet,
        onDismiss = { showContextSheet = false },
        contexts = allContexts,
        onContextSelected = { contextName ->
            viewModel.onContextSelected(contextName)
            showContextSheet = false
        }
    )

    SearchHistoryBottomSheet(
        showSheet = showSearchHistorySheet,
        onDismiss = { showSearchHistorySheet = false },
        searchHistory = searchHistory,
        onHistoryClick = { query ->
            viewModel.onSearchQueryFromHistory(query)
            showSearchHistorySheet = false
        }
    )

    RecentListsSheet(
        showSheet = showRecentSheet,
        recentLists = recentProjects,
        onDismiss = { viewModel.onDismissRecentLists() },
        onListClick = { projectId -> viewModel.onRecentProjectSelected(projectId) },
    )

    HandleDialogs(
        dialogState = dialogState,
        viewModel = viewModel,
        listChooserFilterText = "",
        listChooserExpandedIds = listChooserFinalExpandedIds,
        filteredListHierarchyForDialog = filteredListHierarchyForDialog,
    )

    if (showHistoryMenu) {
        NavigationHistoryMenu(
            navManager = viewModel.enhancedNavigationManager,
            onDismiss = { viewModel.onHideNavigationHistory() }
        )
    }
    // endregion
}

@Composable
private fun MainScreenTopAppBar(
    isSearchActive: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onShowHistory: () -> Unit,
    onAddNewProject: () -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onExportToFile: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
) {
    TopAppBar(
        title = { Text("Projects") },
        actions = {
            if (!isSearchActive) {
                AnimatedVisibility(visible = canGoBack, enter = fadeIn(animationSpec = tween(200)), exit = fadeOut(animationSpec = tween(200))) {
                    IconButton(onClick = onGoBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") }
                }
                AnimatedVisibility(visible = canGoForward, enter = fadeIn(animationSpec = tween(200)), exit = fadeOut(animationSpec = tween(200))) {
                    IconButton(onClick = onGoForward) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Вперед") }
                }
                IconButton(onClick = onShowHistory) { Icon(Icons.Outlined.History, "Історія") }

                IconButton(onClick = onAddNewProject) { Icon(Icons.Default.Add, "Add new project") }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Run Wi-Fi Server") }, onClick = { onShowWifiServer(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Import from Wi-Fi") }, onClick = { onShowWifiImport(); menuExpanded = false })
                    HorizontalDivider()
                    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { _ ->
                        // This logic needs to be hoisted to the MainScreen to access the ViewModel
                    }
                    DropdownMenuItem(text = { Text("Export to file") }, onClick = { onExportToFile(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Import from file") }, onClick = { importLauncher.launch("application/json"); menuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { onShowSettings(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("About") }, onClick = { onShowAbout(); menuExpanded = false })
                }
            }
        },
    )
}

@Composable
private fun MainScreenContent(
    modifier: Modifier = Modifier,
    isSearchActive: Boolean,
    searchQueryText: String,
    searchResults: List<SearchResult>,
    onRevealClick: (String) -> Unit,
    onOpenClick: (String) -> Unit,
    currentBreadcrumbs: List<BreadcrumbItem>,
    hierarchy: ListHierarchyData,
    onNavigateToBreadcrumb: (BreadcrumbItem) -> Unit,
    onClearNavigation: () -> Unit,
    onFocusedListMenuClick: (String) -> Unit,
    planningMode: PlanningMode,
    planningSettings: PlanningSettingsState,
    listState: LazyListState,
    viewModel: MainScreenViewModel
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (isSearchActive && searchQueryText.isNotBlank()) {
            SearchResultsView(
                results = searchResults,
                onRevealClick = onRevealClick,
                onOpenClick = onOpenClick
            )
        } else {
            AnimatedVisibility(
                visible = currentBreadcrumbs.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BreadcrumbNavigation(
                    breadcrumbs = currentBreadcrumbs,
                    onNavigate = onNavigateToBreadcrumb,
                    onClearNavigation = onClearNavigation,
                    onFocusedListMenuClick = onFocusedListMenuClick,
                    onOpenAsProject = onOpenClick
                )
            }

            val isListEmpty = hierarchy.topLevelProjects.isEmpty() && hierarchy.childMap.isEmpty()
            if (isListEmpty) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val emptyText = when (planningMode) {
                        is PlanningMode.Daily -> "No projects with tag '#${planningSettings.dailyTag}'"
                        is PlanningMode.Medium -> "No projects with tag '#${planningSettings.mediumTag}'"
                        is PlanningMode.Long -> "No projects with tag '#${planningSettings.longTag}'"
                        else -> "Create your first project"
                    }
                    Text(emptyText, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                ProjectHierarchyView(
                    modifier = Modifier.weight(1f),
                    hierarchy = hierarchy,
                    focusedProjectId = viewModel.focusedProjectId.collectAsState().value,
                    highlightedProjectId = viewModel.highlightedProjectId.collectAsState().value,
                    searchQuery = searchQueryText,
                    isSearchActive = isSearchActive,
                    planningMode = planningMode,
                    hierarchySettings = viewModel.hierarchySettings.collectAsState().value,
                    listState = listState,
                    longDescendantsMap = viewModel.longDescendantsMap.collectAsState().value,
                    onProjectClicked = viewModel::onProjectClicked,
                    onToggleExpanded = viewModel::onToggleExpanded,
                    onMenuRequested = viewModel::onMenuRequested,
                    onNavigateToProject = viewModel::navigateToProject,
                    onProjectReorder = viewModel::onProjectReorder
                )
            }
        }
    }
}

@Composable
private fun ContextBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    contexts: List<UiContext>,
    onContextSelected: (String) -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Обрати контекст",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (contexts.isEmpty()) {
                    Text(
                        text = "Немає налаштованих контекстів.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(contexts, key = { it.name }) { context ->
                            ListItem(
                                headlineContent = { Text(context.name.replaceFirstChar { it.uppercase() }) },
                                leadingContent = {
                                    if (context.emoji.isNotBlank()) {
                                        Text(context.emoji, fontSize = 24.sp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = context.name)
                                    }
                                },
                                modifier = Modifier.clickable { onContextSelected(context.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    searchHistory: List<String>,
    onHistoryClick: (String) -> Unit
) {
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = "Search History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (searchHistory.isEmpty()) {
                    Text(
                        text = "No recent searches.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(searchHistory, key = { it }) { query ->
                            ListItem(
                                headlineContent = { Text(query) },
                                leadingContent = {
                                    Icon(Icons.Outlined.History, contentDescription = "Search history item")
                                },
                                modifier = Modifier.clickable { onHistoryClick(query) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SearchBottomBar(
    searchQuery: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onCloseSearch: () -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
    onShowSearchHistory: () -> Unit,
    focusRequester: FocusRequester,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding(),
        ) {
            AnimatedVisibility(
                visible = searchQuery.text.isNotBlank(),
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPerformGlobalSearch(searchQuery.text) },
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
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
                            text = "Search everywhere for \"${searchQuery.text}\"",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Close search",
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                SearchTextField(
                    searchQuery = searchQuery,
                    onQueryChange = onQueryChange,
                    onPerformGlobalSearch = onPerformGlobalSearch,
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onShowSearchHistory) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "Search history"
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTextField(
    searchQuery: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        ),
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                if (searchQuery.text.isNotBlank()) {
                    onPerformGlobalSearch(searchQuery.text)
                }
                focusManager.clearFocus()
            },
        ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isFocused) 0.6f else 0.3f),
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
                    if (searchQuery.text.isEmpty()) {
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
                    visible = searchQuery.text.isNotBlank(),
                    enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f),
                ) {
                    IconButton(
                        onClick = { onQueryChange(TextFieldValue("")) },
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