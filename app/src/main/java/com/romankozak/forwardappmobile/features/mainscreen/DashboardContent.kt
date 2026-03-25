package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.ai.insights.AiInsightsViewModel
import com.romankozak.forwardappmobile.features.ai.insights.AiMessage
import com.romankozak.forwardappmobile.features.ai.insights.MessageType
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val DASHBOARD_INSIGHTS_LIMIT = 3
private const val FOCUS_CARD_BACKGROUND_ALPHA = 0.35f
private const val INSIGHT_DISMISS_BACKGROUND_ALPHA = 0.6f
private const val FOCUS_CONTEXTS_LIST_MAX_HEIGHT_DP = 420

@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    onOpenFocusedContext: (String) -> Unit = {},
    aiInsightsViewModel: AiInsightsViewModel = hiltViewModel(),
    focusContextsViewModel: FocusContextsViewModel = hiltViewModel(),
) {
    AnimatedCommandDeck(
        modifier = modifier,
        onOpenFocusedContext = onOpenFocusedContext,
        aiInsightsViewModel = aiInsightsViewModel,
        focusContextsViewModel = focusContextsViewModel,
    )
}

@Composable
private fun AnimatedCommandDeck(
    modifier: Modifier = Modifier,
    onOpenFocusedContext: (String) -> Unit,
    aiInsightsViewModel: AiInsightsViewModel,
    focusContextsViewModel: FocusContextsViewModel,
) {
    var aiInsightsExpanded by remember { mutableStateOf(false) }
    var focusContextsExpanded by remember { mutableStateOf(true) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(0.dp),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DashboardFocusContextsSection(
                isExpanded = focusContextsExpanded,
                onToggleExpanded = { focusContextsExpanded = !focusContextsExpanded },
                onOpenFocusedContext = onOpenFocusedContext,
                focusContextsViewModel = focusContextsViewModel,
            )

            DashboardAiInsightsSection(
                isExpanded = aiInsightsExpanded,
                onToggleExpanded = { aiInsightsExpanded = !aiInsightsExpanded },
                aiInsightsViewModel = aiInsightsViewModel,
            )
        }
    }
}

@Composable
private fun DashboardFocusContextsSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenFocusedContext: (String) -> Unit,
    focusContextsViewModel: FocusContextsViewModel,
) {
    val focusedContexts by focusContextsViewModel.focusedContexts.collectAsStateWithLifecycle()

    SectionHeader(
        title = "Фокус-контексти",
        subtitle =
            if (isExpanded) {
                if (focusedContexts.isEmpty()) "Поки порожньо" else "${focusedContexts.size} активних"
            } else {
                "Згорнуто"
            },
        isExpanded = isExpanded,
        onClick = onToggleExpanded,
    )
    if (!isExpanded) {
        return
    }

    if (focusedContexts.isEmpty()) {
        MetricCard(
            title = "Немає фокус-контекстів",
            value = "Додай контексти у фокус",
            subtitle = "через меню в ієрархії або з екрана контексту",
        )
        return
    }

    FocusContextsReorderableList(
        focusedContexts = focusedContexts,
        onOpenFocusedContext = onOpenFocusedContext,
        onStartTracking = focusContextsViewModel::startTracking,
        onDefocus = focusContextsViewModel::unfocus,
        onReorder = focusContextsViewModel::updateFocusedContextsOrder,
    )
}

