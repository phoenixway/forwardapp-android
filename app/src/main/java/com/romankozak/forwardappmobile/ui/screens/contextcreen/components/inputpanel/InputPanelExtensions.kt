package com.romankozak.forwardappmobile.ui.screens.contextcreen.components.inputpanel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R

@Composable
fun CollapsibleInputPanel(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "collapse_rotation",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            Surface(
                onClick = onToggleExpanded,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription =
                            if (isExpanded) {
                                stringResource(R.string.collapse_panel)
                            } else {
                                stringResource(R.string.expand_panel)
                            },
                        modifier =
                            Modifier
                                .size(20.dp)
                                .rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isExpanded) {
                content()
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    currentMode: InputMode,
    onModeSelected: (InputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        InputMode.values().forEach { mode ->
            val isSelected = currentMode == mode
            val (containerColor, contentColor) =
                if (isSelected) {
                    Pair(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Pair(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text =
                            when (mode) {
                                InputMode.AddGoal -> "Ціль"
                                InputMode.AddQuickRecord -> "Запис"
                                InputMode.SearchInList -> "Пошук"
                                InputMode.SearchGlobal -> "Глобально"
                                InputMode.AddProjectLog -> "Лог"
                                InputMode.AddMilestone -> "Віха"
                            },
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = containerColor,
                        selectedLabelColor = contentColor,
                    ),
                modifier = Modifier.height(32.dp),
            )
        }
    }
}

@Composable
fun InputStatusIndicator(
    mode: InputMode,
    hasText: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    val indicatorColor by animateColorAsState(
        targetValue =
            when {
                isProcessing -> MaterialTheme.colorScheme.secondary
                hasText ->
                    when (mode) {
                        InputMode.AddGoal -> MaterialTheme.colorScheme.primary
                        InputMode.AddQuickRecord -> MaterialTheme.colorScheme.primary
                        InputMode.SearchInList -> MaterialTheme.colorScheme.secondary
                        InputMode.SearchGlobal -> MaterialTheme.colorScheme.tertiary
                        InputMode.AddProjectLog -> MaterialTheme.colorScheme.secondary
                        InputMode.AddMilestone -> MaterialTheme.colorScheme.secondary
                    }
                else -> MaterialTheme.colorScheme.outline
            },
        label = "status_indicator_color",
    )

    val scale by animateFloatAsState(
        targetValue = if (isProcessing) 1.2f else 1f,
        label = "status_indicator_scale",
    )

    Box(
        modifier =
            modifier
                .size(4.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.background(
                    color = indicatorColor,
                    shape = RoundedCornerShape(2.dp),
                ),
    )
}

object InputModeUtils {
    fun getNextMode(current: InputMode): InputMode {
        val modes =
            listOf(
                InputMode.SearchInList,
                InputMode.SearchGlobal,
                InputMode.AddGoal,
                InputMode.AddQuickRecord,
            )
        val currentIndex = modes.indexOf(current)
        return modes[(currentIndex + 1) % modes.size]
    }

    fun getPrevMode(current: InputMode): InputMode {
        val modes =
            listOf(
                InputMode.SearchInList,
                InputMode.SearchGlobal,
                InputMode.AddGoal,
                InputMode.AddQuickRecord,
            )
        val currentIndex = modes.indexOf(current)
        return modes[(currentIndex - 1 + modes.size) % modes.size]
    }

    fun getModeIcon(mode: InputMode) =
        when (mode) {
            InputMode.AddGoal -> Icons.Outlined.Add
            InputMode.AddQuickRecord -> Icons.Outlined.Inbox
            InputMode.SearchInList -> Icons.Outlined.Search
            InputMode.SearchGlobal -> Icons.Outlined.TravelExplore
            InputMode.AddProjectLog -> Icons.Outlined.PostAdd
            InputMode.AddMilestone -> Icons.Outlined.Flag
        }

    fun getModeColor(
        mode: InputMode,
        colorScheme: ColorScheme,
    ) = when (mode) {
        InputMode.AddGoal -> colorScheme.primary
        InputMode.AddQuickRecord -> colorScheme.primary
        InputMode.SearchInList -> colorScheme.secondary
        InputMode.SearchGlobal -> colorScheme.tertiary
        InputMode.AddProjectLog -> colorScheme.secondary
        InputMode.AddMilestone -> colorScheme.secondary
    }
}

@Composable
fun ReminderChip(
    suggestionText: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Нагадування",
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Нагадування: $suggestionText",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Скасувати нагадування",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
