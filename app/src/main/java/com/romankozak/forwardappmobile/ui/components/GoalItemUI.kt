// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/components/GoalItemUI.kt
package com.romankozak.forwardappmobile.ui.components

import android.util.Log
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
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
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

    // Спочатку обробляємо жорстко закодовані іконки, якщо вони є
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
            // Використовуємо lookbehind та lookahead для точного співпадіння
            val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))")
            if (regex.containsMatchIn(currentText)) {
                currentText = currentText.replace(regex, "")
                foundIcons.add(icon)
            }
        }
    }

    // Тепер динамічно обробляємо контексти на основі переданої карти
    contextMarkerToEmojiMap.forEach { (marker, emoji) ->
        // ✨ ВИПРАВЛЕНО: Додано RegexOption.IGNORE_CASE для ігнорування регістру
        val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))", RegexOption.IGNORE_CASE)
        if (regex.containsMatchIn(currentText)) {
            currentText = currentText.replace(regex, "")
            foundIcons.add(emoji)
        }
    }

    // Видаляємо будь-які інші службові теги
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
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String>
) {
    // ✨ ЗМІНЕНО: Передаємо карту в парсер
    val parsedData = remember(goal.text, contextMarkerToEmojiMap) {
        parseTextAndExtractIcons(goal.text, contextMarkerToEmojiMap)
    }

    Log.d("CONTEXT_DEBUG", "--- GoalItemUI ---")
    Log.d("CONTEXT_DEBUG", "Текст завдання: ${goal.text}")
    Log.d("CONTEXT_DEBUG", "Отримано emojiToHide: $emojiToHide")
    Log.d("CONTEXT_DEBUG", "Розпізнані іконки: ${parsedData.icons}")
    Log.d("CONTEXT_DEBUG", "--------------------")


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

                        parsedData.icons
                            .filterNot { it == emojiToHide }
                            .forEach { icon ->
                                Text(
                                    text = icon,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 16.sp, // Збільшуємо розмір для кращої видимості
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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