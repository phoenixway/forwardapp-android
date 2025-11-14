package com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.data.mappers.toDomain
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.Checklist
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.model.ChecklistItem
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChecklistRepositoryImpl(
    private val database: ForwardAppDatabase,
    private val dispatcher: CoroutineDispatcher,
) : ChecklistRepository {

    override fun observeChecklists(projectId: String?): Flow<List<Checklist>> {
        val query = if (projectId == null) {
            database.checklistsQueries.getAllChecklists()
        } else {
            database.checklistsQueries.getChecklistsForProject(projectId)
        }

        return query
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeChecklistItems(checklistId: String): Flow<List<ChecklistItem>> =
        database.checklistItemsQueries.getItemsForChecklist(checklistId)
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getChecklistById(id: String): Checklist? = withContext(dispatcher) {
        database.checklistsQueries.getChecklistById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getChecklistItemById(id: String): ChecklistItem? = withContext(dispatcher) {
        database.checklistItemsQueries.getChecklistItemById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun upsertChecklist(checklist: Checklist) = withContext(dispatcher) {
        database.checklistsQueries.insertChecklist(
            id = checklist.id,
            projectId = checklist.projectId,
            name = checklist.name,
        )
    }

    override suspend fun upsertChecklistItem(item: ChecklistItem) = withContext(dispatcher) {
        database.checklistItemsQueries.insertChecklistItem(
            id = item.id,
            checklistId = item.checklistId,
            content = item.content,
            isChecked = item.isChecked,
            itemOrder = item.order,
        )
    }

    override suspend fun deleteChecklistById(id: String) = withContext(dispatcher) {
        database.transaction {
            database.checklistItemsQueries.deleteChecklistItemsByChecklistId(id)
            database.checklistsQueries.deleteChecklistById(id)
        }
    }

    override suspend fun deleteChecklistItemById(id: String) = withContext(dispatcher) {
        database.checklistItemsQueries.deleteChecklistItemById(id)
    }

    override suspend fun deleteChecklistItemsByChecklist(checklistId: String) = withContext(dispatcher) {
        database.checklistItemsQueries.deleteChecklistItemsByChecklistId(checklistId)
    }

    override suspend fun deleteAllChecklists(projectId: String?) = withContext(dispatcher) {
        database.transaction {
            if (projectId == null) {
                database.checklistItemsQueries.deleteAllChecklistItems()
                database.checklistsQueries.deleteAllChecklists()
            } else {
                database.checklistItemsQueries.deleteChecklistItemsByProjectId(projectId)
                database.checklistsQueries.deleteChecklistsByProjectId(projectId)
            }
        }
    }

    override suspend fun deleteAllChecklistItems(projectId: String?) = withContext(dispatcher) {
        if (projectId == null) {
            database.checklistItemsQueries.deleteAllChecklistItems()
        } else {
            database.checklistItemsQueries.deleteChecklistItemsByProjectId(projectId)
        }
    }
}
