package com.romankozak.forwardappmobile.ui.common.editor

internal object HeadingFolding {
    private val headingRegex = Regex("""^\s*(#{1,6})(?:\s+|$)(.*)$""")

    fun computeHeadingLevels(lines: List<String>): List<Int?> {
        var inCodeFence = false
        return lines.map { line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence
                null
            } else if (inCodeFence) {
                null
            } else {
                headingRegex.find(line)?.groupValues?.get(1)?.length
            }
        }
    }

    fun sanitizeCollapsedHeadings(
        collapsedHeadingLines: Collection<Int>,
        headingLevels: List<Int?>,
    ): Set<Int> =
        collapsedHeadingLines
            .asSequence()
            .filter { index -> index in headingLevels.indices && headingLevels[index] != null }
            .toSet()

    fun computeVisibleLineIndices(
        lines: List<String>,
        headingLevels: List<Int?>,
        collapsedHeadingLines: Set<Int>,
    ): List<Int> {
        val visible = mutableListOf<Int>()
        var index = 0

        while (index < lines.size) {
            visible.add(index)
            val headingLevel = headingLevels[index]

            if (headingLevel != null && collapsedHeadingLines.contains(index)) {
                index++
                while (index < lines.size) {
                    val nextLevel = headingLevels[index]
                    if (nextLevel != null && nextLevel <= headingLevel) {
                        break
                    }
                    index++
                }
            } else {
                index++
            }
        }

        return visible
    }
}
