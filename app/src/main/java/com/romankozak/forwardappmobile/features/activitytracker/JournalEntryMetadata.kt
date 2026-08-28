package com.romankozak.forwardappmobile.features.activitytracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecordKind
import com.romankozak.forwardappmobile.features.activitytracker.dialogs.formatDuration
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityDescriptor
import com.romankozak.forwardappmobile.features.activitytracker.entities.displayName
import com.romankozak.forwardappmobile.features.activitytracker.entities.effectiveEntityLinks
import com.romankozak.forwardappmobile.features.activitytracker.entities.identityKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun JournalMetadataRow(
    record: ActivityRecord,
    onTagClick: (String) -> Unit,
    uiConfig: JournalUiConfig,
    entityCatalog: List<ActivityEntityDescriptor>,
    activeElapsedState: State<Long>?,
    onStopActive: () -> Unit,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val tags = remember(record.text) { extractActivityTags(record.text) }
    val descriptors = remember(entityCatalog) { entityCatalog.associateBy { it.link.identityKey() } }
    val durationText = rememberDurationText(record)
    val entityLinks = remember(record) { record.effectiveEntityLinks() }
    val hasSupplementaryMetadata =
        record.reminderTime != null ||
            (record.xpGained ?: 0) > 0 ||
            (record.antyXp ?: 0) > 0 ||
            entityLinks.isNotEmpty() ||
            tags.isNotEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (activeElapsedState != null) {
            ActiveJournalTimingRow(
                timeText = buildRecordTimeLabel(record, timeFormat),
                elapsedState = activeElapsedState,
                onStop = onStopActive,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                PlainMetadataText(buildRecordTimeLabel(record, timeFormat), tabular = true)
                durationText?.let { PlainMetadataText("·  $it", emphasized = true, tabular = true) }
                recordTypeLabel(record)?.let { PlainMetadataText("·  $it") }
                JournalAttributeMetadata(
                    record = record,
                    tags = tags,
                    entityLinks = entityLinks,
                    descriptors = descriptors,
                    uiConfig = uiConfig,
                    onTagClick = onTagClick,
                )
            }
        }

        if (activeElapsedState != null && hasSupplementaryMetadata) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                JournalAttributeMetadata(
                    record = record,
                    tags = tags,
                    entityLinks = entityLinks,
                    descriptors = descriptors,
                    uiConfig = uiConfig,
                    onTagClick = onTagClick,
                )
            }
        }
    }
}

@Composable
private fun ActiveJournalTimingRow(
    timeText: String,
    elapsedState: State<Long>,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainMetadataText(
            text = timeText,
            modifier = Modifier.weight(1f),
            tabular = true,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        ActiveElapsedText(elapsedState = elapsedState)
        Spacer(modifier = Modifier.width(4.dp))
        ActiveTrackingStopButton(onClick = onStop)
    }
}

@Composable
private fun rememberDurationText(record: ActivityRecord): String? =
    remember(record.startTime, record.endTime, record.isOngoing) {
        val startTime = record.startTime
        val endTime = record.endTime
        if (record.isOngoing || startTime == null || endTime == null) {
            null
        } else {
            val duration = endTime - startTime
            if (duration > 0) formatDuration(duration) else null
        }
    }

@Composable
private fun JournalAttributeMetadata(
    record: ActivityRecord,
    tags: List<String>,
    entityLinks: List<ActivityEntityLink>,
    descriptors: Map<Pair<ActivityEntityType, String>, ActivityEntityDescriptor>,
    uiConfig: JournalUiConfig,
    onTagClick: (String) -> Unit,
) {
    if (record.reminderTime != null) PlainMetadataText("·  нагадування")
    if ((record.xpGained ?: 0) > 0) {
        PlainMetadataText("+${record.xpGained} xp", color = MaterialTheme.colorScheme.primary)
    }
    if ((record.antyXp ?: 0) > 0) {
        PlainMetadataText("-${record.antyXp} xp", color = MaterialTheme.colorScheme.error)
    }
    entityLinks.forEach { link ->
        val descriptor = descriptors[link.identityKey()]
        if (link.entityType == ActivityEntityType.DAY_THEME) {
            AccentMetadataChip(text = descriptor?.title ?: link.entityType.displayName(), uiConfig = uiConfig)
        } else {
            NeutralMetadataChip(
                text = descriptor?.let { "${it.typeLabel}: ${it.title}" } ?: link.entityType.displayName(),
                uiConfig = uiConfig,
            )
        }
    }
    tags.take(uiConfig.maxVisibleTags).forEach { tag ->
        TagMetadataText(tag = tag, onClick = { onTagClick(tag) })
    }
    if (tags.size > uiConfig.maxVisibleTags) PlainMetadataText("+${tags.size - uiConfig.maxVisibleTags}")
}

@Composable
private fun PlainMetadataText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    emphasized: Boolean = false,
    tabular: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = if (tabular) "tnum" else null,
            ),
        color = color,
        fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AccentMetadataChip(
    text: String,
    uiConfig: JournalUiConfig,
) {
    MetadataChip(
        text = text,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        uiConfig = uiConfig,
    )
}

@Composable
private fun NeutralMetadataChip(
    text: String,
    uiConfig: JournalUiConfig,
) {
    MetadataChip(
        text = text,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        uiConfig = uiConfig,
    )
}

@Composable
private fun MetadataChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    uiConfig: JournalUiConfig,
) {
    Surface(color = containerColor, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier =
                Modifier.padding(
                    horizontal = uiConfig.pillHorizontalPadding,
                    vertical = 2.dp,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TagMetadataText(
    tag: String,
    onClick: () -> Unit,
) {
    Text(
        text = tag,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
        fontWeight = FontWeight.Medium,
        maxLines = 1,
    )
}

private fun buildRecordTimeLabel(
    record: ActivityRecord,
    timeFormat: SimpleDateFormat,
): String =
    when {
        record.isTimeless -> timeFormat.format(Date(record.createdAt))
        record.isOngoing -> "${timeFormat.format(Date(record.startTime!!))} → зараз"
        record.endTime != null && record.startTime == record.endTime -> timeFormat.format(Date(record.startTime!!))
        else -> "${timeFormat.format(Date(record.startTime!!))}–${timeFormat.format(Date(record.endTime!!))}"
    }

private fun recordTypeLabel(record: ActivityRecord): String? =
    when {
        record.recordKind == ActivityRecordKind.DAY_SUMMARY -> "резюме дня"
        record.recordKind == ActivityRecordKind.EVENT -> "подія"
        record.recordKind == ActivityRecordKind.COMMENT -> "коментар"
        record.isOngoing -> null
        record.startTime != null && record.endTime != null && record.startTime == record.endTime -> "подія"
        else -> null
    }
