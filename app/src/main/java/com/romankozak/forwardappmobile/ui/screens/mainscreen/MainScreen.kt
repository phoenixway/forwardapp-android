
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.routes.navigateToDayManagement
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.navigation.NavigationHistoryMenu
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ExpandingBottomNav
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.*
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.HandleDialogs
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.flow.collectLatest

private const val UI_TAG = "MainScreenUI_DEBUG"

@Composable
fun MainScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: MainScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    
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
                    
                }
                is ProjectUiEvent.ScrollToIndex -> { }
            }
        }
    }

    
    DisposableEffect(navController, lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
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
            enhancedNavigationManager = navManager,
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun MainScreenScaffold(
    uiState: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
    enhancedNavigationManager: com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager,
) {
    val listState = rememberLazyListState()

    
    var showContextSheet by remember { mutableStateOf(false) }
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(MainScreenEvent.ImportFromFileRequest(it)) }
        }

    val backHandlerEnabled by remember(uiState.subStateStack, uiState.currentBreadcrumbs, uiState.areAnyProjectsExpanded) {
        derivedStateOf {
            val enabled =
                uiState.subStateStack.size > 1 ||
                    uiState.currentBreadcrumbs.isNotEmpty() ||
                    uiState.areAnyProjectsExpanded
            Log.d(UI_TAG, "BackHandler enabled = $enabled")
            enabled
        }
    }

    BackHandler(enabled = backHandlerEnabled) {
        Log.i(UI_TAG, "Custom BackHandler INVOKED")
        onEvent(MainScreenEvent.BackClick)
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            
            val isSearchActive = uiState.subStateStack.any { it is MainSubState.LocalSearch }
            val isFocusMode = uiState.currentBreadcrumbs.isNotEmpty()

            MainScreenTopAppBar(
                isSearchActive = isSearchActive,
                isFocusMode = isFocusMode,
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
                onShowAbout = { onEvent(MainScreenEvent.ShowAboutDialog) },
            )
        },
        bottomBar = {
            
            val isSearchActive = uiState.subStateStack.any { it is MainSubState.LocalSearch }

            if (isSearchActive) {
                SearchBottomBar(
                    searchQuery = uiState.searchQuery,
                    onQueryChange = { onEvent(MainScreenEvent.SearchQueryChanged(it)) },
                    onCloseSearch = {
                        
                        onEvent(MainScreenEvent.CloseSearch)
                    },
                    onPerformGlobalSearch = { onEvent(MainScreenEvent.GlobalSearchPerform(it)) },
                    onShowSearchHistory = { showSearchHistorySheet = true },
                )
            } else {
                ExpandingBottomNav(
                    onToggleSearch = {
                        
                        onEvent(MainScreenEvent.SearchQueryChanged(TextFieldValue("")))
                    },
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
                    onActivityTrackerClick = { onEvent(MainScreenEvent.NavigateToActivityTracker) },
                    onInsightsClick = { onEvent(MainScreenEvent.NavigateToAiInsights) },
                )
            }
        },
    ) { paddingValues ->
        MainScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onEvent = onEvent,
            listState = listState,
        )
    }

    

    if (uiState.showNavigationMenu) {
        NavigationHistoryMenu(
            navManager = enhancedNavigationManager,
            onDismiss = { onEvent(MainScreenEvent.HideHistory) },
        )
    }

    ContextBottomSheet(
        showSheet = showContextSheet,
        onDismiss = { showContextSheet = false },
        contexts = uiState.allContexts,
        onContextSelected = {
            onEvent(MainScreenEvent.ContextSelected(it))
            showContextSheet = false
        },
    )

    SearchHistoryBottomSheet(
        showSheet = showSearchHistorySheet,
        onDismiss = { showSearchHistorySheet = false },
        searchHistory = uiState.searchHistory,
        onHistoryClick = {
            onEvent(MainScreenEvent.SearchFromHistory(it))
            showSearchHistorySheet = false
        },
    )

    RecentListsSheet(
        showSheet = uiState.showRecentListsSheet,
        recentLists = uiState.recentProjects,
        onDismiss = { onEvent(MainScreenEvent.DismissRecentLists) },
        onListClick = { onEvent(MainScreenEvent.RecentProjectSelected(it)) },
    )

    
    HandleDialogs(
        uiState = uiState,
        onEvent = onEvent,
    )
}

@Composable
private fun NeonTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val textColor = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
            color = textColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenTopAppBar(
    isSearchActive: Boolean,
    isFocusMode: Boolean,
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
        title = { NeonTitle("Projects") },
        actions = {
            if (!isSearchActive) {
                AnimatedVisibility(visible = canGoBack) {
                    IconButton(onClick = onGoBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад")
                    }
                }
                AnimatedVisibility(visible = canGoForward) {
                    IconButton(onClick = onGoForward) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Вперед")
                    }
                }
                IconButton(onClick = onShowHistory) {
                    Icon(Icons.Outlined.History, "История")
                }

                AnimatedVisibility(visible = !isFocusMode) {
                    
                    IconButton(onClick = onAddNewProject) {
                        Icon(Icons.Default.Add, "Add new project")
                    }
                }
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, "Menu")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Run Wi-Fi Server") },
                        onClick = {
                            onShowWifiServer()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Import from Wi-Fi") },
                        onClick = {
                            onShowWifiImport()
                            menuExpanded = false
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Export to file") },
                        onClick = {
                            onExportToFile()
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
                            onShowSettings()
                            menuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            onShowAbout()
                            menuExpanded = false
                        },
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = Modifier.shadow(4.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContextBottomSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    contexts: List<UiContext>,
    onContextSelected: (String) -> Unit,
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
    onHistoryClick: (String) -> Unit,
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
                                modifier = Modifier.clickable { onHistoryClick(query) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    searchQuery: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onPerformGlobalSearch: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
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
                    if (searchQuery.text.isNotBlank()) {
                        onPerformGlobalSearch(searchQuery.text)
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
                            color =
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = if (isFocused) 0.6f else 0.3f,
                                ),
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
