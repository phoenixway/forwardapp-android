package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextStructureItem
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceBootstrapper
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextStructureRepositoryRetiredContextRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `existing legacy structure remains readable after Context retirement`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("retired-readable")
            val structure =
                ContextConfiguration.default(source.id).copy(
                    enableInbox = false,
                    updatedAt = 10L,
                )

            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(structure)
            database.contextDao().insert(source.softDelete(20L))

            val result = repository(database).ensureStructure(source.id)

            assertEquals(structure, result)
            assertEquals(structure, database.contextStructureDao().getStructureByContext(source.id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `retired Context cannot recreate missing legacy structure`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("retired-missing")
            database.contextDao().insert(source)
            database.contextDao().insert(source.softDelete(20L))

            try {
                repository(database).ensureStructure(source.id)
                fail("Expected retired Context structure creation to fail")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains(source.id))
            }

            assertNull(database.contextStructureDao().getStructureByContext(source.id))
        } finally {
            database.close()
        }
    }

    @Test
    fun `retired Context rejects legacy structure and item mutations`() = runBlocking {
        val database = database()
        try {
            val source = contextEntity("retired-writes")
            val structure =
                ContextConfiguration.default(source.id).copy(
                    enableInbox = false,
                    enableBacklog = false,
                    updatedAt = 10L,
                )
            val existingItem =
                ContextStructureItem(
                    id = "existing-item",
                    contextStructureId = structure.id,
                    entityType = "NOTE",
                    roleCode = "existing",
                    containerType = null,
                    title = "Existing",
                    isEnabled = true,
                    updatedAt = 10L,
                )
            val newItem =
                ContextStructureItem(
                    id = "new-item",
                    contextStructureId = structure.id,
                    entityType = "NOTE",
                    roleCode = "new",
                    containerType = null,
                    title = "New",
                    updatedAt = 30L,
                )

            database.contextDao().insert(source)
            database.contextStructureDao().insertStructure(structure)
            database.contextStructureDao().insertItems(listOf(existingItem))
            database.contextDao().insert(source.softDelete(20L))

            val repository = repository(database)

            repository.updateStructure(
                structure.copy(
                    enableInbox = true,
                    updatedAt = 30L,
                ),
            )
            repository.upsertStructure(
                structure.copy(
                    enableBacklog = true,
                    updatedAt = 31L,
                ),
            )
            repository.addOrUpdateItem(structure.id, newItem)
            repository.setItemEnabled(existingItem, enabled = false)

            assertEquals(
                structure,
                database.contextStructureDao().getStructureByContext(source.id),
            )
            assertEquals(
                listOf(existingItem),
                database.contextStructureDao().getItems(structure.id),
            )
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        ContextStructureRepository(
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
            structurePresetDao = database.structurePresetDao(),
            structurePresetItemDao = database.structurePresetItemDao(),
            workspaceWriteThrough = ContextWorkspaceWriteThrough(bootstrapper(database)),
        )

    private fun bootstrapper(database: AppDatabase) =
        CanonicalWorkspaceBootstrapper(
            database = database,
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            contextDao = database.contextDao(),
            contextStructureDao = database.contextStructureDao(),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun contextEntity(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 2L,
            roleCode = "default",
        )
}
