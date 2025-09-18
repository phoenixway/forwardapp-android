// file: ui/screens/daymanagement/tasklist/DayPlanMarkdownText.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Спрощена версія MarkdownText спеціально для екрану Плану Дня.
 * Цей компонент НЕ обробляє кліки чи довгі натискання,
 * дозволяючи батьківським елементам повністю контролювати жести.
 */
@Composable
fun DayPlanMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isCompleted: Boolean = false,
) {
    val tagColor = MaterialTheme.colorScheme.primary
    val projectColor = MaterialTheme.colorScheme.tertiary
    val linkColor = MaterialTheme.colorScheme.secondary

    val finalTextStyle =
        if (isCompleted) {
            style.copy(
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        } else {
            style.copy(
                color = style.color.takeUnless { it.isUnspecified } ?: MaterialTheme.colorScheme.onSurface,
            )
        }

    val listRegex = remember { Regex("^\\s*([*+-])\\s+(.*)") }
    val inlineContentRegex =
        remember {
            Regex(
                "(\\*\\*|__)(.*?)\\1" +
                        "|(\\*|_)(.*?)\\3" +
                        "|(~~)(.*?)\\5" +
                        "|(\\[\\[)(.*?)(?:\\|(.*?))?]]" +
                        "|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)",
            )
        }

    text.lines().forEach { line ->
        val listMatch = listRegex.find(line)
        val (content, isList) =
            if (listMatch != null) {
                listMatch.destructured.component2() to true
            } else {
                line to false
            }

        val annotatedLine = applyInlineStyles(content, inlineContentRegex, tagColor, projectColor, linkColor, isCompleted)

        val fullLine =
            if (isList) {
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("•  ") }
                    append(annotatedLine)
                }
            } else {
                annotatedLine
            }

        Text(
            text = fullLine,
            style = finalTextStyle, // Видалено merge для гнучкості
            modifier = modifier
        )
    }
}

private fun applyInlineStyles(
    content: String,
    regex: Regex,
    tagColor: Color,
    projectColor: Color,
    linkColor: Color,
    isCompleted: Boolean,
): AnnotatedString =
    buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(content)) {
            append(content.substring(lastIndex, match.range.first))
            val (inlineContent, inlineStyle) =
                when {
                    match.groups[2] != null -> match.groups[2]!!.value to SpanStyle(fontWeight = FontWeight.Bold)
                    match.groups[4] != null -> match.groups[4]!!.value to SpanStyle(fontStyle = FontStyle.Italic)
                    match.groups[6] != null -> match.groups[6]!!.value to SpanStyle(textDecoration = TextDecoration.LineThrough)
                    match.groups[7] != null -> {
                        val linkTarget = match.groups[8]!!.value
                        val linkText = match.groups[9]?.value
                        val displayText = linkText ?: linkTarget
                        val decoration =
                            if (isCompleted) {
                                TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                            } else {
                                TextDecoration.Underline
                            }
                        displayText to SpanStyle(color = linkColor, textDecoration = decoration)
                    }
                    match.groups[10] != null -> {
                        val tagSymbol = match.groups[10]!!.value
                        val tagName = match.groups[11]!!.value
                        val fullTag = "$tagSymbol$tagName"
                        fullTag to SpanStyle(
                            color = if (tagSymbol == "#") tagColor else projectColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                    else -> "" to SpanStyle()
                }

            withStyle(style = inlineStyle) { append(inlineContent) }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) append(content.substring(lastIndex))
    }