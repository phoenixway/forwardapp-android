// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/components/GoalItemUI.kt
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Допоміжна логіка для парсингу іконок ---
private enum class IconCategory { IMPORTANCE, SCALE, ACTIVITY, CUSTOM }
private data class IconConfig(val icon: String, val markers: List<String>, val category: IconCategory)

private val ICON_CONFIGS: List<IconConfig> = listOf(
    IconConfig("🔥", listOf("@critical", "! ", "!"), IconCategory.IMPORTANCE),
    IconConfig("⭐", listOf("@day", "+"), IconCategory.IMPORTANCE),
    IconConfig("📌", listOf("@week", "++"), IconCategory.SCALE),
    IconConfig("🗓️", listOf("@month"), IconCategory.SCALE),
    IconConfig("🎯", listOf("@middle", "+++ "), IconCategory.SCALE),
    IconConfig("🔭", listOf("@long", "~ ", "~"), IconCategory.SCALE),
    IconConfig("✨", listOf("@str"), IconCategory.SCALE),
    IconConfig("🛒", listOf("@buy"), IconCategory.ACTIVITY), // ✨ ДОДАНО
    IconConfig("🛠️", listOf("@manual"), IconCategory.ACTIVITY),
    IconConfig("🧠", listOf("@mental", "@pm"), IconCategory.ACTIVITY),
    IconConfig("📱", listOf("@device"), IconCategory.ACTIVITY),
    IconConfig("⛓️", listOf("@providence"), IconCategory.CUSTOM), // ✨ ДОДАНО
    IconConfig("🔬", listOf("@research"), IconCategory.CUSTOM),
    IconConfig("🌫️", listOf("@unclear"), IconCategory.CUSTOM)
)
private data class ParsedGoalData(val icons: List<IconConfig>, val mainText: String)
private fun parseTextAndExtractIcons(text: String): ParsedGoalData {
    var currentText = text
    val foundIcons = mutableSetOf<IconConfig>()
    ICON_CONFIGS.forEach { config ->
        config.markers.forEach { marker ->
            val regex = Regex("(^|\\s)(${Regex.escape(marker)})(\\s|$)")
            if (regex.containsMatchIn(currentText)) {
                currentText = currentText.replace(regex, "$1$3")
                foundIcons.add(config)
            }
        }
    }
    currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
    val sortedIcons = foundIcons.sortedBy { it.category.ordinal }
    val cleanedText = currentText.replace(Regex("\\s\\s+"), " ").trim()
    return ParsedGoalData(icons = sortedIcons, mainText = cleanedText)
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
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onToggle: () -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    // ✨ ДОДАНО: Новий параметр для приховування іконки контексту
    contextMarkerToHide: String? = null
) {
    val parsedData = remember(goal.text) { parseTextAndExtractIcons(goal.text) }
    val contentAlpha = if (goal.completed) 0.5f else 1.0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(onItemClick, onLongClick) {
                    detectTapGestures(
                        onLongPress = { onLongClick() },
                        onTap = { onItemClick() }
                    )
                }
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                .alpha(contentAlpha),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomCheckbox(
                checked = goal.completed,
                onCheckedChange = { onToggle() },
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = Color.Transparent,
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                        parsedData.icons.isNotEmpty() ||
                        associatedLists.isNotEmpty() ||
                        !goal.description.isNullOrBlank()

                if (hasStatusContent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ScoreStatusBadge(goal = goal)

                        // ✨ ЗМІНЕНО: Фільтруємо іконки перед відображенням
                        parsedData.icons
                            .filterNot { it.markers.contains(contextMarkerToHide) }
                            .forEach { iconData ->
                                Text(
                                    text = iconData.icon,
                                    style = MaterialTheme.typography.labelMedium,
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

                        associatedLists.forEach { list ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .clickable { onAssociatedListClick(list.id) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            IconButton(
                onClick = { /* Клік на ручці нічого не робить */ },
                modifier = dragHandleModifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Перетягнути для сортування",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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