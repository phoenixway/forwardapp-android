package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import java.net.URLEncoder

private const val OBSIDIAN_LINK_TAG = "OBSIDIAN_LINK"
private const val MARKDOWN_TEXT_TAG = "MarkdownText"
private const val BOLD_GROUP_INDEX = 2
private const val ITALIC_GROUP_INDEX = 4
private const val STRIKETHROUGH_GROUP_INDEX = 6
private const val OBSIDIAN_LINK_MARKER_GROUP_INDEX = 7
private const val OBSIDIAN_LINK_TARGET_GROUP_INDEX = 8
private const val OBSIDIAN_LINK_TEXT_GROUP_INDEX = 9
private const val TAG_SYMBOL_GROUP_INDEX = 10
private const val TAG_NAME_GROUP_INDEX = 11
private const val TAG_PLACEHOLDER_CHAR_WIDTH = 7
private const val TAG_PLACEHOLDER_HORIZONTAL_PADDING = 14
private const val TAG_PLACEHOLDER_HEIGHT_SP = 20

data class MarkdownTextState(
    val text: String,
    val style: TextStyle,
    val isCompleted: Boolean = false,
    val obsidianVaultName: String = "",
    val maxLines: Int = Int.MAX_VALUE,
)

data class MarkdownTextActions(
    val onTagClick: (String) -> Unit = {},
    val onTextClick: () -> Unit = {},
    val onLongClick: () -> Unit = {},
)

private data class MarkdownInlineTagSpec(
    val tagId: String,
    val fullTag: String,
    val tagType: TagType,
    val isCompleted: Boolean,
)

private data class MarkdownTapContext(
    val fullAnnotatedString: AnnotatedString,
    val inlineContentMap: Map<String, InlineTextContent>,
    val state: MarkdownTextState,
    val context: android.content.Context,
    val onTextClick: () -> Unit,
)

@Composable
fun MarkdownText(
    state: MarkdownTextState,
    actions: MarkdownTextActions = MarkdownTextActions(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.secondary
    val finalTextStyle = state.resolveFinalTextStyle()

    val (fullAnnotatedString, inlineContentMap) =
        remember(state.text, state.isCompleted) {
            buildMarkdownPresentation(
                state = state,
                linkColor = linkColor,
                onTagClick = actions.onTagClick,
            )
        }

    Column(modifier = modifier) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        val tapContext =
            MarkdownTapContext(
                fullAnnotatedString = fullAnnotatedString,
                inlineContentMap = inlineContentMap,
                state = state,
                context = context,
                onTextClick = actions.onTextClick,
            )

        Text(
            text = fullAnnotatedString,
            style = finalTextStyle.merge(MaterialTheme.typography.bodyLarge),
            maxLines = state.maxLines,
            overflow = TextOverflow.Ellipsis,
            inlineContent = inlineContentMap,
            modifier =
                Modifier.markdownTextPointerInput(
                    layoutResult = layoutResult,
                    tapContext = tapContext,
                    onLongClick = actions.onLongClick,
                ),
            onTextLayout = { layoutResult = it },
        )
    }
}

@Composable
private fun MarkdownTextState.resolveFinalTextStyle(): TextStyle =
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

private fun buildMarkdownPresentation(
    state: MarkdownTextState,
    linkColor: Color,
    onTagClick: (String) -> Unit,
): Pair<AnnotatedString, Map<String, InlineTextContent>> {
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

    for (match in inlineContentRegex.findAll(state.text)) {
        builder.append(state.text.substring(lastIndex, match.range.first))
        builder.appendMarkdownMatch(
            match = match,
            state = state,
            linkColor = linkColor,
            inlineContentMap = map,
            onTagClick = onTagClick,
        )
        lastIndex = match.range.last + 1
    }

    if (lastIndex < state.text.length) {
        builder.append(state.text.substring(lastIndex))
    }

    return builder.toAnnotatedString() to map
}

