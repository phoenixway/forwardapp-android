package com.romankozak.forwardappmobile.features.checklists.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.shared.database.ChecklistItems
import com.romankozak.forwardappmobile.shared.database.Checklists
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.checklists.data.model.Checklist
import com.romankozak.forwardappmobile.shared.features.checklists.data.model.ChecklistItem
import com.romankozak.forwardappmobile.shared.features.recentitems.data.RecentItemsRepository
import com.romankozak.forwardappmobile.shared.logging.logError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

private const val CHECKLIST_ATTACHMENT_TYPE = "CHECKLIST"

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

class ChecklistRepository(
    private val database: ForwardAppDatabase,
    private val attachmentRepository: AttachmentRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val queryContext: CoroutineContext = EmptyCoroutineContext,
) {

    private val tag = "ChecklistRepository"
    private val checklistsQueries = database.checklistsQueries
    private val checklistItemsQueries = database.checklistItemsQueries

    fun getChecklistsForProject(projectId: String): Flow<List<Checklist>> =
        checklistsQueries
            .getChecklistsForProject(projectId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllChecklistsAsFlow(): Flow<List<Checklist>> =
        checklistsQueries
            .getAllChecklists()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun observeChecklistById(checklistId: String): Flow<Checklist?> =
        checklistsQueries
            .getChecklistById(checklistId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.firstOrNull()?.toModel() }

    suspend fun getChecklistById(checklistId: String): Checklist? =
        withContext(queryContext) {
            checklistsQueries
                .getChecklistById(checklistId)
                .executeAsOneOrNull()
                ?.toModel()
        }

    suspend fun createChecklist(name: String, projectId: String): String? {
        return try {
            val checklist =
                Checklist(
                    id = uuid4().toString(),
                    projectId = projectId,
                    name = name,
                )
            withContext(queryContext) { insertChecklistInternal(checklist) }
            attachmentRepository.ensureAttachmentLinkedToProject(
                attachmentType = CHECKLIST_ATTACHMENT_TYPE,
                entityId = checklist.id,
                projectId = projectId,
                ownerProjectId = projectId,
                createdAt = nowMillis(),
            )
            recentItemsRepository.logChecklistAccess(checklist.id, checklist.name)
            checklist.id
        } catch (e: Exception) {
            logError(tag, "Failed to create checklist", e)
            null
        }
    }

    suspend fun updateChecklist(checklist: Checklist) {
        withContext(queryContext) {
            checklistsQueries.updateChecklist(
                id = checklist.id,
                projectId = checklist.projectId,
                name = checklist.name,
            )
        }
        recentItemsRepository.logChecklistAccess(checklist.id, checklist.name)
    }

    suspend fun deleteChecklist(checklistId: String) {
        withContext(queryContext) { checklistsQueries.deleteChecklistById(checklistId) }
        attachmentRepository.findAttachmentByEntity(CHECKLIST_ATTACHMENT_TYPE, checklistId)?.let {
            attachmentRepository.deleteAttachment(it.id)
        }
    }

    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItem>> =
        checklistItemsQueries
            .getItemsForChecklist(checklistId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun addItem(
        checklistId: String,
        content: String = "",
        isChecked: Boolean = false,
        order: Long = nowMillis(),
    ): String {
        val item =
            ChecklistItem(
                id = uuid4().toString(),
                checklistId = checklistId,
                content = content,
                isChecked = isChecked,
                itemOrder = order,
            )
        withContext(queryContext) { insertItemInternal(item) }
        return item.id
    }

    suspend fun addItems(items: List<ChecklistItem>) {
        if (items.isEmpty()) return
        withContext(queryContext) {
            database.transaction {
                items.forEach { insertItemInternal(it) }
            }
        }
    }

    suspend fun updateItem(item: ChecklistItem) {
        withContext(queryContext) { updateItemInternal(item) }
    }

    suspend fun updateItems(items: List<ChecklistItem>) {
        if (items.isEmpty()) return
        withContext(queryContext) {
            database.transaction {
                items.forEach { updateItemInternal(it) }
            }
        }
    }

    suspend fun deleteItem(itemId: String) {
        withContext(queryContext) { checklistItemsQueries.deleteChecklistItemById(itemId) }
    }

    suspend fun deleteItemsByChecklist(checklistId: String) {
        withContext(queryContext) { checklistItemsQueries.deleteChecklistItemsByChecklistId(checklistId) }
    }

    suspend fun getItemsSnapshot(checklistId: String): List<ChecklistItem> =
        withContext(queryContext) {
            checklistItemsQueries
                .getItemsForChecklist(checklistId)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getAllItemsSnapshot(): List<ChecklistItem> =
        withContext(queryContext) {
            checklistItemsQueries
                .getAllChecklistItems()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun deleteAllChecklists() {
        withContext(queryContext) { checklistsQueries.deleteAllChecklists() }
    }

    suspend fun deleteAllChecklistItems() {
        withContext(queryContext) { checklistItemsQueries.deleteAllChecklistItems() }
    }

    suspend fun replaceAll(checklists: List<Checklist>) {
        withContext(queryContext) {
            database.transaction {
                checklistsQueries.deleteAllChecklists()
                checklists.forEach { insertChecklistInternal(it) }
            }
        }
    }

    suspend fun replaceAllItems(items: List<ChecklistItem>) {
        withContext(queryContext) {
            database.transaction {
                checklistItemsQueries.deleteAllChecklistItems()
                items.forEach { insertItemInternal(it) }
            }
        }
    }

    suspend fun getAllChecklistsSnapshot(): List<Checklist> =
        withContext(queryContext) {
            checklistsQueries
                .getAllChecklists()
                .executeAsList()
                .map { it.toModel() }
        }

private fun insertChecklistInternal(checklist: Checklist) {
    checklistsQueries.insertChecklist(
        id = checklist.id,
        projectId = checklist.projectId,
        name = checklist.name,
    )
}

private fun insertItemInternal(item: ChecklistItem) {
    checklistItemsQueries.insertChecklistItem(
        id = item.id,
        checklistId = item.checklistId,
        content = item.content,
        isChecked = item.isChecked,
        itemOrder = item.itemOrder,
    )
}

private fun updateItemInternal(item: ChecklistItem) {
    checklistItemsQueries.updateChecklistItem(
        id = item.id,
        checklistId = item.checklistId,
        content = item.content,
isChecked = item.isChecked,
        itemOrder = item.itemOrder,
    )
}

    private fun Checklists.toModel(): Checklist =
        Checklist(
            id = id,
            projectId = projectId,
            name = name,
        )

    private fun ChecklistItems.toModel(): ChecklistItem =
        ChecklistItem(
            id = id,
            checklistId = checklistId,
            content = content,
            isChecked = isChecked,
            itemOrder = itemOrder,
        )
}