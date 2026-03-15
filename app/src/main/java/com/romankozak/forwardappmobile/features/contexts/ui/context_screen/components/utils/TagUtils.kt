package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.EnhancedTagChip
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.EnhancedTagChipState
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.TagType

object TagUtils {
    fun extractTags(text: String): List<ParsedTag> {
        val tagRegex = Regex("([#@])(\\p{L}[\\p{L}0-9_-]*\\b)")
        return tagRegex.findAll(text).map { match ->
            val symbol = match.groups[1]!!.value
            val name = match.groups[2]!!.value
            ParsedTag(
                fullTag = "$symbol$name",
                name = name,
                type = if (symbol == "#") TagType.HASHTAG else TagType.PROJECT,
            )
        }.toList()
    }

    fun removeTagsFromText(text: String): String {
        val tagRegex = Regex("([#@])(\\p{L}[\\p{L}0-9_-]*\\b)")
        return text.replace(tagRegex, "").replace(Regex("\\s+"), " ").trim()
    }

    fun getTagFrequency(texts: List<String>): Map<String, Int> {
        return texts.flatMap { extractTags(it) }
            .groupBy { it.fullTag }
            .mapValues { it.value.size }
    }
}

data class ParsedTag(
    val fullTag: String,
    val name: String,
    val type: TagType,
)

data class AnimatedTagCollectionState(
    val tags: List<String>,
    val selectedTags: Set<String> = emptySet(),
    val maxVisibleTags: Int = 5,
    val showAddButton: Boolean = false,
)

data class TagSearchBarState(
    val query: String,
    val suggestions: List<String> = emptyList(),
    val placeholder: String = "Пошук тегів...",
)

private const val TAG_STAGGER_DELAY_MS = 100L
private const val DEFAULT_SUGGESTION_LIMIT = 5

@Composable
fun AnimatedTagCollection(
    state: AnimatedTagCollectionState,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAddTag: (() -> Unit)? = null,
) {
    if (state.tags.isEmpty()) return

    val visibleTags =
        if (state.maxVisibleTags > 0) {
            state.tags.take(state.maxVisibleTags)
        } else {
            state.tags
        }
    val hasMoreTags = state.tags.size > state.maxVisibleTags && state.maxVisibleTags > 0

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(visibleTags, key = { it }) { tag ->
            var isVisible by remember { mutableStateOf(false) }
            val index = visibleTags.indexOf(tag)

            LaunchedEffect(tag) {
                kotlinx.coroutines.delay(index * TAG_STAGGER_DELAY_MS)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter =
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 2 },
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    ) + fadeIn() + scaleIn(initialScale = 0.8f),
                exit = slideOutHorizontally() + fadeOut() + scaleOut(),
            ) {
                AnimatedTagChipItem(
                    tag = tag,
                    isSelected = tag in state.selectedTags,
                    onTagClick = onTagClick,
                )
            }
        }

        if (hasMoreTags) {
            item {
                MoreTagsIndicator(
                    count = state.tags.size - state.maxVisibleTags,
                    onClick = { },
                )
            }
        }

        if (state.showAddButton && onAddTag != null) {
            item {
                AddTagButton(onClick = onAddTag)
            }
        }
    }
}

@Composable
private fun AnimatedTagChipItem(
    tag: String,
    isSelected: Boolean,
    onTagClick: (String) -> Unit,
) {
    EnhancedTagChip(
        state =
            EnhancedTagChipState(
                text = tag,
                isDismissible = false,
                isSelected = isSelected,
                tagType = if (tag.startsWith("#")) TagType.HASHTAG else TagType.PROJECT,
            ),
        onClick = { onTagClick(tag) },
    )
}

@Composable
private fun MoreTagsIndicator(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EnhancedTagChip(
        state =
            EnhancedTagChipState(
                text = "+$count",
                isDismissible = false,
                tagType = TagType.HASHTAG,
            ),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun AddTagButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Add,
            contentDescription = "Додати тег",
            modifier = Modifier.size(12.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        androidx.compose.material3.Text(
            text = "Тег",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
fun TagSearchBar(
    state: TagSearchBarState,
    onQueryChange: (String) -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = {
                androidx.compose.material3.Text(state.placeholder)
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = "Пошук",
                )
            },
            trailingIcon =
                if (state.query.isNotEmpty()) {
                    {
                        androidx.compose.material3.IconButton(
                            onClick = { onQueryChange("") },
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                contentDescription = "Очистити",
                            )
                        }
                    }
                } else {
                    null
                },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.suggestions.isNotEmpty() && state.query.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.suggestions.take(DEFAULT_SUGGESTION_LIMIT)) { suggestion ->
                    androidx.compose.material3.SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            androidx.compose.material3.Text(
                                suggestion,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }
    }
}

fun List<String>.filterByTag(tag: String): List<String> {
    return filter { text ->
        TagUtils.extractTags(text).any { it.fullTag == tag }
    }
}

fun List<String>.getAllUniqueTags(): List<String> {
    return flatMap { TagUtils.extractTags(it) }
        .map { it.fullTag }
        .distinct()
        .sorted()
}

fun String.containsTag(tag: String): Boolean {
    return TagUtils.extractTags(this).any { it.fullTag == tag }
}
