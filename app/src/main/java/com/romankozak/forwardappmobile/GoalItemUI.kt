package com.romankozak.forwardappmobile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.CustomCheckbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- (код з парсингом іконок, форматуванням дати залишається без змін) ---

private enum class IconCategory {
    IMPORTANCE, SCALE, ACTIVITY, CUSTOM
}

private data class IconConfig(
    val icon: String,
    val markers: List<String>,
    val category: IconCategory,
)

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
    IconConfig("🌫️", listOf("#unclear"), IconCategory.CUSTOM),
)

private data class ParsedGoalData(
    val icons: List<IconConfig>,
    val mainText: String
)

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

// ✨ ЗМІНА: Цей Composable видалено, бо його функціональність переїхала в MarkdownText.kt
// @Composable fun ParsedGoalText(...) { ... }


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalItem(
    goal: Goal,
    associatedLists: List<GoalList>,
    obsidianVaultName: String,
    onToggle: () -> Unit,
    onItemClick: () -> Unit,
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
            .clickable(onClick = onItemClick)
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomCheckbox(
                checked = goal.completed,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // ✨ ЗМІНА: Тепер ми просто викликаємо наш універсальний MarkdownText
                MarkdownText(
                    text = parsedData.mainText,
                    isCompleted = goal.completed,
                    obsidianVaultName = obsidianVaultName,
                    onTagClick = onTagClick,
                    style = MaterialTheme.typography.bodyLarge
                )

                val hasStatusContent = parsedData.icons.isNotEmpty() || associatedLists.isNotEmpty()
                if (hasStatusContent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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