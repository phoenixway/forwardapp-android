package com.romankozak.forwardappmobile.features.globalsearch

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val SEARCH_SKELETON_PULSE_DURATION_MILLIS = 850
private const val SEARCH_SKELETON_ITEM_COUNT = 4
private const val RECENT_COMMANDS_LIMIT = 6
private val SELECTED_COMMAND_BORDER_WIDTH = 1.35.dp

internal data class CommandResultsArgs(
    val results: List<OmniboxCommandResult>,
    val query: String,
    val recentCommands: List<OmniboxCommandId>,
    val showPreview: Boolean,
    val showRecents: Boolean,
    val selectedCommandIndex: Int?,
    val onCommandClick: (OmniboxCommandId) -> Unit,
    val accentColor: Color,
)

internal data class EmptyDataSearchActions(
    val onQuickCatch: () -> Unit,
    val onStartActivity: () -> Unit,
    val onAddActivityEvent: () -> Unit,
    val onCreateContext: () -> Unit,
    val onCreateDocument: () -> Unit,
    val onRunBestCommand: () -> Unit,
)

internal data class EmptyDataSearchArgs(
    val query: String,
    val commandResults: List<OmniboxCommandResult>,
    val selectedCommandIndex: Int?,
    val accentColor: Color,
    val onCommandClick: (OmniboxCommandId) -> Unit,
    val actions: EmptyDataSearchActions,
)

internal data class ModeRecentInputsArgs(
    val mode: OmniboxMode,
    val recents: List<String>,
    val showPreview: Boolean,
    val showRecents: Boolean,
    val onRecentClick: (String) -> Unit,
    val onRemoveRecentEntry: (String) -> Unit,
    val onClearRecentEntries: () -> Unit,
)

