package com.romankozak.forwardappmobile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.CustomCheckbox
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- (код з іконками залишається без змін) ---
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
    val formatter = SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH)
    return formatter.format(date)
}

@Composable
fun ParsedGoalText(
    text: String,
    modifier: Modifier = Modifier,
    isCompleted: Boolean,
    obsidianVaultName: String,
    onTagClick: (String) -> Unit
) {
    val context = LocalContext.current
    val tagColor = MaterialTheme.colorScheme.primary
    val projectColor = MaterialTheme.colorScheme.tertiary
    val linkColor = MaterialTheme.colorScheme.tertiary

    // ВИПРАВЛЕНО: Додано \b для тегів, щоб уникнути часткових збігів
    val combinedRegex = remember { Regex("(\\[\\[(.*?)(?:\\|(.*?))?]])|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)") }


    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in combinedRegex.findAll(text)) {
            append(text.substring(lastIndex, match.range.first))

            // ВИПРАВЛЕНО: Більш надійний доступ до груп
            val groups = match.groups
            val wikilinkGroup = groups[1] // Повний збіг [[...]]
            val tagSymbolGroup = groups[4] // # або @

            if (wikilinkGroup != null) {
                // Це вікі-посилання
                val linkTarget = groups[2]?.value ?: ""
                val linkText = groups[3]?.value
                val displayText = if (!linkText.isNullOrEmpty()) linkText else linkTarget

                pushStringAnnotation("OBSIDIAN_LINK", linkTarget)
                withStyle(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(displayText)
                }
                pop()
            } else if (tagSymbolGroup != null) {
                // Це тег або проект
                val tagSymbol = tagSymbolGroup.value
                val tagName = groups[5]?.value ?: ""
                val fullTag = "$tagSymbol$tagName"

                pushStringAnnotation("SEARCH_TERM", fullTag)
                val style = when (tagSymbol) {
                    "#" -> SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold)
                    "@" -> SpanStyle(color = projectColor, fontWeight = FontWeight.Medium)
                    else -> SpanStyle()
                }
                withStyle(style = style) {
                    append(fullTag)
                }
                pop()
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (isCompleted) TextDecoration.None else null
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations("SEARCH_TERM", start = offset, end = offset)
                .firstOrNull()?.let { onTagClick(it.item) }

            annotatedString.getStringAnnotations("OBSIDIAN_LINK", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val noteName = annotation.item
                    if (obsidianVaultName.isNotBlank()) {
                        try {
                            val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                            val encodedFile = URLEncoder.encode(noteName, "UTF-8")
                            val uri = Uri.parse("obsidian://open?vault=$encodedVault&file=$encodedFile")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Obsidian не встановлено.", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Помилка відкриття посилання.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Назва Obsidian Vault не вказана в налаштуваннях.", Toast.LENGTH_LONG).show()
                    }
                }
        }
    )
}

// ... решта файлу (AssociatedListsRow, GoalItem) залишається без змін ...
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssociatedListsRow(
    lists: List<GoalList>,
    onListClick: (String) -> Unit
) {
    if (lists.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            lists.forEach { list ->
                AssistChip(
                    onClick = { onListClick(list.id) },
                    label = { Text(list.name) }
                )
            }
        }
    }
}

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
    modifier: Modifier = Modifier
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (parsedData.icons.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            parsedData.icons.forEach { iconData ->
                                Text(text = iconData.icon)
                            }
                        }
                    }
                    ParsedGoalText(
                        text = parsedData.mainText,
                        isCompleted = goal.completed,
                        obsidianVaultName = obsidianVaultName,
                        onTagClick = onTagClick
                    )
                }

                Text(
                    text = formatDate(goal.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                AssociatedListsRow(
                    lists = associatedLists,
                    onListClick = onAssociatedListClick
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Перетягнути",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}