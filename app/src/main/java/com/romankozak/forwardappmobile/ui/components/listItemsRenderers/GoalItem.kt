package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.*

private data class ParsedGoalData(val icons: List<String>, val mainText: String)

private fun parseTextAndExtractIcons(
    text: String,
    contextMarkerToEmojiMap: Map<String, String>,
): ParsedGoalData {
    var currentText = text
    val foundIcons = mutableSetOf<String>()

    val allMarkersToIcons = mutableMapOf<String, String>()

    // Жорстко закодовані іконки
    val hardcodedIconsData = mapOf(
        "🔥" to listOf("@critical", "! ", "!"),
        "⭐" to listOf("@day", "+"),
        "📌" to listOf("@week", "++"),
        "🗓️" to listOf("@month"),
        "🎯" to listOf("+++ "),
        "🔭" to listOf("~ ", "~"),
        "✨" to listOf("@str"),
        "🌫️" to listOf("@unclear"),
    )
    hardcodedIconsData.forEach { (icon, markers) ->
        markers.forEach { marker ->
            allMarkersToIcons[marker] = icon
        }
    }

    allMarkersToIcons.putAll(contextMarkerToEmojiMap)

    val sortedMarkers = allMarkersToIcons.keys.sortedByDescending { it.length }

    sortedMarkers.forEach { marker ->
        val icon = allMarkersToIcons[marker] ?: return@forEach
        val regexOptions = if (marker.startsWith("@")) setOf(RegexOption.IGNORE_CASE) else emptySet()
        val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))", regexOptions)

        if (regex.containsMatchIn(currentText)) {
            foundIcons.add(icon)
            currentText = currentText.replace(regex, " ")
        }
    }

    currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
    val cleanedText = currentText.replace(Regex("\\s+"), " ").trim()

    return ParsedGoalData(icons = foundIcons.toList(), mainText = cleanedText)
}

@Composable
fun EnhancedCustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    val animatedColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
        label = "checkbox_color",
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 150),
        label = "checkbox_border",
    )

    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkbox_scale",
    )

    Box(
        modifier = modifier
            .size(26.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(animatedColor)
            .border(2.dp, animatedBorderColor, RoundedCornerShape(8.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .semantics {
                role = Role.Checkbox
                stateDescription = if (checked) "Виконано" else "Не виконано"
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(initialScale = 0.6f) + fadeIn(),
            exit = scaleOut(targetScale = 0.6f) + fadeOut(),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
fun EnhancedScoreStatusBadge(goal: Goal) {
    when (goal.scoringStatus) {
        ScoringStatus.ASSESSED -> {
            if (goal.displayScore > 0) {
                val (color, label) = when {
                    goal.displayScore >= 80 -> Color(0xFF4CAF50) to "Висока оцінка"
                    goal.displayScore >= 60 -> Color(0xFFFF9800) to "Середня оцінка"
                    goal.displayScore >= 40 -> Color(0xFFFFEB3B) to "Низька оцінка"
                    else -> Color(0xFFE91E63) to "Дуже низька оцінка"
                }

                val animatedColor by animateColorAsState(
                    targetValue = color,
                    label = "score_color",
                )

                AssistChip(
                    onClick = { /* no-op */ },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = animatedColor,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${goal.displayScore}/100",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = animatedColor,
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = animatedColor.copy(alpha = 0.15f),
                        labelColor = animatedColor,
                    ),
                    border = BorderStroke(1.dp, animatedColor.copy(alpha = 0.3f)),
                    modifier = Modifier.height(32.dp),
                )
            }
        }

        ScoringStatus.IMPOSSIBLE_TO_ASSESS -> {
            AssistChip(
                onClick = { /* no-op */ },
                label = {
                    Icon(
                        imageVector = Icons.Default.FlashOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier.height(32.dp),
            )
        }

        ScoringStatus.NOT_ASSESSED -> {
            // Нічого не відображаємо
        }
    }
}

@Composable
fun EnhancedRelatedLinkChip(
    link: RelatedLink,
    onClick: () -> Unit,
) {
    val chipColor = when (link.type) {
        LinkType.GOAL_LIST -> MaterialTheme.colorScheme.primary
        LinkType.NOTE -> MaterialTheme.colorScheme.secondary
        LinkType.URL -> MaterialTheme.colorScheme.tertiary
        LinkType.OBSIDIAN -> Color(0xFF8B5CF6) // Фіолетовий
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale",
    )

    Surface(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Посилання: ${link.type.name}" },
        shape = RoundedCornerShape(20.dp),
        color = chipColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.25f)),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = when (link.type) {
                    LinkType.GOAL_LIST -> Icons.AutoMirrored.Filled.ListAlt
                    LinkType.NOTE -> Icons.AutoMirrored.Filled.Notes
                    LinkType.URL -> Icons.Default.Link
                    LinkType.OBSIDIAN -> Icons.Default.Book
                },
                contentDescription = null,
                tint = chipColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = link.displayName ?: link.target,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = chipColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AnimatedContextEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = scaleIn(
            initialScale = 0.6f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(8.dp)
                .semantics { contentDescription = "Контекст: $emoji" },
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
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
        goal.completed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    val background by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 300f),
        label = "bgAnim",
    )

    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(targetValue = if (isPressed) 8.dp else 3.dp, label = "elevation")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics { contentDescription = "Завдання: ${parsedData.mainText}" },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.elevatedCardElevation(elevation),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Клікабельна зона
            Row(
                modifier = Modifier
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onLongPress = { onLongClick() },
                            onTap = { onItemClick() },
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EnhancedCustomCheckbox(
                    checked = goal.completed,
                    onCheckedChange = onToggle,
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    MarkdownText(
                        text = parsedData.mainText,
                        isCompleted = goal.completed,
                        obsidianVaultName = obsidianVaultName,
                        onTagClick = onTagClick,
                        onTextClick = onItemClick,
                        onLongClick = onLongClick,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp,
                            letterSpacing = 0.15.sp,
                            fontWeight = if (goal.completed) FontWeight.Normal else FontWeight.Medium,
                        ),
                    )

                    val hasStatusContent by remember {
                        derivedStateOf {
                            goal.scoringStatus != ScoringStatus.NOT_ASSESSED ||
                                    parsedData.icons.isNotEmpty() ||
                                    goal.description != null ||
                                    !goal.relatedLinks.isNullOrEmpty()
                        }
                    }

                    AnimatedVisibility(
                        visible = hasStatusContent,
                        enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                EnhancedScoreStatusBadge(goal = goal)

                                // Іконки контексту
                                parsedData.icons
                                    .filterNot { it == emojiToHide }
                                    .forEach { icon ->
                                        key(icon) { AnimatedContextEmoji(emoji = icon) }
                                    }

                                // Нотатка
                                if (goal.description != null) {
                                    AssistChip(
                                        onClick = { /* no-op */ },
                                        label = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        ),
                                        modifier = Modifier.height(32.dp),
                                    )
                                }

                                // Посилання
                                goal.relatedLinks?.forEach { link ->
                                    key(link.target + link.type.name) {
                                        EnhancedRelatedLinkChip(link = link) { onRelatedLinkClick(link) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ручка перетягування
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(8.dp)
                        .semantics { contentDescription = "Перетягнути для сортування" },
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = dragHandleModifier.size(24.dp),
                    )
                }
            }
        }
    }
}