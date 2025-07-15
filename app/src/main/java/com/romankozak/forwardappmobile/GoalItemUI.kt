package com.romankozak.forwardappmobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.CustomCheckbox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onTagClick: (String) -> Unit
) {
    val tagColor = MaterialTheme.colorScheme.primary
    val projectColor = MaterialTheme.colorScheme.tertiary
    val regex = Regex("([#@])(\\p{L}[\\p{L}0-9_]*)")

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in regex.findAll(text)) {
            append(text.substring(lastIndex, match.range.first))
            val symbol = match.groups[1]!!.value
            val word = match.groups[2]!!.value
            val fullTag = "$symbol$word"
            pushStringAnnotation("SEARCH_TERM", fullTag)
            val style = when (symbol) {
                "#" -> SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold)
                "@" -> SpanStyle(color = projectColor, fontWeight = FontWeight.Medium)
                else -> SpanStyle()
            }
            withStyle(style = style) {
                append(fullTag)
            }
            pop()
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
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "SEARCH_TERM", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onTagClick(annotation.item)
                }
        }
    )
}

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
    onToggle: () -> Unit,
    onItemClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onAssociatedListClick: (String) -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
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
                ParsedGoalText(
                    text = goal.text,
                    isCompleted = goal.completed,
                    onTagClick = onTagClick
                )
                Text(
                    text = formatDate(goal.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                // --- ВІДОБРАЖЕННЯ ПОВ'ЯЗАНИХ СПИСКІВ ---
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
