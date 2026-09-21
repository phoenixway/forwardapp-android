package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context as AndroidContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
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
class CanonicalHierarchyAuthorityActivatorRoomTest {
    private val context: AndroidContext = ApplicationProvider.getApplicationContext()

    @Test
    fun `first establishment materializes exact hierarchy provenance and persists marker`() =
        runBlocking {
            val db = database()
            try {
                seedLinkedWorkspaceHierarchy(db)

                val activator = activator(db)
                val result = activator.ensureEstablished(now = 100L)

                assertTrue(result.performed)
                assertEquals(
                    CanonicalV1HierarchyMaterializationOutcome.CREATED,
                    result.materialization?.outcome,
                )

                val marker =
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value)
                assertEquals(
                    CanonicalHierarchyAuthorityActivator.CURRENT_ACTIVATION_VERSION,
                    marker?.version,
                )
                assertEquals(100L, marker?.activatedAt)

                val placements = db.hierarchyPlacementDao().getAll()
                assertEquals(4, placements.size)

                val linked = db.hierarchyPlacementLinkedAppearanceDao().getAll()
                assertEquals(1, linked.size)
                val linkedPlacement =
                    placements.single { it.id == linked.single().placementId }
                assertEquals("shared", linkedPlacement.targetId)
                assertEquals("LINK", linkedPlacement.placementKind)
            } finally {
                db.close()
            }
        }

    @Test
    fun `durable marker prevents all later V1 recapture and rematerialization`() =
        runBlocking {
            val db = database()
            try {
                seedLinkedWorkspaceHierarchy(db)
                val activator = activator(db)

                val first = activator.ensureEstablished(now = 100L)
                assertTrue(first.performed)

                val placementsAfterFirst = db.hierarchyPlacementDao().getAll()
                val linkedAfterFirst =
                    db.hierarchyPlacementLinkedAppearanceDao().getAll()

                // This makes a fresh CURRENT V1 capture fail closed. A second
                // activation must never observe it because the durable marker
                // is checked before any legacy hierarchy read.
                db.mainBeaconDao().insertBeacon(
                    MainBeacon(
                        id = "post-activation-unmapped",
                        title = "must-never-be-read",
                        createdAt = 150L,
                        updatedAt = 150L,
                    ),
                )

                val second = activator.ensureEstablished(now = 200L)

                assertFalse(second.performed)
                assertNull(second.materialization)
                assertEquals(
                    placementsAfterFirst,
                    db.hierarchyPlacementDao().getAll(),
                )
                assertEquals(
                    linkedAfterFirst,
                    db.hierarchyPlacementLinkedAppearanceDao().getAll(),
                )

                val marker =
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value)
                assertEquals(100L, marker?.activatedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `structural mismatch fails closed and never writes activation marker`() =
        runBlocking {
            val db = database()
            try {
                context(db, "root", null, 0L)
                workspace(db, "root", null, 0L)

                db.hierarchyPlacementDao().upsert(
                    HierarchyPlacementEntity(
                        id = "non-deterministic-existing-placement",
                        hierarchyId = HierarchyId.GENERAL.value,
                        targetType = "WORKSPACE",
                        targetId = "root",
                        parentPlacementId = null,
                        placementKind = "PRIMARY",
                        siblingOrder = 0L,
                        createdAt = 50L,
                        updatedAt = 50L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                )

                val failure =
                    runCatching {
                        activator(db).ensureEstablished(now = 100L)
                    }.exceptionOrNull()

                assertTrue(
                    failure is CanonicalV1HierarchyMaterializationConflictException,
                )
                assertNull(
                    db.hierarchyAuthorityActivationStateDao()
                        .get(HierarchyId.GENERAL.value),
                )
                assertEquals(
                    "non-deterministic-existing-placement",
                    db.hierarchyPlacementDao().getAll().single().id,
                )
            } finally {
                db.close()
            }
        }

    private suspend fun seedLinkedWorkspaceHierarchy(db: AppDatabase) {
        context(db, "wa", null, 0L)
        context(db, "wb", null, 1L)
        context(db, "shared", "wa", 0L)

        workspace(db, "wa", null, 0L)
        workspace(db, "wb", null, 1L)
        workspace(db, "shared", "wa", 0L)

        db.contextParentLinkDao().insertAll(
            listOf(
                ContextParentLink(
                    parentContextId = "wb",
                    childContextId = "shared",
                    order = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            ),
        )
    }

    private fun activator(db: AppDatabase): CanonicalHierarchyAuthorityActivator =
        CanonicalHierarchyAuthorityActivator(
            database = db,
            snapshotReader = reader(db),
            materializer = CanonicalV1HierarchyMaterializer(db),
        )

    private fun reader(db: AppDatabase): CanonicalV1HierarchySnapshotReader {
        val canonicalTags = CanonicalWorkspaceTagRepository(db)
        val tagAuthority =
            SystemWorkspaceTagAuthority(
                workspaceDao = db.workspaceDao(),
                workspaceTagRefDao = db.workspaceTagRefDao(),
                systemWorkspaceTagSeedStateDao = db.systemWorkspaceTagSeedStateDao(),
                canonicalWorkspaceTagRepository = canonicalTags,
            )
        val projector =
            SystemWorkspacePresentationContextProjector(
                workspaceDao = db.workspaceDao(),
                systemWorkspaceTagAuthority = tagAuthority,
                canonicalWorkspaceTagRepository = canonicalTags,
                contextDao = db.contextDao(),
            )

        return CanonicalV1HierarchySnapshotReader(
            database = db,
            presentationProjector = projector,
            builder = CanonicalV1HierarchySnapshotBuilder(),
        )
    }

    private suspend fun context(
        db: AppDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.contextDao().insertContexts(
            listOf(
                Context(
                    id = id,
                    name = id,
                    description = null,
                    parentId = parentId,
                    createdAt = 1L,
                    updatedAt = 1L,
                    isDeleted = false,
                    version = 1L,
                    order = order,
                ),
            ),
        )
    }

    private suspend fun workspace(
        db: AppDatabase,
        id: String,
        parentId: String?,
        order: Long,
    ) {
        db.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    parentWorkspaceId = parentId,
                    roleCode = null,
                    workspaceOrder = order,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                    sourceContextId = id,
                ),
            ),
        )
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
