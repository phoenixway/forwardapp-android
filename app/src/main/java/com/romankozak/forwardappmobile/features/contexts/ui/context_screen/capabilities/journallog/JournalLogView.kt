package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val AUTOSAVE_DELAY_MS = 500L

@Composable
fun JournalLogView(
    modifier: Modifier = Modifier,
    contextTitle: String?,
    document: NoteDocumentEntity?,
    onSave: (title: String, content: String, cursorPosition: Int) -> Unit,
) {
    val fallbackTitle = contextTitle?.trim()?.takeIf { it.isNotEmpty() }?.let { "$it Journal" } ?: "Journal Log"
    val externalTitle = document?.name?.takeIf { it.isNotBlank() } ?: fallbackTitle
    val externalBody = document?.content.orEmpty()
    val externalCursor = document?.lastCursorPosition?.coerceIn(0, externalBody.length) ?: externalBody.length

    var title by rememberSaveable(document?.id) { mutableStateOf(externalTitle) }
    var bodyField by rememberSaveable(document?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(externalBody, TextRange(externalCursor)))
    }
    var lastSavedTitle by rememberSaveable(document?.id) { mutableStateOf(externalTitle) }
    var lastSavedBody by rememberSaveable(document?.id) { mutableStateOf(externalBody) }
    var lastSavedAt by rememberSaveable(document?.id) { mutableLongStateOf(document?.updatedAt ?: 0L) }
    var isEditingTitle by rememberSaveable(document?.id) { mutableStateOf(false) }
    val parsedLines = remember(bodyField.text) { bodyField.text.lines().mapNotNull(::parseJournalLine) }

    LaunchedEffect(document?.id, document?.updatedAt, externalTitle, externalBody, externalCursor) {
        val localMatchesSaved = title == lastSavedTitle && bodyField.text == lastSavedBody
        if (localMatchesSaved) {
            title = externalTitle
            bodyField = bodyField.copy(text = externalBody, selection = TextRange(externalCursor))
            lastSavedTitle = externalTitle
            lastSavedBody = externalBody
            lastSavedAt = document?.updatedAt ?: 0L
        }
    }

    LaunchedEffect(title, bodyField.text, bodyField.selection.start, document?.id) {
        if (title == lastSavedTitle && bodyField.text == lastSavedBody) return@LaunchedEffect
        delay(AUTOSAVE_DELAY_MS)
        onSave(title, bodyField.text, bodyField.selection.start)
        lastSavedTitle = title
        lastSavedBody = bodyField.text
        lastSavedAt = System.currentTimeMillis()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (isEditingTitle) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.headlineSmall,
                                label = { Text("Title") },
                            )
                        } else {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isEditingTitle = true },
                            )
                        }
                        Text(
                            text = "Tap title to edit. Journal autosaves automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier =
                            Modifier
                                .size(42.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = CircleShape,
                                )
                                .clickable { isEditingTitle = !isEditingTitle },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isEditingTitle) Icons.Outlined.Save else Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(
                    text = autosaveStatus(lastSavedAt, title != lastSavedTitle || bodyField.text != lastSavedBody),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Rendered markers", style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider()
                JournalLogPreview(
                    lines = bodyField.text.lines(),
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Editor", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = journalSummary(parsedLines),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Кожен рядок: `маркер текст`. Наприклад: `- задача`, `!! критично`, `> рішення`, `? питання`.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MarkerToolbar(
                    onMarkerClick = { marker ->
                        bodyField = insertMarkerIntoCurrentLine(bodyField, marker)
                    },
                )
                BasicTextField(
                    value = bodyField,
                    onValueChange = { bodyField = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                    .padding(14.dp),
                        ) {
                            if (bodyField.text.isBlank()) {
                                Text(
                                    text = "- next action\n!! critical target\n> decision\n? open question",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                TextButton(
                    onClick = {
                        bodyField = appendJournalLine(bodyField)
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("New line")
                }
            }
        }
    }
}

@Composable
private fun MarkerToolbar(onMarkerClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        JOURNAL_MARKERS.forEach { marker ->
            val palette = markerPalette(marker)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.container.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                modifier = Modifier.clickable { onMarkerClick(marker) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = marker,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = palette.marker,
                    )
                    Text(
                        text = markerLabel(marker),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalLogPreview(
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (lines.all { it.isBlank() }) {
            Text(
                text = "Поки порожньо. Почни писати в editor нижче.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        lines.forEach { line ->
            val parsed = parseJournalLine(line)
            if (parsed == null) {
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                JournalPreviewRow(parsed)
            }
        }
    }
}

@Composable
private fun JournalPreviewRow(line: ParsedJournalLine) {
    val palette = markerPalette(line.marker)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = palette.container.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (line.marker != null) {
            Text(
                text = line.marker,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = palette.marker,
                modifier =
                    Modifier
                        .background(
                            color = palette.container,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = line.textWeight),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private data class ParsedJournalLine(
    val marker: String?,
    val text: String,
    val textWeight: FontWeight = FontWeight.Normal,
)

private data class MarkerPalette(
    val container: Color,
    val marker: Color,
)

private val JOURNAL_MARKERS = listOf("-", "!", "!!", ">", "?")

private fun parseJournalLine(raw: String): ParsedJournalLine? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(Regex("\\s+"), limit = 2)
    if (parts.size < 2) return ParsedJournalLine(marker = null, text = trimmed)
    val token = parts[0]
    val text = parts[1].trim()
    val tokenLooksLikeMarker = token.any { !it.isLetterOrDigit() } && token.length <= 4
    if (!tokenLooksLikeMarker) return ParsedJournalLine(marker = null, text = trimmed)
    val weight =
        when (token) {
            "!!" -> FontWeight.ExtraBold
            "!" -> FontWeight.Bold
            else -> FontWeight.Normal
        }
    return ParsedJournalLine(marker = token, text = text, textWeight = weight)
}

private fun markerPalette(marker: String?): MarkerPalette =
    when (marker) {
        "!!" -> MarkerPalette(container = Color(0xFFFFE5E5), marker = Color(0xFFC62828))
        "!" -> MarkerPalette(container = Color(0xFFFFF0E0), marker = Color(0xFFEF6C00))
        ">" -> MarkerPalette(container = Color(0xFFE3F2FD), marker = Color(0xFF1565C0))
        "?" -> MarkerPalette(container = Color(0xFFFFF8E1), marker = Color(0xFFF9A825))
        "-" -> MarkerPalette(container = Color(0xFFE8F5E9), marker = Color(0xFF2E7D32))
        else -> MarkerPalette(container = Color(0xFFEDE7F6), marker = Color(0xFF5E35B1))
    }

private fun markerLabel(marker: String): String =
    when (marker) {
        "-" -> "Action"
        "!" -> "Important"
        "!!" -> "Critical"
        ">" -> "Decision"
        "?" -> "Question"
        else -> "Marker"
    }

private fun journalSummary(lines: List<ParsedJournalLine>): String {
    val lineCount = lines.size
    val highlightedCount = lines.count { it.marker != null }
    return "$lineCount lines • $highlightedCount markers"
}

private fun insertMarkerIntoCurrentLine(
    field: TextFieldValue,
    marker: String,
): TextFieldValue {
    val text = field.text
    val cursor = field.selection.start.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0)).let { it + 1 }
    val lineEnd = text.indexOf('\n', startIndex = cursor).let { if (it == -1) text.length else it }
    val line = text.substring(lineStart, lineEnd)
    val trimmedStartLength = line.indexOfFirst { !it.isWhitespace() }.let { if (it == -1) line.length else it }
    val indent = line.take(trimmedStartLength)
    val content = line.drop(trimmedStartLength)
    val existingMarkerMatch = MARKER_PREFIX_REGEX.find(content)
    val bodyWithoutMarker = if (existingMarkerMatch != null) content.removePrefix(existingMarkerMatch.value).trimStart() else content
    val replacementLine = buildString {
        append(indent)
        append(marker)
        if (bodyWithoutMarker.isNotBlank()) {
            append(' ')
            append(bodyWithoutMarker)
        } else {
            append(' ')
        }
    }
    val updatedText = text.replaceRange(lineStart, lineEnd, replacementLine)
    val newCursor = (lineStart + replacementLine.length).coerceIn(0, updatedText.length)
    return field.copy(text = updatedText, selection = TextRange(newCursor))
}

private fun appendJournalLine(field: TextFieldValue): TextFieldValue {
    val suffix = if (field.text.isEmpty() || field.text.endsWith('\n')) "- " else "\n- "
    val updatedText = field.text + suffix
    return field.copy(text = updatedText, selection = TextRange(updatedText.length))
}

private val MARKER_PREFIX_REGEX = Regex("^([^\\p{L}\\p{N}\\s]{1,4})\\s*")

private fun autosaveStatus(
    lastSavedAt: Long,
    isDirty: Boolean,
): String {
    if (isDirty) return "Saving..."
    if (lastSavedAt <= 0L) return "Autosave ready"
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return "Saved at ${formatter.format(Date(lastSavedAt))}"
}
