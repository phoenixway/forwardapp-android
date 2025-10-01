package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import android.text.format.DateUtils
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal object ReminderTextUtil {
    private const val ONE_MINUTE_MILLIS = 60 * 1000L
    private const val ONE_HOUR_MILLIS = 60 * ONE_MINUTE_MILLIS

    fun formatReminderTime(
        reminderTime: Long,
        now: Long,
    ): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = reminderTime

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formattedTime = timeFormat.format(calendar.time)

        if (reminderTime < now) {
            val relativeTime =
                DateUtils
                    .getRelativeTimeSpanString(
                        reminderTime,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                    ).toString()
            return "Пропущено ($relativeTime)"
        }

        val diff = reminderTime - now
        if (diff < ONE_MINUTE_MILLIS) {
            return "Через хвилину"
        }
        if (diff < ONE_HOUR_MILLIS) {
            val minutes = (diff / ONE_MINUTE_MILLIS).toInt()
            return "Через $minutes хв"
        }

        if (DateUtils.isToday(reminderTime)) {
            return "Сьогодні о $formattedTime"
        }

        if (isTomorrow(reminderTime)) {
            return "Завтра о $formattedTime"
        }

        val dateFormat = SimpleDateFormat("d MMM о HH:mm", Locale("uk", "UA"))
        return dateFormat.format(calendar.time)
    }

    private fun isTomorrow(time: Long): Boolean {
        val tomorrow = Calendar.getInstance()
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)

        val target = Calendar.getInstance()
        target.timeInMillis = time

        return tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
}

@Composable
internal fun EnhancedReminderBadge(
    reminderTime: Long,
    currentTimeMillis: Long,
) {
    val reminderText =
        remember(reminderTime, currentTimeMillis) {
            ReminderTextUtil.formatReminderTime(reminderTime, currentTimeMillis)
        }
    val isPastDue = reminderTime < currentTimeMillis

    val backgroundColor by animateColorAsState(
        targetValue =
            if (isPastDue) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
            },
        label = "reminder_badge_bg",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isPastDue) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onTertiaryContainer
            },
        label = "reminder_badge_content",
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(0.7.dp, contentColor.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (isPastDue) Icons.Default.AlarmOff else Icons.Default.AlarmOn,
                contentDescription = "Нагадування",
                tint = contentColor,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = reminderText,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                    ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun EnhancedCustomCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current

    val animatedColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "checkbox_color",
    )

    val animatedBorderColor by animateColorAsState(
        targetValue =
            if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        label = "checkbox_border",
    )

    val scale by animateFloatAsState(
        targetValue = if (checked) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkbox_scale",
    )

    Box(
        modifier =
            modifier
                .size(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(4.dp))
                .background(animatedColor)
                .border(1.dp, animatedBorderColor, RoundedCornerShape(4.dp))
                .clickable(enabled = enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(!checked)
                }
                .semantics {
                    role = Role.Checkbox
                    this.stateDescription = if (checked) "Виконано" else "Не виконано"
                },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter =
                scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ) + fadeIn(),
            exit =
                scaleOut(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                ) + fadeOut(),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

