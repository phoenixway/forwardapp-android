package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.AccountTree
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
import androidx.compose.material.icons.filled.Snooze
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
import com.romankozak.forwardappmobile.data.database.models.Reminder
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

        val dateFormat = SimpleDateFormat("d MMM о HH:mm", Locale.forLanguageTag("uk-UA"))
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
    reminder: Reminder,
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val reminderText = if (reminder.status == "COMPLETED") {
        "Completed"
    } else if (reminder.status == "SNOOZED") {
        "Snoozed"
    } else {
        remember(reminder.reminderTime, currentTime) {
            ReminderTextUtil.formatReminderTime(reminder.reminderTime, currentTime)
        }
    }
    val isPastDue = reminder.reminderTime < currentTime && reminder.status != "COMPLETED"

    val backgroundColor by animateColorAsState(
        targetValue = when {
            reminder.status == "COMPLETED" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            isPastDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            reminder.status == "SNOOZED" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
        },
        label = "reminder_badge_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            reminder.status == "COMPLETED" -> MaterialTheme.colorScheme.primary
            isPastDue -> MaterialTheme.colorScheme.error
            reminder.status == "SNOOZED" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        },
        label = "reminder_badge_content",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = when {
                    isPastDue -> Icons.Default.AlarmOff
                    reminder.status == "SNOOZED" -> Icons.Default.Snooze
                    else -> Icons.Default.AlarmOn
                },
                contentDescription = "Нагадування",
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = reminderText,
                style = MaterialTheme.typography.labelSmall.copy(
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
internal fun EnhancedRelatedLinkChip(
    link: RelatedLink,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chip_scale",
    )

    val isSubProject = link.type == LinkType.PROJECT
    val backgroundColor = if (isSubProject) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    }
    val contentColor = if (isSubProject) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }


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
                    onTap = { onClick() },
                )
            }
            .semantics {
                contentDescription = "${link.type?.name ?: "LINK"}: ${link.displayName ?: link.target}"
                role = Role.Button
            },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = 1.dp,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = when (link.type) {
                    LinkType.PROJECT -> Icons.Default.AccountTree
                    LinkType.URL -> Icons.Default.Link
                    LinkType.OBSIDIAN -> Icons.Default.Book
                    null -> Icons.Default.BrokenImage
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            if (!isSubProject) {
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
}

@Composable
fun AnimatedContextEmoji(
    emoji: String,
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "emoji_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emoji_scale",
    )

    LaunchedEffect(emoji) {
        delay(100)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        ) + fadeIn(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape,
                )
                .padding(6.dp)
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
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
        shadowElevation = 1.dp,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .semantics {
                    contentDescription = "Містить нотатку"
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
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
    isSelected: Boolean,
    showCheckbox: Boolean,
    reminders: List<Reminder> = emptyList(),
    endAction: @Composable () -> Unit = {},
) {
    val reminder = reminders.firstOrNull()
    val parsedData = rememberParsedText(goal.text, contextMarkerToEmojiMap)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        tonalElevation = if (isSelected) 3.dp else 1.dp,
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(visible = showCheckbox) {
                Checkbox(
                    checked = goal.completed,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Goal",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
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
                                            (reminder != null) ||
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
                            reminder?.let { 
                                EnhancedReminderBadge(
                                    reminder = it,
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
            endAction()
        }
    }
}