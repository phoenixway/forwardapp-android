package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MAX_VISIBLE_SUGGESTIONS = 5

data class SuggestionChipsState(
    val visible: Boolean,
    val contexts: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

data class SuggestionChipsActions(
    val onContextClick: (String) -> Unit = {},
    val onTagClick: (String) -> Unit = {},
)

@Composable
fun SuggestionChipsRow(
    state: SuggestionChipsState,
    actions: SuggestionChipsActions = SuggestionChipsActions(),
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.visible,
        enter =
            slideInVertically(
                initialOffsetY = { -it },
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) + fadeIn(),
        exit =
            slideOutVertically(
                targetOffsetY = { -it },
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            ) + fadeOut(),
        modifier = modifier,
    ) {
        LazyRow(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(state.contexts.take(MAX_VISIBLE_SUGGESTIONS), key = { "context_$it" }) { context ->
                EnhancedSuggestionChip(
                    text = "@$context",
                    onClick = { actions.onContextClick(context) },
                    icon = Icons.Default.Tag,
                    chipType = SuggestionChipType.CONTEXT,
                )
            }

            items(state.tags.take(MAX_VISIBLE_SUGGESTIONS), key = { "tag_$it" }) { tag ->
                EnhancedSuggestionChip(
                    text = tag,
                    onClick = { actions.onTagClick(tag) },
                    icon = Icons.Default.Tag,
                    chipType = if (tag.startsWith("#")) SuggestionChipType.HASHTAG else SuggestionChipType.PROJECT,
                )
            }
        }
    }
}

enum class SuggestionChipType {
    CONTEXT,
    HASHTAG,
    PROJECT,
}

@Composable
private fun EnhancedSuggestionChip(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector,
    chipType: SuggestionChipType,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val colors = suggestionChipColors(chipType)

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "suggestion_chip_scale",
    )

    Surface(
        onClick = onClick,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        color = colors.containerColor,
        border = BorderStroke(1.dp, colors.borderColor),
        contentColor = colors.contentColor,
    ) {
        SuggestionChipContent(
            text = text,
            icon = icon,
            colors = colors,
        )
    }
}

@Stable
private data class SuggestionChipColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
)

@Composable
private fun suggestionChipColors(chipType: SuggestionChipType): SuggestionChipColors =
    when (chipType) {
        SuggestionChipType.CONTEXT ->
            SuggestionChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
            )
        SuggestionChipType.HASHTAG ->
            SuggestionChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            )
        SuggestionChipType.PROJECT ->
            SuggestionChipColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
            )
    }

@Composable
private fun SuggestionChipContent(
    text: String,
    icon: ImageVector,
    colors: SuggestionChipColors,
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = colors.contentColor,
        )
    }
}

object SuggestionUtils {
    fun getCurrentWord(
        text: String,
        cursorPosition: Int,
    ): Pair<String?, Int>? {
        if (cursorPosition <= 0 || cursorPosition > text.length) return null

        val textUpToCursor = text.substring(0, cursorPosition)
        val lastSpaceIndex = textUpToCursor.lastIndexOf(' ')
        val startIndex = if (lastSpaceIndex == -1) 0 else lastSpaceIndex + 1
        val currentWord = textUpToCursor.substring(startIndex)

        return if (currentWord.isNotEmpty() && (currentWord.startsWith("@") || currentWord.startsWith("#"))) {
            Pair(currentWord, startIndex)
        } else {
            null
        }
    }

    fun replaceCurrentWord(
        originalText: String,
        cursorPosition: Int,
        newWord: String,
    ): Pair<String, Int>? {
        val currentWordInfo = getCurrentWord(originalText, cursorPosition) ?: return null
        val (currentWord, startIndex) = currentWordInfo

        val textBefore = originalText.substring(0, startIndex)
        val textAfter = originalText.substring(cursorPosition)
        val newText = "$textBefore$newWord $textAfter"
        val newCursorPosition = textBefore.length + newWord.length + 1

        return Pair(newText, newCursorPosition)
    }
}
