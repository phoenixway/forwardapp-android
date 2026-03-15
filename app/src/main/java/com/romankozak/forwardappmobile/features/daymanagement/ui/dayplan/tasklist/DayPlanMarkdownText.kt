package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private const val COMPLETED_TEXT_ALPHA = 0.6f
private const val BOLD_GROUP_INDEX = 2
private const val ITALIC_GROUP_INDEX = 4
private const val STRIKETHROUGH_GROUP_INDEX = 6
private const val WIKI_LINK_GROUP_INDEX = 7
private const val WIKI_LINK_TARGET_GROUP_INDEX = 8
private const val WIKI_LINK_TEXT_GROUP_INDEX = 9
private const val TAG_SYMBOL_GROUP_INDEX = 10
private const val TAG_NAME_GROUP_INDEX = 11
private val TAG_FONT_SIZE = 13.sp

private data class MarkdownColorPalette(
    val tagColor: Color,
    val projectColor: Color,
    val linkColor: Color,
)

@Composable
fun DayPlanMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isCompleted: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    val palette =
        MarkdownColorPalette(
            tagColor = MaterialTheme.colorScheme.primary,
            projectColor = MaterialTheme.colorScheme.tertiary,
            linkColor = MaterialTheme.colorScheme.secondary,
        )

    val finalTextStyle =
        if (isCompleted) {
            style.copy(
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = COMPLETED_TEXT_ALPHA),
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

    val annotatedString =
        buildAnnotatedString {
            text.lines().forEachIndexed { index, line ->
                val listMatch = listRegex.find(line)
                val (content, isList) =
                    if (listMatch != null) {
                        listMatch.destructured.component2() to true
                    } else {
                        line to false
                    }

                val annotatedLine =
                    applyInlineStyles(
                        content = content,
                        regex = inlineContentRegex,
                        palette = palette,
                        isCompleted = isCompleted,
                    )

                if (isList) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { append("•  ") }
                }
                append(annotatedLine)

                if (index < text.lines().size - 1) {
                    append("\n")
                }
            }
        }

    Text(
        text = annotatedString,
        style = finalTextStyle,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun applyInlineStyles(
    content: String,
    regex: Regex,
    palette: MarkdownColorPalette,
    isCompleted: Boolean,
): AnnotatedString =
    buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(content)) {
            append(content.substring(lastIndex, match.range.first))
            val (inlineContent, inlineStyle) =
                when {
                    match.groups[BOLD_GROUP_INDEX] != null ->
                        match.groups[BOLD_GROUP_INDEX]!!.value to SpanStyle(fontWeight = FontWeight.Bold)
                    match.groups[ITALIC_GROUP_INDEX] != null ->
                        match.groups[ITALIC_GROUP_INDEX]!!.value to SpanStyle(fontStyle = FontStyle.Italic)
                    match.groups[STRIKETHROUGH_GROUP_INDEX] != null ->
                        match.groups[STRIKETHROUGH_GROUP_INDEX]!!.value to
                            SpanStyle(textDecoration = TextDecoration.LineThrough)
                    match.groups[WIKI_LINK_GROUP_INDEX] != null -> {
                        val linkTarget = match.groups[WIKI_LINK_TARGET_GROUP_INDEX]!!.value
                        val linkText = match.groups[WIKI_LINK_TEXT_GROUP_INDEX]?.value
                        val displayText = linkText ?: linkTarget
                        val decoration =
                            if (isCompleted) {
                                TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                            } else {
                                TextDecoration.Underline
                            }
                        displayText to SpanStyle(color = palette.linkColor, textDecoration = decoration)
                    }
                    match.groups[TAG_SYMBOL_GROUP_INDEX] != null -> {
                        val tagSymbol = match.groups[TAG_SYMBOL_GROUP_INDEX]!!.value
                        val tagName = match.groups[TAG_NAME_GROUP_INDEX]!!.value
                        val fullTag = "$tagSymbol$tagName"
                        fullTag to
                            SpanStyle(
                                color = if (tagSymbol == "#") palette.tagColor else palette.projectColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = TAG_FONT_SIZE,
                            )
                    }
                    else -> "" to SpanStyle()
                }

            withStyle(style = inlineStyle) { append(inlineContent) }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) append(content.substring(lastIndex))
    }
