package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository

import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.Checklist
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.ChecklistItem
import kotlinx.coroutines.flow.Flow

interface ChecklistRepository {
    fun observeChecklists(projectId: String? = null): Flow<List<Checklist>>
    fun observeChecklistItems(checklistId: String): Flow<List<ChecklistItem>>

    suspend fun getChecklistById(id: String): Checklist?
    suspend fun getChecklistItemById(id: String): ChecklistItem?

    suspend fun upsertChecklist(checklist: Checklist)
    suspend fun upsertChecklistItem(item: ChecklistItem)

    suspend fun deleteChecklistById(id: String)
    suspend fun deleteChecklistItemById(id: String)
    suspend fun deleteChecklistItemsByChecklist(checklistId: String)

    suspend fun deleteAllChecklists(projectId: String? = null)
    suspend fun deleteAllChecklistItems(projectId: String? = null)
}
