package com.romankozak.forwardappmobile.features.activitytracker

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import org.junit.Test

class ActivityLiveTrackingUiTest {
    private val activeRecord =
        ActivityRecord(
            id = "active",
            text = "підготовка документації для recurrence migration",
            startTime = 1_000L,
            endTime = null,
        )

    @Test
    fun `no active activity never shows sticky strip`() {
        assertThat(shouldShowActiveTrackingStrip(null, JournalLiveEntryVisibility.HIDDEN)).isFalse()
    }

    @Test
    fun `visible active entry hides sticky strip`() {
        assertThat(shouldShowActiveTrackingStrip(activeRecord, JournalLiveEntryVisibility.VISIBLE)).isFalse()
    }

    @Test
    fun `hidden active entry shows sticky strip`() {
        assertThat(shouldShowActiveTrackingStrip(activeRecord, JournalLiveEntryVisibility.HIDDEN)).isTrue()
    }

    @Test
    fun `completed entry no longer projects active sticky state`() {
        val completedRecord = activeRecord.copy(endTime = 61_000L)

        assertThat(shouldShowActiveTrackingStrip(completedRecord, JournalLiveEntryVisibility.HIDDEN)).isFalse()
    }

    @Test
    fun `elapsed projection uses canonical start and clamps negative values`() {
        assertThat(activeElapsedMillis(startTime = 1_000L, now = 62_000L)).isEqualTo(61_000L)
        assertThat(activeElapsedMillis(startTime = 5_000L, now = 4_000L)).isEqualTo(0L)
        assertThat(formatActiveElapsedTime(61_000L)).isEqualTo("01:01")
    }

    @Test
    fun `visibility requires a meaningful part of the live entry`() {
        val visibleItem = JournalVisibleItemBounds(key = "active", offset = 70, size = 100)

        val visibility =
            resolveLiveEntryVisibility(
                activeKey = "active",
                knownItemKeys = listOf("header", "active"),
                visibleItems = listOf(visibleItem),
                viewportStart = 0,
                viewportEnd = 100,
                previous = JournalLiveEntryVisibility.HIDDEN,
            )

        assertThat(visibility).isEqualTo(JournalLiveEntryVisibility.HIDDEN)
    }

    @Test
    fun `visibility hysteresis prevents flicker near viewport boundary`() {
        val partiallyVisibleItem = JournalVisibleItemBounds(key = "active", offset = 70, size = 100)
        val commonArguments =
            listOf(partiallyVisibleItem)

        val whilePreviouslyVisible =
            resolveLiveEntryVisibility(
                activeKey = "active",
                knownItemKeys = listOf("active"),
                visibleItems = commonArguments,
                viewportStart = 0,
                viewportEnd = 100,
                previous = JournalLiveEntryVisibility.VISIBLE,
            )
        val whilePreviouslyHidden =
            resolveLiveEntryVisibility(
                activeKey = "active",
                knownItemKeys = listOf("active"),
                visibleItems = commonArguments,
                viewportStart = 0,
                viewportEnd = 100,
                previous = JournalLiveEntryVisibility.HIDDEN,
            )

        assertThat(whilePreviouslyVisible).isEqualTo(JournalLiveEntryVisibility.VISIBLE)
        assertThat(whilePreviouslyHidden).isEqualTo(JournalLiveEntryVisibility.HIDDEN)
    }
}
