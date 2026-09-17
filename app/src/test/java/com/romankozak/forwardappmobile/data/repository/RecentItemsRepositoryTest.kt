package com.romankozak.forwardappmobile.data.repository

import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentItemsRepositoryTest {
    @Test
    fun `project recent display name follows canonical presentation rename`() = runTest {
        val dao = mockk<RecentItemDao>(relaxed = true)
        val existing =
            RecentItem(
                id = "project-1",
                type = RecentItemType.PROJECT,
                lastAccessed = 100L,
                displayName = "Legacy raw title",
                target = "project-1",
            )
        val updatedItems = slot<List<RecentItem>>()

        coEvery { dao.getAllSync() } returns listOf(existing)

        val repository = RecentItemsRepository(dao)

        repository.syncProjectRecentItems(
            mapOf(
                "project-1" to "Canonical presentation title",
            ),
        )

        coVerify(exactly = 0) {
            dao.deleteByIds(any())
        }
        coVerify(exactly = 1) {
            dao.insertAllSync(capture(updatedItems))
        }

        val updated = updatedItems.captured.single()
        assertEquals(existing.id, updated.id)
        assertEquals(existing.type, updated.type)
        assertEquals(existing.lastAccessed, updated.lastAccessed)
        assertEquals(existing.target, updated.target)
        assertEquals(existing.isPinned, updated.isPinned)
        assertEquals("Canonical presentation title", updated.displayName)
    }

    @Test
    fun `empty presentation universe does not mutate project recents`() = runTest {
        val dao = mockk<RecentItemDao>(relaxed = true)
        val repository = RecentItemsRepository(dao)

        repository.syncProjectRecentItems(emptyMap())

        coVerify(exactly = 0) {
            dao.getAllSync()
        }
        coVerify(exactly = 0) {
            dao.deleteByIds(any())
        }
        coVerify(exactly = 0) {
            dao.insertAllSync(any())
        }
    }
}
