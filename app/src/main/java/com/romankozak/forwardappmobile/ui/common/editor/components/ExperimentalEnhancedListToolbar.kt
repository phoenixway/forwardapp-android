@file:Suppress("TooManyFunctions")

package com.romankozak.forwardappmobile.ui.common.editor.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CommandGroup { РЕДАГУВАННЯ, СПИСКИ, ВСТАВКА, ФОРМАТУВАННЯ }

private data class ToolbarCallbacks(
    val onIndentBlock: () -> Unit,
    val onDeIndentBlock: () -> Unit,
    val onMoveBlockUp: () -> Unit,
    val onMoveBlockDown: () -> Unit,
    val onIndentLine: () -> Unit,
    val onDeIndentLine: () -> Unit,
    val onMoveLineUp: () -> Unit,
    val onMoveLineDown: () -> Unit,
    val onDeleteLine: () -> Unit,
    val onCopyLine: () -> Unit,
    val onCutLine: () -> Unit,
    val onPasteLine: () -> Unit,
    val onToggleBullet: () -> Unit,
    val onToggleCheckbox: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onToggleVisibility: () -> Unit,
    val onInsertDateTime: () -> Unit,
    val onInsertTime: () -> Unit,
    val onInsertAttachmentLink: () -> Unit,
    val onInsertContextLink: () -> Unit,
    val onH1: () -> Unit,
    val onH2: () -> Unit,
    val onH3: () -> Unit,
    val onBold: () -> Unit,
    val onItalic: () -> Unit,
    val onInsertSeparator: () -> Unit,
)

private data class ToolbarAvailability(
    val canInsertAttachmentLink: Boolean,
    val canInsertContextLink: Boolean,
)

private data class ToolbarButtonSpec(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isActive: Boolean = false,
)

private data class ToolbarSectionSpec(
    val title: String,
    val actions: List<ToolbarButtonSpec>,
)

