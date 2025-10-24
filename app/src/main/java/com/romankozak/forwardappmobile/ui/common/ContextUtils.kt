package com.romankozak.forwardappmobile.ui.common

import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTextData(
    val icons: List<String>,
    val mainText: String,
)

@Singleton
class ContextUtils @Inject constructor(
    private val iconProvider: IconProvider
) {
    fun parseTextAndExtractIcons(
        text: String,
        contextMarkerToEmojiMap: Map<String, String>,
    ): ParsedTextData {
        var currentText = text
        val foundIcons = mutableSetOf<String>()

        val allMarkersToIcons = mutableMapOf<String, String>()

        val hardcodedIconsData = iconProvider.getIconMappings()
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
