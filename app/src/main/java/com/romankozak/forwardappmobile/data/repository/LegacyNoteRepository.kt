package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyNoteRepository
    @Inject
    constructor(
        private val legacyNoteDao: LegacyNoteDao,
        private val recentItemsRepository: RecentItemsRepository,
        private val backlogPlacementCommands: BacklogPlacementCommands,
    ) {
        suspend fun getNoteById(noteId: String): LegacyNoteEntity? = legacyNoteDao.getNoteById(noteId)

        fun getNotesForContext(contextId: String): Flow<List<LegacyNoteEntity>> = legacyNoteDao.getNotesForContext(contextId)

        fun getAllAsFlow(): Flow<List<LegacyNoteEntity>> = legacyNoteDao.getAllAsFlow()

        @androidx.room.Transaction
        suspend fun saveNote(note: LegacyNoteEntity) {
            val existingNote = legacyNoteDao.getNoteById(note.id)
            if (existingNote == null) {
                val now = System.currentTimeMillis()
                val newNote = note.bumpSync(now)
                legacyNoteDao.insert(newNote)

                backlogPlacementCommands.addToContextBacked(
                    contextId = note.contextId,
                    itemType = BacklogItemTypeValues.NOTE,
                    entityId = note.id,
                )
            } else {
                val now = System.currentTimeMillis()
                val bumped =
                    note.copy(
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
            backlogPlacementCommands.tombstoneContextBackedTarget(
                itemType = BacklogItemTypeValues.NOTE,
                entityId = noteId,
                now = now,
            )
        }
    }