@Composable
internal fun EnhancedRelatedLinkChip(
    link: RelatedLink,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(value = false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale",
    )

    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Surface(
        modifier =
            Modifier
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
                        onTap = { onClick() },
                    )
                }
                .semantics {
                    contentDescription = "${link.type?.name ?: "LINK"}: ${link.displayName ?: link.target}"
                    role = Role.Button
                },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(0.7.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector =
                    when (link.type) {
                        LinkType.PROJECT -> Icons.AutoMirrored.Filled.ListAlt
                        LinkType.URL -> Icons.Default.Link
                        LinkType.OBSIDIAN -> Icons.Default.Book
                        null -> Icons.Default.BrokenImage
                        else -> Icons.Default.BrokenImage
                    },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = link.displayName ?: link.target,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.15.sp,
                        fontSize = 10.sp,
                    ),
                color = contentColor,
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
    var isVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(emoji) {
        delay(100)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter =
            scaleIn(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            ) + fadeIn(),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
                    .padding(4.dp)
                    .semantics {
                        contentDescription = "Контекст: $emoji"
                    },
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun NoteIndicatorBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = CircleShape,
                )
                .padding(4.dp)
                .semantics {
                    contentDescription = "Містить нотатку"
                },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun GoalItem(
    goal: Goal,
    obsidianVaultName: String,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    onTagClick: (String) -> Unit,
    onRelatedLinkClick: (RelatedLink) -> Unit,
    modifier: Modifier = Modifier,
    emojiToHide: String? = null,
    contextMarkerToEmojiMap: Map<String, String>,
    currentTimeMillis: Long,
    isSelected: Boolean,
) {
    val parsedData = rememberParsedText(goal.text, contextMarkerToEmojiMap)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border =
            BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EnhancedCustomCheckbox(
                checked = goal.completed,
                onCheckedChange = onCheckedChange,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .pointerInput(onItemClick, onLongClick) {
                            detectTapGestures(
                                onLongPress = { onLongClick() },
                                onTap = { onItemClick() },
                            )
                        },
            ) {
                MarkdownText(
                    text = parsedData.mainText,
                    isCompleted = goal.completed,
                    obsidianVaultName = obsidianVaultName,
                    onTagClick = onTagClick,
                    onTextClick = onItemClick,
                    onLongClick = onLongClick,
                    maxLines = 4,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 16.sp,
                            letterSpacing = 0.1.sp,
                            fontSize = 12.sp,
                            fontWeight = if (goal.completed) FontWeight.Normal else FontWeight.Medium,
                        ),
                )

                val hasStatusContent =
                    (goal.scoringStatus != ScoringStatusValues.NOT_ASSESSED) ||
                        (goal.reminderTime != null) ||
                        (parsedData.icons.isNotEmpty()) ||
                        (!goal.description.isNullOrBlank()) ||
                        (!goal.relatedLinks.isNullOrEmpty())

                AnimatedVisibility(
                    visible = hasStatusContent,
                    enter =
                        slideInVertically(
                            initialOffsetY = { height -> -height },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        ) + fadeIn(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            goal.reminderTime?.let { time ->
                                EnhancedReminderBadge(
                                    reminderTime = time,
                                    currentTimeMillis = currentTimeMillis,
                                )
                            }

                            EnhancedScoreStatusBadge(
                                scoringStatus = goal.scoringStatus,
                                displayScore = goal.displayScore,
                            )

                            parsedData.icons
                                .filterNot { icon -> icon == emojiToHide }
                                .forEachIndexed { index, icon ->
                                    key(icon) {
                                        var delayedVisible by remember { mutableStateOf(false) }
                                        LaunchedEffect(Unit) {
                                            delay(index * 50L)
                                            delayedVisible = true
                                        }
                                        AnimatedVisibility(
                                            visible = delayedVisible,
                                            enter =
                                                scaleIn(
                                                    animationSpec =
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        ),
                                                ) + fadeIn(),
                                        ) {
                                            AnimatedContextEmoji(
                                                emoji = icon,
                                                modifier = Modifier.align(Alignment.CenterVertically),
                                            )
                                        }
                                    }
                                }

                            if (!goal.description.isNullOrBlank()) {
                                NoteIndicatorBadge(modifier = Modifier.align(Alignment.CenterVertically))
                            }

                            goal.relatedLinks?.filter { it.type != null }?.forEachIndexed { index, link ->
                                key(link.target + link.type?.name) {
                                    var delayedVisible by remember { mutableStateOf(false) }
                                    LaunchedEffect(Unit) {
                                        delay((parsedData.icons.size + index) * 50L)
                                        delayedVisible = true
                                    }
                                    AnimatedVisibility(
                                        visible = delayedVisible,
                                        enter =
                                            slideInHorizontally(
                                                initialOffsetX = { fullWidth -> fullWidth },
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                            ) + fadeIn(),
                                    ) {
                                        EnhancedRelatedLinkChip(
                                            link = link,
                                            onClick = { onRelatedLinkClick(link) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
