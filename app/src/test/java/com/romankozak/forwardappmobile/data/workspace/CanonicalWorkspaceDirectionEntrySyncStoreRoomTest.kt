package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceDirectionEntrySyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceDirectionEntrySyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `legacy projection ingress is ignored while canonical-only entry persists`() = runBlocking {
        val database = database()
        try {
            seedNavigationDependencies(database)
            val store = store(database)

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "legacy-projection",
                        provenance = WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
                    ),
                ),
            )

            assertNull(database.workspaceDirectionEntryDao().getById("legacy-projection"))

            val canonical =
                entry(
                    id = "canonical",
                    provenance = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                    version = 3L,
                    updatedAt = 30L,
                )

            store.mergeIncoming(listOf(canonical))

            val persisted =
                requireNotNull(database.workspaceDirectionEntryDao().getById("canonical"))

            assertEquals(
                WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
                persisted.provenance,
            )
            assertEquals("owner", persisted.workspaceId)
            assertEquals("capability-owner", persisted.capabilityInstanceId)
            assertEquals("target", persisted.targetWorkspaceId)
            assertEquals(3L, persisted.version)
            assertEquals(30L, persisted.updatedAt)
            assertNull(persisted.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical-only ingress rejects id owned by legacy Direction source`() = runBlocking {
        val database = database()
        try {
            seedNavigationDependencies(database)

            database.contextDao().insert(
                ContextEntity(
                    id = "owner",
                    name = "Owner",
                    description = null,
                    parentId = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    roleCode = "management",
                ),
            )

            database.directionDao().insert(
                DirectionItemEntity(
                    id = "shared-id",
                    contextId = "owner",
                    text = "Legacy source",
                    linkedContextId = "target",
                    itemOrder = 1,
                    updatedAt = 10L,
                    version = 1L,
                ),
            )

            val failure =
                runCatching {
                    store(database).mergeIncoming(
                        listOf(entry(id = "shared-id")),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertNull(database.workspaceDirectionEntryDao().getById("shared-id"))
        } finally {
            database.close()
        }
    }

    @Test
    fun `equal freshness tombstone wins and equal live cannot resurrect it`() = runBlocking {
        val database = database()
        try {
            seedNavigationDependencies(database)
            val store = store(database)

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "entry",
                        version = 5L,
                        updatedAt = 50L,
                        deleted = false,
                    ),
                ),
            )

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "entry",
                        version = 5L,
                        updatedAt = 50L,
                        deleted = true,
                    ),
                ),
            )

            var persisted =
                requireNotNull(database.workspaceDirectionEntryDao().getById("entry"))
            assertTrue(persisted.isDeleted)

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "entry",
                        version = 5L,
                        updatedAt = 50L,
                        deleted = false,
                    ),
                ),
            )

            persisted =
                requireNotNull(database.workspaceDirectionEntryDao().getById("entry"))

            assertTrue(persisted.isDeleted)
            assertEquals(5L, persisted.version)
            assertEquals(50L, persisted.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `owner capability and target identities are immutable`() = runBlocking {
        val database = database()
        try {
            seedNavigationDependencies(database)

            database.workspaceDao().upsert(
                listOf(
                    workspace("owner-2"),
                    workspace("target-2"),
                ),
            )

            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    capability(
                        id = "capability-owner-alt",
                        workspaceId = "owner",
                        instanceKey = "alternate",
                    ),
                    capability(
                        id = "capability-owner-2",
                        workspaceId = "owner-2",
                    ),
                ),
            )

            val store = store(database)
            val original = entry(id = "entry")

            store.mergeIncoming(listOf(original))

            val ownerFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            original.copy(
                                workspaceId = "owner-2",
                                capabilityInstanceId = "capability-owner-2",
                                version = 2L,
                                updatedAt = 20L,
                            ),
                        ),
                    )
                }.exceptionOrNull()
            assertTrue(ownerFailure is IllegalArgumentException)

            val capabilityFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            original.copy(
                                capabilityInstanceId = "capability-owner-alt",
                                version = 2L,
                                updatedAt = 20L,
                            ),
                        ),
                    )
                }.exceptionOrNull()
            assertTrue(capabilityFailure is IllegalArgumentException)

            val targetFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            original.copy(
                                targetWorkspaceId = "target-2",
                                version = 2L,
                                updatedAt = 20L,
                            ),
                        ),
                    )
                }.exceptionOrNull()
            assertTrue(targetFailure is IllegalArgumentException)

            val preserved =
                requireNotNull(database.workspaceDirectionEntryDao().getById("entry"))

            assertEquals("owner", preserved.workspaceId)
            assertEquals("capability-owner", preserved.capabilityInstanceId)
            assertEquals("target", preserved.targetWorkspaceId)
            assertEquals(1L, preserved.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `reorder and label are mutable placement fields`() = runBlocking {
        val database = database()
        try {
            seedNavigationDependencies(database)
            val store = store(database)

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "entry",
                        order = 1L,
                        label = "Before",
                        version = 1L,
                        updatedAt = 10L,
                    ),
                ),
            )

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "entry",
                        order = 9L,
                        label = "After",
                        version = 2L,
                        updatedAt = 20L,
                    ),
                ),
            )

            val persisted =
                requireNotNull(database.workspaceDirectionEntryDao().getById("entry"))

            assertEquals(9L, persisted.entryOrder)
            assertEquals("After", persisted.labelOverride)
            assertEquals(2L, persisted.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `sync acknowledgement requires exact version`() = runBlocking {
        val database = database()
        try {
            seedNavigationDependencies(database)
            val store = store(database)

            store.mergeIncoming(
                listOf(
                    entry(
                        id = "entry",
                        version = 7L,
                        updatedAt = 70L,
                    ),
                ),
            )

            store.markSynced(
                listOf(
                    WorkspaceDirectionEntrySyncVersion(
                        id = "entry",
                        version = 6L,
                    ),
                ),
            )

            assertNull(
                database.workspaceDirectionEntryDao().getById("entry")?.syncedAt,
            )

            store.markSynced(
                listOf(
                    WorkspaceDirectionEntrySyncVersion(
                        id = "entry",
                        version = 7L,
                    ),
                ),
            )

            val persisted =
                requireNotNull(database.workspaceDirectionEntryDao().getById("entry"))

            assertNotNull(persisted.syncedAt)
            assertFalse(persisted.isDeleted)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun store(database: AppDatabase) =
        CanonicalWorkspaceDirectionEntrySyncStore(
            database = database,
            entryDao = database.workspaceDirectionEntryDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            directionDao = database.directionDao(),
            materializer = mockk<WorkspaceDirectionEntryShadowMaterializer>(relaxed = true),
        )

    private suspend fun seedNavigationDependencies(database: AppDatabase) {
        database.workspaceDao().upsert(
            listOf(
                workspace("owner"),
                workspace("target"),
            ),
        )

        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                capability(
                    id = "capability-owner",
                    workspaceId = "owner",
                ),
            ),
        )
    }

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
        )

    private fun capability(
        id: String,
        workspaceId: String,
        instanceKey: String = "default",
    ) = WorkspaceCapabilityInstanceEntity(
        id = id,
        workspaceId = workspaceId,
        capabilityType = "DIRECTION",
        instanceKey = instanceKey,
        capabilityOrder = 0L,
        state = "ACTIVE",
        configurationVersion = 1,
        configuration = """{"autoLinkChildWorkspaces":true}""",
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun entry(
        id: String,
        workspaceId: String = "owner",
        capabilityInstanceId: String = "capability-owner",
        targetWorkspaceId: String = "target",
        order: Long = 1L,
        label: String? = "Target",
        provenance: String = WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name,
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) = WorkspaceDirectionEntrySnapshot(
        id = id,
        workspaceId = workspaceId,
        capabilityInstanceId = capabilityInstanceId,
        orientationId = null,
        targetWorkspaceId = targetWorkspaceId,
        labelOverride = label,
        entryOrder = order,
        provenance = provenance,
        createdAt = 1L,
        updatedAt = updatedAt,
        version = version,
        isDeleted = deleted,
    )
}
