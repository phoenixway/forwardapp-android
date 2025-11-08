package com.romankozak.forwardappmobile.shared.features.recentitems.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.recentitems.data.model.RecentItem
import com.romankozak.forwardappmobile.shared.features.recentitems.data.model.RecentItemType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class RecentItemsRepository(
    private val database: ForwardAppDatabase,
    private val ioDispatcher: CoroutineDispatcher,
) {

    fun getRecentItems(limit: Long = 20): Flow<List<RecentItem>> =
        database.recentItemQueries
            .getRecentItems(limit, ::mapRow)
            .asFlow()
            .mapToList(ioDispatcher)

    suspend fun logProjectAccess(projectId: String, projectName: String) {
        upsertRecentItem(
            RecentItem(
                id = projectId,
                type = RecentItemType.PROJECT,
                lastAccessed = currentTimestamp(),
                displayName = projectName,
                target = projectId,
            ),
        )
    }

    suspend fun logLegacyNoteAccess(noteId: String, title: String) {
        upsertRecentItem(
            RecentItem(
                id = noteId,
                type = RecentItemType.NOTE,
                lastAccessed = currentTimestamp(),
                displayName = title,
                target = noteId,
            ),
        )
    }

    suspend fun logNoteDocumentAccess(documentId: String, name: String) {
        upsertRecentItem(
            RecentItem(
                id = documentId,
                type = RecentItemType.NOTE_DOCUMENT,
                lastAccessed = currentTimestamp(),
                displayName = name,
                target = documentId,
            ),
        )
    }

    suspend fun logChecklistAccess(checklistId: String, name: String) {
        upsertRecentItem(
            RecentItem(
                id = checklistId,
                type = RecentItemType.CHECKLIST,
                lastAccessed = currentTimestamp(),
                displayName = name,
                target = checklistId,
            ),
        )
    }

    suspend fun logObsidianLinkAccess(linkId: String, displayName: String?) {
        upsertRecentItem(
            RecentItem(
                id = linkId,
                type = RecentItemType.OBSIDIAN_LINK,
                lastAccessed = currentTimestamp(),
                displayName = displayName ?: linkId,
                target = linkId,
            ),
        )
    }

    suspend fun updateRecentItem(item: RecentItem) {
        upsertRecentItem(item)
    }

    suspend fun updateRecentItemDisplayName(itemId: String, displayName: String) {
        val existing = getRecentItemById(itemId) ?: return
        upsertRecentItem(existing.copy(displayName = displayName))
    }

    suspend fun getRecentItemById(itemId: String): RecentItem? =
        withContext(ioDispatcher) { getRecentItemByIdInternal(itemId) }

    suspend fun clearAll() =
        withContext(ioDispatcher) {
            database.recentItemQueries.deleteAllRecentItems()
        }

    suspend fun replaceAll(items: List<RecentItem>) =
        withContext(ioDispatcher) {
            database.transaction {
                database.recentItemQueries.deleteAllRecentItems()
                items.forEach { insertRecord(it) }
            }
        }

    private suspend fun upsertRecentItem(item: RecentItem) =
        withContext(ioDispatcher) {
            val existing = getRecentItemByIdInternal(item.id)
            val updated =
                if (existing != null) {
                    existing.copy(
                        type = item.type,
                        lastAccessed = item.lastAccessed,
                        displayName = item.displayName,
                        target = item.target,
                        isPinned = item.isPinned,
                    )
                } else {
                    item
                }
            insertRecord(updated)
        }

    private fun getRecentItemByIdInternal(id: String): RecentItem? =
        database.recentItemQueries.getRecentItemById(id, ::mapRow).executeAsOneOrNull()

    private fun currentTimestamp(): Long = Clock.System.now().toEpochMilliseconds()

    private fun insertRecord(item: RecentItem) {
        database.recentItemQueries.insertRecentItem(
            id = item.id,
            type = item.type.name,
            lastAccessed = item.lastAccessed,
            displayName = item.displayName,
            target = item.target,
            isPinned = if (item.isPinned) 1L else 0L,
        )
    }

    private fun mapRow(
        id: String,
        type: String,
        lastAccessed: Long,
        displayName: String,
        target: String,
        isPinned: Long,
    ): RecentItem =
        RecentItem(
            id = id,
            type = type.toRecentItemType(),
            lastAccessed = lastAccessed,
            displayName = displayName,
            target = target,
            isPinned = isPinned != 0L,
        )

    private fun String.toRecentItemType(): RecentItemType =
        runCatching { RecentItemType.valueOf(this) }.getOrElse { RecentItemType.PROJECT }
}