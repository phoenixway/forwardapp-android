package com.romankozak.forwardappmobile.sync.local

import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.sync.ChangeType
import com.romankozak.forwardappmobile.sync.DatabaseContent
import com.romankozak.forwardappmobile.sync.SyncChange
import com.romankozak.forwardappmobile.sync.SyncLocalService
import com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource
import javax.inject.Inject

class MergeLocalDataSourceImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val goalDao: GoalDao,
    private val contextDao: ContextDao,
    private val listItemDao: ListItemDao,
    private val attachmentDao: AttachmentDao,
    private val syncLocalService: SyncLocalService,
) : MergeLocalDataSource {

    override suspend fun getContexts(): List<Context> = contextDao.getAll()

    override suspend fun getGoals(): List<Goal> = goalDao.getAll()

    override suspend fun getLocalDatabaseContent(): DatabaseContent = syncLocalService.loadLocalDatabaseContent()

    override suspend fun applyChanges(approvedChanges: List<SyncChange>) {
        appDatabase.withTransaction {
            approvedChanges.forEach { change ->
                when (change.type) {
                    ChangeType.Delete -> {
                        when (change.entityType) {
                            "Список" -> contextDao.deleteContextById(change.id)
                            "Ціль" -> goalDao.deleteGoalById(change.id)
                            "Привʼязка" -> listItemDao.deleteItemsByIds(listOf(change.id))
                        }
                    }
                    ChangeType.Update, ChangeType.Add, ChangeType.Move -> {
                        when (val entity = change.entity) {
                            is Context -> contextDao.insert(entity)
                            is Goal -> goalDao.insertGoal(entity)
                            is BacklogItem -> listItemDao.insertItem(entity)
                        }
                    }
                }
            }
        }
    }

    override suspend fun insertContexts(contexts: List<Context>) = contextDao.insertContexts(contexts)

    override suspend fun insertGoals(goals: List<Goal>) = goalDao.insertGoals(goals)

    override suspend fun insertAttachments(attachments: List<AttachmentEntity>) = attachmentDao.insertAttachments(attachments)

    override suspend fun insertContextAttachmentLinks(refs: List<ContextAttachmentCrossRef>) = attachmentDao.insertContextAttachmentLinks(refs)

    override suspend fun insertListItems(items: List<BacklogItem>) = listItemDao.insertItems(items)

    override suspend fun importSelectedData(
        projects: List<Context>,
        goals: List<Goal>,
        listItems: List<BacklogItem>,
        attachments: List<AttachmentEntity>,
        crossRefs: List<ContextAttachmentCrossRef>
    ) {
        val ts = System.currentTimeMillis()
        appDatabase.withTransaction {
            if (projects.isNotEmpty()) contextDao.insertContexts(projects.map { it.copy(syncedAt = ts) })
            if (goals.isNotEmpty()) goalDao.insertGoals(goals.map { it.copy(syncedAt = ts) })
            if (listItems.isNotEmpty()) listItemDao.insertItems(listItems.map { it.copy(syncedAt = ts) })
            if (attachments.isNotEmpty()) attachmentDao.insertAttachments(attachments.map { it.copy(syncedAt = ts) })
            if (crossRefs.isNotEmpty()) attachmentDao.insertContextAttachmentLinks(crossRefs.map { it.copy(syncedAt = ts) })
        }
    }
}
