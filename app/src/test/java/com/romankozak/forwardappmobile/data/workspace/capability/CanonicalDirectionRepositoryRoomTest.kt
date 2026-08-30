package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DirectionCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalDirectionRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `semantic create persists full canonical Direction aggregate`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            val repository = repository(database)

            val entryId = repository.createSemanticDirection("owner", "  North star  ", now = 10L)

            val entry = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals("owner", entry.workspaceId)
            assertEquals("direction-owner", entry.capabilityInstanceId)
            assertEquals(WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name, entry.provenance)
            assertEquals(1L, entry.version)
            assertNull(entry.targetWorkspaceId)
            assertNull(entry.labelOverride)

            val orientationId = entry.orientationId!!
            val subject = database.orientationDao().getManagedSubject(orientationId)!!
            assertEquals("North star", subject.title)
            assertEquals(1L, subject.version)

            val orientation =
                database.orientationDao().getAllOrientations().single { it.subjectId == orientationId }
            assertEquals(OrientationKind.DIRECTION.name, orientation.kind)

            val revision =
                database.orientationDao().getAllAssessmentRevisions()
                    .single { it.orientationId == orientationId }
            assertEquals(AssessmentRevisionSource.USER.name, revision.source)
            assertEquals(orientationId, revision.orientationId)

            assertTrue(
                database.orientationDao().getAllAssessments()
                    .any { it.orientationId == orientationId },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace link creates navigation placement without Orientation`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(listOf(workspace("target")))
            val repository = repository(database)

            val entryId =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target",
                    label = "  Child workspace  ",
                    now = 20L,
                )

            val entry = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals("target", entry.targetWorkspaceId)
            assertEquals("Child workspace", entry.labelOverride)
            assertNull(entry.orientationId)
            assertEquals(WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name, entry.provenance)

            assertTrue(database.orientationDao().getAllManagedSubjects().isEmpty())
            assertTrue(database.orientationDao().getAllOrientations().isEmpty())
            assertTrue(database.orientationDao().getAllAssessmentRevisions().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `semantic converts to Workspace link while preserving placement and old Orientation`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(listOf(workspace("target")))
            val repository = repository(database)

            val entryId =
                repository.createSemanticDirection(
                    workspaceId = "owner",
                    title = "North star",
                    now = 10L,
                )
            val beforeEntry = database.workspaceDirectionEntryDao().getById(entryId)!!
            val oldOrientationId = beforeEntry.orientationId!!
            val oldSubject = database.orientationDao().getManagedSubject(oldOrientationId)!!

            repository.convertSemanticToWorkspaceLink(
                entryId = entryId,
                targetWorkspaceId = "target",
                now = 20L,
            )

            val afterEntry = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals(entryId, afterEntry.id)
            assertNull(afterEntry.orientationId)
            assertEquals("target", afterEntry.targetWorkspaceId)
            assertEquals("North star", afterEntry.labelOverride)
            assertEquals(beforeEntry.entryOrder, afterEntry.entryOrder)
            assertEquals(beforeEntry.version + 1L, afterEntry.version)
            assertEquals(20L, afterEntry.updatedAt)
            assertNull(afterEntry.syncedAt)

            val preservedSubject =
                database.orientationDao().getManagedSubject(oldOrientationId)!!
            assertEquals(oldSubject.id, preservedSubject.id)
            assertEquals("North star", preservedSubject.title)
            assertFalse(preservedSubject.isDeleted)
            assertTrue(
                database.orientationDao().getAllOrientations().any {
                    it.subjectId == oldOrientationId &&
                        it.kind == OrientationKind.DIRECTION.name
                },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace link converts to fresh semantic Direction while preserving placement`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(listOf(workspace("target")))
            val repository = repository(database)

            val entryId =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target",
                    label = "Child workspace",
                    now = 10L,
                )
            val beforeEntry = database.workspaceDirectionEntryDao().getById(entryId)!!

            repository.convertWorkspaceLinkToSemantic(
                entryId = entryId,
                now = 20L,
            )

            val afterEntry = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals(entryId, afterEntry.id)
            assertNull(afterEntry.targetWorkspaceId)
            assertNull(afterEntry.labelOverride)
            assertEquals(beforeEntry.entryOrder, afterEntry.entryOrder)
            assertEquals(beforeEntry.version + 1L, afterEntry.version)
            assertEquals(20L, afterEntry.updatedAt)
            assertNull(afterEntry.syncedAt)

            val newOrientationId = afterEntry.orientationId!!
            val subject =
                database.orientationDao().getManagedSubject(newOrientationId)!!
            assertEquals("Child workspace", subject.title)
            assertEquals(1L, subject.version)
            assertFalse(subject.isDeleted)

            val orientation =
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == newOrientationId }
            assertEquals(OrientationKind.DIRECTION.name, orientation.kind)

            val revision =
                database.orientationDao().getAllAssessmentRevisions()
                    .single { it.orientationId == newOrientationId }
            assertEquals(AssessmentRevisionSource.USER.name, revision.source)
            assertEquals(
                "Converted from DIRECTION Workspace link",
                revision.reason,
            )

            assertTrue(
                database.orientationDao().getAllAssessments()
                    .any { it.orientationId == newOrientationId },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `semantic rename changes subject but not placement`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            val repository = repository(database)
            val entryId = repository.createSemanticDirection("owner", "Old", now = 10L)

            val beforeEntry = database.workspaceDirectionEntryDao().getById(entryId)!!
            val orientationId = beforeEntry.orientationId!!
            val beforeSubject = database.orientationDao().getManagedSubject(orientationId)!!

            repository.rename(entryId, "  New title  ", now = 20L)

            val afterEntry = database.workspaceDirectionEntryDao().getById(entryId)!!
            val afterSubject = database.orientationDao().getManagedSubject(orientationId)!!

            assertEquals("New title", afterSubject.title)
            assertEquals(beforeSubject.version + 1L, afterSubject.version)
            assertEquals(20L, afterSubject.updatedAt)
            assertNull(afterSubject.syncedAt)

            assertEquals(beforeEntry.version, afterEntry.version)
            assertEquals(beforeEntry.updatedAt, afterEntry.updatedAt)
            assertEquals(beforeEntry.entryOrder, afterEntry.entryOrder)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace link rename changes placement only`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(listOf(workspace("target")))
            val repository = repository(database)
            val entryId =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target",
                    label = "Old label",
                    now = 10L,
                )

            repository.rename(entryId, "  New label  ", now = 20L)

            val entry = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals("New label", entry.labelOverride)
            assertEquals(2L, entry.version)
            assertEquals(20L, entry.updatedAt)
            assertNull(entry.syncedAt)
            assertTrue(database.orientationDao().getAllManagedSubjects().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `reorder and tombstone mutate placement without deleting semantic Orientation`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            val repository = repository(database)

            val first = repository.createSemanticDirection("owner", "First", now = 10L)
            val second = repository.createSemanticDirection("owner", "Second", now = 11L)
            val firstOrientationId =
                database.workspaceDirectionEntryDao().getById(first)!!.orientationId!!

            repository.reorder("owner", listOf(second, first), now = 20L)

            val reordered = database.workspaceDirectionEntryDao().getLiveForWorkspace("owner")
            assertEquals(listOf(second, first), reordered.map { it.id })
            assertEquals(listOf(1L, 2L), reordered.map { it.entryOrder })
            assertEquals(listOf(2L, 2L), reordered.map { it.version })

            repository.tombstone(first, now = 30L)

            val deletedEntry = database.workspaceDirectionEntryDao().getById(first)!!
            assertTrue(deletedEntry.isDeleted)
            assertEquals(3L, deletedEntry.version)

            val subject = database.orientationDao().getManagedSubject(firstOrientationId)!!
            assertFalse(subject.isDeleted)
            assertTrue(
                database.orientationDao().getAllOrientations()
                    .any { it.subjectId == firstOrientationId },
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `front Workspace link preserves legacy insertion semantics`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(
                listOf(
                    workspace("target-a"),
                    workspace("target-b"),
                ),
            )
            val repository = repository(database)

            val semantic =
                repository.createSemanticDirection(
                    workspaceId = "owner",
                    title = "Semantic",
                    now = 10L,
                )
            val existingLink =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target-a",
                    label = "Existing",
                    now = 11L,
                )

            val front =
                repository.createWorkspaceLinkAtFront(
                    workspaceId = "owner",
                    targetWorkspaceId = "target-b",
                    label = "Front",
                    now = 20L,
                )

            val entries = database.workspaceDirectionEntryDao().getLiveForWorkspace("owner")
            assertEquals(listOf(front, semantic, existingLink), entries.map { it.id })
            assertEquals(listOf(0L, 2L, 3L), entries.map { it.entryOrder })

            val frontEntry = database.workspaceDirectionEntryDao().getById(front)!!
            assertEquals(1L, frontEntry.version)
            assertEquals(20L, frontEntry.updatedAt)

            val shiftedSemantic = database.workspaceDirectionEntryDao().getById(semantic)!!
            val shiftedLink = database.workspaceDirectionEntryDao().getById(existingLink)!!
            assertEquals(2L, shiftedSemantic.version)
            assertEquals(2L, shiftedLink.version)
            assertEquals(20L, shiftedSemantic.updatedAt)
            assertEquals(20L, shiftedLink.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Workspace link retarget preserves placement label and identity`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(
                listOf(
                    workspace("target-a"),
                    workspace("target-b"),
                ),
            )
            val repository = repository(database)

            val entryId =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target-a",
                    label = "Child",
                    now = 10L,
                )
            val before = database.workspaceDirectionEntryDao().getById(entryId)!!

            repository.retargetWorkspaceLink(
                entryId = entryId,
                targetWorkspaceId = "target-b",
                now = 20L,
            )

            val after = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals(before.id, after.id)
            assertEquals(before.entryOrder, after.entryOrder)
            assertEquals(before.labelOverride, after.labelOverride)
            assertEquals("target-b", after.targetWorkspaceId)
            assertNull(after.orientationId)
            assertEquals(before.version + 1L, after.version)
            assertEquals(20L, after.updatedAt)
            assertNull(after.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `target Workspace deletion tombstones only matching navigation entries`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            database.workspaceDao().upsert(
                listOf(
                    workspace("target-a"),
                    workspace("target-b"),
                ),
            )
            val repository = repository(database)

            val semantic =
                repository.createSemanticDirection(
                    workspaceId = "owner",
                    title = "Semantic",
                    now = 10L,
                )
            val targetA =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target-a",
                    label = "A",
                    now = 11L,
                )
            val targetB =
                repository.createWorkspaceLink(
                    workspaceId = "owner",
                    targetWorkspaceId = "target-b",
                    label = "B",
                    now = 12L,
                )

            val changed =
                repository.tombstoneWorkspaceLinksTargeting(
                    targetWorkspaceIds = listOf("target-a"),
                    now = 30L,
                )

            assertEquals(1, changed)

            val semanticEntry = database.workspaceDirectionEntryDao().getById(semantic)!!
            val aEntry = database.workspaceDirectionEntryDao().getById(targetA)!!
            val bEntry = database.workspaceDirectionEntryDao().getById(targetB)!!

            assertFalse(semanticEntry.isDeleted)
            assertTrue(aEntry.isDeleted)
            assertFalse(bEntry.isDeleted)
            assertEquals(2L, aEntry.version)
            assertEquals(30L, aEntry.updatedAt)
            assertNull(aEntry.syncedAt)
        } finally {
            database.close()
        }
    }


    @Test
    fun `legacy provenance remains mutable after authority cutover`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, "owner")
            val repository = repository(database)
            val entryId =
                repository.createSemanticDirection(
                    workspaceId = "owner",
                    title = "Migrated",
                    now = 10L,
                )

            val canonical = database.workspaceDirectionEntryDao().getById(entryId)!!
            database.workspaceDirectionEntryDao().upsert(
                listOf(
                    canonical.copy(
                        provenance =
                            WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
                    ),
                ),
            )

            repository.rename(entryId, "Renamed after cutover", now = 20L)

            val afterRename = database.workspaceDirectionEntryDao().getById(entryId)!!
            val subject =
                database.orientationDao().getManagedSubject(afterRename.orientationId!!)!!

            assertEquals(
                WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
                afterRename.provenance,
            )
            assertEquals("Renamed after cutover", subject.title)
            assertEquals(2L, subject.version)

            repository.tombstone(entryId, now = 30L)

            val tombstoned = database.workspaceDirectionEntryDao().getById(entryId)!!
            assertEquals(
                WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name,
                tombstoned.provenance,
            )
            assertTrue(tombstoned.isDeleted)
            assertEquals(2L, tombstoned.version)
            assertEquals(30L, tombstoned.updatedAt)
            assertNull(tombstoned.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `context backed Workspace is authorized for Direction after cutover`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(
                listOf(
                    workspace("legacy-owner").copy(
                        provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                        sourceContextId = "legacy-owner",
                    ),
                    workspace("target"),
                ),
            )
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(directionCapability("legacy-owner")),
            )

            val repository = repository(database)
            val entryId =
                repository.createWorkspaceLink(
                    workspaceId = "legacy-owner",
                    targetWorkspaceId = "target",
                    label = "Target",
                    now = 20L,
                )

            assertTrue(entryId.isNotBlank())
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalDirectionRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            entryDao = database.workspaceDirectionEntryDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            orientationRepository =
                CanonicalOrientationRepository(
                    database = database,
                    dao = database.orientationDao(),
                ),
        )

    private suspend fun seedWorkspace(database: AppDatabase, id: String) {
        database.workspaceDao().upsert(listOf(workspace(id)))
        database.orientationDao().upsertWorkspaceCapabilities(listOf(directionCapability(id)))
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

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
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun directionCapability(workspaceId: String) =
        WorkspaceCapabilityInstanceEntity(
            id = "direction-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = WorkspaceCapabilityType.DIRECTION.name,
            instanceKey = "default",
            capabilityOrder = 0L,
            state = WorkspaceCapabilityState.ACTIVE.name,
            configurationVersion = 1,
            configuration =
                DirectionCapabilityConfigurationCodec.encode(
                    DirectionCapabilityConfigurationV1(
                        autoLinkChildWorkspaces = true,
                    ),
                ),
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