private const val TOOLBAR_TAB_ANIMATION_MILLIS = 300
private const val TOOLBAR_PULSE_ANIMATION_MILLIS = 1500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ExperimentalEnhancedListToolbar(
    modifier: Modifier = Modifier,
    state: ListToolbarState,
    // Block actions
    onIndentBlock: () -> Unit,
    onDeIndentBlock: () -> Unit,
    onMoveBlockUp: () -> Unit,
    onMoveBlockDown: () -> Unit,
    // Line actions
    onIndentLine: () -> Unit,
    onDeIndentLine: () -> Unit,
    onMoveLineUp: () -> Unit,
    onMoveLineDown: () -> Unit,
    onDeleteLine: () -> Unit,
    onCopyLine: () -> Unit,
    onCutLine: () -> Unit,
    onPasteLine: () -> Unit,
    // Other actions
    onToggleBullet: () -> Unit,
    onToggleCheckbox: () -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onToggleVisibility: () -> Unit = {},
    onInsertDateTime: () -> Unit = {},
    onInsertTime: () -> Unit = {},
    onInsertAttachmentLink: () -> Unit = {},
    onInsertContextLink: () -> Unit = {},
    canInsertAttachmentLink: Boolean = false,
    canInsertContextLink: Boolean = false,
    onH1: () -> Unit = {},
    onH2: () -> Unit = {},
    onH3: () -> Unit = {},
    onBold: () -> Unit = {},
    onItalic: () -> Unit = {},
    onInsertSeparator: () -> Unit = {},
) {
    ExperimentalEnhancedListToolbarContent(
        modifier = modifier,
        state = state,
        callbacks =
            ToolbarCallbacks(
                onIndentBlock = onIndentBlock,
                onDeIndentBlock = onDeIndentBlock,
                onMoveBlockUp = onMoveBlockUp,
                onMoveBlockDown = onMoveBlockDown,
                onIndentLine = onIndentLine,
                onDeIndentLine = onDeIndentLine,
                onMoveLineUp = onMoveLineUp,
                onMoveLineDown = onMoveLineDown,
                onDeleteLine = onDeleteLine,
                onCopyLine = onCopyLine,
                onCutLine = onCutLine,
                onPasteLine = onPasteLine,
                onToggleBullet = onToggleBullet,
                onToggleCheckbox = onToggleCheckbox,
                onUndo = onUndo,
                onRedo = onRedo,
                onToggleVisibility = onToggleVisibility,
                onInsertDateTime = onInsertDateTime,
                onInsertTime = onInsertTime,
                onInsertAttachmentLink = onInsertAttachmentLink,
                onInsertContextLink = onInsertContextLink,
                onH1 = onH1,
                onH2 = onH2,
                onH3 = onH3,
                onBold = onBold,
                onItalic = onItalic,
                onInsertSeparator = onInsertSeparator,
            ),
        availability =
            ToolbarAvailability(
                canInsertAttachmentLink = canInsertAttachmentLink,
                canInsertContextLink = canInsertContextLink,
            ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun ExperimentalEnhancedListToolbarContent(
    modifier: Modifier = Modifier,
    state: ListToolbarState,
    callbacks: ToolbarCallbacks,
    availability: ToolbarAvailability,
) {
    var selectedTab by remember { mutableStateOf(CommandGroup.РЕДАГУВАННЯ) }
    val haptics = LocalHapticFeedback.current
    val sections =
        remember(selectedTab, state, callbacks, availability, haptics) {
            buildToolbarSections(
                tab = selectedTab,
                state = state,
                callbacks = callbacks,
                availability = availability,
                haptics = haptics,
            )
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = if (state.isEditing) 12.dp else 4.dp,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ToolbarAccentLine(isEditing = state.isEditing)
            ToolbarDragHandle(
                isEditing = state.isEditing,
                onToggleVisibility = callbacks.onToggleVisibility,
            )

            AnimatedVisibility(
                visible = state.isEditing,
                enter =
                    expandVertically(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ) + fadeIn(),
                exit =
                    shrinkVertically(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                    ) + fadeOut(),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolbarTabSelector(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        haptics = haptics,
                    )
                    ToolbarSectionsContent(
                        selectedTab = selectedTab,
                        sections = sections,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarAccentLine(isEditing: Boolean) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(
                        alpha = if (isEditing) 0.6f else 0.3f,
                    ),
                ),
    )
}

@Composable
private fun ToolbarDragHandle(
    isEditing: Boolean,
    onToggleVisibility: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(TOOLBAR_PULSE_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_alpha",
    )

    Box(
        modifier =
            Modifier
                .height(24.dp)
                .fillMaxWidth()
                .clickable(onClick = onToggleVisibility),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isEditing) 0.5f else pulseAlpha,
                        ),
                    ),
        )
    }
}

