package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyNoteRepository @Inject constructor(
    private val legacyNoteDao: LegacyNoteDao,
    private val listItemDao: ListItemDao,
    private val recentItemsRepository: RecentItemsRepository,
) {
    suspend fun getNoteById(noteId: String): LegacyNoteEntity? = legacyNoteDao.getNoteById(noteId)

    fun getNotesForProject(projectId: String): Flow<List<LegacyNoteEntity>> = legacyNoteDao.getNotesForProject(projectId)

    fun getAllAsFlow(): Flow<List<LegacyNoteEntity>> = legacyNoteDao.getAllAsFlow()

    @androidx.room.Transaction
    suspend fun saveNote(note: LegacyNoteEntity) {
        val existingNote = legacyNoteDao.getNoteById(note.id)
        if (existingNote == null) {
            val now = System.currentTimeMillis()
            val newNote = note.bumpSync(now)
            legacyNoteDao.insert(newNote)

            val newListItem =
                ListItem(
                    id = UUID.randomUUID().toString(),
                    projectId = note.projectId,
                    itemType = ListItemTypeValues.NOTE,
                    entityId = note.id,
                    order = -now,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                )
            listItemDao.insertItem(newListItem)
        } else {
            val now = System.currentTimeMillis()
            val bumped = note.copy(
                updatedAt = now,
                syncedAt = null,
                version = note.version + 1,
            )
            legacyNoteDao.update(bumped)
            recentItemsRepository.updateRecentItemDisplayName(note.id, note.title)
        }
    }

    @androidx.room.Transaction
    suspend fun deleteNote(noteId: String) {
        val now = System.currentTimeMillis()
        val existingNote = legacyNoteDao.getNoteById(noteId)
        if (existingNote != null) {
            legacyNoteDao.insert(
                existingNote.softDelete(now),
            )
        } else {
            legacyNoteDao.deleteNoteById(noteId)
        }
        listItemDao.getListItemByEntityId(noteId)?.let { listItem ->
            listItemDao.insertItem(
                listItem.softDelete(now),
            )
        } ?: listItemDao.deleteItemByEntityId(noteId)
    }
}
