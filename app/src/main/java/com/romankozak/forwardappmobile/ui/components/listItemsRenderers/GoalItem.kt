package com.romankozak.forwardappmobile.ui.components.listItemsRenderers

import android.util.Log
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
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
        "🌫️" to listOf("@unclear")
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
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val animatedColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkbox_color"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        label = "checkbox_border"
    )

    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkbox_scale"
    )

    Box(
        modifier = modifier
            .size(16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(4.dp))
            .background(animatedColor)
            .border(1.dp, animatedBorderColor, RoundedCornerShape(4.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
            .semantics {
                role = Role.Checkbox
                this.stateDescription = if (checked) "Виконано" else "Не виконано"
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(),
            exit = scaleOut(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeOut()
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun EnhancedScoreStatusBadge(goal: Goal) {
    when (goal.scoringStatus) {
        ScoringStatus.ASSESSED -> {
            if (goal.displayScore > 0) {
                val animatedColor by animateColorAsState(
                    targetValue = when {
                        goal.displayScore >= 80 -> Color(0xFF4CAF50)
                        goal.displayScore >= 60 -> Color(0xFFFF9800)
                        goal.displayScore >= 40 -> Color(0xFFFFEB3B)
                        else -> Color(0xFFE91E63)
                    },
                    label = "score_color"
                )

                var isVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    isVisible = true
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = animatedColor.copy(alpha = 0.15f),
                        border = BorderStroke(0.6.dp, animatedColor.copy(alpha = 0.3f)),
                        modifier = Modifier.semantics {
                            contentDescription = "Оцінка: ${goal.displayScore} з 100"
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = animatedColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "${goal.displayScore}/100",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.2.sp,
                                    fontSize = 10.sp // Залишаємо 10.sp як у посилань
                                ),
                                color = animatedColor,
                            )
                        }
                    }
                }
            }
        }
        ScoringStatus.IMPOSSIBLE_TO_ASSESS -> {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(1.dp)
                    .semantics {
                        contentDescription = "Неможливо оцінити"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(10.dp)
                        .padding(3.dp)
                )
            }
        }
        ScoringStatus.NOT_ASSESSED -> {
            // Нічого не відображаємо
        }
    }
}

@Composable
fun EnhancedRelatedLinkChip(
    link: RelatedLink,
    onClick: () -> Unit
) {
    val chipColor = when (link.type) {
        LinkType.GOAL_LIST -> MaterialTheme.colorScheme.primary
        LinkType.NOTE -> MaterialTheme.colorScheme.secondary
        LinkType.URL -> MaterialTheme.colorScheme.tertiary
        LinkType.OBSIDIAN -> Color(0xFF8B5CF6)
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale"
    )

    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .semantics {
                contentDescription = "${link.type.name}: ${link.displayName ?: link.target}"
                role = Role.Button
            },
        shape = RoundedCornerShape(12.dp),
        color = chipColor.copy(alpha = 0.12f),
        border = BorderStroke(0.6.dp, chipColor.copy(alpha = 0.25f)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = when (link.type) {
                    LinkType.GOAL_LIST -> Icons.Default.ListAlt
                    LinkType.NOTE -> Icons.Default.Notes
                    LinkType.URL -> Icons.Default.Link
                    LinkType.OBSIDIAN -> Icons.Default.Book
                },
                contentDescription = null,
                tint = chipColor,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = link.displayName ?: link.target,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.15.sp,
                    fontSize = 10.sp // Розмір шрифту посилань
                ),
                color = chipColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnimatedContextEmoji(
    emoji: String,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(emoji) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .padding(4.dp)
                .semantics {
                    contentDescription = "Контекст: $emoji"
                }
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 12.sp,
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bgAnim"
    )

    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 1.dp,
        label = "elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .semantics {
                contentDescription = "Завдання: ${parsedData.mainText}"
                if (goal.completed) {
                    stateDescription = "Виконано"
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.elevatedCardElevation(elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (goal.completed) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    } else {
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(onItemClick, onLongClick) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onLongPress = { onLongClick() },
                                onTap = { onItemClick() }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EnhancedCustomCheckbox(
                        checked = goal.completed,
                        onCheckedChange = onToggle
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        MarkdownText(
                            text = parsedData.mainText,
                            isCompleted = goal.completed,
                            obsidianVaultName = obsidianVaultName,
                            onTagClick = onTagClick,
                            onTextClick = onItemClick,
                            onLongClick = onLongClick,
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 16.sp,
                                letterSpacing = 0.1.sp,
                                fontSize = 12.sp,
                                fontWeight = if (goal.completed) FontWeight.Normal else FontWeight.Medium
                            )
                        )

                        val hasStatusContent = goal.scoringStatus != ScoringStatus.NOT_ASSESSED ||
                                parsedData.icons.isNotEmpty() ||
                                !goal.description.isNullOrBlank() ||
                                !goal.relatedLinks.isNullOrEmpty()

                        AnimatedVisibility(
                            visible = hasStatusContent,
                            enter = slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            ) + fadeIn()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    EnhancedScoreStatusBadge(goal = goal)

                                    parsedData.icons
                                        .filterNot { it == emojiToHide }
                                        .forEachIndexed { index, icon ->
                                            key(icon) {
                                                var delayedVisible by remember { mutableStateOf(false) }
                                                LaunchedEffect(Unit) {
                                                    kotlinx.coroutines.delay(index * 50L)
                                                    delayedVisible = true
                                                }
                                                AnimatedVisibility(
                                                    visible = delayedVisible,
                                                    enter = scaleIn(
                                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                    ) + fadeIn()
                                                ) {
                                                    AnimatedContextEmoji(
                                                        emoji = icon,
                                                        modifier = Modifier.align(Alignment.CenterVertically)
                                                    )
                                                }
                                            }
                                        }

                                    if (!goal.description.isNullOrBlank()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .align(Alignment.CenterVertically)
                                                .semantics {
                                                    contentDescription = "Містить нотатку"
                                                }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.StickyNote2,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .padding(3.dp)
                                            )
                                        }
                                    }

                                    goal.relatedLinks?.forEachIndexed { index, link ->
                                        key(link.target + link.type.name) {
                                            var delayedVisible by remember { mutableStateOf(false) }
                                            LaunchedEffect(Unit) {
                                                kotlinx.coroutines.delay((parsedData.icons.size + index) * 50L)
                                                delayedVisible = true
                                            }
                                            AnimatedVisibility(
                                                visible = delayedVisible,
                                                enter = slideInHorizontally(
                                                    initialOffsetX = { it },
                                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                                ) + fadeIn()
                                            ) {
                                                EnhancedRelatedLinkChip(
                                                    link = link,
                                                    onClick = { onRelatedLinkClick(link) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 4.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .semantics {
                                    contentDescription = "Перетягнути для переупорядкування"
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = dragHandleModifier
                                    .size(24.dp)
                                    .padding(4.dp)
                                    .pointerInput(Unit) { detectTapGestures { } }
                            )
                        }
                    }
                }
            }
        }
    }
}