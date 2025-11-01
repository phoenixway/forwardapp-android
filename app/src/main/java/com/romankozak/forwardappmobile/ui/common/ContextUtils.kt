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
        val allMarkersToIcons = mutableMapOf<String, String>()
        val hardcodedIconsData = iconProvider.getIconMappings()
        hardcodedIconsData.forEach { (icon, markers) ->
            markers.forEach { marker ->
                allMarkersToIcons[marker] = icon
            }
        }
        allMarkersToIcons.putAll(contextMarkerToEmojiMap)

        val foundIcons = mutableSetOf<String>()
        var currentText = text

        val pattern = allMarkersToIcons.keys
            .sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }

        val regex = Regex("(?<=(^|\\s))($pattern)(?=(\\s|$))", setOf(RegexOption.IGNORE_CASE))
        val matches = regex.findAll(currentText)

        matches.forEach {
            val marker = it.groupValues[2]
            val icon = allMarkersToIcons[marker] ?: allMarkersToIcons[marker.lowercase()]
            if (icon != null) {
                foundIcons.add(icon)
            }
        }

        currentText = currentText.replace(regex, " ")
        currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
        val cleanedText = currentText.replace(Regex("\\s+"), " ").trim()

        return ParsedTextData(icons = foundIcons.toList(), mainText = cleanedText)
    }
}
