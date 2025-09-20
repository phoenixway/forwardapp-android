package com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.net.URLEncoder
import com.romankozak.forwardappmobile.ui.screens.backlog.components.InlineTagChip
import com.romankozak.forwardappmobile.ui.screens.backlog.components.TagType

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isCompleted: Boolean = false,
    obsidianVaultName: String = "",
    onTagClick: (String) -> Unit = {},
    onTextClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.secondary

    val finalTextStyle =
        if (isCompleted) {
            style.copy(
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            style.copy(
                color = style.color.takeUnless { it.isUnspecified }
                    ?: MaterialTheme.colorScheme.onSurface,
            )
        }

    val listRegex = remember { Regex("^\\s*([*+-])\\s+(.*)") }

    // Regex для markdown + тегів (#tag або @tag)
    val inlineContentRegex = remember {
        Regex(
            "(\\*\\*|__)(.*?)\\1" +                // жирний
                    "|(\\*|_)(.*?)\\3" +          // курсив
                    "|(~~)(.*?)\\5" +             // закреслений
                    "|(\\[\\[)(.*?)(?:\\|(.*?))?]]" + // Obsidian link
                    "|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)" // теги
        )
    }

    // зберігаємо знайдені теги окремо
    val foundTags = remember { mutableStateListOf<Pair<String, String>>() }
    foundTags.clear()

    Column(modifier = modifier) {
        text.lines().forEach { line ->
            val listMatch = listRegex.find(line)
            val (content, isList) =
                if (listMatch != null) {
                    listMatch.destructured.component2() to true
                } else {
                    line to false
                }

            val annotatedLine = applyEnhancedInlineStyles(
                content,
                inlineContentRegex,
                linkColor,
                isCompleted,
                onTagFound = { symbol, name ->
                    foundTags.add(symbol to name)
                }
            )

            val fullLine =
                if (isList) {
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("• ") }
                        append(annotatedLine)
                    }
                } else {
                    annotatedLine
                }

            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

            Text(
                text = fullLine,
                style = finalTextStyle.merge(MaterialTheme.typography.bodyLarge),
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { pos ->
                            layoutResult?.let { layout ->
                                val offset = layout.getOffsetForPosition(pos)
                                val obsidianLinkClicked =
                                    annotatedLine.getStringAnnotations("OBSIDIAN_LINK", offset, offset)
                                        .firstOrNull()

                                when {
                                    obsidianLinkClicked != null -> {
                                        val noteName = obsidianLinkClicked.item
                                        if (obsidianVaultName.isNotBlank()) {
                                            try {
                                                val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                                                val encodedFile = URLEncoder.encode(noteName, "UTF-8")
                                                val uri = Uri.parse("obsidian://open?vault=$encodedVault&file=$encodedFile")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            } catch (e: ActivityNotFoundException) {
                                                Toast.makeText(context, "Obsidian не встановлено", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Помилка відкриття: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    else -> onTextClick()
                                }
                            }
                        },
                    )
                },
                onTextLayout = { layoutResult = it },
            )
        }

        // показуємо всі знайдені теги окремо в рядку
        if (foundTags.isNotEmpty()) {
            FlowRow(modifier = Modifier.padding(top = 0.dp)) {
                foundTags.forEach { (symbol, name) ->
                    val fullTag = "$symbol$name"
                    InlineTagChip(
                        text = fullTag,
                        tagType = if (symbol == "#") TagType.HASHTAG else TagType.PROJECT,
                        onClick = { onTagClick(fullTag) },
                        isInCompletedText = isCompleted,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun applyEnhancedInlineStyles(
    content: String,
    regex: Regex,
    linkColor: Color,
    isCompleted: Boolean,
    onTagFound: (String, String) -> Unit,
): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(content)) {
            append(content.substring(lastIndex, match.range.first))

            when {
                match.groups[2] != null -> appendBoldText(match.groups[2]!!.value)
                match.groups[4] != null -> appendItalicText(match.groups[4]!!.value)
                match.groups[6] != null -> appendStrikethroughText(match.groups[6]!!.value)
                match.groups[7] != null -> appendObsidianLink(
                    match.groups[8]!!.value,
                    match.groups[9]?.value,
                    linkColor,
                    isCompleted
                )
                match.groups[10] != null -> {
                    val symbol = match.groups[10]!!.value
                    val name = match.groups[11]!!.value
                    // в текст не додаємо чіп, просто записуємо тег
                    append(name)
                    onTagFound(symbol, name)
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < content.length) {
            append(content.substring(lastIndex))
        }
    }
}

private fun AnnotatedString.Builder.appendBoldText(text: String) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
}

private fun AnnotatedString.Builder.appendItalicText(text: String) {
    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) { append(text) }
}

private fun AnnotatedString.Builder.appendStrikethroughText(text: String) {
    withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(text) }
}

private fun AnnotatedString.Builder.appendObsidianLink(
    linkTarget: String,
    linkText: String?,
    linkColor: Color,
    isCompleted: Boolean
) {
    val displayText = linkText ?: linkTarget
    val decoration = if (isCompleted) {
        TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
    } else {
        TextDecoration.Underline
    }

    pushStringAnnotation("OBSIDIAN_LINK", linkTarget)
    withStyle(style = SpanStyle(color = linkColor, textDecoration = decoration)) {
        append(displayText)
    }
    pop()
}
