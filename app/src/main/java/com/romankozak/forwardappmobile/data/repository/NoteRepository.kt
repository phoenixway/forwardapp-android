package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDao
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val listItemDao: ListItemDao,
    private val recentItemsRepository: RecentItemsRepository
) {
    suspend fun getNoteById(noteId: String): NoteEntity? = noteDao.getNoteById(noteId)

    fun getNotesForProject(projectId: String): Flow<List<NoteEntity>> = noteDao.getNotesForProject(projectId)

    fun getAllAsFlow(): Flow<List<NoteEntity>> = noteDao.getAllAsFlow()

    @androidx.room.Transaction
    suspend fun saveNote(note: NoteEntity) {
        val existingNote = noteDao.getNoteById(note.id)
        if (existingNote == null) {
            noteDao.insert(note)

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
            noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
            recentItemsRepository.updateRecentItemDisplayName(note.id, note.title)
        }
    }

    @androidx.room.Transaction
    suspend fun deleteNote(noteId: String) {
        noteDao.deleteNoteById(noteId)
        listItemDao.deleteItemByEntityId(noteId)
    }
}
