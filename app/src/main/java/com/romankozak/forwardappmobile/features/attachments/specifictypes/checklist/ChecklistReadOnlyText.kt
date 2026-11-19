package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

@Composable
internal fun ChecklistReadOnlyText(
    text: String,
    onWikiLinkClick: (String) -> Unit,
    onPlainTextClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val wikiLinkRegex = Regex("\\[\\[([^\\[\\]]+)\\]\\]")

    val annotatedText =
        remember(text, accentColor, textColor) {
            buildAnnotatedString {
                var cursor = 0
                for (match in wikiLinkRegex.findAll(text)) {
                    if (match.range.first > cursor) {
                        withStyle(SpanStyle(color = textColor)) {
                            append(text.substring(cursor, match.range.first))
                        }
                    }
                    val linkValue = match.groupValues[1]
                    val start = length
                    append(extractChecklistWikiDisplay(linkValue))
                    val end = length
                    addStyle(
                        style = SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline),
                        start = start,
                        end = end,
                    )
                    addStringAnnotation(
                        tag = "wikilink",
                        annotation = linkValue,
                        start = start,
                        end = end,
                    )
                    cursor = match.range.last + 1
                }
                if (cursor < text.length) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(text.substring(cursor))
                    }
                }
            }
        }

    ClickableText(
        text = annotatedText,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        onClick = { offset ->
            val link =
                if (offset < annotatedText.length) {
                    annotatedText.getStringAnnotations("wikilink", offset, offset).firstOrNull()
                } else {
                    null
                }
            if (link != null) {
                onWikiLinkClick(link.item)
            } else {
                onPlainTextClick(sourceOffsetForRenderedOffset(text, offset, wikiLinkRegex))
            }
        },
    )
}

private fun sourceOffsetForRenderedOffset(
    text: String,
    renderedOffset: Int,
    wikiLinkRegex: Regex,
): Int {
    var hiddenCharacters = 0
    for (match in wikiLinkRegex.findAll(text)) {
        val displayLength = extractChecklistWikiDisplay(match.groupValues[1]).length
        val displayEnd = match.range.first - hiddenCharacters + displayLength
        if (renderedOffset < displayEnd) break
        hiddenCharacters += match.value.length - displayLength
    }
    return (renderedOffset + hiddenCharacters).coerceIn(0, text.length)
}

