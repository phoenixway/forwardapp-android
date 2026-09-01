// Модуль :core-data-interfaces
package com.romankozak.forwardappmobile.sync.datasource

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.SyncChange

interface MergeLocalDataSource {
    suspend fun getContexts(): List<Context>
    suspend fun getGoals(): List<Goal>
    suspend fun insertContexts(contexts: List<Context>)
    suspend fun insertGoals(goals: List<Goal>)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)
    suspend fun insertContextAttachmentLinks(links: List<ContextAttachmentCrossRef>)

    suspend fun applyChanges(changes: List<SyncChange>)
    suspend fun importSelectedData(
        projects: List<Context>,
        goals: List<Goal>,
        attachments: List<AttachmentEntity>,
        crossRefs: List<ContextAttachmentCrossRef>
    )
    suspend fun applySnapshotBundle(bundle: SnapshotBundle)
}
