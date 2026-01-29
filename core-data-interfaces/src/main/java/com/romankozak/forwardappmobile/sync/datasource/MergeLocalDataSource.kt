// Модуль :core-data-interfaces
package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange

interface MergeLocalDataSource {
    suspend fun getContexts(): List<Context>
    suspend fun getGoals(): List<Goal>
    suspend fun getLocalDatabaseContent(): DatabaseContent

    suspend fun insertContexts(contexts: List<Context>)
    suspend fun insertGoals(goals: List<Goal>)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)
    suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>)
    suspend fun insertListItems(items: List<BacklogItem>)

    suspend fun applyChanges(changes: List<SyncChange>)
    suspend fun importSelectedData(
        projects: List<Context>,
        goals: List<Goal>,
        listItems: List<BacklogItem>,
        attachments: List<AttachmentEntity>,
        crossRefs: List<ContextAttachmentCrossRef>
    )
}