package com.romankozak.forwardappmobile.ui.screens.contextcreen.components.backlogitems

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    onLongClick: () -> Unit = {},
    maxLines: Int = Int.MAX_VALUE,
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
                color =
                    style.color.takeUnless { it.isUnspecified } ?: MaterialTheme.colorScheme.onSurface,
            )
        }

    val (fullAnnotatedString, inlineContentMap) =
        remember(text, isCompleted) {
val inlineContentRegex =
                Regex(
                    "(\\*\\*|__)(.*?)\\1" +
                        "|(\\*|_)(.*?)\\3" +
                        "|(~~)(.*?)\\5" +
                        "|(\\[\\[)(.*?)(?:\\|(.*?))?]]" +
                        "|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)",
                )

            val map = mutableMapOf<String, InlineTextContent>()
            val builder = AnnotatedString.Builder()
            var lastIndex = 0

            for (match in inlineContentRegex.findAll(text)) {
                builder.append(text.substring(lastIndex, match.range.first))

                when {
                    match.groups[2] != null ->
                        builder.withStyle(
                            style = SpanStyle(fontWeight = FontWeight.Bold),
                        ) { append(match.groups[2]!!.value) }
                    match.groups[4] != null ->
                        builder.withStyle(
                            style = SpanStyle(fontStyle = FontStyle.Italic),
                        ) { append(match.groups[4]!!.value) }
                    match.groups[6] != null ->
                        builder.withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(match.groups[6]!!.value)
                        }
                    match.groups[7] != null -> {
                        val linkTarget = match.groups[8]!!.value
                        val linkText = match.groups[9]?.value
                        builder.appendObsidianLink(linkTarget, linkText, linkColor, isCompleted)
                    }
                    match.groups[10] != null -> {
                        val symbol = match.groups[10]!!.value
                        val name = match.groups[11]!!.value
                        val fullTag = "$symbol$name"
                        val tagId = "tag_${fullTag}_${match.range.first}"

                        val tagPlaceholderWidth = (fullTag.length * 7 + 14).sp
                        val tagPlaceholderHeight = 20.sp

                        builder.appendInlineContent(tagId, fullTag)

                        map[tagId] =
                            InlineTextContent(
                                placeholder =
                                    Placeholder(
                                        width = tagPlaceholderWidth,
                                        height = tagPlaceholderHeight,
                                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                                    ),
                            ) {
                                InlineTagChip(
                                    text = fullTag,
                                    tagType = if (symbol == "#") TagType.HASHTAG else TagType.PROJECT,
                                    onClick = { onTagClick(fullTag) },
                                    isInCompletedText = isCompleted,
                                )
                            }
                    }
                }
                lastIndex = match.range.last + 1
            }

            if (lastIndex < text.length) {
                builder.append(text.substring(lastIndex))
            }

            builder.toAnnotatedString() to map
        }

    Column(modifier = modifier) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

        Text(
            text = fullAnnotatedString,
            style = finalTextStyle.merge(MaterialTheme.typography.bodyLarge),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            inlineContent = inlineContentMap,
            modifier =
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { pos ->
                            layoutResult?.let { layout ->
                                val offset = layout.getOffsetForPosition(pos)
                                val obsidianLinkClicked =
                                    fullAnnotatedString.getStringAnnotations("OBSIDIAN_LINK", offset, offset)
                                        .firstOrNull()

                                if (obsidianLinkClicked != null) {
                                    val noteName = obsidianLinkClicked.item
                                    if (obsidianVaultName.isNotBlank()) {
                                        try {
                                            val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                                            val encodedFile = URLEncoder.encode(noteName, "UTF-8")
                                            val uri = "obsidian://open?vault=$encodedVault&file=$encodedFile".toUri()
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(context, "Obsidian not installed", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error opening: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    val isClickOnTag =
                                        inlineContentMap.keys.any { tagId ->
                                            val tagRange = fullAnnotatedString.getStringAnnotations(tagId, offset, offset).firstOrNull()
                                            tagRange != null
                                        }
                                    if (!isClickOnTag) {
                                        onTextClick()
                                    }
                                }
                            }
                        },
                    )
                },
            onTextLayout = { layoutResult = it },
        )
    }
}

private fun AnnotatedString.Builder.appendObsidianLink(
    linkTarget: String,
    linkText: String?,
    linkColor: Color,
    isCompleted: Boolean,
) {
    val displayText = linkText ?: linkTarget
    val decoration =
        if (isCompleted) {
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
