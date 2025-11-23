package com.romankozak.forwardappmobile.ui.common.editor

import android.util.Log

object NoteTitleExtractor {
  private const val TAG = "NoteTitleExtractor"

  fun extract(content: String): String {
    val firstLine = content.lineSequence().firstOrNull().orEmpty()
    Log.d(TAG, "Raw first line: '$firstLine'")

    val prefixPatterns =
      listOf(
        Regex("^\\s*#{1,6}\\s*(.*)$"),
        Regex("^\\s*[-*+]\\s*\\[(?: |x|X)?]\\s*(.*)$"),
        Regex("^\\s*\\[(?: |x|X)?]\\s*(.*)$"),
        Regex("^\\s*\\d+[.)]\\s+(.*)$"),
        Regex("^\\s*[-*+]\\s+(.*)$"),
        Regex("^\\s*>\\s*(.*)$"),
      )

    fun stripPrefixes(line: String): String {
      var current = line
      var changed: Boolean
      do {
        changed = false
        for (regex in prefixPatterns) {
          val match = regex.find(current)
          if (match != null) {
            val candidate = match.groupValues.getOrNull(1).orEmpty()
            if (candidate != current) {
              current = candidate
              changed = true
            }
            break
          }
        }
      } while (changed)
      return current
    }

    fun stripInlineMarkers(line: String): String {
      var result = line
      result = result.replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
      result = result.replace(Regex("`([^`]*)`"), "$1")
      result = result.replace(Regex("~~([^~]+)~~"), "$1")
      result = result.replace(Regex("(\\*\\*|__)([^*_]+)\\1"), "$2")
      result = result.replace(Regex("(\\*|_)([^*_]+)\\1"), "$2")
      return result
    }

    val noPrefixes = stripPrefixes(firstLine).trimStart()
    Log.d(TAG, "After stripping prefixes: '$noPrefixes'")
    val cleaned = stripInlineMarkers(noPrefixes)
    Log.d(TAG, "After stripping inline markers: '$cleaned'")
    val normalized = cleaned.replace(Regex("\\s+"), " ").trim()
    Log.d(TAG, "Normalized title: '$normalized'")

    return if (normalized.isNotBlank()) normalized else "Новий документ"
  }
}
