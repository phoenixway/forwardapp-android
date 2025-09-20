package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.backlog.components.TagUtils
import com.romankozak.forwardappmobile.ui.screens.backlog.components.AnimatedTagCollection

@Composable
fun GoalTextPreview(
    text: String,
    onTagClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val extractedTags = remember(text) {
        TagUtils.extractTags(text)
    }

    val cleanText = remember(text) {
        TagUtils.removeTagsFromText(text)
    }

    AnimatedVisibility(
        visible = extractedTags.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { -it }
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it }
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cleanText.isNotEmpty()) {
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (extractedTags.isNotEmpty()) {
                    Text(
                        text = "Теги:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AnimatedTagCollection(
                        tags = extractedTags.map { it.fullTag },
                        onTagClick = onTagClick,
                        maxVisibleTags = 0, // Show all tags
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Optional: Add this component to show tag statistics
@Composable
fun TagStatistics(
    allTags: List<String>,
    modifier: Modifier = Modifier
) {
    if (allTags.isEmpty()) return

    val tagGroups = remember(allTags) {
        allTags.groupBy { tag ->
            when {
                tag.startsWith("#") -> "Хештеги"
                tag.startsWith("@") -> "Контексти"
                else -> "Інше"
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Статистика тегів",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            tagGroups.forEach { (groupName, tags) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = tags.size.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}