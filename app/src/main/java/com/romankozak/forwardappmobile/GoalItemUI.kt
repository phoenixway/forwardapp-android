package com.romankozak.forwardappmobile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Anchor
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.CustomCheckbox
import java.net.URLEncoder
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

    // ✨ ЗМІНА: Об'єднуємо всі регулярні вирази в один для єдиного проходу
    val combinedRegex = remember {
        Regex(
            // Група 1-3: Markdown (жирний, курсив, закреслений)
            "(\\*\\*|__)(.*?)\\1" +       // **bold** або __bold__
                    "|(\\*|_)(.*?)\\3" +          // *italic* або _italic_
                    "|(~~)(.*?)\\5" +             // ~~strikethrough~~
                    // Група 7-9: Спеціальні посилання Obsidian
                    "|(\\[\\[)(.*?)(?:\\|(.*?))?]]" +
                    // Група 10-11: Теги та проекти
                    "|([#@])(\\p{L}[\\p{L}0-9_-]*\\b)"
        )
    }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in combinedRegex.findAll(text)) {
            // Додаємо звичайний текст перед знайденим елементом
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }

            // Визначаємо, що саме ми знайшли, і застосовуємо відповідний стиль
            val (content, style, annotation) = when {
                // Жирний
                match.groups[2] != null -> Triple(match.groups[2]!!.value, SpanStyle(fontWeight = FontWeight.Bold), null)
                // Курсив
                match.groups[4] != null -> Triple(match.groups[4]!!.value, SpanStyle(fontStyle = FontStyle.Italic), null)
                // Закреслений
                match.groups[6] != null -> Triple(match.groups[6]!!.value, SpanStyle(textDecoration = TextDecoration.LineThrough), null)
                // Посилання Obsidian
                match.groups[7] != null -> {
                    val linkTarget = match.groups[8]!!.value
                    val linkText = match.groups[9]?.value
                    val displayText = if (!linkText.isNullOrEmpty()) linkText else linkTarget
                    Triple(displayText, SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline), "OBSIDIAN_LINK" to linkTarget)
                }
                // Теги/Проекти
                match.groups[10] != null -> {
                    val tagSymbol = match.groups[10]!!.value
                    val tagName = match.groups[11]!!.value
                    val fullTag = "$tagSymbol$tagName"
                    val tagStyle = when (tagSymbol) {
                        "#" -> SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold)
                        "@" -> SpanStyle(color = projectColor, fontWeight = FontWeight.Medium)
                        else -> SpanStyle()
                    }
                    Triple(fullTag, tagStyle, "SEARCH_TERM" to fullTag)
                }
                else -> Triple("", SpanStyle(), null)
            }

            // Застосовуємо стиль і анотацію (якщо є)
            if (annotation != null) {
                pushStringAnnotation(annotation.first, annotation.second)
            }
            withStyle(style = style) {
                append(content)
            }
            if (annotation != null) {
                pop()
            }

            lastIndex = match.range.last + 1
        }
        // Додаємо залишок тексту, якщо він є
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
                // Основний текст цілі (тепер з підтримкою Markdown)
                ParsedGoalText(
                    text = parsedData.mainText,
                    isCompleted = goal.completed,
                    obsidianVaultName = obsidianVaultName,
                    onTagClick = onTagClick
                )

                // Рядок зі статусними іконками та асоційованими списками
                val hasStatusContent = parsedData.icons.isNotEmpty() || associatedLists.isNotEmpty()
                if (hasStatusContent) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Рендеримо статусні іконки
                        parsedData.icons.forEach { iconData ->
                            Text(
                                text = iconData.icon,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }

                        // Рендеримо асоційовані списки без іконки якоря
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
