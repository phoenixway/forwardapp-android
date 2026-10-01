package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context as AndroidContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.database.HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceMaterializer
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagSeed
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalFreshHierarchyEstablishmentSourceRoomTest {
    private val context: AndroidContext = ApplicationProvider.getApplicationContext()

    @Test
    fun `native fresh evidence is deterministic complete and source neutral`() =
        runBlocking {
            val db = database()
            try {
                seedFreshPrerequisites(db)

                val source = CanonicalFreshHierarchyEstablishmentSource(db)
                val builder = CanonicalV1HierarchySnapshotBuilder()

                val firstInput = source.read()
                val secondInput = source.read()
                val first = builder.build(firstInput)
                val second = builder.build(secondInput)

                assertEquals(firstInput, secondInput)
                assertEquals(first, second)
                assertEquals(SystemOperationalDefinitions.all.size, first.occurrences.size)
                assertTrue(first.diagnostics.isEmpty())
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    @Test
    fun `native source does not mutate persisted schema180 state`() =
        runBlocking {
            val db = database()
            try {
                seedFreshPrerequisites(db)
                val source = CanonicalFreshHierarchyEstablishmentSource(db)

                val workspacesBefore = db.workspaceDao().getAll().sortedBy { it.id }
                val markerBefore =
                    db.hierarchyAuthorityActivationStateDao().get(HierarchyId.GENERAL.value)
                val originBefore =
                    db.hierarchyEstablishmentOriginDao().get(HierarchyId.GENERAL.value)

                val first = source.read()
                val second = source.read()

                assertEquals(first, second)
                assertEquals(workspacesBefore, db.workspaceDao().getAll().sortedBy { it.id })
                assertEquals(
                    markerBefore,
                    db.hierarchyAuthorityActivationStateDao().get(HierarchyId.GENERAL.value),
                )
                assertEquals(
                    originBefore,
                    db.hierarchyEstablishmentOriginDao().get(HierarchyId.GENERAL.value),
                )
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    @Test
    fun `missing canonical System owner fails closed`() =
        runBlocking {
            val db = database()
            try {
                val missingId = SystemContexts.INBOX.raw
                db.workspaceDao().upsert(
                    SystemOperationalDefinitions.all
                        .filterNot { it.id == missingId }
                        .mapIndexed { index, definition -> definition.workspace(index.toLong()) },
                )

                val failure =
                    runCatching {
                        CanonicalFreshHierarchyEstablishmentSource(db).read()
                    }.exceptionOrNull()

                assertTrue(failure is CanonicalFreshHierarchyEstablishmentException)
                assertTrue(failure?.message?.contains(missingId) == true)
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
                assertNull(db.hierarchyAuthorityActivationStateDao().get(HierarchyId.GENERAL.value))
            } finally {
                db.close()
            }
        }

    @Test
    fun `unexpected ordinary Workspace before fresh establishment fails closed`() =
        runBlocking {
            val db = database()
            try {
                seedFreshPrerequisites(db)
                db.workspaceDao().upsert(
                    listOf(
                        WorkspaceEntity(
                            id = "ordinary-before-establishment",
                            nameOverride = "ordinary",
                            descriptionOverride = null,
                            roleCode = null,
                            createdAt = 20L,
                            updatedAt = 20L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            provenance = WorkspaceProvenance.STANDALONE.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val failure =
                    runCatching {
                        CanonicalFreshHierarchyEstablishmentSource(db).read()
                    }.exceptionOrNull()

                assertTrue(failure is CanonicalFreshHierarchyEstablishmentException)
                assertTrue(failure?.message?.contains("ordinary-before-establishment") == true)
                assertTrue(db.hierarchyPlacementDao().getAll().isEmpty())
            } finally {
                db.close()
            }
        }

    @Test
    fun `native evidence materializes complete deterministic pristine H1`() =
        runBlocking {
            val db = database()
            try {
                seedFreshPrerequisites(db)
                val builder = CanonicalV1HierarchySnapshotBuilder()
                val snapshot =
                    builder.build(CanonicalFreshHierarchyEstablishmentSource(db).read())

                val report = CanonicalV1HierarchyMaterializer(db).materialize(snapshot, now = 100L)

                assertEquals(CanonicalV1HierarchyMaterializationOutcome.CREATED, report.outcome)
                assertEquals(SystemOperationalDefinitions.all.size, report.occurrenceCount)
                assertEquals(report.occurrenceCount, report.primaryCount)
                assertEquals(0, report.linkCount)
                assertTrue(db.hierarchyPlacementGroupScopeDao().getAll().isEmpty())
                assertTrue(db.hierarchyPlacementLinkedAppearanceDao().getAll().isEmpty())

                val placements = db.hierarchyPlacementDao().getAll().associateBy { it.targetId }
                val inbox = requireNotNull(placements[SystemContexts.INBOX.raw])
                val today = requireNotNull(placements[SystemContexts.TODAY.raw])
                assertEquals(today.id, inbox.parentPlacementId)
                assertNull(db.hierarchyAuthorityActivationStateDao().get(HierarchyId.GENERAL.value))
            } finally {
                db.close()
            }
        }

    private suspend fun seedFreshPrerequisites(db: AppDatabase) {
        SystemWorkspaceMaterializer(
            database = db,
            contextDao = db.contextDao(),
            workspaceDao = db.workspaceDao(),
        ).also { materializer ->
        }.materializeAll(
            now = 10L,
            seedMissingFactoryCapabilities = false,
        )
        SystemWorkspaceTagSeed(
            database = db,
            contextDao = db.contextDao(),
        ).seedMissingCanonicalCollections(now = 11L)
    }

    private fun com.romankozak.forwardappmobile.core.context.SystemOperationalDefinition.workspace(
        order: Long,
    ): WorkspaceEntity =
        WorkspaceEntity(
            id = id,
            nameOverride = defaultName,
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(HIERARCHY_ESTABLISHMENT_FRESH_DATABASE_CALLBACK)
            .allowMainThreadQueries()
            .build()
}
