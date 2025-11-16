package com.romankozak.forwardappmobile.features.projectscreen.components.backlogitems

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.shared.data.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.goals.data.models.Goal
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.shared.features.reminders.domain.model.Reminder

@Composable
fun StatusIconsRow(
    goal: Goal? = null,
    project: Project? = null,
    parsedData: Any,
    reminder: Reminder?,
    emojiToHide: String?,
    onRelatedLinkClick: (RelatedLink) -> Unit
) {
    Row {
        // TODO: Implement StatusIconsRow
    }
}