@Composable
private fun ToolbarTabSelector(
    selectedTab: CommandGroup,
    onTabSelected: (CommandGroup) -> Unit,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            indicator = { },
            divider = { },
        ) {
            CommandGroup.values().forEach { group ->
                ToolbarTab(
                    group = group,
                    isSelected = selectedTab == group,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(group)
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolbarTab(
    group: CommandGroup,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Tab(
        modifier = Modifier.height(40.dp),
        selected = isSelected,
        onClick = onClick,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = group.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ToolbarSectionsContent(
    selectedTab: CommandGroup,
    sections: List<ToolbarSectionSpec>,
) {
    Crossfade(
        targetState = selectedTab,
        animationSpec = tween(TOOLBAR_TAB_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
    ) {
        ToolbarSectionsRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
            sections = sections,
        )
    }
}

@Composable
private fun ToolbarSectionsRow(
    modifier: Modifier = Modifier,
    sections: List<ToolbarSectionSpec>,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sections.forEachIndexed { index, section ->
            ToolbarSection(title = section.title) {
                section.actions.forEach { action ->
                    EnhancedToolbarButton(spec = action)
                }
            }
            if (index < sections.lastIndex) {
                VerticalDivider(
                    modifier = Modifier.height(36.dp).padding(horizontal = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun buildToolbarSections(
    tab: CommandGroup,
    state: ListToolbarState,
    callbacks: ToolbarCallbacks,
    availability: ToolbarAvailability,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
): List<ToolbarSectionSpec> =
    when (tab) {
        CommandGroup.РЕДАГУВАННЯ -> editingSections(state, callbacks, haptics)
        CommandGroup.СПИСКИ -> listSections(state, callbacks, haptics)
        CommandGroup.ВСТАВКА -> insertSections(callbacks, availability, haptics)
        CommandGroup.ФОРМАТУВАННЯ -> formattingSections(callbacks, haptics)
    }

private fun toolbarAction(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit,
): ToolbarButtonSpec =
    ToolbarButtonSpec(
        icon = icon,
        description = description,
        enabled = enabled,
        isActive = isActive,
        onClick = onClick,
    )

private fun editingSections(
    state: ListToolbarState,
    callbacks: ToolbarCallbacks,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
): List<ToolbarSectionSpec> =
    listOf(
        ToolbarSectionSpec(
            title = "Історія",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        description = "Скасувати",
                        enabled = state.canUndo,
                        onClick = hapticAction(haptics, callback = callbacks.onUndo),
                    ),
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        description = "Повторити",
                        enabled = state.canRedo,
                        onClick = hapticAction(haptics, callback = callbacks.onRedo),
                    ),
                ),
        ),
        ToolbarSectionSpec(
            title = "Редагування",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.Default.DeleteOutline,
                        description = "Видалити рядок",
                        onClick =
                            hapticAction(
                                haptics = haptics,
                                hapticType = HapticFeedbackType.LongPress,
                                callback = callbacks.onDeleteLine,
                            ),
                    ),
                    toolbarAction(
                        icon = Icons.Default.ContentCopy,
                        description = "Копіювати рядок",
                        onClick = hapticAction(haptics, callback = callbacks.onCopyLine),
                    ),
                    toolbarAction(
                        icon = Icons.Default.ContentCut,
                        description = "Вирізати рядок",
                        onClick = hapticAction(haptics, callback = callbacks.onCutLine),
                    ),
                    toolbarAction(
                        icon = Icons.Default.ContentPaste,
                        description = "Вставити рядок",
                        onClick = hapticAction(haptics, callback = callbacks.onPasteLine),
                    ),
                ),
        ),
    )

@Suppress("LongMethod")
private fun listSections(
    state: ListToolbarState,
    callbacks: ToolbarCallbacks,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
): List<ToolbarSectionSpec> =
    listOf(
        ToolbarSectionSpec(
            title = "Формат",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                        description = "Маркери",
                        isActive = state.formatMode == ListFormatMode.BULLET,
                        onClick = hapticAction(haptics, callback = callbacks.onToggleBullet),
                    ),
                    toolbarAction(
                        icon = Icons.Default.Checklist,
                        description = "Чекбокс",
                        onClick = hapticAction(haptics, callback = callbacks.onToggleCheckbox),
                    ),
                ),
        ),
        ToolbarSectionSpec(
            title = "Відступи",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        description = "Збільшити відступ",
                        enabled = state.canIndent,
                        onClick = hapticAction(haptics, callback = callbacks.onIndentLine),
                    ),
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                        description = "Зменшити відступ",
                        enabled = state.canDeIndent,
                        onClick = hapticAction(haptics, callback = callbacks.onDeIndentLine),
                    ),
                ),
        ),
        ToolbarSectionSpec(
            title = "Рядки",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.Default.KeyboardArrowUp,
                        description = "Рядок вгору",
                        enabled = state.canMoveUp,
                        onClick = hapticAction(haptics, callback = callbacks.onMoveLineUp),
                    ),
                    toolbarAction(
                        icon = Icons.Default.KeyboardArrowDown,
                        description = "Рядок вниз",
                        enabled = state.canMoveDown,
                        onClick = hapticAction(haptics, callback = callbacks.onMoveLineDown),
                    ),
                ),
        ),
        ToolbarSectionSpec(
            title = "Блоки",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        description = "Відступ блоку",
                        onClick = hapticAction(haptics, callback = callbacks.onIndentBlock),
                    ),
                    toolbarAction(
                        icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                        description = "Зняти відступ блоку",
                        onClick = hapticAction(haptics, callback = callbacks.onDeIndentBlock),
                    ),
                    toolbarAction(
                        icon = Icons.Default.KeyboardDoubleArrowUp,
                        description = "Блок вгору",
                        onClick = hapticAction(haptics, callback = callbacks.onMoveBlockUp),
                    ),
                    toolbarAction(
                        icon = Icons.Default.KeyboardDoubleArrowDown,
                        description = "Блок вниз",
                        onClick = hapticAction(haptics, callback = callbacks.onMoveBlockDown),
                    ),
                ),
        ),
    )

