package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity
import com.romankozak.forwardappmobile.features.contexts.data.dao.MusicNoteDao
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicNoteRepository
    @Inject
    constructor(
        private val musicNoteDao: MusicNoteDao,
        private val attachmentRepository: AttachmentsRepository,
    ) {
        fun getMusicNotesForContext(contextId: String): Flow<List<MusicNoteEntity>> =
            musicNoteDao.getForContext(contextId, BacklogItemTypeValues.MUSIC_NOTE)

        fun getAllMusicNotesAsFlow(): Flow<List<MusicNoteEntity>> = musicNoteDao.getAllAsFlow()

        suspend fun findByName(name: String): MusicNoteEntity? = musicNoteDao.findByName(name)

        suspend fun getById(id: String): MusicNoteEntity? = musicNoteDao.getById(id)

        @Transaction
        suspend fun create(
            name: String,
            contextId: String,
            content: String = "",
            roleCode: String? = null,
            isSystem: Boolean = false,
        ): String {
            val now = System.currentTimeMillis()
            val musicNote = MusicNoteEntity(name = name, contextId = contextId, content = content, updatedAt = now, version = 1)
            musicNoteDao.insert(musicNote)
            attachmentRepository.ensureAttachmentLinkedToContext(
                attachmentType = BacklogItemTypeValues.MUSIC_NOTE,
                entityId = musicNote.id,
                contextId = contextId,
                ownerContextId = contextId,
                createdAt = musicNote.createdAt,
                roleCode = roleCode,
                isSystem = isSystem,
            )
            return musicNote.id
        }

        suspend fun update(musicNote: MusicNoteEntity) {
            val now = System.currentTimeMillis()
            musicNoteDao.update(
                musicNote.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = musicNote.version + 1,
                ),
            )
        }

        @Transaction
        suspend fun delete(id: String) {
            val now = System.currentTimeMillis()
            val existing = musicNoteDao.getById(id)
            if (existing != null) {
                musicNoteDao.insert(
                    existing.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = true,
                        version = existing.version + 1,
                    ),
                )
            } else {
                musicNoteDao.deleteById(id)
            }
            attachmentRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, id)?.let {
                attachmentRepository.deleteAttachment(it.id)
            }
        }
    }
