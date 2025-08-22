package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.ui.components.MarkdownText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ParsedGoalData(val icons: List<String>, val mainText: String)

private fun parseTextAndExtractIcons(
    text: String,
    contextMarkerToEmojiMap: Map<String, String>
): ParsedGoalData {
    var currentText = text
    val foundIcons = mutableSetOf<String>()

    val hardcodedIcons = mapOf(
        "🔥" to listOf("@critical", "! ", "!"),
        "⭐" to listOf("@day", "+"),
        "📌" to listOf("@week", "++"),
        "🗓️" to listOf("@month"),
        "🎯" to listOf("+++ "),
        "🔭" to listOf("~ ", "~"),
        "✨" to listOf("@str"),
        "🌫️" to listOf("@unclear")
    )

    hardcodedIcons.forEach { (icon, markers) ->
        markers.forEach { marker ->
            val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))")
            if (regex.containsMatchIn(currentText)) {
                currentText = currentText.replace(regex, "")
                foundIcons.add(icon)
            }
        }
    }

    contextMarkerToEmojiMap.forEach { (marker, emoji) ->
        val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))", RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(currentText)) {
            currentText = currentText.replace(regex, "")
            foundIcons.add(emoji)
        }
    }

    currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
    val cleanedText = currentText.replace(Regex("\\s\\s+"), " ").trim()
    return ParsedGoalData(icons = foundIcons.toList(), mainText = cleanedText)
}

fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(date)
}
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun GoalItem(
    goal: Goal,
    obsidianVaultName: String,
    onToggle: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String>,
) {
    val parsedData = remember(goal.text, contextMarkerToEmojiMap) {
        parseTextAndExtractIcons(goal.text, contextMarkerToEmojiMap)
    }

    val contentAlpha = if (goal.completed) 0.5f else 1.0f

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Чекбокс без додаткових модифікаторів
            Box(modifier = Modifier.wrapContentSize()) {
                CustomCheckbox(
                    checked = goal.completed,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Основний контент
            Box(
                modifier = Modifier
                    .weight(1f)

            ) {
                Column {
                    MarkdownText(
                        text = parsedData.mainText,
                        isCompleted = goal.completed,
                        obsidianVaultName = obsidianVaultName,
                        onTagClick = onTagClick,
                        onTextClick = onItemClick,
                        onLongClick = onLongClick,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    val hasStatusContent = goal.scoringStatus != ScoringStatus.NOT_ASSESSED ||
                            !goal.relatedLinks.isNullOrEmpty() ||
                            !goal.description.isNullOrBlank() ||
                            parsedData.icons.isNotEmpty()

                    if (hasStatusContent) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScoreStatusBadge(goal = goal)

                            parsedData.icons
                                .filterNot { it == emojiToHide }
                                .forEach { icon ->
                                    Text(
                                        text = icon,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontSize = 16.sp,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }

                            if (!goal.description.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Outlined.StickyNote2,
                                    contentDescription = "Contains a note",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.CenterVertically)
                                )
                            }

                            goal.relatedLinks?.forEach { link ->
                                RelatedLinkChip(link = link, onClick = { onRelatedLinkClick(link) })
                            }
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Перетягнути для сортування",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = dragHandleModifier
                    .padding(end = 8.dp)
                    .size(32.dp) // достатній розмір для захвату
            )

        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun RelatedLinkChip(link: RelatedLink, onClick: () -> Unit) {
    val icon = when (link.type) {
        LinkType.GOAL_LIST -> Icons.Default.ListAlt
        LinkType.NOTE -> Icons.Default.Notes
        LinkType.URL -> Icons.Default.Link
        LinkType.OBSIDIAN -> Icons.Default.Book
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = link.type.name, modifier = Modifier.size(14.dp))
            Text(
                text = link.displayName ?: link.target,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScoreStatusBadge(goal: Goal) {
    when (goal.scoringStatus) {
        ScoringStatus.ASSESSED -> {
            if (goal.displayScore > 0) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = "Оцінено",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${goal.displayScore}/100",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        ScoringStatus.IMPOSSIBLE_TO_ASSESS -> {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOff,
                    contentDescription = "Неможливо оцінити",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        ScoringStatus.NOT_ASSESSED -> {
            // Нічого не відображаємо
        }
    }
}