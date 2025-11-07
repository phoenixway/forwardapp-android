package com.romankozak.forwardappmobile.data.notes

import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLink
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLinkDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidNoteBacklogLinkDataSource @Inject constructor(
    private val listItemDao: ListItemDao,
) : NoteBacklogLinkDataSource {

    override suspend fun insertLink(entry: NoteBacklogLink) {
        val listItem =
            ListItem(
                id = entry.id,
                projectId = entry.projectId,
                itemType = entry.itemType,
                entityId = entry.entityId,
                order = entry.order,
            )
        listItemDao.insertItem(listItem)
    }

    override suspend fun deleteLinkByEntityId(entityId: String) {
        listItemDao.deleteItemByEntityId(entityId)
    }
}
