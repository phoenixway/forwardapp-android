package com.romankozak.forwardappmobile.features.globalsearch

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import kotlinx.coroutines.launch
import java.util.Locale

private const val SCROLL_TO_TOP_VISIBILITY_INDEX = 5

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlobalSearchScreen(
    navController: NavController,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTopButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > SCROLL_TO_TOP_VISIBILITY_INDEX }
    }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val configuration = LocalConfiguration.current
    val typeOptions = remember { GlobalSearchType.entries }
    var selectedTypes by remember { mutableStateOf(typeOptions.toSet()) }
    var selectedSort by remember { mutableStateOf(GlobalSearchSort.Relevance) }
    var showTypeSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showDataActionsSheet by remember { mutableStateOf(false) }
    var selectedCommandIndex by remember { mutableStateOf<Int?>(null) }
    var selectedDataIndex by remember { mutableStateOf<Int?>(null) }
    var selectionArea by remember { mutableStateOf(OmniboxSelectionArea.None) }
    val currentMode = uiState.mode
    val modePalette = rememberModePalette(currentMode)
    val submitCommandIndex = selectedCommandIndex.takeIf { selectionArea == OmniboxSelectionArea.Command }
    val submitWithKeyboardHide: () -> Unit = {
        viewModel.onSubmitSearch(submitCommandIndex)
        keyboardController?.hide()
    }
    val filteredResults by remember(uiState.results, selectedTypes, selectedSort, uiState.query) {
        derivedStateOf {
            uiState.results
                .filter { result -> selectedTypes.any { it.matches(result) } }
                .let { results ->
                    when (selectedSort) {
                        GlobalSearchSort.Relevance -> results
                        GlobalSearchSort.Type ->
                            results.sortedBy { it.groupKey() }
                        GlobalSearchSort.Alphabetical ->
                            results.sortedBy { resultTitle(it).lowercase(Locale.getDefault()) }
                    }
                }
        }
    }

    val loadingScale by animateFloatAsState(
        targetValue = if (uiState.isLoading) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "loading_scale",
    )

    val resultsAlpha by animateFloatAsState(
        targetValue = if (!uiState.isLoading && uiState.results.isNotEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
        label = "results_alpha",
    )
    val selectedDataResultUniqueId =
        remember(filteredResults, selectedDataIndex) {
            selectedDataIndex?.let { index -> filteredResults.getOrNull(index)?.uniqueId }
        }

    LaunchedEffect(currentMode, uiState.commandResults, uiState.hybridCommandResults, filteredResults) {
        when (currentMode) {
            OmniboxMode.Command -> {
                val size = uiState.commandResults.size
                selectedCommandIndex = if (size > 0) 0 else null
                selectedDataIndex = null
                selectionArea = if (size > 0) OmniboxSelectionArea.Command else OmniboxSelectionArea.None
            }
            OmniboxMode.DataSearch -> {
                val commandSize = uiState.hybridCommandResults.size
                val dataSize = filteredResults.size
                when {
                    commandSize > 0 -> {
                        selectedCommandIndex = 0
                        selectedDataIndex = if (dataSize > 0) 0 else null
                        selectionArea = OmniboxSelectionArea.Command
                    }
                    dataSize > 0 -> {
                        selectedCommandIndex = null
                        selectedDataIndex = 0
                        selectionArea = OmniboxSelectionArea.Data
                    }
                    else -> {
                        selectedCommandIndex = null
                        selectedDataIndex = null
                        selectionArea = OmniboxSelectionArea.None
                    }
                }
            }
            else -> {
                selectedCommandIndex = null
                selectedDataIndex = null
                selectionArea = OmniboxSelectionArea.None
            }
        }
    }

    fun openDataResultPrimary(result: GlobalSearchResultItem) {
        when (result) {
            is GlobalSearchResultItem.GoalItem -> {
                viewModel.onDataResultOpened(result.uniqueId)
                viewModel.navigateToProjectForResult(result.backlogItem.contextId, result.projectName)
            }
            is GlobalSearchResultItem.LinkItem -> {
                viewModel.onDataResultOpened(result.uniqueId)
                viewModel.navigateToProjectForResult(
                    result.searchResult.contextId,
                    result.searchResult.contextName,
                )
            }
            is GlobalSearchResultItem.SubcontextItem -> {
                viewModel.onDataResultOpened(result.uniqueId)
                viewModel.navigateToProjectForResult(
                    result.searchResult.subcontext.id,
                    result.searchResult.subcontext.name,
                )
            }
            is GlobalSearchResultItem.ContextItem -> {
                viewModel.onDataResultOpened(result.uniqueId)
                viewModel.navigateToProjectForResult(result.searchResult.context.id, result.searchResult.context.name)
            }
            is GlobalSearchResultItem.ActivityItem -> {
                val contextId = result.record.contextId
                if (!contextId.isNullOrBlank()) {
                    viewModel.onDataResultOpened(result.uniqueId)
                    viewModel.navigateToProjectForResult(contextId, null)
                }
            }
            is GlobalSearchResultItem.InboxItem -> {
                viewModel.onDataResultOpened(result.uniqueId)
                viewModel.navigateToProjectForResult(result.record.contextId, null)
            }
            is GlobalSearchResultItem.AttachmentItem -> {
                val contextId = result.searchResult.ownerContextId
                if (!contextId.isNullOrBlank()) {
                    viewModel.onDataResultOpened(result.uniqueId)
                    viewModel.navigateToProjectForResult(contextId, result.searchResult.contextName)
                }
            }
        }
    }

    BackHandler {
        if (!navController.popBackStack()) {
            viewModel.enhancedNavigationManager.goBack()
        }
    }

    LaunchedEffect(configuration.keyboard, configuration.hardKeyboardHidden) {
        focusRequester.requestFocus()
        val hasVisibleHardwareKeyboard =
            configuration.keyboard != Configuration.KEYBOARD_NOKEYS &&
                configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        if (!hasVisibleHardwareKeyboard) {
            keyboardController?.show()
        }
    }

    if (showTypeSheet) {
        TypeBottomSheet(
            options = typeOptions,
            selected = selectedTypes,
            onApply = {
                selectedTypes = if (it.isEmpty()) typeOptions.toSet() else it
                showTypeSheet = false
            },
            onDismiss = { showTypeSheet = false },
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            selectedSort = selectedSort,
            onSortSelected = {
                selectedSort = it
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false },
        )
    }
    if (showDataActionsSheet) {
        DataActionsBottomSheet(
            onSelectTypes = {
                showDataActionsSheet = false
                showTypeSheet = true
            },
            onSelectSorting = {
                showDataActionsSheet = false
                showSortSheet = true
            },
            onDismiss = { showDataActionsSheet = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Пошук",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navController.popBackStack()) {
                            viewModel.enhancedNavigationManager.goBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible =
                            !uiState.isLoading &&
                                when (currentMode) {
                                    OmniboxMode.DataSearch -> filteredResults.isNotEmpty()
                                    OmniboxMode.Command -> uiState.commandResults.isNotEmpty()
                                    else -> false
                                },
                        enter = fadeIn(animationSpec = tween(delayMillis = 200)) + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        ResultsCountBadge(
                            count =
                                when (currentMode) {
                                    OmniboxMode.DataSearch -> filteredResults.size
                                    OmniboxMode.Command -> uiState.commandResults.size
                                    else -> 0
                                },
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ),
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Нагору")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { paddingValues ->
        val contentBottomPadding =
            when (currentMode) {
                OmniboxMode.DataSearch -> 150.dp
                OmniboxMode.Command -> 116.dp
                OmniboxMode.QuickCatchInbox, OmniboxMode.StartActivity, OmniboxMode.AddActivityEvent -> 116.dp
            }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        modePalette.screenBottomTint,
                                    ),
                                startY = 0.1f,
                            ),
                    ).padding(paddingValues)
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = contentBottomPadding),
            ) {
                when (currentMode) {
                    OmniboxMode.DataSearch -> {
                        when {
                            uiState.query.isBlank() -> {
                                SearchStartContent(
                                    history = uiState.searchHistory,
                                    onHistoryClick = viewModel::onSelectHistoryQuery,
                                    onRemoveHistoryEntry = viewModel::removeSearchHistoryEntry,
                                    onClearHistory = viewModel::clearSearchHistory,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            uiState.isLoading -> {
                                DataSearchLoadingContent(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(scaleX = loadingScale, scaleY = loadingScale),
                                )
                            }
                            filteredResults.isEmpty() -> {
                                EmptyDataSearchContent(
                                    args =
                                        EmptyDataSearchArgs(
                                            query = uiState.query,
                                            commandResults = uiState.hybridCommandResults,
                                            selectedCommandIndex = selectedCommandIndex,
                                            accentColor = modePalette.iconTint,
                                            onCommandClick = viewModel::onCommandClick,
                                            actions =
                                                EmptyDataSearchActions(
                                                    onQuickCatch = viewModel::quickCatchCurrentQuery,
                                                    onStartActivity = viewModel::startActivityFromCurrentQuery,
                                                    onAddActivityEvent = viewModel::addActivityEventFromCurrentQuery,
                                                    onRunBestCommand = viewModel::runBestCommandForCurrentQuery,
                                                ),
                                        ),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            else -> {
                                SearchResultsContent(
                                    args =
                                        SearchResultsContentArgs(
                                            commandResults = uiState.hybridCommandResults,
                                            selectedCommandIndex =
                                                selectedCommandIndex.takeIf {
                                                    selectionArea == OmniboxSelectionArea.Command
                                                },
                                            onCommandClick = viewModel::onCommandClick,
                                            accentColor = modePalette.iconTint,
                                            results = filteredResults,
                                            query = uiState.query,
                                            viewModel = viewModel,
                                            obsidianVaultName = obsidianVaultName,
                                            context = context,
                                            listState = listState,
                                            selectedResultUniqueId = selectedDataResultUniqueId,
                                        ),
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(alpha = resultsAlpha),
                                )
                            }
                        }
                    }
                    OmniboxMode.Command -> {
                        CommandResultsContent(
                            args =
                                CommandResultsArgs(
                                    results = uiState.commandResults,
                                    query = uiState.query,
                                    recentCommands = uiState.recentCommands,
                                    selectedCommandIndex = selectedCommandIndex,
                                    onCommandClick = viewModel::onCommandClick,
                                    accentColor = modePalette.iconTint,
                                ),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    OmniboxMode.QuickCatchInbox -> {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                    OmniboxMode.StartActivity -> {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                    OmniboxMode.AddActivityEvent -> {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                }
            }
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                shape = RoundedCornerShape(18.dp),
                color = modePalette.searchSurface,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ModePill(
                            mode = currentMode,
                            palette = modePalette,
                        )
                        if (currentMode == OmniboxMode.DataSearch) {
                            Surface(
                                onClick = { showDataActionsSheet = true },
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreHoriz,
                                        contentDescription = "Додаткові дії пошуку",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }
                    val modeHint = ""
                    if (modeHint.isNotBlank()) {
                        val hintContainerColor =
                            if (currentMode == OmniboxMode.QuickCatchInbox) {
                                modePalette.modeIconContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = hintContainerColor,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        ) {
                            Text(
                                text = modeHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    val isQuickCatchMode = currentMode == OmniboxMode.QuickCatchInbox

                    TextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isQuickCatchMode) {
                                        Modifier.heightIn(min = 56.dp, max = 160.dp)
                                    } else {
                                        Modifier.heightIn(min = 56.dp)
                                    },
                                )
                                .focusRequester(focusRequester)
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                    when (keyEvent.key) {
                                        Key.DirectionDown -> {
                                            when (currentMode) {
                                                OmniboxMode.Command -> {
                                                    val commands = uiState.commandResults
                                                    if (commands.isEmpty()) return@onPreviewKeyEvent false
                                                    val current = selectedCommandIndex ?: -1
                                                    selectedCommandIndex =
                                                        (current + 1).coerceAtMost(commands.lastIndex)
                                                    true
                                                }
                                                OmniboxMode.DataSearch -> {
                                                    val commandSize = uiState.hybridCommandResults.size
                                                    val dataSize = filteredResults.size
                                                    if (commandSize == 0 && dataSize == 0) {
                                                        return@onPreviewKeyEvent false
                                                    }
                                                    when (selectionArea) {
                                                        OmniboxSelectionArea.Command -> {
                                                            val current = selectedCommandIndex ?: 0
                                                            if (current < commandSize - 1) {
                                                                selectedCommandIndex = current + 1
                                                            } else if (dataSize > 0) {
                                                                selectionArea = OmniboxSelectionArea.Data
                                                                selectedDataIndex = 0
                                                            }
                                                        }
                                                        OmniboxSelectionArea.Data -> {
                                                            val current = selectedDataIndex ?: -1
                                                            selectedDataIndex = (current + 1).coerceAtMost(dataSize - 1)
                                                        }
                                                        OmniboxSelectionArea.None -> {
                                                            if (commandSize > 0) {
                                                                selectionArea = OmniboxSelectionArea.Command
                                                                selectedCommandIndex = 0
                                                            } else if (dataSize > 0) {
                                                                selectionArea = OmniboxSelectionArea.Data
                                                                selectedDataIndex = 0
                                                            }
                                                        }
                                                    }
                                                    true
                                                }
                                                else -> false
                                            }
                                        }
                                        Key.DirectionUp -> {
                                            when (currentMode) {
                                                OmniboxMode.Command -> {
                                                    val commands = uiState.commandResults
                                                    if (commands.isEmpty()) return@onPreviewKeyEvent false
                                                    val current = selectedCommandIndex ?: commands.size
                                                    selectedCommandIndex = (current - 1).coerceAtLeast(0)
                                                    true
                                                }
                                                OmniboxMode.DataSearch -> {
                                                    val commandSize = uiState.hybridCommandResults.size
                                                    val dataSize = filteredResults.size
                                                    if (commandSize == 0 && dataSize == 0) {
                                                        return@onPreviewKeyEvent false
                                                    }
                                                    when (selectionArea) {
                                                        OmniboxSelectionArea.Command -> {
                                                            val current = selectedCommandIndex ?: commandSize
                                                            selectedCommandIndex = (current - 1).coerceAtLeast(0)
                                                        }
                                                        OmniboxSelectionArea.Data -> {
                                                            val current = selectedDataIndex ?: dataSize
                                                            if (current > 0) {
                                                                selectedDataIndex = current - 1
                                                            } else if (commandSize > 0) {
                                                                selectionArea = OmniboxSelectionArea.Command
                                                                selectedCommandIndex = commandSize - 1
                                                            }
                                                        }
                                                        OmniboxSelectionArea.None -> {
                                                            if (dataSize > 0) {
                                                                selectionArea = OmniboxSelectionArea.Data
                                                                selectedDataIndex = dataSize - 1
                                                            } else if (commandSize > 0) {
                                                                selectionArea = OmniboxSelectionArea.Command
                                                                selectedCommandIndex = commandSize - 1
                                                            }
                                                        }
                                                    }
                                                    true
                                                }
                                                else -> false
                                            }
                                        }
                                        Key.Enter, Key.NumPadEnter -> {
                                            when (currentMode) {
                                                OmniboxMode.Command -> {
                                                    viewModel.onSubmitSearch(selectedCommandIndex)
                                                    keyboardController?.hide()
                                                    true
                                                }
                                                OmniboxMode.DataSearch -> {
                                                    when (selectionArea) {
                                                        OmniboxSelectionArea.Command -> {
                                                            viewModel.onSubmitSearch(selectedCommandIndex)
                                                            keyboardController?.hide()
                                                            true
                                                        }
                                                        OmniboxSelectionArea.Data -> {
                                                            val result =
                                                                selectedDataIndex?.let {
                                                                    filteredResults.getOrNull(it)
                                                                }
                                                            if (result != null) {
                                                                openDataResultPrimary(result)
                                                                keyboardController?.hide()
                                                                true
                                                            } else {
                                                                false
                                                            }
                                                        }
                                                        OmniboxSelectionArea.None -> {
                                                            viewModel.onSubmitSearch(selectedCommandIndex)
                                                            keyboardController?.hide()
                                                            true
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    viewModel.onSubmitSearch()
                                                    keyboardController?.hide()
                                                    true
                                                }
                                            }
                                        }
                                        else -> false
                                    }
                                },
                        singleLine = !isQuickCatchMode,
                        maxLines = if (isQuickCatchMode) 6 else 1,
                        placeholder = { Text(placeholderForMode(currentMode)) },
                        leadingIcon = {
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = modePalette.modeIconContainer,
                                    modifier =
                                        Modifier
                                            .size(34.dp)
                                            .pointerInput(currentMode) {
                                                var totalDrag = 0f
                                                detectHorizontalDragGestures(
                                                    onHorizontalDrag = { _, dragAmount ->
                                                        totalDrag += dragAmount
                                                    },
                                                    onDragEnd = {
                                                        when {
                                                            totalDrag > 36f -> viewModel.cycleMode(forward = false)
                                                            totalDrag < -36f -> viewModel.cycleMode(forward = true)
                                                        }
                                                        totalDrag = 0f
                                                    },
                                                )
                                            }
                                            .clickable { showModeMenu = true },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = modeIcon(currentMode),
                                            contentDescription = "Режим omnibox",
                                            tint = modePalette.iconTint,
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = showModeMenu,
                                    onDismissRequest = { showModeMenu = false },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    OmniboxMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(modeTitle(mode)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = modeIcon(mode),
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                showModeMenu = false
                                                viewModel.setMode(mode)
                                            },
                                        )
                                    }
                                }
                            }
                        },
                        trailingIcon = {
                            Row {
                                if (uiState.query.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Очистити запит",
                                        )
                                    }
                                }
                                IconButton(onClick = submitWithKeyboardHide) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = submitDescriptionForMode(currentMode),
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submitWithKeyboardHide() }),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = modePalette.inputFocusedContainer.copy(alpha = 1f),
                                unfocusedContainerColor = modePalette.inputContainer.copy(alpha = 1f),
                                disabledContainerColor = modePalette.inputContainer.copy(alpha = 1f),
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                focusedLeadingIconColor = modePalette.iconTint,
                                unfocusedLeadingIconColor = modePalette.iconTint,
                                focusedTrailingIconColor = modePalette.iconTint,
                                unfocusedTrailingIconColor = modePalette.iconTint,
                            ),
                    )
                }
            }
        }
    }
}