private fun insertSections(
    callbacks: ToolbarCallbacks,
    availability: ToolbarAvailability,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
): List<ToolbarSectionSpec> =
    listOf(
        ToolbarSectionSpec(
            title = "Вставка",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.Default.DateRange,
                        description = "Вставити дату і час",
                        onClick = hapticAction(haptics, callback = callbacks.onInsertDateTime),
                    ),
                    toolbarAction(
                        icon = Icons.Default.AccessTime,
                        description = "Вставити час",
                        onClick = hapticAction(haptics, callback = callbacks.onInsertTime),
                    ),
                    toolbarAction(
                        icon = Icons.Default.HorizontalRule,
                        description = "Вставити роздільник",
                        onClick = hapticAction(haptics, callback = callbacks.onInsertSeparator),
                    ),
                    toolbarAction(
                        icon = Icons.Default.AttachFile,
                        description = "Вставити посилання на вкладення",
                        enabled = availability.canInsertAttachmentLink,
                        onClick = hapticAction(haptics, callback = callbacks.onInsertAttachmentLink),
                    ),
                    toolbarAction(
                        icon = Icons.Default.AccountTree,
                        description = "Вставити посилання на контекст",
                        enabled = availability.canInsertContextLink,
                        onClick = hapticAction(haptics, callback = callbacks.onInsertContextLink),
                    ),
                ),
        ),
    )

private fun formattingSections(
    callbacks: ToolbarCallbacks,
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
): List<ToolbarSectionSpec> =
    listOf(
        ToolbarSectionSpec(
            title = "Заголовки",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.Default.HMobiledata,
                        description = "H1",
                        onClick = hapticAction(haptics, callback = callbacks.onH1),
                    ),
                    toolbarAction(
                        icon = Icons.Default.HPlusMobiledata,
                        description = "H2",
                        onClick = hapticAction(haptics, callback = callbacks.onH2),
                    ),
                    toolbarAction(
                        icon = Icons.Default.HPlusMobiledata,
                        description = "H3",
                        onClick = hapticAction(haptics, callback = callbacks.onH3),
                    ),
                ),
        ),
        ToolbarSectionSpec(
            title = "Стиль",
            actions =
                listOf(
                    toolbarAction(
                        icon = Icons.Default.FormatBold,
                        description = "Жирний",
                        onClick = hapticAction(haptics, callback = callbacks.onBold),
                    ),
                    toolbarAction(
                        icon = Icons.Default.FormatItalic,
                        description = "Курсив",
                        onClick = hapticAction(haptics, callback = callbacks.onItalic),
                    ),
                ),
        ),
    )

private fun hapticAction(
    haptics: androidx.compose.ui.hapticfeedback.HapticFeedback,
    hapticType: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    callback: () -> Unit,
): () -> Unit = {
    haptics.performHapticFeedback(hapticType)
    callback()
}

@Composable
private fun ToolbarSection(
    title: String,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun EnhancedToolbarButton(
    spec: ToolbarButtonSpec,
    modifier: Modifier = Modifier,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (spec.enabled) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale",
    )

    val animatedColor by animateColorAsState(
        targetValue =
            when {
                spec.isActive -> MaterialTheme.colorScheme.primary
                spec.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            },
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "button_color",
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue =
            if (spec.isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "button_background",
    )

    FilledIconButton(
        onClick = spec.onClick,
        enabled = spec.enabled,
        modifier =
            modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                },
        shape = RoundedCornerShape(12.dp),
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = animatedBackgroundColor,
                contentColor = animatedColor,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = spec.description,
            modifier = Modifier.size(20.dp),
        )
    }
}
