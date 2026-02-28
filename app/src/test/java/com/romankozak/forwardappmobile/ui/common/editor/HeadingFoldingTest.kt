package com.romankozak.forwardappmobile.ui.common.editor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HeadingFoldingTest {
    @Test
    fun `computeHeadingLevels ignores headings inside code fences`() {
        val lines =
            listOf(
                "# Title",
                "```kotlin",
                "## inside fence",
                "```",
                "## Subtitle",
            )

        val levels = HeadingFolding.computeHeadingLevels(lines)

        assertThat(levels).containsExactly(1, null, null, null, 2).inOrder()
    }

    @Test
    fun `computeVisibleLineIndices collapses nested section until same level heading`() {
        val lines =
            listOf(
                "# A",
                "intro",
                "## B",
                "b1",
                "### C",
                "c1",
                "## D",
                "d1",
                "# E",
                "e1",
            )
        val levels = HeadingFolding.computeHeadingLevels(lines)

        val visible =
            HeadingFolding.computeVisibleLineIndices(
                lines = lines,
                headingLevels = levels,
                collapsedHeadingLines = setOf(2),
            )

        assertThat(visible).containsExactly(0, 1, 2, 6, 7, 8, 9).inOrder()
    }

    @Test
    fun `computeVisibleLineIndices collapses top heading until next top heading`() {
        val lines =
            listOf(
                "# A",
                "intro",
                "## B",
                "b1",
                "# E",
                "e1",
            )
        val levels = HeadingFolding.computeHeadingLevels(lines)

        val visible =
            HeadingFolding.computeVisibleLineIndices(
                lines = lines,
                headingLevels = levels,
                collapsedHeadingLines = setOf(0),
            )

        assertThat(visible).containsExactly(0, 4, 5).inOrder()
    }

    @Test
    fun `sanitizeCollapsedHeadings drops non-heading indices`() {
        val levels = listOf(1, null, 2, null)

        val sanitized = HeadingFolding.sanitizeCollapsedHeadings(setOf(0, 1, 3, 9), levels)

        assertThat(sanitized).containsExactly(0)
    }
}
