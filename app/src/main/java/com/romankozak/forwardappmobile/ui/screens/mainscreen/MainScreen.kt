// File: MainScreen.kt
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
import kotlinx.coroutines.flow.collectLatest

// File: MainScreen.kt
@Composable
fun MainScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Обробка одноразових подій з ViewModel (навігація, тости, фокус)
    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collectLatest { event ->
            when (event) {
                is ProjectUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navController.navigate("sync_screen")
                }
                is ProjectUiEvent.NavigateToDetails -> navController.navigate("goal_detail_screen/${event.projectId}")
                is ProjectUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is ProjectUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
                is ProjectUiEvent.NavigateToSettings -> navController.navigate("settings_screen")
                is ProjectUiEvent.NavigateToEditProjectScreen -> navController.navigate("edit_list_screen/${event.projectId}")
                is ProjectUiEvent.Navigate -> navController.navigate(event.route)
                is ProjectUiEvent.NavigateToDayPlan -> navController.navigateToDayManagement(event.date)
                is ProjectUiEvent.FocusSearchField -> {
                    // ЗМІНА: Видаляємо focusRequester.requestFocus() звідси
                    // Фокус тепер обробляється безпосередньо в SearchBottomBar
                }
                is ProjectUiEvent.ScrollToIndex -> { /* Ця логіка тепер вбудована в projectHierarchyFlow */ }
            }
        }
    }

    // Обробка результатів з інших екранів
    DisposableEffect(navController, lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.remove<String?>("list_chooser_result")
                    ?.let { result ->
                        viewModel.onEvent(MainScreenEvent.MoveConfirm(result))
                    }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    viewModel.enhancedNavigationManager?.let { navManager ->
        MainScreenScaffold(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            enhancedNavigationManager = navManager
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreenScaffold(
    uiState: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
    enhancedNavigationManager: com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
) {
    val listState = rememberLazyListState()

    // Локальний стан для модальних вікон, які не потребують збереження у ViewModel
    var showContextSheet by remember { mutableStateOf(false) }
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onEvent(MainScreenEvent.ImportFromFileRequest(it)) }
    }

    val backHandlerEnabled by remember(uiState.isSearchActive, uiState.focusedProjectId, uiState.canGoBack, uiState.areAnyProjectsExpanded) {
        derivedStateOf {
            uiState.isSearchActive || uiState.focusedProjectId != null || uiState.canGoBack || uiState.areAnyProjectsExpanded
        }
    }

    BackHandler(enabled = backHandlerEnabled) {
        when {
            uiState.isSearchActive -> onEvent(MainScreenEvent.ToggleSearch(false))
            uiState.focusedProjectId != null -> onEvent(MainScreenEvent.ClearBreadcrumbNavigation)
            uiState.canGoBack -> onEvent(MainScreenEvent.BackClick)
            uiState.areAnyProjectsExpanded -> { /* onEvent(MainScreenEvent.CollapseAllProjects) */ }
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            MainScreenTopAppBar(
                isSearchActive = uiState.isSearchActive,
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                onGoBack = { onEvent(MainScreenEvent.BackClick) },
                onGoForward = { onEvent(MainScreenEvent.ForwardClick) },
                onShowHistory = { onEvent(MainScreenEvent.HistoryClick) },
                onAddNewProject = { onEvent(MainScreenEvent.AddNewProjectRequest) },
                onShowWifiServer = { onEvent(MainScreenEvent.ShowWifiServerDialog) },
                onShowWifiImport = { onEvent(MainScreenEvent.ShowWifiImportDialog) },
                onExportToFile = { onEvent(MainScreenEvent.ExportToFile) },
                onImportFromFile = { importLauncher.launch("application/json") },
                onShowSettings = { onEvent(MainScreenEvent.GoToSettings) },
                onShowAbout = { onEvent(MainScreenEvent.ShowAboutDialog) }
            )
        },
        bottomBar = {
            if (uiState.isSearchActive) {
                SearchBottomBar(
                    searchQuery = uiState.searchQuery,
                    onQueryChange = { onEvent(MainScreenEvent.SearchQueryChanged(it)) },
                    onCloseSearch = { onEvent(MainScreenEvent.ToggleSearch(false)) },
                    onPerformGlobalSearch = { onEvent(MainScreenEvent.GlobalSearchPerform(it)) },
                    onShowSearchHistory = { showSearchHistorySheet = true }
                )
            } else {
                ExpandingBottomNav(
                    onToggleSearch = { onEvent(MainScreenEvent.ToggleSearch(true)) },
                    onGlobalSearchClick = { onEvent(MainScreenEvent.ShowSearchDialog) },
                    currentMode = uiState.planningMode,
                    onPlanningModeChange = { onEvent(MainScreenEvent.PlanningModeChange(it)) },
                    onContextsClick = { showContextSheet = true },
                    onRecentsClick = { onEvent(MainScreenEvent.ShowRecentLists) },
                    onDayPlanClick = { onEvent(MainScreenEvent.DayPlanClick) },
                    onHomeClick = { onEvent(MainScreenEvent.HomeClick) },
                    isExpanded = uiState.isBottomNavExpanded,
                    onExpandedChange = { onEvent(MainScreenEvent.BottomNavExpandedChange(it)) },
                    onAiChatClick = { onEvent(MainScreenEvent.NavigateToChat) },
                    onActivityTrackerClick = { onEvent(MainScreenEvent.NavigateToActivityTracker) }
                )
            }
        }
    ) { paddingValues ->
        MainScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onEvent = onEvent,
            listState = listState
        )
    }

    // --- Модальні вікна та діалоги ---

    if (uiState.showNavigationMenu) {
        NavigationHistoryMenu(
            navManager = enhancedNavigationManager,
            onDismiss = { onEvent(MainScreenEvent.HideHistory) }
        )
    }

    ContextBottomSheet(
        showSheet = showContextSheet,
        onDismiss = { showContextSheet = false },
        contexts = uiState.allContexts,
        onContextSelected = {
            onEvent(MainScreenEvent.ContextSelected(it))
            showContextSheet = false
        }
    )

    SearchHistoryBottomSheet(
        showSheet = showSearchHistorySheet,
        onDismiss = { showSearchHistorySheet = false },
        searchHistory = uiState.searchHistory,
        onHistoryClick = {
            onEvent(MainScreenEvent.SearchFromHistory(it))
            showSearchHistorySheet = false
        }
    )

    RecentListsSheet(
        showSheet = uiState.showRecentListsSheet,
        recentLists = uiState.recentProjects,
        onDismiss = { onEvent(MainScreenEvent.DismissRecentLists) },
        onListClick = { onEvent(MainScreenEvent.RecentProjectSelected(it)) },
    )
}

