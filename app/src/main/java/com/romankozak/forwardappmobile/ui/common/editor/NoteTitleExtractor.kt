package com.romankozak.forwardappmobile.ui.common.editor

import android.util.Log

object NoteTitleExtractor {
    private const val TAG = "NoteTitleExtractor"
    private val prefixPatterns =
        listOf(
            Regex("^\\s*#{1,6}\\s*(.*)$"),
            Regex("^\\s*[-*+]\\s*\\[(?: |x|X)?]\\s*(.*)$"),
            Regex("^\\s*\\[(?: |x|X)?]\\s*(.*)$"),
            Regex("^\\s*\\d+[.)]\\s+(.*)$"),
            Regex("^\\s*[-*+]\\s+(.*)$"),
            Regex("^\\s*>\\s*(.*)$"),
        )

    fun extractOrNull(content: String): String? {
        val firstLine = content.lineSequence().firstOrNull().orEmpty()
        Log.d(TAG, "Raw first line: '$firstLine'")

        val noPrefixes = stripPrefixes(firstLine).trimStart()
        Log.d(TAG, "After stripping prefixes: '$noPrefixes'")
        val cleaned = stripInlineMarkers(noPrefixes)
        Log.d(TAG, "After stripping inline markers: '$cleaned'")
        val normalized = cleaned.replace(Regex("\\s+"), " ").trim()
        Log.d(TAG, "Normalized title: '$normalized'")

        return normalized.takeIf { it.isNotBlank() }
    }

    fun extract(content: String): String = extractOrNull(content) ?: "Новий документ"

    private fun stripPrefixes(line: String): String {
        var current = line
        while (true) {
            val candidate = stripSinglePrefix(current) ?: return current
            if (candidate == current) return current
            current = candidate
        }
    }

    private fun stripSinglePrefix(line: String): String? {
        val match = prefixPatterns.asSequence().mapNotNull { regex -> regex.find(line) }.firstOrNull() ?: return null
        return match.groupValues.getOrNull(1).orEmpty()
    }

    private fun stripInlineMarkers(line: String): String {
        var result = line
        result = result.replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        result = result.replace(Regex("`([^`]*)`"), "$1")
        result = result.replace(Regex("~~([^~]+)~~"), "$1")
        result = result.replace(Regex("(\\*\\*|__)([^*_]+)\\1"), "$2")
        result = result.replace(Regex("(\\*|_)([^*_]+)\\1"), "$2")
        return result
    }
}
