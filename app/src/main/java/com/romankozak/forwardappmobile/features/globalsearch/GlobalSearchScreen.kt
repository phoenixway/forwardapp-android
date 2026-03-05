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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Пошук",
                        style = MaterialTheme.typography.titleMedium,
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
                        visible = !uiState.isLoading && filteredResults.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(delayMillis = 200)) + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        ResultsCountBadge(
                            count = filteredResults.size,
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.background,
                                    ),
                                startY = 0.1f,
                            ),
                    ).padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    TextField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .focusRequester(focusRequester),
                        singleLine = true,
                        placeholder = { Text("Пошук по контекстах, цілях, активностях і вкладеннях") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                                IconButton(onClick = viewModel::onSubmitSearch) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Запустити пошук",
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.onSubmitSearch() }),
                        shape = RoundedCornerShape(14.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterSelectorChip(
                            icon = Icons.Default.Tune,
                            label = formatTypeChipLabel(selectedTypes, typeOptions.toSet()),
                            isActive = selectedTypes.size != typeOptions.size,
                            onClick = { showTypeSheet = true },
                            modifier = Modifier.weight(1f),
                        )

                        FilterSelectorChip(
                            icon = Icons.Default.Sort,
                            label = selectedSort.label,
                            isActive = selectedSort != GlobalSearchSort.Relevance,
                            onClick = { showSortSheet = true },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxSize()) {
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
                        LoadingContent(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(scaleX = loadingScale, scaleY = loadingScale),
                        )
                    }
                    filteredResults.isEmpty() -> {
                        EmptySearchContent(
                            query = uiState.query,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    else -> {
                        SearchResultsContent(
                            results = filteredResults,
                            query = uiState.query,
                            viewModel = viewModel,
                            obsidianVaultName = obsidianVaultName,
                            context = context,
                            listState = listState,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(alpha = resultsAlpha),
                        )
                    }
                }
            }
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Пошук по всьому застосунку",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Контексти, цілі, активності, inbox, вкладення та посилання.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
    results: List<GlobalSearchResultItem>,
    query: String,
    viewModel: GlobalSearchViewModel,
    obsidianVaultName: String,
    context: Context,
    listState: LazyListState,
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

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        groupedResults.forEach { group ->
            stickyHeader(key = "header_${group.key}") {
                SearchResultGroupHeader(
                    presentation = group.presentation,
                    count = group.items.size,
                )
            }

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
                                query = query,
                                title = result.goal.text,
                                subtitle = result.goal.description,
                                supporting = result.pathSegments.joinToString(" → ").ifBlank { result.projectName },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(result.backlogItem.contextId, result.projectName)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                            viewModel.navigateToProjectForResult(linkData.target, null)
                                        }
                                    }
                                    LinkType.URL,
                                    LinkType.OBSIDIAN,
                                    -> {
                                        {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                query = query,
                                title = linkData.displayName ?: linkData.target,
                                subtitle = linkData.target,
                                supporting = "Контекст: ${searchResult.contextName}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                query = query,
                                title = subproject.name,
                                subtitle = "Батьківський контекст: ${result.searchResult.parentContextName}",
                                supporting = result.searchResult.pathSegments.joinToString(" → "),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(subproject.id, subproject.name)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.goBackToRevealProject(subproject.id)
                                },
                            )
                        }
                        is GlobalSearchResultItem.ContextItem -> {
                            val project = result.searchResult.context
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                query = query,
                                title = project.name,
                                subtitle = project.description,
                                supporting = result.searchResult.pathSegments.joinToString(" → "),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(project.id, project.name)
                                },
                                secondaryActionIcon = Icons.Default.Navigation,
                                secondaryActionDescription = "Відкрити в навігації",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.goBackToRevealProject(project.id)
                                },
                            )
                        }

                        is GlobalSearchResultItem.ActivityItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                query = query,
                                title = result.record.text,
                                subtitle = result.record.noteText,
                                supporting = "Трекер: ${formatter.format(Date(result.record.createdAt))}",
                                onClick = {
                                    val contextId = result.record.contextId
                                    if (!contextId.isNullOrBlank()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                query = query,
                                title = result.record.text,
                                subtitle = null,
                                supporting = "Inbox: ${formatter.format(Date(result.record.createdAt))}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                                secondaryActionIcon = Icons.Default.ChevronRight,
                                secondaryActionDescription = "Відкрити контекст",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                            )
                        }
                        is GlobalSearchResultItem.AttachmentItem -> {
                            UnifiedSearchResultCard(
                                presentation = typePresentation,
                                query = query,
                                title = result.searchResult.title,
                                subtitle = result.searchResult.subtitle,
                                supporting = "Контекст: ${result.searchResult.contextName ?: "не вказано"}",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val contextId = result.searchResult.ownerContextId
                                    if (!contextId.isNullOrBlank()) {
                                        viewModel.navigateToProjectForResult(contextId, result.searchResult.contextName)
                                    }
                                },
                                secondaryActionIcon = Icons.Default.ChevronRight,
                                secondaryActionDescription = "Відкрити контекст",
                                onSecondaryAction = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val contextId = result.searchResult.ownerContextId
                                    if (!contextId.isNullOrBlank()) {
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
}

@Composable
private fun SearchResultGroupHeader(
    presentation: ResultTypePresentation,
    count: Int,
) {
    val (container, content) = resultBadgeColors(presentation.tone)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
        }
    }
}

@Composable
private fun UnifiedSearchResultCard(
    presentation: ResultTypePresentation,
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
