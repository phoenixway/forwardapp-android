package com.romankozak.forwardappmobile.data.repository

import androidx.room.Transaction
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord
import com.romankozak.forwardappmobile.data.sync.bumpSync
import com.romankozak.forwardappmobile.data.sync.softDelete
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InboxRepository @Inject constructor(
    private val inboxRecordDao: InboxRecordDao,
    private val goalRepository: GoalRepository
) {

    suspend fun getInboxRecordById(id: String): InboxRecord? = inboxRecordDao.getRecordById(id)

    fun getInboxRecordsStream(projectId: String): Flow<List<InboxRecord>> = inboxRecordDao.getRecordsForProjectStream(projectId)

    suspend fun addInboxRecord(
        text: String,
        projectId: String,
    ) {
        val currentTime = System.currentTimeMillis()
        val newRecord =
            InboxRecord(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                text = text,
                createdAt = currentTime,
                order = -currentTime,
                updatedAt = currentTime,
                syncedAt = null,
                version = 1,
            )
        inboxRecordDao.insert(newRecord)
    }

    suspend fun updateInboxRecord(record: InboxRecord) {
        inboxRecordDao.update(record.bumpSync())
    }

    suspend fun deleteInboxRecordById(recordId: String) {
        inboxRecordDao.deleteById(recordId)
    }

    @Transaction
    suspend fun promoteInboxRecordToGoal(record: InboxRecord) {
        goalRepository.addGoalToProject(record.text, record.projectId)
        deleteInboxRecordById(record.id)
    }

    @Transaction
    suspend fun promoteInboxRecordToGoal(
        record: InboxRecord,
        targetProjectId: String,
    ) {
        goalRepository.addGoalToProject(record.text, targetProjectId)
        deleteInboxRecordById(record.id)
    }
}
