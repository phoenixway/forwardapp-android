package com.romankozak.forwardappmobile.ui.components

import android.R.attr.scaleY
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.backlog.components.EnhancedTagChip
import com.romankozak.forwardappmobile.ui.screens.backlog.components.TagType

@Composable
fun SuggestionChipsRow(
    visible: Boolean,
    contexts: List<String> = emptyList(),
    tags: List<String> = emptyList(),
    onContextClick: (String) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(),
        modifier = modifier
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            // Context suggestions (@contexts)
            items(contexts.take(5), key = { "context_$it" }) { context ->
                EnhancedSuggestionChip(
                    text = "@$context",
                    onClick = { onContextClick(context) },
                    icon = Icons.Default.Tag,
                    chipType = SuggestionChipType.CONTEXT
                )
            }

            // Tag suggestions (#tags)
            items(tags.take(5), key = { "tag_$it" }) { tag ->
                EnhancedSuggestionChip(
                    text = tag,
                    onClick = { onTagClick(tag) },
                    icon = Icons.Default.Tag,
                    chipType = if (tag.startsWith("#")) SuggestionChipType.HASHTAG else SuggestionChipType.PROJECT
                )
            }
        }
    }
}

enum class SuggestionChipType {
    CONTEXT,
    HASHTAG,
    PROJECT
}

@Composable
private fun EnhancedSuggestionChip(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chipType: SuggestionChipType,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }

    val colors = when (chipType) {
        SuggestionChipType.CONTEXT -> SuggestionChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
        )
        SuggestionChipType.HASHTAG -> SuggestionChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        SuggestionChipType.PROJECT -> SuggestionChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        )
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "suggestion_chip_scale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(20.dp),
        color = colors.containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderColor),
        contentColor = colors.contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = colors.contentColor
            )
        }
    }
}

@Stable
private data class SuggestionChipColors(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val borderColor: androidx.compose.ui.graphics.Color
)

// Helper functions for text parsing
object SuggestionUtils {
    fun getCurrentWord(text: String, cursorPosition: Int): Pair<String?, Int>? {
        if (cursorPosition <= 0 || cursorPosition > text.length) return null

        val textUpToCursor = text.substring(0, cursorPosition)
        val lastSpaceIndex = textUpToCursor.lastIndexOf(' ')
        val startIndex = if (lastSpaceIndex == -1) 0 else lastSpaceIndex + 1
        val currentWord = textUpToCursor.substring(startIndex)

        return if (currentWord.isNotEmpty() && (currentWord.startsWith("@") || currentWord.startsWith("#"))) {
            Pair(currentWord, startIndex)
        } else null
    }

    fun replaceCurrentWord(
        originalText: String,
        cursorPosition: Int,
        newWord: String
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