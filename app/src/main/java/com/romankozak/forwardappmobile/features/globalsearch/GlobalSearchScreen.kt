package com.romankozak.forwardappmobile.features.globalsearch

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalGoalSearchResult
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.features.globalsearch.components.InboxSearchResultItem
import com.romankozak.forwardappmobile.features.globalsearch.components.LinkSearchResultItem
import com.romankozak.forwardappmobile.features.globalsearch.components.ProjectSearchResultItem
import com.romankozak.forwardappmobile.features.globalsearch.components.SearchResultItem
import com.romankozak.forwardappmobile.features.globalsearch.components.SubprojectSearchResultItem
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SEARCH_DEBUG"

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
    val filters = remember { GlobalSearchFilter.values().toList() }
    var selectedFilter by remember { mutableStateOf(GlobalSearchFilter.All) }
    val filteredResults by remember(uiState.results, selectedFilter) {
        derivedStateOf { uiState.results.filter { selectedFilter.matches(it) } }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Search everywhere",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (uiState.query.isNotBlank()) {
                            Text(
                                text = "\"${uiState.query}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Введіть запит для пошуку по контекстах і вкладеннях") },
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
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when {
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

@Composable
private fun SearchResultsContent(
    results: List<GlobalSearchResultItem>,
    viewModel: GlobalSearchViewModel,
    obsidianVaultName: String,
    context: Context,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        itemsIndexed(
            items = results,
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
                                    delayMillis = index * 40,
                                ),
                        ),
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    when (result) {
                        is GlobalSearchResultItem.GoalItem -> {
                            val searchResult =
                                GlobalGoalSearchResult(
                                    goal = result.goal,
                                    contextId = result.backlogItem.contextId,
                                    contextName = result.projectName,
                                    pathSegments = result.pathSegments,
                                )
                            SearchResultItem(
                                result = searchResult,
                                onOpenAsProject = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(searchResult.contextId, searchResult.contextName)
                                },
                                onOpenInNavigation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.goBackToRevealProject(searchResult.contextId)
                                },
                            )
                        }
                        is GlobalSearchResultItem.LinkItem -> {
                            val searchResult = result.searchResult
                            val linkData = searchResult.link.linkData
                            LinkSearchResultItem(
                                result = searchResult,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(searchResult.contextId, searchResult.contextName)
                                },
                                onGoToTargetProject = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(linkData.target, null)
                                },
                                onOpenInObsidian = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    handleRelatedLinkClick(
                                        link = linkData,
                                        obsidianVaultName = obsidianVaultName,
                                        context = context,
                                    )
                                },
                                onOpenUrl = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    handleRelatedLinkClick(
                                        link = linkData,
                                        obsidianVaultName = obsidianVaultName,
                                        context = context,
                                    )
                                },
                            )
                        }
                        is GlobalSearchResultItem.SubcontextItem -> {
                            val subproject = result.searchResult.subcontext
                            SubprojectSearchResultItem(
                                result = result.searchResult,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(subproject.id, subproject.name)
                                },
                                onOpenInNavigation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.goBackToRevealProject(subproject.id)
                                },
                            )
                        }
                        is GlobalSearchResultItem.ContextItem -> {
                            val project = result.searchResult.context
                            ProjectSearchResultItem(
                                result = result.searchResult,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(project.id, project.name)
                                },
                                onOpenInNavigation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.goBackToRevealProject(project.id)
                                },
                            )
                        }

                        is GlobalSearchResultItem.ActivityItem -> {
                            ActivitySearchResultItem(record = result.record)
                        }
                        is GlobalSearchResultItem.InboxItem -> {
                            InboxSearchResultItem(
                                record = result.record,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.navigateToProjectForResult(result.record.contextId, null)
                                },
                            )
                        }
                        is GlobalSearchResultItem.AttachmentItem -> {
                            AttachmentSearchResultItem(
                                searchResult = result.searchResult,
                                onClick = {
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
                        presentation = result.typePresentation(),
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private enum class GlobalSearchFilter(val label: String) {
    All("Всі"),
    Attachments("Вкладення"),
    Contexts("Контексти"),
    Goals("Цілі"),
    Links("Посилання"),
    Activity("Активності"),
    Inbox("Inbox"),
    ;

    fun matches(item: GlobalSearchResultItem): Boolean =
        when (this) {
            All -> true
            Attachments -> item is GlobalSearchResultItem.AttachmentItem
            Contexts -> item is GlobalSearchResultItem.ContextItem || item is GlobalSearchResultItem.SubcontextItem
            Goals -> item is GlobalSearchResultItem.GoalItem
            Links -> item is GlobalSearchResultItem.LinkItem
            Activity -> item is GlobalSearchResultItem.ActivityItem
            Inbox -> item is GlobalSearchResultItem.InboxItem
        }
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
    val (container, content) =
        when (presentation.tone) {
            ResultBadgeTone.Primary -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            ResultBadgeTone.Secondary -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            ResultBadgeTone.Tertiary -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            ResultBadgeTone.Surface ->
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) to MaterialTheme.colorScheme.onSurfaceVariant
        }

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

@Composable
private fun ActivitySearchResultItem(record: ActivityRecord) {
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Запис трекера",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.text,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Запис трекера від ${formatter.format(Date(record.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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

@Composable
private fun AttachmentSearchResultItem(
    searchResult: com.romankozak.forwardappmobile.core.data.models.entities.GlobalAttachmentSearchResult,
    onClick: () -> Unit,
) {
    val subtitle = searchResult.subtitle

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = searchResult.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!searchResult.contextName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Контекст: ${searchResult.contextName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