private fun modeIcon(mode: OmniboxMode): ImageVector =
    when (mode) {
        OmniboxMode.DataSearch -> Icons.Default.Search
        OmniboxMode.Command -> Icons.Default.Tune
        OmniboxMode.QuickCatchInbox -> Icons.Outlined.MoveToInbox
        OmniboxMode.StartActivity -> Icons.Default.History
        OmniboxMode.AddActivityEvent -> Icons.Default.CheckCircle
    }

private fun modeTitle(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Пошук даних"
        OmniboxMode.Command -> "Команди"
        OmniboxMode.QuickCatchInbox -> "Quick catch to inbox"
        OmniboxMode.StartActivity -> "Start record activity"
        OmniboxMode.AddActivityEvent -> "Add tracker event"
    }

@Composable
private fun ModePill(
    mode: OmniboxMode,
    palette: OmniboxModePalette,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = palette.modeIconContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = modeIcon(mode),
                contentDescription = null,
                tint = palette.iconTint,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = modeTitle(mode),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private enum class OmniboxSelectionArea {
    None,
    Command,
    Data,
}

private fun placeholderForMode(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Пошук по контекстах, цілях, активностях і вкладеннях"
        OmniboxMode.Command -> "Команда або екран (fuzzy), напр. ctx, rem, ai"
        OmniboxMode.QuickCatchInbox -> "Швидкий запис в inbox..."
        OmniboxMode.StartActivity -> "Назва нової активності..."
        OmniboxMode.AddActivityEvent -> "Назва події трекера..."
    }


private fun submitDescriptionForMode(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Виконати пошук"
        OmniboxMode.Command -> "Виконати команду"
        OmniboxMode.QuickCatchInbox -> "Додати в inbox"
        OmniboxMode.StartActivity -> "Почати активність"
        OmniboxMode.AddActivityEvent -> "Додати подію"
    }

internal fun commandIcon(commandId: OmniboxCommandId): ImageVector =
    when (commandId) {
        OmniboxCommandId.OpenContexts -> Icons.Default.AccountTree
        OmniboxCommandId.OpenInbox -> Icons.Outlined.MoveToInbox
        OmniboxCommandId.OpenTracker -> Icons.Default.History
        OmniboxCommandId.OpenReminders -> Icons.Default.History
        OmniboxCommandId.OpenSettings -> Icons.Default.Tune
        OmniboxCommandId.OpenSearch -> Icons.Default.Search
        OmniboxCommandId.OpenAttachments -> Icons.Default.Description
        OmniboxCommandId.OpenScripts -> Icons.AutoMirrored.Filled.Sort
        OmniboxCommandId.OpenAiChat -> Icons.Default.Navigation
        OmniboxCommandId.OpenAiInsights -> Icons.Default.Navigation
        OmniboxCommandId.OpenAiLife -> Icons.Default.Navigation
        OmniboxCommandId.OpenStructurePresets -> Icons.Default.Tune
    }

internal fun commandTitle(commandId: OmniboxCommandId): String =
    when (commandId) {
        OmniboxCommandId.OpenContexts -> "Контексти"
        OmniboxCommandId.OpenInbox -> "Inbox"
        OmniboxCommandId.OpenTracker -> "Tracker"
        OmniboxCommandId.OpenReminders -> "Reminders"
        OmniboxCommandId.OpenSettings -> "Settings"
        OmniboxCommandId.OpenSearch -> "Search"
        OmniboxCommandId.OpenAttachments -> "Attachments"
        OmniboxCommandId.OpenScripts -> "Scripts"
        OmniboxCommandId.OpenAiChat -> "AI Chat"
        OmniboxCommandId.OpenAiInsights -> "AI Insights"
        OmniboxCommandId.OpenAiLife -> "AI Life"
        OmniboxCommandId.OpenStructurePresets -> "Presets"
    }

private data class OmniboxModePalette(
    val searchSurface: Color,
    val screenBottomTint: Color,
    val inputContainer: Color,
    val inputFocusedContainer: Color,
    val modeIconContainer: Color,
    val iconTint: Color,
)

@Composable
private fun rememberModePalette(mode: OmniboxMode): OmniboxModePalette {
    val scheme = MaterialTheme.colorScheme
    return remember(mode, scheme) {
        when (mode) {
            OmniboxMode.DataSearch ->
                OmniboxModePalette(
                    searchSurface = scheme.surfaceContainerLow,
                    screenBottomTint = scheme.background,
                    inputContainer = scheme.surfaceVariant.copy(alpha = 0.88f),
                    inputFocusedContainer = scheme.surfaceVariant,
                    modeIconContainer = scheme.primaryContainer.copy(alpha = 0.45f),
                    iconTint = scheme.primary.copy(alpha = 0.9f),
                )
            OmniboxMode.Command ->
                OmniboxModePalette(
                    searchSurface = scheme.secondaryContainer.copy(alpha = 0.22f),
                    screenBottomTint = scheme.secondaryContainer.copy(alpha = 0.10f),
                    inputContainer = scheme.secondaryContainer.copy(alpha = 0.28f),
                    inputFocusedContainer = scheme.secondaryContainer.copy(alpha = 0.36f),
                    modeIconContainer = scheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = scheme.secondary.copy(alpha = 0.92f),
                )
            OmniboxMode.QuickCatchInbox ->
                OmniboxModePalette(
                    searchSurface = scheme.tertiaryContainer.copy(alpha = 0.20f),
                    screenBottomTint = scheme.tertiaryContainer.copy(alpha = 0.10f),
                    inputContainer = scheme.tertiaryContainer.copy(alpha = 0.28f),
                    inputFocusedContainer = scheme.tertiaryContainer.copy(alpha = 0.36f),
                    modeIconContainer = scheme.tertiaryContainer.copy(alpha = 0.52f),
                    iconTint = scheme.tertiary.copy(alpha = 0.92f),
                )
            OmniboxMode.StartActivity ->
                OmniboxModePalette(
                    searchSurface = scheme.primaryContainer.copy(alpha = 0.20f),
                    screenBottomTint = scheme.primaryContainer.copy(alpha = 0.09f),
                    inputContainer = scheme.primaryContainer.copy(alpha = 0.28f),
                    inputFocusedContainer = scheme.primaryContainer.copy(alpha = 0.36f),
                    modeIconContainer = scheme.primaryContainer.copy(alpha = 0.54f),
                    iconTint = scheme.primary.copy(alpha = 0.9f),
                )
            OmniboxMode.AddActivityEvent ->
                OmniboxModePalette(
                    searchSurface = scheme.secondaryContainer.copy(alpha = 0.20f),
                    screenBottomTint = scheme.secondaryContainer.copy(alpha = 0.10f),
                    inputContainer = scheme.secondaryContainer.copy(alpha = 0.28f),
                    inputFocusedContainer = scheme.secondaryContainer.copy(alpha = 0.36f),
                    modeIconContainer = scheme.secondaryContainer.copy(alpha = 0.52f),
                    iconTint = scheme.secondary.copy(alpha = 0.92f),
                )
        }
    }
}
