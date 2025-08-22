package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ParsedGoalData(val icons: List<String>, val mainText: String)

// --- Вставте цей оновлений код у файл app/src/main/java/com/romankozak/forwardappmobile/ui/components/listItemsRenderers/GoalItem.kt ---

private fun parseTextAndExtractIcons(
    text: String,
    contextMarkerToEmojiMap: Map<String, String>
): ParsedGoalData {
    var currentText = text
    val foundIcons = mutableSetOf<String>()

    // Створюємо єдину мапу всіх маркерів, де пріоритет надається користувацьким налаштуванням
    val allMarkersToIcons = mutableMapOf<String, String>()

    // 1. Спочатку додаємо жорстко закодовані іконки
    val hardcodedIconsData = mapOf(
        "🔥" to listOf("@critical", "! ", "!"),
        "⭐" to listOf("@day", "+"),
        "📌" to listOf("@week", "++"),
        "🗓️" to listOf("@month"),
        "🎯" to listOf("+++ "),
        "🔭" to listOf("~ ", "~"),
        "✨" to listOf("@str"),
        "🌫️" to listOf("@unclear")
    )
    hardcodedIconsData.forEach { (icon, markers) ->
        markers.forEach { marker ->
            allMarkersToIcons[marker] = icon
        }
    }

    // 2. Додаємо користувацькі емодзі, перезаписуючи жорстко закодовані у разі збігу маркерів
    allMarkersToIcons.putAll(contextMarkerToEmojiMap)

    // 3. Сортуємо маркери за довжиною (від найдовшого), щоб уникнути конфліктів (напр., "++" обробити раніше, ніж "+")
    val sortedMarkers = allMarkersToIcons.keys.sortedByDescending { it.length }

    sortedMarkers.forEach { marker ->
        val icon = allMarkersToIcons[marker] ?: return@forEach

        // Ігноруємо регістр тільки для маркерів, що починаються з "@"
        val regexOptions = if (marker.startsWith("@")) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))", regexOptions)

        // Перевіряємо наявність маркера перед тим, як модифікувати текст
        if (regex.containsMatchIn(currentText)) {
            foundIcons.add(icon)
            // Замінюємо маркер на пробіл, щоб не з'єднувати слова, які були по боках
            currentText = currentText.replace(regex, " ")
        }
    }

    // 4. Видаляємо застарілі маркери іконок та зайві пробіли
    currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
    val cleanedText = currentText.replace(Regex("\\s+"), " ").trim()

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

    val targetColor = when {
        goal.completed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val background by animateColorAsState(
        targetValue = targetColor,
        label = "bgAnim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ЗОНА 1: Клікабельний контент
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(onItemClick, onLongClick) {
                        detectTapGestures(
                            onLongPress = { onLongClick() },
                            onTap = { onItemClick() }
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomCheckbox(
                    checked = goal.completed,
                    onCheckedChange = onToggle
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    MarkdownText(
                        text = parsedData.mainText,
                        isCompleted = goal.completed,
                        obsidianVaultName = obsidianVaultName,
                        onTagClick = onTagClick,
                        onTextClick = onItemClick,
                        onLongClick = onLongClick
                    )

                    // --- ПОЧАТОК ЗМІН: Відновлюємо FlowRow з усією інформацією ---
                    val hasStatusContent = goal.scoringStatus != ScoringStatus.NOT_ASSESSED ||
                            parsedData.icons.isNotEmpty() ||
                            !goal.description.isNullOrBlank() ||
                            !goal.relatedLinks.isNullOrEmpty()

                    if (hasStatusContent) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Бейдж з очками
                            ScoreStatusBadge(goal = goal)

                            // 2. Іконки контекстів (@day, @week, etc.)
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

                            // 3. Індикатор наявності нотатки (опису)
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

                            // 4. Пов'язані посилання (використовуємо сучасні relatedLinks)
                            goal.relatedLinks?.forEach { link ->
                                RelatedLinkChip(link = link, onClick = { onRelatedLinkClick(link) })
                            }
                        }
                    }
                    // --- КІНЕЦЬ ЗМІН ---
                }
            }

            // ЗОНА 2: Ручка для перетягування
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = dragHandleModifier
                        .size(36.dp)
                        .pointerInput(Unit) { detectTapGestures { } }
                )
            }
        }
    }
}