private fun AnnotatedString.Builder.appendMarkdownMatch(
    match: MatchResult,
    state: MarkdownTextState,
    linkColor: Color,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    onTagClick: (String) -> Unit,
) {
    when {
        match.groups[BOLD_GROUP_INDEX] != null ->
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groups[BOLD_GROUP_INDEX]!!.value)
            }
        match.groups[ITALIC_GROUP_INDEX] != null ->
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(match.groups[ITALIC_GROUP_INDEX]!!.value)
            }
        match.groups[STRIKETHROUGH_GROUP_INDEX] != null ->
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(match.groups[STRIKETHROUGH_GROUP_INDEX]!!.value)
            }
        match.groups[OBSIDIAN_LINK_MARKER_GROUP_INDEX] != null -> {
            val linkTarget = match.groups[OBSIDIAN_LINK_TARGET_GROUP_INDEX]!!.value
            val linkText = match.groups[OBSIDIAN_LINK_TEXT_GROUP_INDEX]?.value
            appendObsidianLink(linkTarget, linkText, linkColor, state.isCompleted)
        }
        match.groups[TAG_SYMBOL_GROUP_INDEX] != null -> {
            val symbol = match.groups[TAG_SYMBOL_GROUP_INDEX]!!.value
            val name = match.groups[TAG_NAME_GROUP_INDEX]!!.value
            val fullTag = "$symbol$name"
            appendInlineTag(
                spec =
                    MarkdownInlineTagSpec(
                        tagId = "tag_${fullTag}_${match.range.first}",
                        fullTag = fullTag,
                        tagType = if (symbol == "#") TagType.HASHTAG else TagType.PROJECT,
                        isCompleted = state.isCompleted,
                    ),
                inlineContentMap = inlineContentMap,
                onTagClick = onTagClick,
            )
        }
    }
}

private fun AnnotatedString.Builder.appendInlineTag(
    spec: MarkdownInlineTagSpec,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    onTagClick: (String) -> Unit,
) {
    val tagPlaceholderWidth =
        (spec.fullTag.length * TAG_PLACEHOLDER_CHAR_WIDTH + TAG_PLACEHOLDER_HORIZONTAL_PADDING).sp
    val tagPlaceholderHeight = TAG_PLACEHOLDER_HEIGHT_SP.sp

    appendInlineContent(spec.tagId, spec.fullTag)
    inlineContentMap[spec.tagId] =
        InlineTextContent(
            placeholder =
                Placeholder(
                    width = tagPlaceholderWidth,
                    height = tagPlaceholderHeight,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
        ) {
            InlineTagChip(
                text = spec.fullTag,
                tagType = spec.tagType,
                onClick = { onTagClick(spec.fullTag) },
                isInCompletedText = spec.isCompleted,
            )
        }
}

private fun Modifier.markdownTextPointerInput(
    layoutResult: TextLayoutResult?,
    tapContext: MarkdownTapContext,
    onLongClick: () -> Unit,
): Modifier =
    pointerInput(layoutResult, tapContext) {
        detectTapGestures(
            onLongPress = { onLongClick() },
            onTap = { pos ->
                layoutResult?.let { layout ->
                    handleMarkdownTap(
                        layout = layout,
                        tapPosition = pos,
                        tapContext = tapContext,
                    )
                }
            },
        )
    }

private fun handleMarkdownTap(
    layout: TextLayoutResult,
    tapPosition: androidx.compose.ui.geometry.Offset,
    tapContext: MarkdownTapContext,
) {
    val offset = layout.getOffsetForPosition(tapPosition)
    val obsidianLinkClicked =
        tapContext.fullAnnotatedString.getStringAnnotations(
            OBSIDIAN_LINK_TAG,
            offset,
            offset,
        ).firstOrNull()

    if (obsidianLinkClicked != null) {
        openObsidianNote(
            context = tapContext.context,
            obsidianVaultName = tapContext.state.obsidianVaultName,
            noteName = obsidianLinkClicked.item,
        )
    } else if (!isTagClick(tapContext.inlineContentMap, tapContext.fullAnnotatedString, offset)) {
        tapContext.onTextClick()
    }
}

private fun openObsidianNote(
    context: android.content.Context,
    obsidianVaultName: String,
    noteName: String,
) {
    if (obsidianVaultName.isBlank()) return

    try {
        val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
        val encodedFile = URLEncoder.encode(noteName, "UTF-8")
        val uri = "obsidian://open?vault=$encodedVault&file=$encodedFile".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: ActivityNotFoundException) {
        Log.w(
            MARKDOWN_TEXT_TAG,
            "Obsidian app is not installed for note: $noteName",
            e,
        )
        Toast.makeText(context, "Obsidian not installed", Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.e(
            MARKDOWN_TEXT_TAG,
            "Failed to open obsidian note: $noteName",
            e,
        )
        Toast.makeText(
            context,
            "Error opening: ${e.message}",
            Toast.LENGTH_SHORT,
        ).show()
    }
}

private fun isTagClick(
    inlineContentMap: Map<String, InlineTextContent>,
    fullAnnotatedString: AnnotatedString,
    offset: Int,
): Boolean =
    inlineContentMap.keys.any { tagId ->
        fullAnnotatedString
            .getStringAnnotations(tagId, offset, offset)
            .firstOrNull() != null
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

    pushStringAnnotation(OBSIDIAN_LINK_TAG, linkTarget)
    withStyle(style = SpanStyle(color = linkColor, textDecoration = decoration)) {
        append(displayText)
    }
    pop()
}
