package com.romankozak.forwardappmobile.features.attachments.ui.context.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemLayout
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton

@Composable
fun ChecklistItemRow(
    checklistItem: BacklogItemContent.ChecklistItem,
    onDelete: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    UnifiedListItemLayout(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
        leading = {
            Icon(
                imageVector = Icons.Outlined.Checklist,
                contentDescription = stringResource(R.string.attachment_checklist_icon_description),
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        },
        trailing = {
            if (trailingContent != null) {
                trailingContent()
            } else {
                UnifiedTrailingActionButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.attachment_checklist_delete_description),
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        main = {
            Text(
                text = checklistItem.checklist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