@Composable
private fun DashboardAiInsightsSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    aiInsightsViewModel: AiInsightsViewModel,
) {
    var dismissedInsightIds by remember { mutableStateOf(emptySet<String>()) }
    val aiInsights by aiInsightsViewModel.messages.collectAsStateWithLifecycle()
    val latestInsights =
        remember(aiInsights, dismissedInsightIds) {
            aiInsights
                .filterNot { it.isRead || dismissedInsightIds.contains(it.id) }
                .take(DASHBOARD_INSIGHTS_LIMIT)
        }

    SectionHeader(
        title = "AI Insights",
        subtitle = if (isExpanded) "Останні інсайти" else "Згорнуто",
        isExpanded = isExpanded,
        onClick = onToggleExpanded,
    )
    if (!isExpanded) {
        return
    }

    if (latestInsights.isNotEmpty()) {
        latestInsights.forEach { insight ->
            AiInsightCard(
                insight = insight,
                onMarkRead = {
                    dismissedInsightIds = dismissedInsightIds + insight.id
                    aiInsightsViewModel.markRead(insight.id)
                },
            )
        }
    } else {
        MetricCard(
            title = "Немає інсайтів",
            value = "Поки що немає аналітики",
            subtitle = "AI ще не згенерував інсайти",
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FocusContextsReorderableList(
    focusedContexts: List<FocusContextsViewModel.FocusedContextUi>,
    onOpenFocusedContext: (String) -> Unit,
    onStartTracking: (String) -> Unit,
    onDefocus: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val internalItems = remember { mutableStateListOf<FocusContextsViewModel.FocusedContextUi>() }
    LaunchedEffect(focusedContexts) {
        internalItems.clear()
        internalItems.addAll(focusedContexts)
    }
    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalItems.add(to.index, internalItems.removeAt(from.index))
            onReorder(internalItems.map { it.contextId })
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.heightIn(max = FOCUS_CONTEXTS_LIST_MAX_HEIGHT_DP.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(internalItems, key = { it.contextId }) { focusedContext ->
            ReorderableItem(reorderableState, key = focusedContext.contextId) {
                FocusContextCard(
                    name = focusedContext.name,
                    onOpen = { onOpenFocusedContext(focusedContext.contextId) },
                    onStartTracking = { onStartTracking(focusedContext.contextId) },
                    onDefocus = { onDefocus(focusedContext.contextId) },
                    dragHandleModifier =
                        with(this@ReorderableItem) {
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                )
                        },
                )
            }
        }
    }
}

@Composable
private fun FocusContextCard(
    name: String,
    onOpen: () -> Unit,
    onStartTracking: () -> Unit,
    onDefocus: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = FOCUS_CARD_BACKGROUND_ALPHA),
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier =
                        dragHandleModifier
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.heightIn(min = 20.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = onStartTracking) {
                    Icon(Icons.Outlined.PlayCircle, contentDescription = "Start tracking")
                }
                FilledTonalIconButton(onClick = onDefocus) {
                    Icon(Icons.Outlined.VisibilityOff, contentDescription = "Зняти фокус")
                }
            }
        }
    }
}


@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiInsightCard(
    insight: AiMessage,
    onMarkRead: () -> Unit,
) {
    val insightColors = insightColors(insight.type)
    val timeFormatter = rememberInsightTimeFormatter()
    val dismissState = rememberInsightDismissState(onMarkRead)

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = INSIGHT_DISMISS_BACKGROUND_ALPHA),
                            RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Позначити прочитаним",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = insightColors.backgroundColor),
        ) {
            AiInsightContent(
                insight = insight,
                textColor = insightColors.textColor,
                timeFormatter = timeFormatter,
            )
        }
    }
}

@Composable
private fun AiInsightContent(
    insight: AiMessage,
    textColor: androidx.compose.ui.graphics.Color,
    timeFormatter: java.text.SimpleDateFormat,
) {
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = insight.text,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (insight.isRead) FontWeight.Normal else FontWeight.Bold,
        )
        Text(
            text = formatInsightMeta(insight, timeFormatter),
            color = textColor.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun rememberInsightTimeFormatter(): java.text.SimpleDateFormat =
    remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberInsightDismissState(
    onMarkRead: () -> Unit,
) = rememberSwipeToDismissBoxState(
    confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
            onMarkRead()
            true
        } else {
            false
        }
    },
)

private data class InsightColors(
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun insightColors(type: MessageType): InsightColors =
    when (type) {
        MessageType.MOTIVATION ->
            InsightColors(
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        MessageType.JOKE ->
            InsightColors(
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        MessageType.INFO ->
            InsightColors(
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                textColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        MessageType.WARNING,
        MessageType.ERROR ->
            InsightColors(
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                textColor = MaterialTheme.colorScheme.onErrorContainer,
            )
    }

private fun formatInsightMeta(
    insight: AiMessage,
    timeFormatter: java.text.SimpleDateFormat,
): String =
    "${insight.type.name.lowercase().replaceFirstChar { it.uppercase() }} • " +
        timeFormatter.format(java.util.Date(insight.timestamp))
