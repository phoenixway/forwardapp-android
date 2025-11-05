package com.romankozak.forwardappmobile.data.repository

import android.util.Log
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.data.database.models.RecentItemType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentItemsRepository @Inject constructor(
    private val recentItemDao: RecentItemDao
) {
    fun getRecentItems(limit: Int = 20): Flow<List<RecentItem>> = recentItemDao.getRecentItems(limit)

    suspend fun logProjectAccess(project: Project) {
        val existingItem = recentItemDao.getRecentItemById(project.id)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = project.id,
                type = RecentItemType.PROJECT,
                lastAccessed = System.currentTimeMillis(),
                displayName = project.name,
                target = project.id
            )
        }
        Log.d("Recents_Debug", "Logging project access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun logNoteAccess(note: LegacyNoteEntity) {
        val existingItem = recentItemDao.getRecentItemById(note.id)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = note.id,
                type = RecentItemType.NOTE,
                lastAccessed = System.currentTimeMillis(),
                displayName = note.title,
                target = note.id
            )
        }
        Log.d("Recents_Debug", "Logging note access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun logNoteDocumentAccess(document: NoteDocumentEntity) {
        val existingItem = recentItemDao.getRecentItemById(document.id)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = document.id,
                type = RecentItemType.NOTE_DOCUMENT,
                lastAccessed = System.currentTimeMillis(),
                displayName = document.name,
                target = document.id
            )
        }
        Log.d("Recents_Debug", "Logging note document access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun logChecklistAccess(checklist: ChecklistEntity) {
        val existingItem = recentItemDao.getRecentItemById(checklist.id)
        val recentItem =
            if (existingItem != null) {
                existingItem.copy(lastAccessed = System.currentTimeMillis(), displayName = checklist.name)
            } else {
                RecentItem(
                    id = checklist.id,
                    type = RecentItemType.CHECKLIST,
                    lastAccessed = System.currentTimeMillis(),
                    displayName = checklist.name,
                    target = checklist.id,
                )
            }
        Log.d("Recents_Debug", "Logging checklist access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun logObsidianLinkAccess(link: RelatedLink) {
        val existingItem = recentItemDao.getRecentItemById(link.target)
        val recentItem = if (existingItem != null) {
            existingItem.copy(lastAccessed = System.currentTimeMillis())
        } else {
            RecentItem(
                id = link.target,
                type = RecentItemType.OBSIDIAN_LINK,
                lastAccessed = System.currentTimeMillis(),
                displayName = link.displayName ?: link.target,
                target = link.target
            )
        }
        Log.d("Recents_Debug", "Logging obsidian link access: $recentItem")
        recentItemDao.logAccess(recentItem)
    }

    suspend fun updateRecentItem(item: RecentItem) {
        recentItemDao.logAccess(item)
    }
    
    suspend fun updateRecentItemDisplayName(itemId: String, displayName: String) {
        val recentItem = recentItemDao.getRecentItemById(itemId)
        if (recentItem != null) {
            recentItemDao.logAccess(recentItem.copy(displayName = displayName))
        }
    }
}
