package com.romankozak.forwardappmobile.features.checklists.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.benasher44.uuid.uuid4
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.shared.database.ChecklistQueries
import com.romankozak.forwardappmobile.shared.database.Checklist_items
import com.romankozak.forwardappmobile.shared.database.Checklists
import com.romankozak.forwardappmobile.shared.features.checklists.data.model.Checklist
import com.romankozak.forwardappmobile.shared.features.checklists.data.model.ChecklistItem
import com.romankozak.forwardappmobile.shared.features.recentitems.data.RecentItemsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

import com.romankozak.forwardappmobile.shared.logging.logError

private const val CHECKLIST_ATTACHMENT_TYPE = "CHECKLIST"

private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

class ChecklistRepository(
    private val checklistQueries: ChecklistQueries,
    private val attachmentRepository: AttachmentRepository,
    private val recentItemsRepository: RecentItemsRepository,
    private val queryContext: CoroutineContext = EmptyCoroutineContext,
) {

    private val tag = "ChecklistRepository"

    fun getChecklistsForProject(projectId: String): Flow<List<Checklist>> =
        checklistQueries
            .getChecklistsForProject(projectId)
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun getAllChecklistsAsFlow(): Flow<List<Checklist>> =
        checklistQueries
            .getAllChecklists()
            .asFlow()
            .mapToList(queryContext)
            .map { rows -> rows.map { it.toModel() } }

    fun observeChecklistById(checklistId: String): Flow<Checklist?> =
        checklistQueries
            .getChecklistById(checklistId)
            .asFlow()
            .mapToOneOrNull(queryContext)
            .map { it?.toModel() }

    suspend fun getChecklistById(checklistId: String): Checklist? =
        withContext(queryContext) {
            checklistQueries
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
            checklistQueries.updateChecklist(
                id = checklist.id,
                projectId = checklist.projectId,
                name = checklist.name,
            )
        }
        recentItemsRepository.logChecklistAccess(checklist.id, checklist.name)
    }

    suspend fun deleteChecklist(checklistId: String) {
        withContext(queryContext) { checklistQueries.deleteChecklistById(checklistId) }
        attachmentRepository.findAttachmentByEntity(CHECKLIST_ATTACHMENT_TYPE, checklistId)?.let {
            attachmentRepository.deleteAttachment(it.id)
        }
    }

    fun getItemsForChecklist(checklistId: String): Flow<List<ChecklistItem>> =
        checklistQueries
            .getItemsForProjectStream(checklistId)
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
            checklistQueries.transaction {
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
            checklistQueries.transaction {
                items.forEach { updateItemInternal(it) }
            }
        }
    }

    suspend fun deleteItem(itemId: String) {
        withContext(queryContext) { checklistQueries.deleteItemsByIds(listOf(itemId)) }
    }

    suspend fun deleteItemsByChecklist(checklistId: String) {
        withContext(queryContext) { checklistQueries.deleteItemsForProjects(listOf(checklistId)) }
    }

    suspend fun getItemsSnapshot(checklistId: String): List<ChecklistItem> =
        withContext(queryContext) {
            checklistQueries
                .getItemsForProjectStream(checklistId)
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun getAllItemsSnapshot(): List<ChecklistItem> =
        withContext(queryContext) {
            checklistQueries
                .getAll()
                .executeAsList()
                .map { it.toModel() }
        }

    suspend fun deleteAllChecklists() {
        withContext(queryContext) { checklistQueries.deleteAllChecklists() }
    }

    suspend fun deleteAllChecklistItems() {
        withContext(queryContext) { checklistQueries.deleteAll() }
    }

    suspend fun replaceAll(checklists: List<Checklist>) {
        withContext(queryContext) {
            checklistQueries.transaction {
                checklistQueries.deleteAllChecklists()
                checklists.forEach { insertChecklistInternal(it) }
            }
        }
    }

    suspend fun replaceAllItems(items: List<ChecklistItem>) {
        withContext(queryContext) {
            checklistQueries.transaction {
                checklistQueries.deleteAll()
                items.forEach { insertItemInternal(it) }
            }
        }
    }

    suspend fun getAllChecklistsSnapshot(): List<Checklist> =
        withContext(queryContext) {
            checklistQueries
                .getAllChecklists()
                .executeAsList()
                .map { it.toModel() }
        }

    private fun insertChecklistInternal(checklist: Checklist) {
        checklistQueries.insertChecklist(
            id = checklist.id,
            projectId = checklist.projectId,
            name = checklist.name,
        )
    }

    private fun insertItemInternal(item: ChecklistItem) {
        checklistQueries.insertItem(
            id = item.id,
            checklistId = item.checklistId,
            content = item.content,
            isCompleted = item.isChecked.toLong(),
            itemOrder = item.itemOrder,
        )
    }

    private fun updateItemInternal(item: ChecklistItem) {
        checklistQueries.updateChecklistItem(
            id = item.id,
            checklistId = item.checklistId,
            content = item.content,
            isCompleted = item.isChecked.toLong(),
            itemOrder = item.itemOrder,
        )
    }

    private fun Boolean.toLong(): Long = if (this) 1L else 0L

    private fun Checklists.toModel(): Checklist =
        Checklist(
            id = id,
            projectId = projectId,
            name = name,
        )

    private fun Checklist_items.toModel(): ChecklistItem =
        ChecklistItem(
            id = id,
            checklistId = checklistId,
            content = content,
            isChecked = isCompleted != 0L,
            itemOrder = itemOrder,
        )
}
