package com.romankozak.forwardappmobile.features.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class ParsedTextData(
    val mainText: String,
    val icons: List<String>
)

@Composable
fun rememberParsedText(text: String, contextMarkerToEmojiMap: Map<String, String>): ParsedTextData {
    return remember(text, contextMarkerToEmojiMap) {
        if (contextMarkerToEmojiMap.isEmpty()) {
            return@remember ParsedTextData(text, emptyList())
        }

        val foundIcons = mutableSetOf<String>()
        var currentText = text

        val pattern = contextMarkerToEmojiMap.keys
            .asSequence()
            .sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }

        val regex = Regex("(?<=(^|\\s))($pattern)(?=(\\s|$))", setOf(RegexOption.IGNORE_CASE))
        val matches = regex.findAll(currentText)

        matches.forEach {
            val marker = it.groupValues[2]
            val icon = contextMarkerToEmojiMap[marker] ?: contextMarkerToEmojiMap[marker.lowercase()]
            if (icon != null) {
                foundIcons.add(icon)
            }
        }

        currentText = currentText.replace(regex, " ")
        currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
        val cleanedText = currentText.replace(Regex("\\s+"), " ").trim()

        ParsedTextData(cleanedText, foundIcons.toList())
    }
}
