package com.romankozak.forwardappmobile.features.activitytracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import java.util.concurrent.TimeUnit

internal fun shouldShowActiveTrackingStrip(
    activeActivity: ActivityRecord?,
    liveEntryVisibility: JournalLiveEntryVisibility,
): Boolean = activeActivity?.isOngoing == true && liveEntryVisibility == JournalLiveEntryVisibility.HIDDEN

internal fun activeElapsedMillis(
    startTime: Long?,
    now: Long,
): Long = startTime?.let { (now - it).coerceAtLeast(0L) } ?: 0L

internal fun formatActiveElapsedTime(elapsedMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

internal fun activeDurationContentDescription(elapsedMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMillis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis) % 60
    return buildString {
        if (hours > 0) append("$hours годин, ")
        append("$minutes хвилин $seconds секунд")
    }
}

@Composable
internal fun rememberActiveElapsedState(activeActivity: ActivityRecord?): State<Long> {
    val activeId = activeActivity?.id
    val startTime = activeActivity?.startTime
    return produceState(
        initialValue = activeElapsedMillis(startTime, System.currentTimeMillis()),
        key1 = activeId,
        key2 = startTime,
    ) {
        if (activeId == null || startTime == null) {
            value = 0L
            return@produceState
        }
        while (currentCoroutineContext().isActive) {
            value = activeElapsedMillis(startTime, System.currentTimeMillis())
            delay(1_000L)
        }
    }
}

@Composable
internal fun ActiveTrackingDot(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
internal fun ActiveTrackingStickyStrip(
    activity: ActivityRecord?,
    elapsedState: State<Long>,
    liveEntryVisibility: JournalLiveEntryVisibility,
    onOpen: () -> Unit,
    onStop: () -> Unit,
) {
    AnimatedVisibility(
        visible = shouldShowActiveTrackingStrip(activity, liveEntryVisibility),
        enter =
            fadeIn(animationSpec = tween(durationMillis = 120)) +
                expandVertically(animationSpec = tween(durationMillis = 140), expandFrom = Alignment.Bottom),
        exit =
            fadeOut(animationSpec = tween(durationMillis = 100)) +
                shrinkVertically(animationSpec = tween(durationMillis = 120), shrinkTowards = Alignment.Bottom),
    ) {
        val activeActivity = activity ?: return@AnimatedVisibility
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
            Surface(
                color =
                    lerp(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.primaryContainer,
                        0.05f,
                    ),
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clickable(onClick = onOpen)
                                .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription =
                                        "Активність ${activeActivity.text}, триває " +
                                        activeDurationContentDescription(elapsedState.value)
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActiveTrackingDot()
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeActivity.text,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ActiveElapsedText(elapsedState = elapsedState)
                    }
                    ActiveTrackingStopButton(onClick = onStop)
                }
            }
        }
    }
}

@Composable
internal fun ActiveElapsedText(
    elapsedState: State<Long>,
    modifier: Modifier = Modifier,
) {
    val elapsedMillis = elapsedState.value
    Text(
        text = formatActiveElapsedTime(elapsedMillis),
        modifier =
            modifier.semantics {
                contentDescription = "Триває ${activeDurationContentDescription(elapsedMillis)}"
            },
        style =
            MaterialTheme.typography.labelMedium.copy(
                fontFeatureSettings = "tnum",
            ),
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
internal fun ActiveTrackingStopButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f),
                contentColor = MaterialTheme.colorScheme.error,
            ),
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Зупинити відстеження",
            modifier = Modifier.size(14.dp),
        )
    }
}
