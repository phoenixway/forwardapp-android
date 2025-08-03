package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOff // ✨ ДОДАНО: Іконка для "неможливо оцінити"
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus // ✨ ДОДАНО: Імпорт статусу
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Допоміжна логіка для парсингу іконок ---
private enum class IconCategory { IMPORTANCE, SCALE, ACTIVITY, CUSTOM }
private data class IconConfig(val icon: String, val markers: List<String>, val category: IconCategory)
private val ICON_CONFIGS: List<IconConfig> = listOf(
    IconConfig("🔥", listOf("#critical", "! ", "!"), IconCategory.IMPORTANCE),
    IconConfig("⭐", listOf("#day", "+"), IconCategory.IMPORTANCE),
    IconConfig("📌", listOf("#week", "++"), IconCategory.SCALE),
    IconConfig("🗓️", listOf("#month"), IconCategory.SCALE),
    IconConfig("🎯", listOf("#middle-term", "+++ "), IconCategory.SCALE),
    IconConfig("🔭", listOf("#long-term", "~ ", "~"), IconCategory.SCALE),
    IconConfig("✨", listOf("#str"), IconCategory.SCALE),
    IconConfig("🛠️", listOf("#manual"), IconCategory.ACTIVITY),
    IconConfig("🧠", listOf("#mental", "#pm"), IconCategory.ACTIVITY),
    IconConfig("📱", listOf("#device"), IconCategory.ACTIVITY),
    IconConfig("🔬", listOf("#research"), IconCategory.CUSTOM), // ✨ ДОДАНО
    IconConfig("🌫️", listOf("#unclear"), IconCategory.CUSTOM),
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


@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class) // ✨ ЗМІНА: Додано ExperimentalFoundationApi
@Composable
fun GoalItem(
    goal: Goal,
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onToggle: () -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit, // ✨ ЗМІНА: Додано обробник довгого натискання
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    dragHandle: @Composable (() -> Unit)? = null
) {
    val parsedData = remember(goal.text) { parseTextAndExtractIcons(goal.text) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // ✨ ЗМІНА: Використовуємо combinedClickable для підтримки звичайного і довгого натискання
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onLongClick
            )
            .background(backgroundColor)
    ) {
        val contentAlpha = if (goal.completed) 0.5f else 1.0f

        Row(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                .alpha(contentAlpha), // ✨ ЗАСТОСОВУЄМО ПРОЗОРІСТЬ
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomCheckbox(
                checked = goal.completed,
                onCheckedChange = { onToggle() },
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = Color.Transparent, // Внутрішня частина буде прозорою
                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // Бліда рамка
                checkmarkColor = MaterialTheme.colorScheme.onPrimary // Колір галочки для контрасту
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                MarkdownText(
                    text = parsedData.mainText,
                    isCompleted = goal.completed,
                    obsidianVaultName = obsidianVaultName,
                    onTagClick = onTagClick,
                    onTextClick = onItemClick,
                    style = MaterialTheme.typography.bodyLarge,
                    onLongClick = onLongClick
                )

                // ✨ ЗМІНА: Перевірка наявності контенту тепер враховує scoringStatus
                val hasStatusContent = goal.scoringStatus != ScoringStatus.NOT_ASSESSED || parsedData.icons.isNotEmpty() || associatedLists.isNotEmpty()
                if (hasStatusContent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // ✨ ЗМІНА: Використовуємо новий компонент для відображення статусу
                        ScoreStatusBadge(goal = goal)

                        parsedData.icons.forEach { iconData ->
                            Text(
                                text = iconData.icon,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
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

            if (dragHandle != null) {
                Spacer(modifier = Modifier.width(4.dp))
                dragHandle()
            }
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

/**
 * ✨ НОВИЙ КОМПОНЕНТ для відображення статусу оцінки цілі.
 */
@Composable
private fun ScoreStatusBadge(goal: Goal) {
    when (goal.scoringStatus) {
        ScoringStatus.ASSESSED -> {
            // Показуємо оцінку, лише якщо вона більша за нуль
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
            // Іконка для статусу "Неможливо оцінити"
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
            // Нічого не відображаємо, як і було прохання
        }
    }
}