package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.ai.insights.AiInsightsViewModel
import com.romankozak.forwardappmobile.features.ai.insights.AiMessage
import com.romankozak.forwardappmobile.features.ai.insights.MessageType

@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    onOpenFocusedContext: (String) -> Unit = {},
) {
    AnimatedCommandDeck(
        modifier = modifier,
        onOpenFocusedContext = onOpenFocusedContext,
    )
}

@Composable
private fun AnimatedCommandDeck(
    modifier: Modifier = Modifier,
    onOpenFocusedContext: (String) -> Unit,
) {
    var aiInsightsExpanded by remember { mutableStateOf(false) }
    var focusContextsExpanded by remember { mutableStateOf(true) }
    var dismissedInsightIds by remember { mutableStateOf(emptySet<String>()) }

    val aiInsightsViewModel: AiInsightsViewModel = hiltViewModel()
    val focusContextsViewModel: FocusContextsViewModel = hiltViewModel()
    val aiInsights by aiInsightsViewModel.messages.collectAsStateWithLifecycle()
    val focusedContexts by focusContextsViewModel.focusedContexts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            SectionHeader(
                title = "Фокус-контексти",
                subtitle =
                    if (focusContextsExpanded) {
                        if (focusedContexts.isEmpty()) "Поки порожньо" else "${focusedContexts.size} активних"
                    } else {
                        "Згорнуто"
                    },
                isExpanded = focusContextsExpanded,
                onClick = { focusContextsExpanded = !focusContextsExpanded },
            )
        }

        if (focusContextsExpanded) {
            if (focusedContexts.isEmpty()) {
                item {
                    MetricCard(
                        title = "Немає фокус-контекстів",
                        value = "Додай контексти у фокус",
                        subtitle = "через меню в ієрархії або з екрана контексту",
                    )
                }
            } else {
                focusedContexts.forEach { focusedContext ->
                    item {
                        FocusContextCard(
                            name = focusedContext.name,
                            onOpen = { onOpenFocusedContext(focusedContext.contextId) },
                            onStartTracking = { focusContextsViewModel.startTracking(focusedContext.contextId) },
                            onDefocus = { focusContextsViewModel.unfocus(focusedContext.contextId) },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "AI Insights",
                subtitle = if (aiInsightsExpanded) "Останні інсайти" else "Згорнуто",
                isExpanded = aiInsightsExpanded,
                onClick = { aiInsightsExpanded = !aiInsightsExpanded },
            )
        }

        if (aiInsightsExpanded) {
            val latestInsights =
                aiInsights
                    .filterNot { it.isRead || dismissedInsightIds.contains(it.id) }
                    .take(3)
            if (latestInsights.isNotEmpty()) {
                latestInsights.forEach { insight ->
                    item(key = "deck_insight_${insight.id}") {
                        AiInsightCard(
                            insight = insight,
                            onMarkRead = {
                                dismissedInsightIds = dismissedInsightIds + insight.id
                                aiInsightsViewModel.markRead(insight.id)
                            },
                        )
                    }
                }
            } else {
                item {
                    MetricCard(
                        title = "Немає інсайтів",
                        value = "Поки що немає аналітики",
                        subtitle = "AI ще не згенерував інсайти",
                    )
                }
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
) {
    Card(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)),
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
                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
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
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiInsightCard(
    insight: AiMessage,
    onMarkRead: () -> Unit,
) {
    val backgroundColor =
        when (insight.type) {
            MessageType.MOTIVATION -> MaterialTheme.colorScheme.primaryContainer
            MessageType.JOKE -> MaterialTheme.colorScheme.secondaryContainer
            MessageType.INFO -> MaterialTheme.colorScheme.tertiaryContainer
            MessageType.WARNING -> MaterialTheme.colorScheme.errorContainer
            MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
        }
    val textColor =
        when (insight.type) {
            MessageType.MOTIVATION -> MaterialTheme.colorScheme.onPrimaryContainer
            MessageType.JOKE -> MaterialTheme.colorScheme.onSecondaryContainer
            MessageType.INFO -> MaterialTheme.colorScheme.onTertiaryContainer
            MessageType.WARNING -> MaterialTheme.colorScheme.onErrorContainer
            MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        }
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                    onMarkRead()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
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
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
                    text = "${insight.type.name.lowercase().replaceFirstChar { it.uppercase() }} • ${timeFormatter.format(
                        java.util.Date(insight.timestamp),
                    )}",
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
