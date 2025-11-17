package com.romankozak.forwardappmobile.features.projectscreen.components.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItem
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder

data class ParsedData(
    val mainText: String,
    val contextEmojis: List<String>,
    val relatedLinks: List<RelatedLink>
)

/*@Composable
fun rememberParsedText(text: String, contextMarkerToEmojiMap: Map<String, String>): ParsedData {
    return remember(text, contextMarkerToEmojiMap) {
        // This is a placeholder implementation.
        // You should replace this with your actual parsing logic.
        ParsedData(text, emptyList(), emptyList())
    }
}*/

@Composable
fun StatusIconsRow(
    goal: ListItem,
    parsedData: ParsedData,
    reminder: Reminder?,
    emojiToHide: String?,
    onRelatedLinkClick: (RelatedLink) -> Unit
) {
    // This is a placeholder implementation.
}
