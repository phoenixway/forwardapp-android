package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HierarchyPlacementGroupScopeMutationCoordinatorRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `target lifecycle tombstones occurrence Group scope`() = runBlocking {
        val db = database()
        try {
            subject(db, TARGET)
            placement(db, "root", TARGET)
            scope(db, "root", null)

            db.withTransaction {
                HierarchyPlacementLifecycleCoordinator(db)
                    .tombstoneManagedSubjectTarget(TARGET, 20L)
                val current = requireNotNull(db.orientationDao().getManagedSubject(TARGET))
                db.orientationDao().upsertManagedSubjects(
                    listOf(
                        current.copy(
                            isDeleted = true,
                            updatedAt = 20L,
                            syncedAt = null,
                            version = current.version + 1L,
                        ),
                    ),
                )
            }

            assertTrue(requireNotNull(db.hierarchyPlacementDao().getById("root")).isDeleted)
            val storedScope =
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("root"),
                )
            assertTrue(storedScope.isDeleted)
            assertEquals(2L, storedScope.version)
            assertEquals(20L, storedScope.updatedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `retiring sole Group converts exact root to explicit NoGroup`() = runBlocking {
        val db = database()
        try {
            group(db, GROUP_A, "legacy-a")
            subject(db, TARGET)
            placement(db, "root-a", TARGET)
            scope(db, "root-a", GROUP_A)
            partOf(db, "part-a", TARGET, GROUP_A)

            val coordinator = HierarchyPlacementGroupScopeMutationCoordinator(db)

            db.withTransaction {
                val result = coordinator.reconcileGroupRetirement(GROUP_A, 20L)
                assertEquals(listOf(PlacementId("root-a")), result.convertedToNoGroup)
                assertTrue(result.removedOccurrences.isEmpty())

                tombstoneRelation(db, "part-a", 21L)
                tombstoneSubject(db, GROUP_A, 21L)
                coordinator.validateAuthoritativeState()
            }

            assertFalse(requireNotNull(db.hierarchyPlacementDao().getById("root-a")).isDeleted)
            val storedScope =
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("root-a"),
                )
            assertFalse(storedScope.isDeleted)
            assertNull(storedScope.groupSubjectId)
            assertEquals(2L, storedScope.version)
        } finally {
            db.close()
        }
    }

    @Test
    fun `retiring one of multiple Groups removes only its occurrence subtree`() = runBlocking {
        val db = database()
        try {
            group(db, GROUP_A, "legacy-a")
            group(db, GROUP_B, "legacy-b")
            subject(db, TARGET)
            subject(db, CHILD)

            placement(db, "root-a", TARGET, kind = "PRIMARY", order = 0L)
            placement(db, "child-a", CHILD, parent = "root-a")
            placement(db, "root-b", TARGET, kind = "LINK", order = 1L)
            placement(db, "child-b", CHILD, parent = "root-b", kind = "LINK")
            scope(db, "root-a", GROUP_A)
            scope(db, "root-b", GROUP_B)
            partOf(db, "part-a", TARGET, GROUP_A)
            partOf(db, "part-b", TARGET, GROUP_B)

            val coordinator = HierarchyPlacementGroupScopeMutationCoordinator(db)

            db.withTransaction {
                val result = coordinator.reconcileGroupRetirement(GROUP_A, 20L)
                assertEquals(listOf(PlacementId("root-a")), result.removedRootOccurrences)
                assertEquals(
                    setOf(PlacementId("root-a"), PlacementId("child-a")),
                    result.removedOccurrences.toSet(),
                )

                tombstoneRelation(db, "part-a", 21L)
                tombstoneSubject(db, GROUP_A, 21L)
                coordinator.validateAuthoritativeState()
            }

            assertTrue(requireNotNull(db.hierarchyPlacementDao().getById("root-a")).isDeleted)
            assertTrue(requireNotNull(db.hierarchyPlacementDao().getById("child-a")).isDeleted)
            assertTrue(
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("root-a"),
                ).isDeleted,
            )

            assertFalse(requireNotNull(db.hierarchyPlacementDao().getById("root-b")).isDeleted)
            assertFalse(requireNotNull(db.hierarchyPlacementDao().getById("child-b")).isDeleted)
            assertEquals(
                GROUP_B,
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao().getByPlacementId("root-b"),
                ).groupSubjectId,
            )
        } finally {
            db.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun subject(
        db: AppDatabase,
        id: String,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = ManagedSubjectType.ORIENTATION.name,
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

    private suspend fun group(
        db: AppDatabase,
        subjectId: String,
        legacyId: String,
    ) {
        subject(db, subjectId)
        db.orientationDao().upsertOrientations(
            listOf(
                OrientationEntity(
                    subjectId = subjectId,
                    kind = OrientationKind.MAIN_BEACON_GROUP.name,
                    lifecycle = null,
                    lifecycleOrigin = "UNSET",
                ),
            ),
        )
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "mapping-$legacyId",
                    sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                    sourceId = legacyId,
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun placement(
        db: AppDatabase,
        id: String,
        targetId: String,
        parent: String? = null,
        kind: String = "PRIMARY",
        order: Long = 0L,
    ) {
        db.hierarchyPlacementDao().upsert(
            HierarchyPlacementEntity(
                id = id,
                hierarchyId = "GENERAL",
                targetType = "MANAGED_SUBJECT",
                targetId = targetId,
                parentPlacementId = parent,
                placementKind = kind,
                siblingOrder = order,
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            ),
        )
    }

    private suspend fun scope(
        db: AppDatabase,
        placementId: String,
        groupSubjectId: String?,
    ) {
        db.hierarchyPlacementGroupScopeDao().upsert(
            HierarchyPlacementGroupScopeEntity(
                placementId = placementId,
                hierarchyId = "GENERAL",
                groupSubjectId = groupSubjectId,
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            ),
        )
    }

    private suspend fun partOf(
        db: AppDatabase,
        id: String,
        from: String,
        to: String,
    ) {
        db.orientationDao().upsertOrientationRelations(
            listOf(
                OrientationRelationEntity(
                    id = id,
                    fromOrientationId = from,
                    toOrientationId = to,
                    relationType = OrientationRelationType.PART_OF.name,
                    relationOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun tombstoneRelation(
        db: AppDatabase,
        id: String,
        now: Long,
    ) {
        val current =
            requireNotNull(
                db.orientationDao()
                    .getAllOrientationRelations()
                    .firstOrNull { it.id == id },
            )
        db.orientationDao().upsertOrientationRelations(
            listOf(
                current.copy(
                    isDeleted = true,
                    updatedAt = now,
                    syncedAt = null,
                    version = current.version + 1L,
                ),
            ),
        )
    }

    private suspend fun tombstoneSubject(
        db: AppDatabase,
        id: String,
        now: Long,
    ) {
        val current = requireNotNull(db.orientationDao().getManagedSubject(id))
        db.orientationDao().upsertManagedSubjects(
            listOf(
                current.copy(
                    isDeleted = true,
                    updatedAt = now,
                    syncedAt = null,
                    version = current.version + 1L,
                ),
            ),
        )
    }

    private companion object {
        const val TARGET = "beacon-target"
        const val CHILD = "beacon-child"
        const val GROUP_A = "group-a"
        const val GROUP_B = "group-b"
    }
}
