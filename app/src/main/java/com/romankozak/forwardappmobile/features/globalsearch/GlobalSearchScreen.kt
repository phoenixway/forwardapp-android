package com.romankozak.forwardappmobile.features.globalsearch

import android.content.res.Configuration
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
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
                viewModel.navigateToProjectForResult(result.searchResult.contextId, result.searchResult.contextName)
            }
            is GlobalSearchResultItem.SubcontextItem -> {
                viewModel.onDataResultOpened(result.uniqueId)
                viewModel.navigateToProjectForResult(result.searchResult.subcontext.id, result.searchResult.subcontext.name)
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
                        visible = !uiState.isLoading && when (currentMode) {
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
                OmniboxMode.QuickCatchInbox, OmniboxMode.StartActivity -> 116.dp
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
                                    query = uiState.query,
                                    commandResults = uiState.hybridCommandResults,
                                    selectedCommandIndex = selectedCommandIndex,
                                    accentColor = modePalette.iconTint,
                                    onCommandClick = viewModel::onCommandClick,
                                    onQuickCatch = viewModel::quickCatchCurrentQuery,
                                    onStartActivity = viewModel::startActivityFromCurrentQuery,
                                    onRunBestCommand = viewModel::runBestCommandForCurrentQuery,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            else -> {
                                SearchResultsContent(
                                    commandResults = uiState.hybridCommandResults,
                                    selectedCommandIndex = selectedCommandIndex.takeIf { selectionArea == OmniboxSelectionArea.Command },
                                    onCommandClick = viewModel::onCommandClick,
                                    accentColor = modePalette.iconTint,
                                    results = filteredResults,
                                    query = uiState.query,
                                    viewModel = viewModel,
                                    obsidianVaultName = obsidianVaultName,
                                    context = context,
                                    listState = listState,
                                    selectedResultUniqueId = selectedDataResultUniqueId,
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
                            results = uiState.commandResults,
                            query = uiState.query,
                            recentCommands = uiState.recentCommands,
                            selectedCommandIndex = selectedCommandIndex,
                            onCommandClick = viewModel::onCommandClick,
                            accentColor = modePalette.iconTint,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    OmniboxMode.QuickCatchInbox -> {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                    OmniboxMode.StartActivity -> {
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
                    val modeHint = inputHintForMode(currentMode)
                    if (modeHint.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
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

                    TextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
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
                                                    selectedCommandIndex = (current + 1).coerceAtMost(commands.lastIndex)
                                                    true
                                                }
                                                OmniboxMode.DataSearch -> {
                                                    val commandSize = uiState.hybridCommandResults.size
                                                    val dataSize = filteredResults.size
                                                    if (commandSize == 0 && dataSize == 0) return@onPreviewKeyEvent false
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
                                                    if (commandSize == 0 && dataSize == 0) return@onPreviewKeyEvent false
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
                                                    true
                                                }
                                                OmniboxMode.DataSearch -> {
                                                    when (selectionArea) {
                                                        OmniboxSelectionArea.Command -> {
                                                            viewModel.onSubmitSearch(selectedCommandIndex)
                                                            true
                                                        }
                                                        OmniboxSelectionArea.Data -> {
                                                            val result = selectedDataIndex?.let { filteredResults.getOrNull(it) }
                                                            if (result != null) {
                                                                openDataResultPrimary(result)
                                                                true
                                                            } else {
                                                                false
                                                            }
                                                        }
                                                        OmniboxSelectionArea.None -> {
                                                            viewModel.onSubmitSearch(selectedCommandIndex)
                                                            true
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    viewModel.onSubmitSearch()
                                                    true
                                                }
                                            }
                                        }
                                        else -> false
                                    }
                                },
                        singleLine = true,
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
                                IconButton(onClick = { viewModel.onSubmitSearch(submitCommandIndex) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = submitDescriptionForMode(currentMode),
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.onSubmitSearch(submitCommandIndex) }),
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

@Composable
private fun CommandResultsContent(
    results: List<OmniboxCommandResult>,
    query: String,
    recentCommands: List<OmniboxCommandId>,
    selectedCommandIndex: Int?,
    onCommandClick: (OmniboxCommandId) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    if (results.isEmpty()) {
        EmptySearchContent(query = query, modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (query.isBlank() && recentCommands.isNotEmpty()) {
            item("recent_commands_label") {
                Text(
                    text = "Нещодавні команди",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item("recent_commands_chips") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recentCommands.take(6).forEach { commandId ->
                        AssistChip(
                            onClick = { onCommandClick(commandId) },
                            label = { Text(commandTitle(commandId)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = commandIcon(commandId),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
        itemsIndexed(
            items = results,
            key = { _, item -> item.id.name },
        ) { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCommandClick(item.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border =
                    androidx.compose.foundation.BorderStroke(
                        if (selectedCommandIndex == index) 1.4.dp else 1.dp,
                        if (selectedCommandIndex == index) {
                            accentColor.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        },
                    ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = commandIcon(item.id),
                        contentDescription = null,
                        tint = accentColor,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedText(
                            text = item.title,
                            query = query,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun HybridCommandSection(
    results: List<OmniboxCommandResult>,
    selectedCommandIndex: Int?,
    accentColor: Color,
    onCommandClick: (OmniboxCommandId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Команди",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        results.forEachIndexed { index, command ->
            Surface(
                onClick = { onCommandClick(command.id) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedCommandIndex == index) accentColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(commandIcon(command.id), contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(command.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            command.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandSearchResultCard(
    command: OmniboxCommandResult,
    query: String,
    isSelected: Boolean,
    accentColor: Color,
    onCommandClick: (OmniboxCommandId) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCommandClick(command.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border =
            androidx.compose.foundation.BorderStroke(
                if (isSelected) 1.35.dp else 1.dp,
                if (isSelected) accentColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = commandIcon(command.id),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(
                    text = command.title,
                    query = query,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = command.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyDataSearchContent(
    query: String,
    commandResults: List<OmniboxCommandResult>,
    selectedCommandIndex: Int?,
    accentColor: Color,
    onCommandClick: (OmniboxCommandId) -> Unit,
    onQuickCatch: () -> Unit,
    onStartActivity: () -> Unit,
    onRunBestCommand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (commandResults.isNotEmpty()) {
            HybridCommandSection(
                results = commandResults,
                selectedCommandIndex = selectedCommandIndex,
                accentColor = accentColor,
                onCommandClick = onCommandClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        EmptySearchContent(query = query, modifier = Modifier.weight(1f).fillMaxWidth())
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Швидкі дії для \"$query\"",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onQuickCatch, modifier = Modifier.weight(1f)) {
                        Text("В inbox")
                    }
                    FilledTonalButton(onClick = onStartActivity, modifier = Modifier.weight(1f)) {
                        Text("В activity")
                    }
                }
                TextButton(onClick = onRunBestCommand, modifier = Modifier.fillMaxWidth()) {
                    Text("Виконати найкращу команду")
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
    }

private fun modeTitle(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Пошук даних"
        OmniboxMode.Command -> "Команди"
        OmniboxMode.QuickCatchInbox -> "Quick catch to inbox"
        OmniboxMode.StartActivity -> "Start record activity"
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
    }

private fun inputHintForMode(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> ""
        OmniboxMode.Command -> "Введи команду. ↑/↓ переміщення, Enter виконує команду"
        OmniboxMode.QuickCatchInbox -> "Введи текст і натисни пошук/Enter. Запис одразу додасться в inbox"
        OmniboxMode.StartActivity -> "Введи назву активності і натисни пошук/Enter. Буде створено новий запис"
    }

private fun submitDescriptionForMode(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.DataSearch -> "Виконати пошук"
        OmniboxMode.Command -> "Виконати команду"
        OmniboxMode.QuickCatchInbox -> "Додати в inbox"
        OmniboxMode.StartActivity -> "Почати активність"
    }

private fun commandIcon(commandId: OmniboxCommandId): ImageVector =
    when (commandId) {
        OmniboxCommandId.OpenContexts -> Icons.Default.AccountTree
        OmniboxCommandId.OpenInbox -> Icons.Outlined.MoveToInbox
        OmniboxCommandId.OpenTracker -> Icons.Default.History
        OmniboxCommandId.OpenReminders -> Icons.Default.History
        OmniboxCommandId.OpenSettings -> Icons.Default.Tune
        OmniboxCommandId.OpenSearch -> Icons.Default.Search
        OmniboxCommandId.OpenAttachments -> Icons.Default.Description
        OmniboxCommandId.OpenScripts -> Icons.Default.Sort
        OmniboxCommandId.OpenAiChat -> Icons.Default.Navigation
        OmniboxCommandId.OpenAiInsights -> Icons.Default.Navigation
        OmniboxCommandId.OpenAiLife -> Icons.Default.Navigation
        OmniboxCommandId.OpenStructurePresets -> Icons.Default.Tune
    }

private fun commandTitle(commandId: OmniboxCommandId): String =
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
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Пошук...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DataSearchLoadingContent(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "search_skeleton")
    val pulse by shimmer.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.56f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "search_skeleton_alpha",
    )
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) { idx ->
            Surface(
                modifier = Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = pulse - (idx * 0.03f)),
                tonalElevation = 0.dp,
            ) {}
        }
    }
}

@Composable
private fun EmptySearchContent(
    query: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ),
                                ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = "Нічого не знайдено",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = if (query.isBlank()) "Введіть запит" else "Нічого не знайдено",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = if (query.isBlank()) {
                    "Search everywhere шукає по контекстах, цілях, активностях, інбоксу та вкладеннях."
                } else {
                    "За запитом \"$query\" результатів не знайдено.\nСпробуйте змінити пошуковий запит."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchStartContent(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Історія пошуку",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onClearHistory) {
                    Text("Очистити все")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                history.forEach { query ->
                    InputChip(
                        selected = false,
                        onClick = { onHistoryClick(query) },
                        label = { Text(query) },
                        trailingIcon = {
                            IconButton(
                                onClick = { onRemoveHistoryEntry(query) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Видалити з історії",
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSelectorChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        if (isActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataActionsBottomSheet(
    onSelectTypes: () -> Unit,
    onSelectSorting: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Додаткові дії пошуку",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Типи результатів") },
                supportingContent = { Text("Обрати, які типи даних показувати") },
                leadingContent = { Icon(Icons.Default.Tune, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onSelectTypes() },
            )
            ListItem(
                headlineContent = { Text("Сортування") },
                supportingContent = { Text("Змінити порядок відображення результатів") },
                leadingContent = { Icon(Icons.Default.Sort, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onSelectSorting() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeBottomSheet(
    options: List<GlobalSearchType>,
    selected: Set<GlobalSearchType>,
    onApply: (Set<GlobalSearchType>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Типи результатів",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            ListItem(
                headlineContent = { Text("Усі типи") },
                leadingContent = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingContent = {
                    if (draft.size == options.size) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { draft = options.toSet() },
            )
            options.forEach { type ->
                ListItem(
                    headlineContent = { Text(type.label) },
                    leadingContent = { Icon(type.icon, contentDescription = null) },
                    trailingContent = {
                        if (type in draft) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .clickable {
                                draft =
                                    if (type in draft) {
                                        draft - type
                                    } else {
                                        draft + type
                                    }
                            },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("Скасувати") }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f),
                ) { Text("Застосувати") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    selectedSort: GlobalSearchSort,
    onSortSelected: (GlobalSearchSort) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Сортування",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            GlobalSearchSort.entries.forEach { sort ->
                ListItem(
                    headlineContent = { Text(sort.label) },
                    leadingContent = { Icon(Icons.Default.Sort, contentDescription = null) },
                    trailingContent = {
                        if (selectedSort == sort) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onSortSelected(sort) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SearchResultsContent(
    commandResults: List<OmniboxCommandResult>,
    selectedCommandIndex: Int?,
    onCommandClick: (OmniboxCommandId) -> Unit,
    accentColor: Color,
    results: List<GlobalSearchResultItem>,
    query: String,
    viewModel: GlobalSearchViewModel,
    obsidianVaultName: String,
    context: Context,
    listState: LazyListState,
    selectedResultUniqueId: String?,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val formatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val groupedResults = remember(results) {
        results
            .groupBy { it.groupKey() }
            .map { (key, items) ->
                val presentation = items.first().typePresentation()
                ResultGroup(key = key, presentation = presentation, items = items)
            }
    }
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }
    var commandsExpanded by remember(query) { mutableStateOf(true) }

    LaunchedEffect(groupedResults) {
        val activeKeys = groupedResults.map { it.key }.toSet()
        groupedResults.forEach { group ->
            if (groupExpandedState[group.key] == null) {
                groupExpandedState[group.key] = true
            }
        }
        groupExpandedState.keys.toList().forEach { key ->
            if (key !in activeKeys) {
                groupExpandedState.remove(key)
            }
        }
    }
    LaunchedEffect(selectedResultUniqueId, groupedResults) {
        val selectedGroupKey =
            selectedResultUniqueId?.let { id ->
                groupedResults.firstOrNull { group -> group.items.any { it.uniqueId == id } }?.key
            }
        if (selectedGroupKey != null && groupExpandedState[selectedGroupKey] == false) {
            groupExpandedState[selectedGroupKey] = true
        }
    }

    val listItemIndexByResultId = mutableMapOf<String, Int>()
    var lazyIndex = 0
    if (commandResults.isNotEmpty()) {
        lazyIndex += 1 // commands header
        if (commandsExpanded) {
            lazyIndex += commandResults.size
        }
    }
    groupedResults.forEach { group ->
        lazyIndex += 1 // group header
        if (groupExpandedState[group.key] != false) {
            group.items.forEach { item ->
                listItemIndexByResultId[item.uniqueId] = lazyIndex
                lazyIndex += 1
            }
        }
    }
    LaunchedEffect(selectedResultUniqueId, listItemIndexByResultId) {
        val targetIndex = selectedResultUniqueId?.let { id -> listItemIndexByResultId[id] } ?: return@LaunchedEffect
        if (targetIndex > 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (commandResults.isNotEmpty()) {
                stickyHeader(key = "header_commands") {
                    SearchResultGroupHeader(
                        presentation =
                            ResultTypePresentation(
                                label = "Дії",
                                icon = Icons.Default.Tune,
                                tone = ResultBadgeTone.Surface,
                            ),
                        count = commandResults.size,
                        isExpanded = commandsExpanded,
                        onToggle = { commandsExpanded = !commandsExpanded },
                    )
                }
                if (commandsExpanded) {
                    itemsIndexed(
                        items = commandResults,
                        key = { _, item -> "command_${item.id.name}" },
                    ) { index, item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            CommandSearchResultCard(
                                command = item,
                                query = query,
                                isSelected = selectedCommandIndex == index,
                                accentColor = accentColor,
                                onCommandClick = onCommandClick,
                            )
                        }
                    }
                }
            }

            groupedResults.forEach { group ->
                val isExpanded = groupExpandedState[group.key] != false
                stickyHeader(key = "header_${group.key}") {
                    SearchResultGroupHeader(
                        presentation = group.presentation,
                        count = group.items.size,
                        isExpanded = isExpanded,
                        onToggle = { groupExpandedState[group.key] = !isExpanded },
                    )
                }

                if (isExpanded) {
                    itemsIndexed(
                        items = group.items,
                        key = { _, result -> result.uniqueId },
                    ) { index, result ->
                        AnimatedVisibility(
                            visible = true,
                            enter =
                                slideInVertically(
                                    animationSpec =
                                        spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 300f,
                                        ),
                                    initialOffsetY = { it / 2 },
                                ) +
                                    fadeIn(
                                        animationSpec =
                                            tween(
                                                durationMillis = 300,
                                                delayMillis = index * 35,
                                            ),
                                    ),
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                val typePresentation = result.typePresentation()
                                when (result) {
                        is GlobalSearchResultItem.GoalItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.goal.text,
                                subtitle = result.goal.description,
                                supporting = result.pathSegments.joinToString(" → ").ifBlank { result.projectName },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(result.backlogItem.contextId, result.projectName)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.goBackToRevealProject(result.backlogItem.contextId)
                                },
                            )
                        }
                        is GlobalSearchResultItem.LinkItem -> {
                            val searchResult = result.searchResult
                            val linkData = searchResult.link.linkData
                            val secondaryAction: (() -> Unit)? =
                                when (linkData.type) {
                                    LinkType.CONTEXT -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onDataResultOpened(result.uniqueId)
                                            viewModel.navigateToProjectForResult(linkData.target, null)
                                        }
                                    }
                                    LinkType.URL,
                                    LinkType.OBSIDIAN,
                                    -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.onDataResultOpened(result.uniqueId)
                                            handleRelatedLinkClick(
                                                link = linkData,
                                                obsidianVaultName = obsidianVaultName,
                                                context = context,
                                            )
                                        }
                                    }
                                    null -> null
                                }
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = linkData.displayName ?: linkData.target,
                                subtitle = linkData.target,
                                supporting = "Контекст: ${searchResult.contextName}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(searchResult.contextId, searchResult.contextName)
                                },
                                secondaryActionIcon = if (secondaryAction != null) Icons.AutoMirrored.Filled.OpenInNew else null,
                                secondaryActionDescription = "Додаткова дія",
                                onSecondaryAction = secondaryAction,
                            )
                        }
                        is GlobalSearchResultItem.SubcontextItem -> {
                            val subproject = result.searchResult.subcontext
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = subproject.name,
                                subtitle = "Батьківський контекст: ${result.searchResult.parentContextName}",
                                supporting = result.searchResult.pathSegments.joinToString(" → "),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(subproject.id, subproject.name)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.goBackToRevealProject(subproject.id)
                                },
                            )
                        }
                        is GlobalSearchResultItem.ContextItem -> {
                            val project = result.searchResult.context
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = project.name,
                                subtitle = project.description,
                                supporting = result.searchResult.pathSegments.joinToString(" → "),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(project.id, project.name)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.goBackToRevealProject(project.id)
                                },
                            )
                        }

                        is GlobalSearchResultItem.ActivityItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.record.text,
                                subtitle = result.record.noteText,
                                supporting = "Трекер: ${formatter.format(Date(result.record.createdAt))}",
                                onClick = {
                                    val contextId = result.record.contextId
                                    if (!contextId.isNullOrBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.onDataResultOpened(result.uniqueId)
                                        viewModel.navigateToProjectForResult(contextId, null)
                                    }
                                },
                                secondaryActionIcon = null,
                                secondaryActionDescription = null,
                                onSecondaryAction = null,
                            )
                        }
                        is GlobalSearchResultItem.InboxItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.record.text,
                                subtitle = null,
                                supporting = "Inbox: ${formatter.format(Date(result.record.createdAt))}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                                secondaryActionIcon = Icons.Default.ChevronRight,
                                secondaryActionDescription = "Відкрити контекст",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.onDataResultOpened(result.uniqueId)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                            )
                        }
                        is GlobalSearchResultItem.AttachmentItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                isSelected = result.uniqueId == selectedResultUniqueId,
                                query = query,
                                title = result.searchResult.title,
                                subtitle = result.searchResult.subtitle,
                                supporting = "Контекст: ${result.searchResult.contextName ?: "не вказано"}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val contextId = result.searchResult.ownerContextId
                                    if (!contextId.isNullOrBlank()) {
                                        viewModel.onDataResultOpened(result.uniqueId)
                                        viewModel.navigateToProjectForResult(contextId, result.searchResult.contextName)
                                    }
                                },
                                secondaryActionIcon = Icons.Default.ChevronRight,
                                secondaryActionDescription = "Відкрити контекст",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val contextId = result.searchResult.ownerContextId
                                    if (!contextId.isNullOrBlank()) {
                                        viewModel.onDataResultOpened(result.uniqueId)
                                        viewModel.navigateToProjectForResult(contextId, result.searchResult.contextName)
                                    }
                                },
                            )
                        }
                            }
                            ResultTypeBadge(
                                presentation = typePresentation,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 8.dp, end = 8.dp),
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(18.dp)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), Color.Transparent),
                            ),
                    ),
        )
    }
}

@Composable
private fun SearchResultGroupHeader(
    presentation: ResultTypePresentation,
    count: Int,
    isExpanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
) {
    val (container, content) = resultBadgeColors(presentation.tone)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onToggle != null) { onToggle?.invoke() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.label,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = container,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onToggle != null) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути секцію" else "Розгорнути секцію",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UnifiedSearchResultCard(
    presentation: ResultTypePresentation,
    isSelected: Boolean,
    query: String,
    title: String,
    subtitle: String?,
    supporting: String?,
    onClick: () -> Unit,
    secondaryActionIcon: ImageVector?,
    secondaryActionDescription: String?,
    onSecondaryAction: (() -> Unit)?,
) {
    val (badgeContainer, badgeContent) = resultBadgeColors(presentation.tone)
    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = tween(durationMillis = 170),
        label = "selected_result_scale",
    )

    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = selectedScale, scaleY = selectedScale),
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
        shape = RoundedCornerShape(16.dp),
        border =
            androidx.compose.foundation.BorderStroke(
                if (isSelected) 1.35.dp else 1.dp,
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = presentation.icon,
                    contentDescription = presentation.label,
                    tint = badgeContent,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(
                    text = title,
                    query = query,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HighlightedText(
                        text = subtitle,
                        query = query,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                if (!supporting.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HighlightedText(
                        text = supporting,
                        query = query,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }

            if (secondaryActionIcon != null && onSecondaryAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = secondaryActionIcon,
                        contentDescription = secondaryActionDescription ?: "Action",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    color: androidx.compose.ui.graphics.Color,
    maxLines: Int,
) {
    val annotated =
        remember(text, query) {
            if (query.isBlank()) return@remember buildAnnotatedString { append(text) }
            val loweredText = text.lowercase(Locale.getDefault())
            val loweredQuery = query.lowercase(Locale.getDefault())
            val start = loweredText.indexOf(loweredQuery)
            if (start < 0) {
                buildAnnotatedString { append(text) }
            } else {
                val end = start + query.length
                buildAnnotatedString {
                    append(text.substring(0, start))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(start, end))
                    }
                    append(text.substring(end))
                }
            }
        }

    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

private enum class GlobalSearchType(
    val label: String,
    val icon: ImageVector,
) {
    Attachments("Вкладення", Icons.Default.Description),
    Contexts("Контексти", Icons.Default.AccountTree),
    Goals("Цілі", Icons.Default.Flag),
    Links("Посилання", Icons.Default.Link),
    Activity("Активності", Icons.Default.History),
    Inbox("Inbox", Icons.Outlined.MoveToInbox),
    ;

    fun matches(item: GlobalSearchResultItem): Boolean =
        when (this) {
            Attachments -> item is GlobalSearchResultItem.AttachmentItem
            Contexts -> item is GlobalSearchResultItem.ContextItem || item is GlobalSearchResultItem.SubcontextItem
            Goals -> item is GlobalSearchResultItem.GoalItem
            Links -> item is GlobalSearchResultItem.LinkItem
            Activity -> item is GlobalSearchResultItem.ActivityItem
            Inbox -> item is GlobalSearchResultItem.InboxItem
        }
}

private fun formatTypeChipLabel(
    selected: Set<GlobalSearchType>,
    all: Set<GlobalSearchType>,
): String {
    if (selected.size == all.size) return "Типи: Усі"
    if (selected.size == 1) return "Типи: ${selected.first().label}"
    return "Типи: ${selected.size}"
}

private enum class GlobalSearchSort(val label: String) {
    Relevance("Релевантність"),
    Type("Тип"),
    Alphabetical("A-Z"),
}

private fun resultTitle(item: GlobalSearchResultItem): String =
    when (item) {
        is GlobalSearchResultItem.GoalItem -> item.goal.text
        is GlobalSearchResultItem.LinkItem -> item.searchResult.link.linkData.displayName ?: item.searchResult.link.linkData.target
        is GlobalSearchResultItem.SubcontextItem -> item.searchResult.subcontext.name
        is GlobalSearchResultItem.ContextItem -> item.searchResult.context.name
        is GlobalSearchResultItem.ActivityItem -> item.record.text
        is GlobalSearchResultItem.InboxItem -> item.record.text
        is GlobalSearchResultItem.AttachmentItem -> item.searchResult.title
    }

private data class ResultGroup(
    val key: String,
    val presentation: ResultTypePresentation,
    val items: List<GlobalSearchResultItem>,
)

private fun GlobalSearchResultItem.groupKey(): String =
    when (this) {
        is GlobalSearchResultItem.AttachmentItem -> "attachments"
        is GlobalSearchResultItem.ContextItem -> "contexts"
        is GlobalSearchResultItem.SubcontextItem -> "subcontexts"
        is GlobalSearchResultItem.GoalItem -> "goals"
        is GlobalSearchResultItem.LinkItem -> "links"
        is GlobalSearchResultItem.ActivityItem -> "activity"
        is GlobalSearchResultItem.InboxItem -> "inbox"
    }

private fun GlobalSearchResultItem.typePresentation(): ResultTypePresentation =
    when (this) {
        is GlobalSearchResultItem.AttachmentItem ->
            ResultTypePresentation(
                label = "Вкладення",
                icon = Icons.Default.Description,
                tone = ResultBadgeTone.Primary,
            )
        is GlobalSearchResultItem.ContextItem ->
            ResultTypePresentation(
                label = "Контекст",
                icon = Icons.Default.AccountTree,
                tone = ResultBadgeTone.Secondary,
            )
        is GlobalSearchResultItem.SubcontextItem ->
            ResultTypePresentation(
                label = "Підконтекст",
                icon = Icons.Default.AccountTree,
                tone = ResultBadgeTone.Secondary,
            )
        is GlobalSearchResultItem.GoalItem ->
            ResultTypePresentation(
                label = "Ціль",
                icon = Icons.Default.Flag,
                tone = ResultBadgeTone.Tertiary,
            )
        is GlobalSearchResultItem.LinkItem ->
            ResultTypePresentation(
                label = "Посилання",
                icon = Icons.Default.Link,
                tone = ResultBadgeTone.Tertiary,
            )
        is GlobalSearchResultItem.ActivityItem ->
            ResultTypePresentation(
                label = "Активність",
                icon = Icons.Default.History,
                tone = ResultBadgeTone.Surface,
            )
        is GlobalSearchResultItem.InboxItem ->
            ResultTypePresentation(
                label = "Inbox",
                icon = Icons.Outlined.MoveToInbox,
                tone = ResultBadgeTone.Surface,
            )
    }

@Composable
private fun ResultTypeBadge(
    presentation: ResultTypePresentation,
    modifier: Modifier = Modifier,
) {
    val (container, content) = resultBadgeColors(presentation.tone)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = container,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = presentation.label,
                tint = content,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = presentation.label,
                style = MaterialTheme.typography.labelSmall,
                color = content,
            )
        }
    }
}

@Composable
private fun resultBadgeColors(tone: ResultBadgeTone) =
    when (tone) {
        ResultBadgeTone.Primary -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ResultBadgeTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        ResultBadgeTone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        ResultBadgeTone.Surface ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) to MaterialTheme.colorScheme.onSurfaceVariant
    }

private data class ResultTypePresentation(
    val label: String,
    val icon: ImageVector,
    val tone: ResultBadgeTone,
)

private enum class ResultBadgeTone {
    Primary,
    Secondary,
    Tertiary,
    Surface,
}

@Composable
private fun ResultsCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
) {
    try {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, link.target.toUri())
                context.startActivity(intent)
            }
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    val intent = Intent(Intent.ACTION_VIEW, obsidianUri.toUri())
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Назву Obsidian сховища не встановлено.", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Не вдалося відкрити посилання.", Toast.LENGTH_LONG).show()
    }
}
