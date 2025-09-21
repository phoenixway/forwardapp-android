package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.EnhancedTagChip
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.TagType

// Utility functions for tag handling
object TagUtils {
    /**
     * Parse text and extract all tags
     */
    fun extractTags(text: String): List<ParsedTag> {
        val tagRegex = Regex("([#@])(\\p{L}[\\p{L}0-9_-]*\\b)")
        return tagRegex.findAll(text).map { match ->
            val symbol = match.groups[1]!!.value
            val name = match.groups[2]!!.value
            ParsedTag(
                fullTag = "$symbol$name",
                name = name,
                type = if (symbol == "#") TagType.HASHTAG else TagType.PROJECT
            )
        }.toList()
    }

    /**
     * Remove tags from text for clean display
     */
    fun removeTagsFromText(text: String): String {
        val tagRegex = Regex("([#@])(\\p{L}[\\p{L}0-9_-]*\\b)")
        return text.replace(tagRegex, "").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Get tag frequency from a list of texts
     */
    fun getTagFrequency(texts: List<String>): Map<String, Int> {
        return texts.flatMap { extractTags(it) }
            .groupBy { it.fullTag }
            .mapValues { it.value.size }
    }
}

data class ParsedTag(
    val fullTag: String,
    val name: String,
    val type: TagType
)

// Animated tag collection component
@Composable
fun AnimatedTagCollection(
    tags: List<String>,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedTags: Set<String> = emptySet(),
    maxVisibleTags: Int = 5,
    showAddButton: Boolean = false,
    onAddTag: (() -> Unit)? = null,
) {
    if (tags.isEmpty()) return

    val visibleTags = if (maxVisibleTags > 0) tags.take(maxVisibleTags) else tags
    val hasMoreTags = tags.size > maxVisibleTags && maxVisibleTags > 0

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(visibleTags, key = { it }) { tag ->
            var isVisible by remember { mutableStateOf(false) }
            val index = visibleTags.indexOf(tag)

            LaunchedEffect(tag) {
                kotlinx.coroutines.delay(index * 100L)
                isVisible = true
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn() + scaleIn(initialScale = 0.8f),
                exit = slideOutHorizontally() + fadeOut() + scaleOut()
            ) {
                EnhancedTagChip(
                    text = tag,
                    onClick = { onTagClick(tag) },
                    isDismissible = false,
                    isSelected = tag in selectedTags,
                    tagType = if (tag.startsWith("#")) TagType.HASHTAG else TagType.PROJECT
                )
            }
        }

        if (hasMoreTags) {
            item {
                MoreTagsIndicator(
                    count = tags.size - maxVisibleTags,
                    onClick = { /* Handle show more tags */ }
                )
            }
        }

        if (showAddButton && onAddTag != null) {
            item {
                AddTagButton(onClick = onAddTag)
            }
        }
    }
}

@Composable
private fun MoreTagsIndicator(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EnhancedTagChip(
        text = "+$count",
        onClick = onClick,
        isDismissible = false,
        tagType = TagType.HASHTAG,
        modifier = modifier
    )
}

@Composable
private fun AddTagButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Add,
            contentDescription = "Додати тег",
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        androidx.compose.material3.Text(
            text = "Тег",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
        )
    }
}

// Tag filtering and searching utilities
@Composable
fun TagSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String> = emptyList(),
    onSuggestionClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    placeholder: String = "Пошук тегів..."
) {
    Column(modifier = modifier) {
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                androidx.compose.material3.Text(placeholder)
            },
            leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Search,
                    contentDescription = "Пошук"
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    androidx.compose.material3.IconButton(
                        onClick = { onQueryChange("") }
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                            contentDescription = "Очистити"
                        )
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (suggestions.isNotEmpty() && query.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(suggestions.take(5)) { suggestion ->
                    androidx.compose.material3.SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            androidx.compose.material3.Text(
                                suggestion,
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

// Extension functions for tag operations
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