package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.EnhancedRelatedLinkChip
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.handleRelatedLinkClick
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurfaceLayout
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun DayFocusesScreen(
    initialDayPlanId: String,
    navController: NavController? = null,
    predictedDayDurationMinutes: Long? = null,
    globalObsidianVaultName: String? = null,
    viewModel: DayFocusesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialDayPlanId) {
        viewModel.loadDataForPlan(initialDayPlanId)
    }

    DayFocusesContent(
        uiState = uiState,
        onRetry = { viewModel.loadDataForPlan(initialDayPlanId) },
        onEdit = viewModel::openEditDialog,
        onDelete = viewModel::requestDelete,
        onReorder = viewModel::updateItemsOrder,
        predictedDayDurationMinutes = predictedDayDurationMinutes,
        onRelatedLinkClick = { link ->
            val resolvedNavController = navController ?: return@DayFocusesContent
            handleRelatedLinkClick(
                link = link,
                context = context,
                navController = resolvedNavController,
                globalObsidianVaultName = globalObsidianVaultName,
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayFocusesContent(
    uiState: DayFocusesUiState,
    onRetry: () -> Unit,
    onEdit: (DayFocusItem) -> Unit,
    onDelete: (DayFocusItem) -> Unit,
    onReorder: (List<DayFocusItem>) -> Unit,
    predictedDayDurationMinutes: Long?,
    onRelatedLinkClick: (RelatedLink) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val hapticFeedback = LocalHapticFeedback.current
    var internalItems by remember(uiState.items) { mutableStateOf(uiState.items) }
    val isBudgetOverLimit = internalItems.sumOf { it.budgetPercent ?: 0 } > 100
    val contextTitlesById =
        remember(uiState.availableContexts) {
            uiState.availableContexts.associate { it.id to it.name }
        }
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalItems =
                internalItems.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            onReorder(internalItems)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    when {
        uiState.isLoading -> DayFocusesLoadingState()
        uiState.error != null && uiState.items.isEmpty() -> DayFocusesErrorState(error = uiState.error, onRetry = onRetry)
        internalItems.isEmpty() -> DayFocusesEmptyState()
        else ->
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(internalItems, key = { it.id }) { item ->
                    DayFocusItemRow(
                        item = item,
                        reorderableState = reorderableState,
                        onEdit = { onEdit(item) },
                        onDelete = { onDelete(item) },
                        onRelatedLinkClick = onRelatedLinkClick,
                        contextTitlesById = contextTitlesById,
                        isBudgetOverLimit = isBudgetOverLimit,
                        predictedDayDurationMinutes = predictedDayDurationMinutes,
                    )
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LazyItemScope.DayFocusItemRow(
    item: DayFocusItem,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    contextTitlesById: Map<String, String>,
    isBudgetOverLimit: Boolean,
    predictedDayDurationMinutes: Long?,
) {
    ReorderableItem(reorderableState, key = item.id) { isDragging ->
        var showActionsSheet by remember(item.id) { mutableStateOf(false) }
        val hapticFeedback = LocalHapticFeedback.current
        val elevation by animateDpAsState(targetValue = if (isDragging) 8.dp else 0.dp, label = "dayFocusElevation")
        val borderColor =
            when (item.type) {
                DayFocusType.FOCUS -> MaterialTheme.colorScheme.primary.copy(alpha = 0.46f)
                DayFocusType.RESPONSIBILITY -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.46f)
            }
        UnifiedListItemSurface(
            isSelected = isDragging,
            state = UnifiedItemState.DEFAULT,
            layout =
                UnifiedListItemSurfaceLayout(
                    modifier = Modifier.fillMaxWidth().padding(vertical = elevation / 8),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ),
            colors =
                UnifiedListItemColors(
                    container = borderColor.copy(alpha = 0.10f),
                    border = borderColor,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector =
                        if (item.type == DayFocusType.FOCUS) {
                            Icons.Outlined.AutoAwesome
                        } else {
                            Icons.Outlined.TaskAlt
                        },
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.type.title(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    item.budgetPercent?.let { budgetPercent ->
                        Spacer(modifier = Modifier.height(4.dp))
                        DayFocusBudgetBadge(
                            budgetPercent = budgetPercent,
                            isOverLimit = isBudgetOverLimit,
                            predictedDayDurationMinutes = predictedDayDurationMinutes,
                        )
                    }
                    item.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val visibleLinks = item.relatedLinks.orEmpty().filter { it.type != null }
                    if (visibleLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            visibleLinks.forEach { link ->
                                val renderedLink =
                                    if (link.type == LinkType.CONTEXT) {
                                        link.copy(displayName = contextTitlesById[link.target] ?: link.displayName)
                                    } else {
                                        link
                                    }
                                EnhancedRelatedLinkChip(
                                    link = renderedLink,
                                    onClick = { onRelatedLinkClick(renderedLink) },
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { showActionsSheet = true },
                    modifier =
                        with(this@ReorderableItem) {
                            Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        },
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Дії з фокусом",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (showActionsSheet) {
            DayFocusItemOptionsSheet(
                item = item,
                onDismiss = { showActionsSheet = false },
                onEdit = {
                    showActionsSheet = false
                    onEdit()
                },
                onDelete = {
                    showActionsSheet = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun DayFocusBudgetBadge(
    budgetPercent: Int,
    isOverLimit: Boolean,
    predictedDayDurationMinutes: Long?,
) {
    val color =
        if (isOverLimit) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            text = buildDayFocusBudgetLabel(budgetPercent, predictedDayDurationMinutes),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun buildDayFocusBudgetLabel(
    budgetPercent: Int,
    predictedDayDurationMinutes: Long?,
): String {
    val durationMinutes = predictedDayDurationMinutes?.takeIf { it > 0L } ?: return "$budgetPercent%"
    val allocatedMinutes = durationMinutes * budgetPercent / 100
    return "$budgetPercent% · ${formatBudgetHours(allocatedMinutes)}/${formatBudgetHours(durationMinutes)} год"
}

private fun formatBudgetHours(minutes: Long): String {
    val hours = minutes / 60.0
    return if (minutes % 60L == 0L) {
        hours.toInt().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", hours)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DayFocusItemOptionsSheet(
    item: DayFocusItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            DayFocusItemOption(
                label = "Редагувати",
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = onEdit,
            )
            DayFocusItemOption(
                label = "Видалити",
                color = MaterialTheme.colorScheme.error,
                icon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = onDelete,
            )
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun DayFocusItemOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable () -> Unit,
) {
    androidx.compose.material3.ListItem(
        headlineContent = {
            Text(
                text = label,
                color = color,
            )
        },
        leadingContent = icon,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            androidx.compose.material3.ListItemDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
            ),
    )
}

@Composable
private fun DayFocusesLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Завантаження фокусів дня...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayFocusesErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Не вдалося завантажити фокуси дня",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(0.dp))
            Text("Спробувати ще раз")
        }
    }
}

@Composable
private fun DayFocusesEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Фокусів дня ще немає",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Додайте фокус або зону відповідальності через поле внизу чи меню дій.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun DayFocusType.title(): String =
    when (this) {
        DayFocusType.FOCUS -> "Фокус"
        DayFocusType.RESPONSIBILITY -> "Зона відповідальності"
    }
