package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.sync.AttachmentsRepository // Updated import
import com.romankozak.forwardappmobile.core.data.models.ScriptEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepository
    @Inject
    constructor(
        private val scriptDao: ScriptDao,
        private val attachmentRepository: com.romankozak.forwardappmobile.sync.AttachmentsRepository, // Updated type
    ) {
        fun getAllScripts(): Flow<List<ScriptEntity>> = scriptDao.getAllFlow()

        fun getScriptsForContext(contextId: String): Flow<List<ScriptEntity>> = scriptDao.getForContext(contextId)

        suspend fun getScriptById(id: String): ScriptEntity? = scriptDao.getById(id)

        suspend fun createScript(
            name: String,
            content: String,
            contextId: String?,
            description: String? = null,
        ): String {
            val timestamp = System.currentTimeMillis()
            val script =
                ScriptEntity(
                    id = UUID.randomUUID().toString(),
                    contextId = contextId,
                    name = name,
                    description = description,
                    content = content,
                    createdAt = timestamp,
                    updatedAt = timestamp,
                    syncedAt = null,
                    version = 1,
                )
            scriptDao.insert(script)

            if (contextId != null) {
                attachmentRepository.ensureAttachmentLinkedToContext(
                    attachmentType = BacklogItemTypeValues.SCRIPT,
                    entityId = script.id,
                    contextId = contextId,
                    ownerContextId = contextId,
                    createdAt = timestamp,
                )
            }

            return script.id
        }

        suspend fun updateScript(script: ScriptEntity) {
            val now = System.currentTimeMillis()
            scriptDao.update(
                script.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = script.version + 1,
                ),
            )
        }

        suspend fun deleteScript(script: ScriptEntity) {
            val now = System.currentTimeMillis()
            scriptDao.insert(
                script.copy(
                    isDeleted = true,
                    updatedAt = now,
                    syncedAt = null,
                    version = script.version + 1,
                ),
            )
        }
    }
