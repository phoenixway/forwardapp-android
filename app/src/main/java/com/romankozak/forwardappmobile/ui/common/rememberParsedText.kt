package com.romankozak.forwardappmobile.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberParsedText(text: String, contextMarkerToEmojiMap: Map<String, String>): ParsedTextData {
    val contextUtils = LocalContextUtils.current
    return remember(text, contextMarkerToEmojiMap) {
        contextUtils.parseTextAndExtractIcons(text, contextMarkerToEmojiMap)
    }
}
