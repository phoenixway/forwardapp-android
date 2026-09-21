package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HierarchyPlacementLinkedAppearanceMutationCoordinatorRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `occurrence remove and restore preserve correlated linked provenance`() =
        runBlocking {
            val db = database()
            try {
                insertWorkspace(db, "workspace")
                val repository = CanonicalHierarchyPlacementRepository(db)
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val placement =
                    repository.createLinkAppearance(
                        target = workspaceTarget("workspace"),
                        now = 10L,
                    )

                linked.setLinkedAppearance(
                    placementId = placement,
                    now = 11L,
                )

                repository.removePlacement(
                    placementId = placement,
                    now = 20L,
                )

                val retired =
                    requireNotNull(
                        db.hierarchyPlacementLinkedAppearanceDao()
                            .getByPlacementId(placement.value),
                    )
                assertTrue(retired.isDeleted)
                assertEquals(20L, retired.updatedAt)
                assertEquals(2L, retired.version)

                repository.restorePlacement(
                    placementId = placement,
                    now = 30L,
                )

                val restored =
                    requireNotNull(
                        db.hierarchyPlacementLinkedAppearanceDao()
                            .getByPlacementId(placement.value),
                    )
                assertFalse(restored.isDeleted)
                assertEquals(30L, restored.updatedAt)
                assertEquals(3L, restored.version)
                assertTrue(repository.getPlacement(placement) != null)
            } finally {
                db.close()
            }
        }

    @Test
    fun `independently retired provenance is not resurrected by occurrence restore`() =
        runBlocking {
            val db = database()
            try {
                insertWorkspace(db, "workspace")
                val repository = CanonicalHierarchyPlacementRepository(db)
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val placement =
                    repository.createLinkAppearance(
                        target = workspaceTarget("workspace"),
                        now = 10L,
                    )

                linked.setLinkedAppearance(
                    placementId = placement,
                    now = 11L,
                )
                linked.retireLinkedAppearance(
                    placementId = placement,
                    now = 12L,
                )

                repository.removePlacement(
                    placementId = placement,
                    now = 20L,
                )
                repository.restorePlacement(
                    placementId = placement,
                    now = 30L,
                )

                val provenance =
                    requireNotNull(
                        db.hierarchyPlacementLinkedAppearanceDao()
                            .getByPlacementId(placement.value),
                    )
                assertTrue(provenance.isDeleted)
                assertEquals(12L, provenance.updatedAt)
                assertEquals(2L, provenance.version)
                assertTrue(repository.getPlacement(placement) != null)
            } finally {
                db.close()
            }
        }

    @Test
    fun `workspace target lifecycle tombstones exact linked provenance with H1`() =
        runBlocking {
            val db = database()
            try {
                insertWorkspace(db, "workspace")
                val repository = CanonicalHierarchyPlacementRepository(db)
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val lifecycle = HierarchyPlacementLifecycleCoordinator(db)
                val placement =
                    repository.createLinkAppearance(
                        target = workspaceTarget("workspace"),
                        now = 10L,
                    )

                linked.setLinkedAppearance(
                    placementId = placement,
                    now = 11L,
                )

                db.withTransaction {
                    lifecycle.tombstoneWorkspaceTarget(
                        targetId = "workspace",
                        now = 20L,
                    )
                    val workspace = requireNotNull(db.workspaceDao().getById("workspace"))
                    db.workspaceDao().upsert(
                        listOf(
                            workspace.copy(
                                updatedAt = 20L,
                                syncedAt = null,
                                isDeleted = true,
                                version = workspace.version + 1L,
                            ),
                        ),
                    )
                }

                val placementRow =
                    requireNotNull(
                        db.hierarchyPlacementDao().getById(placement.value),
                    )
                val provenance =
                    requireNotNull(
                        db.hierarchyPlacementLinkedAppearanceDao()
                            .getByPlacementId(placement.value),
                    )

                assertTrue(placementRow.isDeleted)
                assertEquals(20L, placementRow.updatedAt)
                assertTrue(provenance.isDeleted)
                assertEquals(20L, provenance.updatedAt)
                assertEquals(2L, provenance.version)
            } finally {
                db.close()
            }
        }

    @Test
    fun `failed target lifecycle transaction rolls linked provenance back with H1`() =
        runBlocking {
            val db = database()
            try {
                insertWorkspace(db, "workspace")
                val repository = CanonicalHierarchyPlacementRepository(db)
                val linked = HierarchyPlacementLinkedAppearanceMutationCoordinator(db)
                val lifecycle = HierarchyPlacementLifecycleCoordinator(db)
                val placement =
                    repository.createLinkAppearance(
                        target = workspaceTarget("workspace"),
                        now = 10L,
                    )

                linked.setLinkedAppearance(
                    placementId = placement,
                    now = 11L,
                )

                val failure =
                    runCatching {
                        db.withTransaction {
                            lifecycle.tombstoneWorkspaceTarget(
                                targetId = "workspace",
                                now = 20L,
                            )
                            error("force rollback")
                        }
                    }.exceptionOrNull()

                assertTrue(failure is IllegalStateException)
                assertFalse(
                    requireNotNull(
                        db.hierarchyPlacementDao().getById(placement.value),
                    ).isDeleted,
                )
                val provenance =
                    requireNotNull(
                        db.hierarchyPlacementLinkedAppearanceDao()
                            .getByPlacementId(placement.value),
                    )
                assertFalse(provenance.isDeleted)
                assertEquals(11L, provenance.updatedAt)
                assertEquals(1L, provenance.version)
            } finally {
                db.close()
            }
        }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun insertWorkspace(
        db: AppDatabase,
        id: String,
    ) {
        db.workspaceDao().upsert(
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

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.WORKSPACE,
            id = id,
        )
}
