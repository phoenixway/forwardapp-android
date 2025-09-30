
package com.romankozak.forwardappmobile.ui.screens.projectscreen.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel

@Composable
fun AttachmentsView(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    listContent: List<ListItemContent>
) {
    val attachments = listContent.filter {
        it is ListItemContent.LinkItem || it is ListItemContent.NoteItem || it is ListItemContent.CustomListItem
    }

    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(attachments) { item ->
            when (item) {
                is ListItemContent.LinkItem -> Text("Link: ${item.link.linkData.displayName}")
                is ListItemContent.NoteItem -> Text("Note: ${item.note.title}")
                is ListItemContent.CustomListItem -> Text("Custom List: ${item.customList.name}")
                else -> {}
            }
        }
    }
}
