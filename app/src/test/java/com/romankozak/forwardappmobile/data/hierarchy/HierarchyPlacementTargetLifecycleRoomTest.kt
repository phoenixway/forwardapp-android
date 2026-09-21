package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HierarchyPlacementTargetLifecycleRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `T target deletion tombstones placements atomically for ManagedSubject and Workspace`() =
        runBlocking {
            val db = database()
            try {
                insertSubject(db, "subject")
                insertWorkspace(db, "workspace")

                val repository = CanonicalHierarchyPlacementRepository(db)
                val lifecycle = HierarchyPlacementLifecycleCoordinator(db)
                val subjectPlacement =
                    repository.createPrimaryAppearance(
                        target = target(HierarchyTargetType.MANAGED_SUBJECT, "subject"),
                        now = 10L,
                    )
                val workspacePlacement =
                    repository.createPrimaryAppearance(
                        target = target(HierarchyTargetType.WORKSPACE, "workspace"),
                        now = 11L,
                    )

                db.withTransaction {
                    lifecycle.tombstoneManagedSubjectTarget("subject", 20L)
                    val subject = requireNotNull(db.orientationDao().getManagedSubject("subject"))
                    db.orientationDao().upsertManagedSubjects(
                        listOf(
                            subject.copy(
                                updatedAt = 20L,
                                syncedAt = null,
                                isDeleted = true,
                                version = subject.version + 1L,
                            ),
                        ),
                    )
                }

                db.withTransaction {
                    lifecycle.tombstoneWorkspaceTarget("workspace", 21L)
                    val workspace = requireNotNull(db.workspaceDao().getById("workspace"))
                    db.workspaceDao().upsert(
                        listOf(
                            workspace.copy(
                                updatedAt = 21L,
                                syncedAt = null,
                                isDeleted = true,
                                version = workspace.version + 1L,
                            ),
                        ),
                    )
                }

                val subjectRow = requireNotNull(db.hierarchyPlacementDao().getById(subjectPlacement.value))
                val workspaceRow = requireNotNull(db.hierarchyPlacementDao().getById(workspacePlacement.value))

                assertTrue(subjectRow.isDeleted)
                assertEquals(2L, subjectRow.version)
                assertEquals(20L, subjectRow.updatedAt)
                assertTrue(workspaceRow.isDeleted)
                assertEquals(2L, workspaceRow.version)
                assertEquals(21L, workspaceRow.updatedAt)
                assertTrue(db.orientationDao().getManagedSubject("subject")?.isDeleted == true)
                assertTrue(db.workspaceDao().getById("workspace")?.isDeleted == true)
            } finally {
                db.close()
            }
        }

    @Test
    fun `U restoring either target does not restore its placements`() = runBlocking {
        val db = database()
        try {
            insertSubject(db, "subject")
            insertWorkspace(db, "workspace")

            val repository = CanonicalHierarchyPlacementRepository(db)
            val lifecycle = HierarchyPlacementLifecycleCoordinator(db)
            val subjectPlacement =
                repository.createPrimaryAppearance(
                    target = target(HierarchyTargetType.MANAGED_SUBJECT, "subject"),
                    now = 10L,
                )
            val workspacePlacement =
                repository.createPrimaryAppearance(
                    target = target(HierarchyTargetType.WORKSPACE, "workspace"),
                    now = 11L,
                )

            db.withTransaction {
                lifecycle.tombstoneManagedSubjectTarget("subject", 20L)
                val row = requireNotNull(db.orientationDao().getManagedSubject("subject"))
                db.orientationDao().upsertManagedSubjects(
                    listOf(row.copy(isDeleted = true, updatedAt = 20L, version = row.version + 1L)),
                )
            }
            db.withTransaction {
                lifecycle.tombstoneWorkspaceTarget("workspace", 21L)
                val row = requireNotNull(db.workspaceDao().getById("workspace"))
                db.workspaceDao().upsert(
                    listOf(row.copy(isDeleted = true, updatedAt = 21L, version = row.version + 1L)),
                )
            }

            val deadSubject = requireNotNull(db.orientationDao().getManagedSubject("subject"))
            db.orientationDao().upsertManagedSubjects(
                listOf(
                    deadSubject.copy(
                        isDeleted = false,
                        updatedAt = 30L,
                        syncedAt = null,
                        version = deadSubject.version + 1L,
                    ),
                ),
            )
            val deadWorkspace = requireNotNull(db.workspaceDao().getById("workspace"))
            db.workspaceDao().upsert(
                listOf(
                    deadWorkspace.copy(
                        isDeleted = false,
                        updatedAt = 31L,
                        syncedAt = null,
                        version = deadWorkspace.version + 1L,
                    ),
                ),
            )

            assertFalse(db.orientationDao().getManagedSubject("subject")?.isDeleted == true)
            assertFalse(db.workspaceDao().getById("workspace")?.isDeleted == true)
            assertTrue(db.hierarchyPlacementDao().getById(subjectPlacement.value)?.isDeleted == true)
            assertTrue(db.hierarchyPlacementDao().getById(workspacePlacement.value)?.isDeleted == true)
            assertTrue(repository.getLiveAppearances(target(HierarchyTargetType.MANAGED_SUBJECT, "subject")).isEmpty())
            assertTrue(repository.getLiveAppearances(target(HierarchyTargetType.WORKSPACE, "workspace")).isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `V failed target delete transaction cannot partially tombstone placement or target`() =
        runBlocking {
            val db = database()
            try {
                insertWorkspace(db, "workspace")
                val repository = CanonicalHierarchyPlacementRepository(db)
                val lifecycle = HierarchyPlacementLifecycleCoordinator(db)
                val placement =
                    repository.createPrimaryAppearance(
                        target = target(HierarchyTargetType.WORKSPACE, "workspace"),
                        now = 10L,
                    )

                val failure =
                    runCatching {
                        db.withTransaction {
                            lifecycle.tombstoneWorkspaceTarget("workspace", 20L)
                            val workspace = requireNotNull(db.workspaceDao().getById("workspace"))
                            db.workspaceDao().upsert(
                                listOf(
                                    workspace.copy(
                                        isDeleted = true,
                                        updatedAt = 20L,
                                        version = workspace.version + 1L,
                                    ),
                                ),
                            )
                            error("force rollback")
                        }
                    }.exceptionOrNull()

                assertTrue(failure is IllegalStateException)
                assertFalse(requireNotNull(db.workspaceDao().getById("workspace")).isDeleted)
                assertFalse(requireNotNull(db.hierarchyPlacementDao().getById(placement.value)).isDeleted)
                assertTrue(repository.getPlacement(placement) != null)
            } finally {
                db.close()
            }
        }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun insertSubject(
        db: AppDatabase,
        id: String,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ASPECT.name,
                    title = id,
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

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

    private fun target(
        type: HierarchyTargetType,
        id: String,
    ) = HierarchyTargetRef(type, id)
}
