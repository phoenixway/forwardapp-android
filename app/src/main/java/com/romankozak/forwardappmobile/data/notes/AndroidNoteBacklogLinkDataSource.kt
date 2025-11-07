package com.romankozak.forwardappmobile.data.notes

import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import com.romankozak.forwardappmobile.core.database.models.ListItem
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLink
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLinkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNoteBacklogLinkDataSource @Inject constructor(
    private val listItemQueries: ListItemQueries,
) : NoteBacklogLinkDataSource {

    override suspend fun insertLink(entry: NoteBacklogLink) {
        listItemQueries.insert(
            id = entry.id,
            project_id = entry.projectId,
            item_type = entry.itemType,
            entity_id = entry.entityId,
            item_order = entry.order
        )
    }

    override suspend fun deleteLinkByEntityId(entityId: String) {
        listItemQueries.deleteByEntityId(entityId)
    }
}
