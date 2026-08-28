package com.romankozak.forwardappmobile.features.activitytracker.reflection

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import com.romankozak.forwardappmobile.domain.tags.extractHashTags
import com.romankozak.forwardappmobile.features.activitytracker.entities.displayName
import com.romankozak.forwardappmobile.features.activitytracker.entities.effectiveEntityLinks
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val UNTAGGED_LABEL = "Без тегу"

fun calculateTimeReflection(
    records: List<ActivityRecord>,
    recordedDayStarts: List<Long>,
    period: ReflectionPeriod,
    now: Long,
    entityTitles: Map<Pair<ActivityEntityType, String>, String> = emptyMap(),
    anchorDayStart: Long? = null,
): TimeReflection {
    val availableStarts =
        recordedDayStarts
            .asSequence()
            .filter { start -> start <= now }
            .distinct()
            .sorted()
            .toList()
    val anchorIndex = anchorDayStart?.let(availableStarts::indexOf) ?: availableStarts.lastIndex
    if (anchorIndex < 0) return TimeReflection(period, null, now, 0, 0, emptyList())

    val applicableStarts =
        availableStarts
            .subList(0, anchorIndex + 1)
            .takeLast(period.operationalDayCount)
    val rangeStart = applicableStarts.firstOrNull()
        ?: return TimeReflection(period, null, now, 0, 0, emptyList())
    val rangeEnd = availableStarts.getOrNull(anchorIndex + 1)?.coerceAtMost(now) ?: now

    val durationByTag = linkedMapOf<String, Long>()
    val displayTagByKey = linkedMapOf<String, String>()
    val durationByEntity = linkedMapOf<Pair<ActivityEntityType, String>, Long>()
    val linkByEntity = linkedMapOf<Pair<ActivityEntityType, String>, ActivityEntityLink>()
    val daysByEntity = linkedMapOf<Pair<ActivityEntityType, String>, MutableSet<Int>>()
    var totalTrackedMillis = 0L

    records.forEach { record ->
        if (record.isDeleted) return@forEach
        val activityStart = record.startTime ?: return@forEach
        val activityEnd = record.endTime ?: rangeEnd
        val overlapStart = max(activityStart, rangeStart)
        val overlapEnd = min(activityEnd, rangeEnd)
        val duration = (overlapEnd - overlapStart).coerceAtLeast(0L)
        if (duration == 0L) return@forEach

        totalTrackedMillis += duration
        val tags = extractHashTags(record.text).ifEmpty { listOf(UNTAGGED_LABEL) }
        tags.distinctBy { tag -> tag.lowercase(Locale.ROOT) }.forEach { tag ->
            val key = tag.lowercase(Locale.ROOT)
            displayTagByKey.putIfAbsent(key, tag)
            durationByTag[key] = durationByTag.getOrDefault(key, 0L) + duration
        }

        record.effectiveEntityLinks().forEach { link ->
            val key = link.entityType to link.entityId
            linkByEntity.putIfAbsent(key, link)
            durationByEntity[key] = durationByEntity.getOrDefault(key, 0L) + duration
            applicableStarts.forEachIndexed { index, dayStart ->
                val dayEnd = applicableStarts.getOrNull(index + 1) ?: rangeEnd
                if (activityStart < dayEnd && activityEnd > dayStart) {
                    daysByEntity.getOrPut(key) { linkedSetOf() }.add(index)
                }
            }
        }
    }

    val stats =
        durationByTag
            .map { (key, duration) ->
                TagTimeStat(
                    tag = displayTagByKey.getValue(key),
                    durationMillis = duration,
                    share = if (totalTrackedMillis == 0L) 0f else duration.toFloat() / totalTrackedMillis,
                )
            }.sortedByDescending(TagTimeStat::durationMillis)

    val entityStats =
        durationByEntity.map { (key, duration) ->
            val link = linkByEntity.getValue(key)
            EntityTimeStat(
                link = link,
                title = entityTitles[key] ?: link.entityType.displayName(),
                durationMillis = duration,
                trackedDayCount = daysByEntity[key]?.size ?: 0,
                share = if (totalTrackedMillis == 0L) 0f else duration.toFloat() / totalTrackedMillis,
            )
        }.sortedWith(compareByDescending<EntityTimeStat> { it.trackedDayCount > 1 }.thenByDescending { it.durationMillis })

    return TimeReflection(
        period = period,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd,
        recordedDayCount = applicableStarts.size,
        totalTrackedMillis = totalTrackedMillis,
        tagStats = stats,
        entityStats = entityStats,
    )
}
