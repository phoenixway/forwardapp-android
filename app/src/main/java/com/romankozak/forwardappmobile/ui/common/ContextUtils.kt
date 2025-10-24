package com.romankozak.forwardappmobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class ParsedTextData(
    val icons: List<String>,
    val mainText: String,
)

object ContextUtils {
    fun parseTextAndExtractIcons(
        text: String,
        contextMarkerToEmojiMap: Map<String, String>,
    ): ParsedTextData {
        var currentText = text
        val foundIcons = mutableSetOf<String>()

        val allMarkersToIcons = mutableMapOf<String, String>()

        val hardcodedIconsData =
            mapOf(
                "🔥" to listOf("@critical", "! ", "!"),
                "⭐" to listOf("@day", "+"),
                "📌" to listOf("@week", "++"),
                "🗓️" to listOf("@month"),
                "🎯" to listOf("+++ "),
                "🔭" to listOf("~ ", "~"),
                "✨" to listOf("@str"),
                "🌫️" to listOf("@unclear"),
                "❓" to listOf("??"),
            )
        hardcodedIconsData.forEach { (icon, markers) ->
            markers.forEach { marker ->
                allMarkersToIcons[marker] = icon
            }
        }

        allMarkersToIcons.putAll(contextMarkerToEmojiMap)

        val sortedMarkers = allMarkersToIcons.keys.sortedByDescending { it.length }

        sortedMarkers.forEach { marker ->
            val icon = allMarkersToIcons[marker] ?: return@forEach
            val regexOptions = if (marker.startsWith("@")) setOf(RegexOption.IGNORE_CASE) else emptySet()
            val regex = Regex("(?<=(^|\\s))${Regex.escape(marker)}(?=(\\s|$))", regexOptions)

            if (regex.containsMatchIn(currentText)) {
                foundIcons.add(icon)
                currentText = currentText.replace(regex, " ")
            }
        }

        currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
        val cleanedText = currentText.replace(Regex("\\s+"), " ").trim()

        return ParsedTextData(icons = foundIcons.toList(), mainText = cleanedText)
    }
}

@Composable
fun rememberParsedText(text: String, contextMarkerToEmojiMap: Map<String, String>): ParsedTextData {
    return remember(text, contextMarkerToEmojiMap) {
        ContextUtils.parseTextAndExtractIcons(text, contextMarkerToEmojiMap)
    }
}
