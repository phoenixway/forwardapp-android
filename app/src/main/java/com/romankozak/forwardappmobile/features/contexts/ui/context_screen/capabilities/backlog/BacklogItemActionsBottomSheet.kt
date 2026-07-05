package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream

private data class BacklogActionItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

private data class BacklogActionSection(
    val title: String,
    val items: List<BacklogActionItem>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BacklogItemActionsBottomSheet(
    onDismiss: () -> Unit,
    itemTitle: String,
    isCompleted: Boolean,
    onOpenGoalProperties: (() -> Unit)? = null,
    onRemindersClick: () -> Unit,
    onAddToDayPlan: () -> Unit,
    missionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    currentTacticalPriorityStreamId: String?,
    onMissionStreamSelected: (String) -> Unit,
    isTacticalPriority: Boolean,
    onToggleTacticalPriority: () -> Unit,
    onStartTracking: () -> Unit,
    onCopyTransport: (() -> Unit)? = null,
    onCutTransport: (() -> Unit)? = null,
    onToggleCompleted: () -> Unit,
    onCopyContent: () -> Unit,
    onDelete: () -> Unit,
    onDeleteEverywhere: () -> Unit,
) {
    val modalBottomSheetState = rememberModalBottomSheetState()
    val colorScheme = MaterialTheme.colorScheme
    val isMovingTacticalPriority =
        isTacticalPriority && currentTacticalPriorityStreamId != selectedMissionStreamId
    val tacticalPriorityActionTitle =
        when {
            isMovingTacticalPriority -> "Перенести в обраний потік"
            isTacticalPriority -> "Зняти з тактичного періоду"
            else -> "Взяти в тактичний тиждень"
        }
    val tacticalPriorityActionSubtitle =
        if (isMovingTacticalPriority) {
            "Оновити потік тактичного пріоритету"
        } else {
            "Показати у беклозі як тактичний пріоритет"
        }

    val sections =
        buildList {
            if (onOpenGoalProperties != null) {
                add(
                    BacklogActionSection(
                        title = "Налаштування",
                        items =
                            listOf(
                                BacklogActionItem(
                                    title = "Властивості цілі",
                                    icon = Icons.Default.Tune,
                                    tint = colorScheme.primary,
                                    onClick = onOpenGoalProperties,
                                ),
                            ),
                    ),
                )
            }

            add(
                BacklogActionSection(
                    title = "Планування",
                    items =
                        listOf(
                            BacklogActionItem(
                                title = "Нагадування",
                                icon = Icons.Default.Notifications,
                                tint = colorScheme.secondary,
                                onClick = onRemindersClick,
                            ),
                            BacklogActionItem(
                                title = "Додати в план дня",
                                icon = Icons.Default.AddCircle,
                                tint = colorScheme.secondary,
                                onClick = onAddToDayPlan,
                            ),
                            BacklogActionItem(
                                title = tacticalPriorityActionTitle,
                                subtitle = tacticalPriorityActionSubtitle,
                                icon = Icons.Default.Flag,
                                tint = colorScheme.primary,
                                onClick = onToggleTacticalPriority,
                            ),
                            BacklogActionItem(
                                title = "Почати трекінг",
                                icon = Icons.Default.PlayCircleOutline,
                                tint = colorScheme.secondary,
                                onClick = onStartTracking,
                            ),
                        ),
                ),
            )

            add(
                BacklogActionSection(
                    title = "Дії",
                    items =
                        buildList {
                            add(
                                BacklogActionItem(
                                    title = if (isCompleted) "Позначити невиконаним" else "Позначити виконаним",
                                    icon = Icons.Default.CheckCircle,
                                    tint = colorScheme.secondary,
                                    onClick = onToggleCompleted,
                                ),
                            )
                            add(
                                BacklogActionItem(
                                    title = "Копіювати текст",
                                    icon = Icons.Default.ContentCopy,
                                    tint = colorScheme.secondary,
                                    onClick = onCopyContent,
                                ),
                            )
                        },
                ),
            )

            val clipboardItems =
                buildList {
                    if (onCopyTransport != null) {
                        add(
                            BacklogActionItem(
                                title = "Копіювати",
                                subtitle = "Покласти елемент у буфер",
                                icon = Icons.Default.ContentCopy,
                                tint = colorScheme.tertiary,
                                onClick = onCopyTransport,
                            ),
                        )
                    }
                    if (onCutTransport != null) {
                        add(
                            BacklogActionItem(
                                title = "Вирізати",
                                subtitle = "Підготувати переміщення через вставку",
                                icon = Icons.Default.ContentCut,
                                tint = colorScheme.tertiary,
                                onClick = onCutTransport,
                            ),
                        )
                    }
                }
            if (clipboardItems.isNotEmpty()) {
                add(
                    BacklogActionSection(
                        title = "Буфер обміну",
                        items = clipboardItems,
                    ),
                )
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Дії з елементом",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = itemTitle,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.size(4.dp))

            if (missionStreams.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "У потік",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        missionStreams.forEach { stream ->
                            FilterChip(
                                selected = selectedMissionStreamId == stream.id,
                                onClick = { onMissionStreamSelected(stream.id) },
                                label = { Text(stream.title, maxLines = 1) },
                            )
                        }
                    }
                }
            }

            sections.forEachIndexed { index, section ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        section.items.forEach { item ->
                            BacklogActionRow(item = item, onDismiss = onDismiss)
                        }
                    }
                }
                if (index != sections.lastIndex) {
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }

            Text(
                text = "Видалення",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            DestructiveBacklogAction(
                title = "Видалити",
                icon = Icons.Default.Delete,
                tint = colorScheme.error,
                onClick = {
                    onDelete()
                    onDismiss()
                },
            )
            DestructiveBacklogAction(
                title = "Видалити всюди",
                icon = Icons.Default.DeleteForever,
                tint = colorScheme.error,
                onClick = {
                    onDeleteEverywhere()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun BacklogActionRow(
    item: BacklogActionItem,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    item.onClick()
                    onDismiss()
                },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(item.tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.tint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
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

@Composable
private fun DestructiveBacklogAction(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
