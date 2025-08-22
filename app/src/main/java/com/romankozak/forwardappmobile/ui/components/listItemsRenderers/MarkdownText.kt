package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.sp
import java.net.URLEncoder

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isCompleted: Boolean = false,
    obsidianVaultName: String = "",
    onTagClick: (String) -> Unit = {},
    onTextClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val tagColor = MaterialTheme.colorScheme.primary
    val projectColor = MaterialTheme.colorScheme.tertiary
    val linkColor = MaterialTheme.colorScheme.secondary

    val finalTextStyle = if (isCompleted) {
        style.copy(
            textDecoration = TextDecoration.LineThrough,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    } else {
        style.copy(
            color = style.color.takeUnless { it.isUnspecified } ?: MaterialTheme.colorScheme.onSurface
        )
    }

    val listRegex = remember { Regex("^\\s*([*+-])\\s+(.*)") }
    val inlineContentRegex = remember {
        Regex(
            "(\\*\\*|__)(.*?)\\1" +
                    "|(\\*|_)(.*?)\\3" +
                    "|(~~)(.*?)\\5" +
                    "|(\\[\\[)(.*?)(?:\\|(.*?))?]]" +
                    "|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)"
        )
    }

    text.lines().forEach { line ->
        val listMatch = listRegex.find(line)
        val (content, isList) = if (listMatch != null) {
            listMatch.destructured.component2() to true
        } else line to false

        val annotatedLine = applyInlineStyles(content, inlineContentRegex, tagColor, projectColor, linkColor, isCompleted)

        val fullLine = if (isList) {
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("•  ") }
                append(annotatedLine)
            }
        } else annotatedLine

        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Text(
            text = fullLine,
            style = finalTextStyle.merge(MaterialTheme.typography.bodyLarge),
            modifier = modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { pos ->
                        layoutResult?.let { layout ->
                            val offset = layout.getOffsetForPosition(pos)
                            val searchTermClicked = annotatedLine.getStringAnnotations("SEARCH_TERM", offset, offset).firstOrNull()
                            val obsidianLinkClicked = annotatedLine.getStringAnnotations("OBSIDIAN_LINK", offset, offset).firstOrNull()

                            when {
                                searchTermClicked != null -> onTagClick(searchTermClicked.item)
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
                    }
                )
            },
            onTextLayout = { layoutResult = it }
        )
    }
}

private fun applyInlineStyles(
    content: String,
    regex: Regex,
    tagColor: Color,
    projectColor: Color,
    linkColor: Color,
    isCompleted: Boolean
): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(content)) {
            append(content.substring(lastIndex, match.range.first))
            val (inlineContent, inlineStyle, annotation) = when {
                match.groups[2] != null -> Triple(match.groups[2]!!.value, SpanStyle(fontWeight = FontWeight.Bold), null)
                match.groups[4] != null -> Triple(match.groups[4]!!.value, SpanStyle(fontStyle = FontStyle.Italic), null)
                match.groups[6] != null -> Triple(match.groups[6]!!.value, SpanStyle(textDecoration = TextDecoration.LineThrough), null)
                match.groups[7] != null -> {
                    val linkTarget = match.groups[8]!!.value
                    val linkText = match.groups[9]?.value
                    val displayText = linkText ?: linkTarget
                    val decoration = if (isCompleted) TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    else TextDecoration.Underline
                    Triple(displayText, SpanStyle(color = linkColor, textDecoration = decoration), "OBSIDIAN_LINK" to linkTarget)
                }
                match.groups[10] != null -> {
                    val tagSymbol = match.groups[10]!!.value
                    val tagName = match.groups[11]!!.value
                    val fullTag = "$tagSymbol$tagName"
                    Triple(
                        fullTag,
                        SpanStyle(
                            color = if (tagSymbol == "#") tagColor else projectColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        "SEARCH_TERM" to fullTag
                    )
                }
                else -> Triple("", SpanStyle(), null)
            }

            if (annotation != null) pushStringAnnotation(annotation.first, annotation.second)
            withStyle(style = inlineStyle) { append(inlineContent) }
            if (annotation != null) pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) append(content.substring(lastIndex))
    }
}
