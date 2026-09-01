package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBacklogEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.BacklogMigrationIssueCode
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class BacklogMigrationDryRunAdapterRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `dry run fully accounts legacy Backlog computes stable future capability and does not mutate Room`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner")
                seedDocument(database, id = "document", ownerContextId = "owner")
                seedPlacement(
                    database = database,
                    id = "placement",
                    ownerContextId = "owner",
                    entityId = "document",
                    order = 0L,
                )
                seedOrder(
                    database = database,
                    id = "placement-order",
                    ownerContextId = "owner",
                    itemId = "placement",
                    order = 0L,
                )

                val before = readSnapshot(database)
                val report = BacklogMigrationDryRunAdapter(database).dryRun()
                val after = readSnapshot(database)

                val expectedCapabilityId = stableBacklogCapabilityId("owner")

                assertTrue(report.canApply)
                assertTrue(report.isFullyAccounted)
                assertTrue(report.preflightIssues.isEmpty())
                assertEquals(1, report.plan.itemSourceCount)
                assertEquals(1, report.plan.orderSourceCount)
                assertEquals(1, report.plan.entries.size)
                assertEquals("placement", report.plan.entries.single().id)
                assertEquals("owner", report.plan.entries.single().workspaceId)
                assertEquals(
                    expectedCapabilityId,
                    report.plan.entries.single().capabilityInstanceId,
                )
                assertEquals(
                    expectedCapabilityId,
                    report.expectedCapabilityInstanceIdByWorkspaceId.getValue("owner"),
                )

                assertEquals(before, after)
                assertTrue(database.orientationDao().getAllWorkspaceCapabilities().isEmpty())
                assertTrue(database.workspaceBacklogEntryDao().getAll().isEmpty())
            } finally {
                database.close()
            }
        }

    @Test
    fun `legacy full backup fallback materializes frozen plan into canonical authority`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner")
                seedDocument(database, id = "document", ownerContextId = "owner")
                seedPlacement(database, "placement", "owner", "document", order = 4L)
                seedOrder(database, "placement-order", "owner", "placement", order = 4L)

                val report = BacklogMigrationDryRunAdapter(database).materializeLegacyFullBackup()

                assertTrue(report.isFullyAccounted)
                val capability =
                    database.orientationDao().getAllWorkspaceCapabilities().single {
                        it.capabilityType == "BACKLOG"
                    }
                assertEquals("DISABLED", capability.state)
                val canonical = database.workspaceBacklogEntryDao().getAll().single()
                assertEquals("placement", canonical.id)
                assertEquals(capability.id, canonical.capabilityInstanceId)
                assertEquals(WorkspaceBacklogTargetKind.NOTE_DOCUMENT.name, canonical.targetKind)
                assertEquals("document", canonical.targetId)
                assertEquals(0L, canonical.entryOrder)
            } finally {
                database.close()
            }
        }

    @Test
    fun `existing logical BACKLOG capability id is preserved by dry run`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner")
                seedCapability(
                    database = database,
                    id = "existing-backlog-id",
                    workspaceId = "owner",
                    capabilityType = "BACKLOG",
                )
                seedDocument(database, id = "document", ownerContextId = "owner")
                seedPlacement(database, "placement", "owner", "document")

                val before = readSnapshot(database)
                val report = BacklogMigrationDryRunAdapter(database).dryRun()
                val after = readSnapshot(database)

                assertTrue(report.canApply)
                assertTrue(report.isFullyAccounted)
                assertEquals(
                    "existing-backlog-id",
                    report.expectedCapabilityInstanceIdByWorkspaceId.getValue("owner"),
                )
                assertEquals(
                    "existing-backlog-id",
                    report.plan.entries.single().capabilityInstanceId,
                )
                assertEquals(before, after)
            } finally {
                database.close()
            }
        }

    @Test
    fun `deleted owner Workspace fails closed`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner", deleted = true)
                seedDocument(database, id = "document", ownerContextId = "owner")
                seedPlacement(database, "placement", "owner", "document")

                val report = BacklogMigrationDryRunAdapter(database).dryRun()

                assertFalse(report.canApply)
                assertFalse(report.isFullyAccounted)
                assertTrue(report.plan.entries.isEmpty())
                assertTrue(
                    report.plan.issues.any {
                        it.code == BacklogMigrationIssueCode.DELETED_OWNER_WORKSPACE
                    },
                )
                assertFalse(
                    report.expectedCapabilityInstanceIdByWorkspaceId.containsKey("owner"),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `deleted target remains fully accounted for tombstoned placement`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner")
                seedDocument(
                    database = database,
                    id = "document",
                    ownerContextId = "owner",
                    deleted = true,
                )
                seedPlacement(
                    database = database,
                    id = "placement",
                    ownerContextId = "owner",
                    entityId = "document",
                    deleted = true,
                )

                val report = BacklogMigrationDryRunAdapter(database).dryRun()

                assertTrue(report.canApply)
                assertTrue(report.isFullyAccounted)
                assertEquals(1, report.plan.entries.size)
                assertTrue(report.plan.entries.single().isDeleted)
                assertEquals(
                    WorkspaceBacklogTargetKind.NOTE_DOCUMENT,
                    report.plan.entries.single().target.kind,
                )
                assertEquals("document", report.plan.entries.single().target.id)
            } finally {
                database.close()
            }
        }

    @Test
    fun `pre-cutover canonical destination contamination fails closed`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner")
                seedCapability(
                    database = database,
                    id = "existing-backlog-id",
                    workspaceId = "owner",
                    capabilityType = "BACKLOG",
                )
                seedDocument(database, id = "document", ownerContextId = "owner")
                seedPlacement(database, "placement", "owner", "document")

                database.workspaceBacklogEntryDao().upsert(
                    listOf(
                        WorkspaceBacklogEntryEntity(
                            id = "already-canonical",
                            workspaceId = "owner",
                            capabilityInstanceId = "existing-backlog-id",
                            targetKind = WorkspaceBacklogTargetKind.NOTE_DOCUMENT.name,
                            targetId = "document",
                            entryOrder = 0L,
                            createdAt = 5L,
                            updatedAt = 5L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        ),
                    ),
                )

                val before = readSnapshot(database)
                val report = BacklogMigrationDryRunAdapter(database).dryRun()
                val after = readSnapshot(database)

                assertFalse(report.canApply)
                assertFalse(report.isFullyAccounted)
                assertTrue(
                    report.preflightIssues.any {
                        it.code ==
                            BacklogMigrationDryRunIssueCode
                                .CONTEXT_BACKED_CANONICAL_DESTINATION_PRESENT
                    },
                )
                assertEquals(before, after)
            } finally {
                database.close()
            }
        }

    @Test
    fun `deterministic future capability id collision fails closed`() =
        runBlocking {
            val database = database()
            try {
                seedContextBackedWorkspace(database, "owner")
                seedCanonicalWorkspace(database, "other")

                val expectedId = stableBacklogCapabilityId("owner")
                seedCapability(
                    database = database,
                    id = expectedId,
                    workspaceId = "other",
                    capabilityType = "DASHBOARD",
                )

                seedDocument(database, id = "document", ownerContextId = "owner")
                seedPlacement(database, "placement", "owner", "document")

                val before = readSnapshot(database)
                val report = BacklogMigrationDryRunAdapter(database).dryRun()
                val after = readSnapshot(database)

                assertFalse(report.canApply)
                assertFalse(report.isFullyAccounted)
                assertTrue(
                    report.preflightIssues.any {
                        it.code == BacklogMigrationDryRunIssueCode.CAPABILITY_ID_COLLISION
                    },
                )
                assertFalse(
                    report.expectedCapabilityInstanceIdByWorkspaceId.containsKey("owner"),
                )
                assertEquals(before, after)
            } finally {
                database.close()
            }
        }

    private suspend fun seedContextBackedWorkspace(
        database: AppDatabase,
        id: String,
        deleted: Boolean = false,
    ) {
        database.contextDao().insert(
            ContextEntity(
                id = id,
                name = id,
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 1L,
                isDeleted = false,
                version = 1L,
            ),
        )
        database.workspaceDao().upsert(
            listOf(
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
                    isDeleted = deleted,
                    version = 1L,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                    sourceContextId = id,
                ),
            ),
        )
    }

    private suspend fun seedCanonicalWorkspace(
        database: AppDatabase,
        id: String,
    ) {
        database.workspaceDao().upsert(
            listOf(
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
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private suspend fun seedCapability(
        database: AppDatabase,
        id: String,
        workspaceId: String,
        capabilityType: String,
    ) {
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                WorkspaceCapabilityInstanceEntity(
                    id = id,
                    workspaceId = workspaceId,
                    capabilityType = capabilityType,
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = "ACTIVE",
                    configurationVersion = 1,
                    configuration = "{}",
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun seedDocument(
        database: AppDatabase,
        id: String,
        ownerContextId: String,
        deleted: Boolean = false,
    ) {
        database.noteDocumentDao().insertAllDocuments(
            listOf(
                NoteDocumentEntity(
                    id = id,
                    contextId = ownerContextId,
                    name = id,
                    createdAt = 2L,
                    updatedAt = 2L,
                    content = "",
                    lastCursorPosition = 0,
                    syncedAt = null,
                    isDeleted = deleted,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun seedPlacement(
        database: AppDatabase,
        id: String,
        ownerContextId: String,
        entityId: String,
        order: Long = 0L,
        deleted: Boolean = false,
    ) {
        database.listItemDao().insertItem(
            BacklogItem(
                id = id,
                contextId = ownerContextId,
                itemType = "NOTE_DOCUMENT",
                entityId = entityId,
                associationOwnerContextId = null,
                associationTag = null,
                order = order,
                updatedAt = 3L,
                syncedAt = null,
                isDeleted = deleted,
                version = 1L,
            ),
        )
    }

    private suspend fun seedOrder(
        database: AppDatabase,
        id: String,
        ownerContextId: String,
        itemId: String,
        order: Long,
    ) {
        database.backlogOrderDao().insertOrders(
            listOf(
                BacklogOrder(
                    id = id,
                    listId = ownerContextId,
                    itemId = itemId,
                    order = order,
                    orderVersion = 1L,
                    updatedAt = 4L,
                    syncedAt = null,
                    isDeleted = false,
                ),
            ),
        )
    }

    private fun stableBacklogCapabilityId(workspaceId: String): String =
        LegacySubjectUuid
            .uuidV5(
                UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
                "WORKSPACE:CAPABILITY:$workspaceId:BACKLOG:default",
            ).toString()

    private suspend fun readSnapshot(database: AppDatabase): ReadSnapshot =
        ReadSnapshot(
            legacyItems = database.listItemDao().getAllRaw().stableStrings(),
            legacyOrders = database.backlogOrderDao().getAllRaw().stableStrings(),
            workspaces = database.workspaceDao().getAll().stableStrings(),
            capabilities =
                database.orientationDao().getAllWorkspaceCapabilities().stableStrings(),
            mappings = database.orientationDao().getAllLegacyMappings().stableStrings(),
            managedSubjects =
                database.orientationDao().getAllManagedSubjects().stableStrings(),
            orientations = database.orientationDao().getAllOrientations().stableStrings(),
            linkItems = database.linkItemDao().getAllRaw().stableStrings(),
            legacyNotes = database.legacyNoteDao().getAllRaw().stableStrings(),
            documents = database.noteDocumentDao().getAllDocumentsRaw().stableStrings(),
            checklists = database.checklistDao().getAllChecklistsRaw().stableStrings(),
            musicNotes = database.musicNoteDao().getAll().stableStrings(),
            canonicalEntries =
                database.workspaceBacklogEntryDao().getAll().stableStrings(),
        )

    private fun List<*>.stableStrings(): List<String> =
        map { it.toString() }.sorted()

    private data class ReadSnapshot(
        val legacyItems: List<String>,
        val legacyOrders: List<String>,
        val workspaces: List<String>,
        val capabilities: List<String>,
        val mappings: List<String>,
        val managedSubjects: List<String>,
        val orientations: List<String>,
        val linkItems: List<String>,
        val legacyNotes: List<String>,
        val documents: List<String>,
        val checklists: List<String>,
        val musicNotes: List<String>,
        val canonicalEntries: List<String>,
    )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
