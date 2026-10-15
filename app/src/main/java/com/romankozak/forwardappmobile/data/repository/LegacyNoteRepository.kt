package com.romankozak.forwardappmobile.data.repository

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyNoteRepository
    @Inject
    constructor(
        private val legacyNoteDao: LegacyNoteDao,
        private val database: AppDatabase,
        private val recentItemsRepository: RecentItemsRepository,
        private val backlogPlacementCommands: BacklogPlacementCommands,
    ) {
        suspend fun getNoteById(noteId: String): LegacyNoteEntity? = legacyNoteDao.getNoteById(noteId)

        /** Historical Context presentation only, never canonical Workspace placement authority. */
        fun getNotesForContext(contextId: String): Flow<List<LegacyNoteEntity>> = legacyNoteDao.getNotesForContext(contextId)

        /** Canonical BACKLOG content projection, scoped exclusively by live placements. */
        fun observePlacedNotesForWorkspace(workspaceId: String): Flow<List<LegacyNoteEntity>> =
            legacyNoteDao.observePlacedNotesForWorkspace(workspaceId)

        fun getAllAsFlow(): Flow<List<LegacyNoteEntity>> = legacyNoteDao.getAllAsFlow()

        /**
         * FK.10B: Note content is standalone. An initial Workspace placement is
         * an explicit, optional operation, not inferred from historical contextId.
         */
        suspend fun saveNote(
            note: LegacyNoteEntity,
            initialWorkspaceId: String? = null,
        ) {
            require(initialWorkspaceId == null || initialWorkspaceId.isNotBlank()) {
                "Initial Note Workspace ID cannot be blank"
            }

            val updatedExisting =
                database.withTransaction {
                    val existingNote = legacyNoteDao.getNoteById(note.id)
                    val now = System.currentTimeMillis()

                    if (existingNote == null) {
                        legacyNoteDao.insert(note.bumpSync(now))

                        initialWorkspaceId?.let { workspaceId ->
                            backlogPlacementCommands.addNoteAtStart(
                                workspaceId = workspaceId,
                                noteId = note.id,
                                now = now,
                            )
                        }
                        false
                    } else {
                        require(initialWorkspaceId == null) {
                            "An existing Note must be placed through BACKLOG commands"
                        }
                        require(!existingNote.isDeleted) {
                            "Cannot edit a deleted Note"
                        }
                        require(note.version == existingNote.version) {
                            "Stale Note version for ${note.id}"
                        }

                        val bumped =
                            note.copy(
                                contextId = existingNote.contextId,
                                updatedAt = now,
                                syncedAt = null,
                                version = existingNote.version + 1,
                            )
                        legacyNoteDao.update(bumped)
                        true
                    }
                }

            if (updatedExisting) {
                recentItemsRepository.updateRecentItemDisplayName(note.id, note.title)
            }
        }

        suspend fun deleteNote(noteId: String) {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val existingNote = legacyNoteDao.getNoteById(noteId)

                if (existingNote != null && !existingNote.isDeleted) {
                    legacyNoteDao.insert(existingNote.softDelete(now))
                }

                backlogPlacementCommands.tombstoneContextBackedTarget(
                    itemType = BacklogItemTypeValues.NOTE,
                    entityId = noteId,
                    now = now,
                )
            }
        }
    }