@Composable
internal fun CommandResultsContent(
    args: CommandResultsArgs,
    modifier: Modifier = Modifier,
) {
    if (args.results.isEmpty()) {
        CommandStartContent(args = args, modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recentCommandsSection(args)
        itemsIndexed(
            items = args.results,
            key = { _, item -> item.id.name },
        ) { index, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { args.onCommandClick(item.id) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = tintedResultSurface(args.accentColor)),
                border =
                    androidx.compose.foundation.BorderStroke(
                        if (args.selectedCommandIndex == index) 1.4.dp else 1.dp,
                        if (args.selectedCommandIndex == index) {
                            args.accentColor.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
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
                        tint = args.accentColor,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        HighlightedText(
                            text = item.title,
                            query = args.query,
                            matchedTags = emptyList(),
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

private fun androidx.compose.foundation.lazy.LazyListScope.recentCommandsSection(args: CommandResultsArgs) {
    if (!args.showRecents || args.query.isNotBlank() || args.recentCommands.isEmpty()) return

    item("recent_commands_label") {
        Text(
            text = "Нещодавні команди",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item("recent_commands_chips") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            args.recentCommands.take(RECENT_COMMANDS_LIMIT).forEach { commandId ->
                AssistChip(
                    onClick = { args.onCommandClick(commandId) },
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

@Composable
internal fun CommandStartContent(
    args: CommandResultsArgs,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (args.showPreview) {
            ModePreviewCard(
                title = "Командний режим",
                description =
                    "Вводьте навігаційні й швидкі команди. " +
                        "Виконання тільки через кнопку send або тап по картці.",
                accentColor = args.accentColor,
                icon = Icons.Default.Tune,
            )
        }
        if (args.showRecents && args.recentCommands.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = tintedResultSurface(args.accentColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Нещодавні команди",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        args.recentCommands.take(RECENT_COMMANDS_LIMIT).forEach { commandId ->
                            AssistChip(
                                onClick = { args.onCommandClick(commandId) },
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
        }
        if (!args.showPreview && (!args.showRecents || args.recentCommands.isEmpty())) {
            EmptySearchContent(query = args.query, modifier = Modifier.fillMaxWidth().weight(1f, fill = false))
        }
    }
}

@Composable
internal fun HybridCommandSection(
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
                color = tintedResultSurface(accentColor),
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedCommandIndex == index) {
                            accentColor.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                        },
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = commandIcon(command.id),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = command.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
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
internal fun CommandSearchResultCard(
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
        colors = CardDefaults.cardColors(containerColor = tintedResultSurface(accentColor)),
        border =
            androidx.compose.foundation.BorderStroke(
                if (isSelected) SELECTED_COMMAND_BORDER_WIDTH else 1.dp,
                if (isSelected) {
                    accentColor.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                },
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
                    matchedTags = emptyList(),
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
internal fun EmptyDataSearchContent(
    args: EmptyDataSearchArgs,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (args.commandResults.isNotEmpty()) {
            HybridCommandSection(
                results = args.commandResults,
                selectedCommandIndex = args.selectedCommandIndex,
                accentColor = args.accentColor,
                onCommandClick = args.onCommandClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        EmptySearchContent(query = args.query, modifier = Modifier.weight(1f).fillMaxWidth())
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = tintedResultSurface(args.accentColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Швидкі дії для \"${args.query}\"",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilledTonalButton(
                        onClick = args.actions.onQuickCatch,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text("В inbox", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = args.actions.onStartActivity,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text("Активність", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilledTonalButton(
                        onClick = args.actions.onCreateContext,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text("Контекст", style = MaterialTheme.typography.labelSmall)
                    }
                    FilledTonalButton(
                        onClick = args.actions.onCreateDocument,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        Text("Документ", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DataSearchLoadingContent(modifier: Modifier = Modifier) {
    val shimmer = rememberInfiniteTransition(label = "search_skeleton")
    val pulse by shimmer.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.56f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(SEARCH_SKELETON_PULSE_DURATION_MILLIS),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "search_skeleton_alpha",
    )
    Column(
        modifier = modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(SEARCH_SKELETON_ITEM_COUNT) { idx ->
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
internal fun EmptySearchContent(
    query: String,
    modifier: Modifier = Modifier,
) {
    val isBlankQuery = query.isBlank()
    Box(
        modifier = modifier,
        contentAlignment = if (isBlankQuery) Alignment.TopCenter else Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isBlankQuery) 10.dp else 16.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = if (isBlankQuery) 16.dp else 32.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(if (isBlankQuery) 56.dp else 80.dp)
                        .clip(RoundedCornerShape(if (isBlankQuery) 16.dp else 20.dp))
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
                    modifier = Modifier.size(if (isBlankQuery) 26.dp else 40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = if (isBlankQuery) "Введіть запит" else "Нічого не знайдено",
                style =
                    if (isBlankQuery) {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    },
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text =
                    if (isBlankQuery) {
                        "Пошук по контекстах, цілях, активностях, inbox і вкладеннях."
                    } else {
                        "За запитом \"$query\" результатів не знайдено.\nСпробуйте змінити пошуковий запит."
                    },
                style = if (isBlankQuery) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight =
                    if (isBlankQuery) {
                        MaterialTheme.typography.bodySmall.lineHeight * 1.15
                    } else {
                        MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                    },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchStartContent(
    history: List<String>,
    showPreview: Boolean,
    showRecents: Boolean,
    onHistoryClick: (String) -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showPreview) {
            ModePreviewCard(
                title = "Глобальний пошук",
                description =
                    "Пошук по контекстах, цілях, активностях, inbox і вкладеннях. " +
                        "Enter більше не запускає пошук, тільки кнопка send.",
                accentColor = MaterialTheme.colorScheme.primary,
                icon = Icons.Default.Search,
            )
        }
        if (showRecents && history.isNotEmpty()) {
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
        if (!showPreview && (!showRecents || history.isEmpty())) {
            EmptySearchContent(query = "", modifier = Modifier.fillMaxWidth().weight(1f, fill = false))
        }
    }
}

@Composable
private fun tintedResultSurface(
    accentColor: Color,
    amount: Float = 0.035f,
): Color = lerp(MaterialTheme.colorScheme.surfaceContainerLow, accentColor, amount)

@Composable
internal fun ModeRecentInputsContent(
    args: ModeRecentInputsArgs,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (args.showPreview) {
            ModePreviewCard(
                title = modeStartTitle(args.mode),
                description = modeStartDescription(args.mode),
                accentColor = MaterialTheme.colorScheme.secondary,
                icon = modeStartIcon(args.mode),
            )
        }
        if (args.showRecents && args.recents.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Нещодавні записи",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = args.onClearRecentEntries) {
                    Text("Очистити все")
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                args.recents.forEach { value ->
                    InputChip(
                        selected = false,
                        onClick = { args.onRecentClick(value) },
                        label = { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        trailingIcon = {
                            IconButton(
                                onClick = { args.onRemoveRecentEntry(value) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Видалити запис",
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
        if (!args.showPreview && (!args.showRecents || args.recents.isEmpty())) {
            EmptySearchContent(query = "", modifier = Modifier.fillMaxWidth().weight(1f, fill = false))
        }
    }
}

@Composable
internal fun ModePreviewCard(
    title: String,
    description: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.padding(10.dp).size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun modeStartTitle(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.Command -> "Команди"
        OmniboxMode.DataSearch -> "Пошук"
        OmniboxMode.QuickCatchInbox -> "Quick catch"
        OmniboxMode.StartActivity -> "Старт активності"
        OmniboxMode.AddActivityEvent -> "Подія активності"
    }

private fun modeStartDescription(mode: OmniboxMode): String =
    when (mode) {
        OmniboxMode.Command -> "Швидкий запуск екрану або дії."
        OmniboxMode.DataSearch -> "Глобальний пошук по даних застосунку."
        OmniboxMode.QuickCatchInbox -> "Швидко скиньте думку або нотатку в inbox."
        OmniboxMode.StartActivity -> "Створіть нову поточну активність із цього поля."
        OmniboxMode.AddActivityEvent -> "Додайте короткий запис у Life Journal."
    }

private fun modeStartIcon(mode: OmniboxMode): ImageVector =
    when (mode) {
        OmniboxMode.Command -> Icons.Default.Tune
        OmniboxMode.DataSearch -> Icons.Default.Search
        OmniboxMode.QuickCatchInbox -> Icons.Default.Add
        OmniboxMode.StartActivity -> Icons.Default.History
        OmniboxMode.AddActivityEvent -> Icons.Default.Check
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataActionsBottomSheet(
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
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onSelectSorting() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateFromSearchBottomSheet(
    onCreateContext: () -> Unit,
    onCreateDocument: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Створити з пошуку",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ListItem(
                headlineContent = { Text("Новий контекст") },
                supportingContent = { Text("Створити новий контекст без виходу в інші розділи") },
                leadingContent = { Icon(Icons.Default.AccountTree, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onCreateContext() },
                colors =
                    androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
            )
            ListItem(
                headlineContent = { Text("Новий документ") },
                supportingContent = { Text("Створити документ у Inbox як початковому контексті") },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { onCreateDocument() },
                colors =
                    androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TypeBottomSheet(
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
            TypeSelectionList(
                options = options,
                draft = draft,
                onSelectAll = { draft = options.toSet() },
                onToggleType = { type -> draft = if (type in draft) draft - type else draft + type },
            )
            TypeBottomSheetActions(
                onDismiss = onDismiss,
                onApply = { onApply(draft) },
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SortBottomSheet(
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
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                    trailingContent = {
                        if (selectedSort == sort) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onSortSelected(sort) },
                )
            }
        }
    }
}