@Composable
private fun SearchBottomBar(
    searchQuery: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onCloseSearch: () -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
    onShowSearchHistory: () -> Unit,
) {
    // ЗМІНА: Створюємо focusRequester безпосередньо тут
    val focusRequester = remember { FocusRequester() }

    // ЗМІНА: Автоматично фокусуємо поле при появі SearchBottomBar
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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

@OptIn(ExperimentalMaterial3Api::class)
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
    onImportFromFile: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
) {
    TopAppBar(
        title = { Text("Projects") },
        actions = {
            if (!isSearchActive) {
                AnimatedVisibility(visible = canGoBack) { IconButton(onClick = onGoBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") } }
                AnimatedVisibility(visible = canGoForward) { IconButton(onClick = onGoForward) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Вперед") } }
                IconButton(onClick = onShowHistory) { Icon(Icons.Outlined.History, "Історія") }

                IconButton(onClick = onAddNewProject) { Icon(Icons.Default.Add, "Add new project") }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Run Wi-Fi Server") }, onClick = { onShowWifiServer(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Import from Wi-Fi") }, onClick = { onShowWifiImport(); menuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Export to file") }, onClick = { onExportToFile(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("Import from file") }, onClick = { onImportFromFile(); menuExpanded = false })
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Settings") }, onClick = { onShowSettings(); menuExpanded = false })
                    DropdownMenuItem(text = { Text("About") }, onClick = { onShowAbout(); menuExpanded = false })
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainScreenContent(
    modifier: Modifier = Modifier,
    uiState: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
    listState: LazyListState,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isSearchActive && uiState.searchQuery.text.isNotBlank()) {
            SearchResultsView(
                results = uiState.searchResults,
                onRevealClick = { onEvent(MainScreenEvent.SearchResultClick(it)) },
                onOpenClick = { onEvent(MainScreenEvent.ProjectClick(it)) }
            )
        } else {
            AnimatedVisibility(
                visible = uiState.currentBreadcrumbs.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BreadcrumbNavigation(
                    breadcrumbs = uiState.currentBreadcrumbs,
                    onNavigate = { onEvent(MainScreenEvent.BreadcrumbNavigation(it)) },
                    onClearNavigation = { onEvent(MainScreenEvent.ClearBreadcrumbNavigation) },
                    onFocusedListMenuClick = { projectId ->
                        uiState.projectHierarchy.allProjects.find { it.id == projectId }
                            ?.let { onEvent(MainScreenEvent.ProjectMenuRequest(it)) }
                    },
                    onOpenAsProject = { onEvent(MainScreenEvent.ProjectClick(it)) }
                )
            }

            val isListEmpty = uiState.projectHierarchy.topLevelProjects.isEmpty() && uiState.projectHierarchy.childMap.isEmpty()
            if (isListEmpty) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val emptyText = when (uiState.planningMode) {
                        is PlanningMode.Daily -> "No projects with tag '#${uiState.planningSettings.dailyTag}'"
                        is PlanningMode.Medium -> "No projects with tag '#${uiState.planningSettings.mediumTag}'"
                        is PlanningMode.Long -> "No projects with tag '#${uiState.planningSettings.longTag}'"
                        else -> "Create your first project"
                    }
                    Text(emptyText, style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                ProjectHierarchyView(
                    modifier = Modifier.weight(1f),
                    hierarchy = uiState.projectHierarchy,
                    focusedProjectId = uiState.focusedProjectId,
                    highlightedProjectId = null, // Потребує додавання в UiState
                    searchQuery = uiState.searchQuery.text,
                    isSearchActive = uiState.isSearchActive,
                    planningMode = uiState.planningMode,
                    hierarchySettings = HierarchyDisplaySettings(), // Потребує додавання в UiState
                    listState = listState,
                    longDescendantsMap = emptyMap(), // Потребує додавання в UiState
                    onProjectClicked = { onEvent(MainScreenEvent.ProjectClick(it)) },
                    onToggleExpanded = { onEvent(MainScreenEvent.ToggleProjectExpanded(it)) },
                    onMenuRequested = { onEvent(MainScreenEvent.ProjectMenuRequest(it)) },
                    onNavigateToProject = { /* Реалізувати через Event */ },
                    onProjectReorder = { from, to, pos -> onEvent(MainScreenEvent.ProjectReorder(from, to, pos)) }
                )
            }
        }
    }
}

// ... (Решта допоміжних Composable-функцій: ContextBottomSheet, SearchHistoryBottomSheet, SearchBottomBar)
// Їх тіла залишаються такими ж, але виклики viewModel.method() замінюються на onEvent(...)
@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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