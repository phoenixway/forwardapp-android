package com.romankozak.forwardappmobile.ui.common

import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTextData(
    val icons: List<String>,
    val mainText: String,
    val contextMarkers: List<String> = emptyList(),
)

@Singleton
class ContextUtils
    @Inject
    constructor(
        private val iconProvider: IconProvider,
    ) {
        fun parseTextAndExtractIcons(
            text: String,
            contextMarkerToEmojiMap: Map<String, String>,
        ): ParsedTextData {
            val allMarkersToIcons = mutableMapOf<String, String>()
            val knownContextMarkerStems =
                contextMarkerToEmojiMap.keys
                    .asSequence()
                    .filter { it.startsWith("@") }
                    .map { it.removePrefix("@").lowercase() }
                    .toSet()
            val hardcodedIconsData = iconProvider.getIconMappings()
            hardcodedIconsData.forEach { (icon, markers) ->
                markers.forEach { marker ->
                    allMarkersToIcons[marker] = icon
                }
            }
            allMarkersToIcons.putAll(contextMarkerToEmojiMap)
            knownContextMarkerStems.forEach { stem ->
                val atMarker = "@$stem"
                val hashMarker = "#$stem"
                val icon = contextMarkerToEmojiMap[atMarker] ?: contextMarkerToEmojiMap[atMarker.lowercase()]
                if (icon != null && hashMarker !in allMarkersToIcons) {
                    allMarkersToIcons[hashMarker] = icon
                }
            }

            val foundIcons = mutableSetOf<String>()
            val foundContextMarkers = linkedSetOf<String>()
            var currentText = text

            val pattern =
                allMarkersToIcons.keys
                    .sortedByDescending { it.length }
                    .joinToString("|") { Regex.escape(it) }

            val regex = Regex("(?<=(^|\\s))($pattern)(?=(\\s|$))", setOf(RegexOption.IGNORE_CASE))
            val matches = regex.findAll(currentText)

            matches.forEach {
                val marker = it.groupValues[2]
                val icon = allMarkersToIcons[marker] ?: allMarkersToIcons[marker.lowercase()]
                val normalizedStem = marker.removePrefix("@").removePrefix("#").lowercase()
                val isKnownContextMarker = normalizedStem in knownContextMarkerStems
                if (icon != null) {
                    foundIcons.add(icon)
                }
                if (isKnownContextMarker && icon == null) {
                    foundContextMarkers.add("@$normalizedStem")
                }
            }

            currentText = currentText.replace(regex, " ")
            currentText = currentText.replace(Regex("\\[icon::\\s*([^]]+?)\\s*]"), "")
            val cleanedText = currentText.replace(Regex("\\s+"), " ").trim()

            return ParsedTextData(
                icons = foundIcons.toList(),
                mainText = cleanedText,
                contextMarkers = foundContextMarkers.toList(),
            )
        }
    }
