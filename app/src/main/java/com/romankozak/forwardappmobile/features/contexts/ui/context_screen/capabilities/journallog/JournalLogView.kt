package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalLogView(
    modifier: Modifier = Modifier,
    document: NoteDocumentEntity?,
    searchQuery: String = "",
    onUpdateLine: (Int, String) -> Unit,
    onDeleteLine: (Int) -> Unit,
    onReorderLines: (List<String>) -> Unit,
    reorderMode: Boolean? = null,
    onReorderModeChange: (Boolean) -> Unit = {},
    autoScrollRequestKey: Any? = null,
    showReorderBottomBar: Boolean = true,
) {
    val baseEntries =
        remember(document?.content) {
            document?.content
                .orEmpty()
                .lines()
                .mapIndexedNotNull { index, rawLine ->
                    parseJournalLine(rawLine)?.let { parsed ->
                        JournalLineEntry(
                            originalIndex = index,
                            rawLine = rawLine,
                            parsed = parsed,
                        )
                    }
                }
        }
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    var selectedEntry by remember { mutableStateOf<JournalLineEntry?>(null) }
    var internalReorderMode by remember { mutableStateOf(false) }
    var uiEntries by remember(document?.content) { mutableStateOf(baseEntries) }
    val effectiveReorderMode = reorderMode ?: internalReorderMode
    val visibleEntries =
        remember(uiEntries, searchQuery) {
            if (searchQuery.isBlank()) {
                uiEntries
            } else {
                uiEntries.filter { it.rawLine.contains(searchQuery, ignoreCase = true) }
            }
        }
    val canReorder = effectiveReorderMode && searchQuery.isBlank()

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            if (!canReorder || uiEntries.isEmpty()) return@rememberReorderableLazyListState
            val safeFromIndex = from.index.coerceIn(0, uiEntries.lastIndex)
            val safeToIndex = to.index.coerceIn(0, uiEntries.lastIndex)
            if (safeFromIndex == safeToIndex) return@rememberReorderableLazyListState
            uiEntries =
                uiEntries.toMutableList().apply {
                    add(safeToIndex, removeAt(safeFromIndex))
                }
            onReorderLines(uiEntries.map { it.rawLine })
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    LaunchedEffect(autoScrollRequestKey, visibleEntries.size) {
        if (visibleEntries.isNotEmpty()) {
            lazyListState.animateScrollToItem(visibleEntries.lastIndex)
        }
    }

    if (visibleEntries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                text = "Поки порожньо.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(visibleEntries, key = { "${it.originalIndex}:${it.rawLine}" }) { entry ->
                ReorderableItem(reorderableState, key = "${entry.originalIndex}:${entry.rawLine}") {
                    JournalPreviewRow(
                        entry = entry,
                        reorderMode = canReorder,
                        dragHandleModifier =
                            with(this@ReorderableItem) {
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                )
                            },
                        onLongClick = {
                            if (!effectiveReorderMode) selectedEntry = entry
                        },
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        JournalLineActionsBottomSheet(
            initialValue = entry.rawLine,
            onDismiss = { selectedEntry = null },
            onSave = { updatedText ->
                onUpdateLine(entry.originalIndex, updatedText)
                selectedEntry = null
            },
            onDelete = {
                onDeleteLine(entry.originalIndex)
                selectedEntry = null
            },
            onEnterReorderMode = {
                if (reorderMode == null) {
                    internalReorderMode = true
                }
                onReorderModeChange(true)
                selectedEntry = null
            },
        )
    }

    if (effectiveReorderMode && showReorderBottomBar) {
        JournalReorderBottomBar(
            onDone = {
                if (reorderMode == null) {
                    internalReorderMode = false
                }
                onReorderModeChange(false)
            },
        )
    }
}

private data class JournalLineEntry(
    val originalIndex: Int,
    val rawLine: String,
    val parsed: ParsedJournalLine,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JournalPreviewRow(
    entry: JournalLineEntry,
    reorderMode: Boolean,
    dragHandleModifier: Modifier,
    onLongClick: () -> Unit,
) {
    val line = entry.parsed
    val palette = markerPalette(line.marker)
    val annotatedText =
        remember(line.text, line.textWeight) {
            buildJournalAnnotatedText(
                text = line.text,
                baseWeight = line.textWeight,
            )
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = palette.container.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp),
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (line.marker != null) {
            Text(
                text = line.marker,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                color = palette.marker,
                modifier =
                    Modifier
                        .background(
                            color = palette.container.copy(alpha = 0.95f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = line.textWeight),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (reorderMode) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragHandleModifier,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalLineActionsBottomSheet(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onEnterReorderMode: () -> Unit,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Редагувати запис",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
            )
            Button(
                onClick = { onSave(value) },
                modifier = Modifier.fillMaxWidth(),
                enabled = value.isNotBlank(),
            ) {
                Text("Зберегти")
            }
            OutlinedButton(
                onClick = onEnterReorderMode,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = null,
                )
                Text(
                    text = "Режим впорядкування",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Видалити",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Surface(color = Color.Transparent, modifier = Modifier.padding(bottom = 12.dp)) {}
        }
    }
}

@Composable
private fun JournalReorderBottomBar(onDone: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Режим впорядкування",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(onClick = onDone) {
                Text("Готово")
            }
        }
    }
}

private data class MarkerPalette(
    val container: Color,
    val marker: Color,
)

private fun markerPalette(marker: String?): MarkerPalette =
    when (marker) {
        "!!" -> MarkerPalette(container = Color(0xFFFFE5E5), marker = Color(0xFFC62828))
        "!" -> MarkerPalette(container = Color(0xFFFFF0E0), marker = Color(0xFFEF6C00))
        ">" -> MarkerPalette(container = Color(0xFFE3F2FD), marker = Color(0xFF1565C0))
        "?" -> MarkerPalette(container = Color(0xFFFFF8E1), marker = Color(0xFFF9A825))
        "-" -> MarkerPalette(container = Color(0xFFE8F5E9), marker = Color(0xFF2E7D32))
        else -> MarkerPalette(container = Color(0xFFEDE7F6), marker = Color(0xFF5E35B1))
    }

private fun buildJournalAnnotatedText(
    text: String,
    baseWeight: FontWeight,
) = buildAnnotatedString {
    val matches = DATE_TIME_REGEX.findAll(text).toList()
    if (matches.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }

    var currentIndex = 0
    matches.forEach { match ->
        val range = match.range
        if (range.first > currentIndex) {
            append(text.substring(currentIndex, range.first))
        }
        withStyle(
            SpanStyle(
                color = Color(0xFF6D5BD0),
                fontWeight = if (baseWeight >= FontWeight.Bold) baseWeight else FontWeight.Medium,
                background = Color(0x146D5BD0),
            ),
        ) {
            append(match.value)
        }
        currentIndex = range.last + 1
    }
    if (currentIndex < text.length) {
        append(text.substring(currentIndex))
    }
}

private val DATE_TIME_REGEX =
    Regex(
        pattern =
            """
            (?:
              \b\d{1,2}:\d{2}(?::\d{2})?\b |
              \b\d{1,2}[./-]\d{1,2}[./-]\d{2,4}\b |
              \b\d{4}[./-]\d{1,2}[./-]\d{1,2}\b |
              \b\d{1,2}\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|січ|лют|бер|кві|тра|чер|лип|сер|вер|жов|лис|гру)[a-zа-яіїє]*\.?,?\s+\d{2,4}\b |
              \b(?:today|tomorrow|yesterday|сьогодні|завтра|вчора)\b
            )
            """.trimIndent(),
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.COMMENTS),
    )
