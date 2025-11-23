package com.romankozak.forwardappmobile.features.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

data class ParsedData(
    val mainText: String,
    val icons: List<String>
)

@Composable
fun rememberParsedText(text: String, contextMarkerToEmojiMap: Map<String, String>): ParsedData {
    return remember(text, contextMarkerToEmojiMap) {
        ParsedData(text, emptyList())
    }
}
