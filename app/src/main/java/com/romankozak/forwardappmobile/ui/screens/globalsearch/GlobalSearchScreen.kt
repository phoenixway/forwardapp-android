package com.romankozak.forwardappmobile.ui.screens.globalsearch

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.GlobalSearchResultItem
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.LinkSearchResultItem
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.ProjectSearchResultItem
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.SubprojectSearchResultItem
import kotlinx.coroutines.launch
import java.net.URLEncoder
import com.romankozak.forwardappmobile.ui.screens.globalsearch.components.SearchResultItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SEARCH_DEBUG"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    navController: NavController,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val query = viewModel.query
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTopButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Результати пошуку",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "\"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = !uiState.isLoading && uiState.results.isNotEmpty(),
                        enter = fadeIn(animationSpec = tween(delayMillis = 200)) + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        ResultsCountBadge(
                            count = uiState.results.size,
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
                                        MaterialTheme.colorScheme.background,
                                    ),
                                startY = 0.1f,
                            ),
                    ).padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    LoadingContent(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = loadingScale, scaleY = loadingScale),
                    )
                }
                uiState.results.isEmpty() -> {
                    EmptySearchContent(
                        query = query,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    SearchResultsContent(
                        results = uiState.results,
                        navController = navController,
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
                text = "Нічого не знайдено",
                style =
                    MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "За запитом \"$query\" результатів не знайдено.\nСпробуйте змінити пошуковий запит.",
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
    navController: NavController,
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
                            SearchResultItem(
                                result = result.searchResult,
                                onOpenAsProject = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val route =
                                        "project_detail_screen/${result.searchResult.projectId}?goalId=${result.searchResult.goal.id}"
                                    navController.navigate(route)
                                },
                                onOpenInNavigation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("project_to_reveal", result.searchResult.projectId)
                                    navController.popBackStack()
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
                                    val route = "project_detail_screen/${searchResult.projectId}?itemIdToHighlight=${searchResult.listItemId}"
                                    navController.navigate(route)
                                },
                                onGoToTargetProject = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val route = "project_detail_screen/${linkData.target}"
                                    navController.navigate(route)
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
                        is GlobalSearchResultItem.SublistItem -> {
                            SubprojectSearchResultItem(
                                result = result.searchResult,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.navigate("project_detail_screen/${result.searchResult.parentProjectId}")
                                },
                                onOpenInNavigation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("project_to_reveal", result.searchResult.subproject.id)
                                    navController.popBackStack()
                                },
                            )
                        }
                        is GlobalSearchResultItem.ProjectItem -> {
                            ProjectSearchResultItem(
                                result = result.searchResult,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.navigate("project_detail_screen/${result.searchResult.project.id}")
                                },
                                onOpenInNavigation = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("project_to_reveal", result.searchResult.project.id)
                                    navController.popBackStack()
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
                                    val route = "project_detail_screen/${result.record.projectId}?inboxRecordIdToHighlight=${result.record.id}"
                                    Log.d(TAG, "Navigating to Inbox. Route: $route")

                                    navController.navigate(route)
                                },
                            )
                        }
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