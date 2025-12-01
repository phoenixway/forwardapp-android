package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScriptRepository @Inject constructor(
    private val scriptDao: ScriptDao,
    private val attachmentRepository: AttachmentRepository,
) {

    fun getAllScripts(): Flow<List<ScriptEntity>> = scriptDao.getAll()

    fun getScriptsForProject(projectId: String): Flow<List<ScriptEntity>> = scriptDao.getForProject(projectId)

    suspend fun getScriptById(id: String): ScriptEntity? = scriptDao.getById(id)

    suspend fun createScript(
        name: String,
        content: String,
        projectId: String?,
        description: String? = null,
    ): String {
        val timestamp = System.currentTimeMillis()
        val script =
            ScriptEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                name = name,
                description = description,
                content = content,
                createdAt = timestamp,
                updatedAt = timestamp,
                syncedAt = null,
                version = 1,
            )
        scriptDao.insert(script)

        if (projectId != null) {
            attachmentRepository.ensureAttachmentLinkedToProject(
                attachmentType = ListItemTypeValues.SCRIPT,
                entityId = script.id,
                projectId = projectId,
                ownerProjectId = projectId,
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
