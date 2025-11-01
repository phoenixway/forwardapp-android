package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
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
            legacyNoteDao.insert(note)

            val newListItem =
                ListItem(
                    id = UUID.randomUUID().toString(),
                    projectId = note.projectId,
                    itemType = ListItemTypeValues.NOTE,
                    entityId = note.id,
                    order = -System.currentTimeMillis(),
                )
            listItemDao.insertItem(newListItem)
        } else {
            legacyNoteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
            recentItemsRepository.updateRecentItemDisplayName(note.id, note.title)
        }
    }

    @androidx.room.Transaction
    suspend fun deleteNote(noteId: String) {
        legacyNoteDao.deleteNoteById(noteId)
        listItemDao.deleteItemByEntityId(noteId)
    }
}
