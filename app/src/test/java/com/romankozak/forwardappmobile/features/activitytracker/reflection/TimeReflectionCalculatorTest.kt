package com.romankozak.forwardappmobile.features.activitytracker.reflection

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeReflectionCalculatorTest {
    @Test
    fun dayStartsAtLatestRecordedWakeAndClipsCrossingActivity() {
        val reflection =
            calculateTimeReflection(
                records =
                    listOf(
                        activity("before #old", 500, 900),
                        activity("crossing #work", 900, 1_200),
                        activity("current #rest", 1_300, 1_500),
                    ),
                recordedDayStarts = listOf(100, 1_000),
                period = ReflectionPeriod.DAY,
                now = 1_600,
            )

        assertEquals(1_000L, reflection.rangeStart)
        assertEquals(400L, reflection.totalTrackedMillis)
        assertEquals(listOf("#work", "#rest"), reflection.tagStats.map { it.tag })
    }

    @Test
    fun threeDaysUsesThirdLatestRecordedStart() {
        val reflection =
            calculateTimeReflection(
                records = listOf(activity("#focus", 250, 950)),
                recordedDayStarts = listOf(100, 300, 600, 900),
                period = ReflectionPeriod.THREE_DAYS,
                now = 1_000,
            )

        assertEquals(300L, reflection.rangeStart)
        assertEquals(650L, reflection.totalTrackedMillis)
        assertEquals(3, reflection.recordedDayCount)
    }

    @Test
    fun ongoingAndMultiTagActivityContributesToEachTag() {
        val reflection =
            calculateTimeReflection(
                records =
                    listOf(
                        activity("Deep work #focus #project", 1_000, null),
                        activity("No tag", 1_500, 1_700),
                        activity("deleted #focus", 1_000, 2_000, isDeleted = true),
                    ),
                recordedDayStarts = listOf(1_000),
                period = ReflectionPeriod.DAY,
                now = 2_000,
            )

        assertEquals(1_200L, reflection.totalTrackedMillis)
        assertEquals(
            mapOf("#focus" to 1_000L, "#project" to 1_000L, "Без тегу" to 200L),
            reflection.tagStats.associate { it.tag to it.durationMillis },
        )
    }

    @Test
    fun noRecordedStartProducesEmptyReflection() {
        val reflection =
            calculateTimeReflection(
                records = listOf(activity("#focus", 100, 200)),
                recordedDayStarts = emptyList(),
                period = ReflectionPeriod.WEEK,
                now = 500,
            )

        assertNull(reflection.rangeStart)
        assertEquals(0L, reflection.totalTrackedMillis)
        assertEquals(emptyList<TagTimeStat>(), reflection.tagStats)
    }

    @Test
    fun entityStatisticsAggregateMultipleLinksAndCountOperationalDays() {
        val taskLink = ActivityEntityLink("task-1", ActivityEntityType.DAY_TASK, "plan-1")
        val themeLink = ActivityEntityLink("theme-1", ActivityEntityType.DAY_THEME, "plan-1")
        val reflection =
            calculateTimeReflection(
                records =
                    listOf(
                        activity("First", 1_100, 1_500, links = listOf(taskLink, themeLink)),
                        activity("Second", 2_100, 2_600, links = listOf(taskLink)),
                        ActivityRecord(
                            text = "Legacy context",
                            startTime = 2_700,
                            endTime = 2_900,
                            contextId = "context-1",
                        ),
                    ),
                recordedDayStarts = listOf(1_000, 2_000),
                period = ReflectionPeriod.THREE_DAYS,
                now = 3_000,
                entityTitles =
                    mapOf(
                        (ActivityEntityType.DAY_TASK to "task-1") to "Прибирання",
                        (ActivityEntityType.DAY_THEME to "theme-1") to "Дім",
                        (ActivityEntityType.CONTEXT to "context-1") to "Побут",
                    ),
            )

        val stats = reflection.entityStats.associateBy { it.link.entityId }
        assertEquals(900L, stats.getValue("task-1").durationMillis)
        assertEquals(2, stats.getValue("task-1").trackedDayCount)
        assertEquals(400L, stats.getValue("theme-1").durationMillis)
        assertEquals(200L, stats.getValue("context-1").durationMillis)
        assertEquals("Прибирання", stats.getValue("task-1").title)
    }

    private fun activity(
        text: String,
        start: Long,
        end: Long?,
        isDeleted: Boolean = false,
        links: List<ActivityEntityLink> = emptyList(),
    ) = ActivityRecord(
        text = text,
        startTime = start,
        endTime = end,
        isDeleted = isDeleted,
        entityLinks = links,
    )
}
