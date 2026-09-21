package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalHierarchyPlacementGroupScopeSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `present empty authoritative stream cannot borrow complete local provenance`() = runBlocking {
        val db = database()
        try {
            seedValidGroupedRoot(db)
            val store = CanonicalHierarchyPlacementGroupScopeSyncStore(db)

            val failure =
                runCatching {
                    store.mergeIncoming(emptyList())
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(
                failure?.message.orEmpty()
                    .contains("must cover every live root MANAGED_SUBJECT"),
            )

            val preserved =
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao()
                        .getByPlacementId(ROOT_PLACEMENT_ID),
                )
            assertEquals(GROUP_SUBJECT_ID, preserved.groupSubjectId)
            assertFalse(preserved.isDeleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun `selective delta validates incoming provenance without requiring unrelated local roots in payload`() =
        runBlocking {
            val db = database()
            try {
                seedValidGroupedRoot(db)

                val incomingSubjectId = "incoming-subject"
                val incomingPlacementId = "incoming-placement"

                db.orientationDao().upsertManagedSubjects(
                    listOf(subject(incomingSubjectId)),
                )
                db.hierarchyPlacementDao().upsert(
                    HierarchyPlacementEntity(
                        id = incomingPlacementId,
                        hierarchyId = "GENERAL",
                        targetType = HierarchyTargetType.MANAGED_SUBJECT.name,
                        targetId = incomingSubjectId,
                        parentPlacementId = null,
                        placementKind = "PRIMARY",
                        siblingOrder = 1L,
                        createdAt = 2L,
                        updatedAt = 2L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                )

                val store = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
                store.mergeIncomingSelective(
                    incoming =
                        listOf(
                            HierarchyPlacementGroupScopeSnapshot(
                                placementId = incomingPlacementId,
                                hierarchyId = "GENERAL",
                                groupSubjectId = null,
                                createdAt = 2L,
                                updatedAt = 2L,
                                syncedAt = 99L,
                                isDeleted = false,
                                version = 1L,
                            ),
                        ),
                    incomingPlacementIds = setOf(incomingPlacementId),
                )

                val scopes =
                    db.hierarchyPlacementGroupScopeDao()
                        .getAll()
                        .associateBy { it.placementId }

                assertEquals(
                    setOf(ROOT_PLACEMENT_ID, incomingPlacementId),
                    scopes.keys,
                )
                assertEquals(
                    GROUP_SUBJECT_ID,
                    scopes.getValue(ROOT_PLACEMENT_ID).groupSubjectId,
                )
                assertEquals(
                    null,
                    scopes.getValue(incomingPlacementId).groupSubjectId,
                )
                assertEquals(
                    null,
                    scopes.getValue(incomingPlacementId).syncedAt,
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `ordinary full-stream merge still rejects the same incomplete scope subset`() =
        runBlocking {
            val db = database()
            try {
                seedValidGroupedRoot(db)

                val incomingSubjectId = "incoming-subject"
                val incomingPlacementId = "incoming-placement"

                db.orientationDao().upsertManagedSubjects(
                    listOf(subject(incomingSubjectId)),
                )
                db.hierarchyPlacementDao().upsert(
                    HierarchyPlacementEntity(
                        id = incomingPlacementId,
                        hierarchyId = "GENERAL",
                        targetType = HierarchyTargetType.MANAGED_SUBJECT.name,
                        targetId = incomingSubjectId,
                        parentPlacementId = null,
                        placementKind = "PRIMARY",
                        siblingOrder = 1L,
                        createdAt = 2L,
                        updatedAt = 2L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                )

                val failure =
                    runCatching {
                        CanonicalHierarchyPlacementGroupScopeSyncStore(db)
                            .mergeIncoming(
                                listOf(
                                    HierarchyPlacementGroupScopeSnapshot(
                                        placementId = incomingPlacementId,
                                        hierarchyId = "GENERAL",
                                        groupSubjectId = null,
                                        createdAt = 2L,
                                        updatedAt = 2L,
                                        syncedAt = null,
                                        isDeleted = false,
                                        version = 1L,
                                    ),
                                ),
                            )
                    }.exceptionOrNull()

                assertTrue(failure is IllegalArgumentException)
                assertTrue(
                    failure?.message.orEmpty()
                        .contains("must cover every live root MANAGED_SUBJECT"),
                )
            } finally {
                db.close()
            }
        }

    @Test
    fun `null stream remains absent and leaves complete local provenance untouched`() = runBlocking {
        val db = database()
        try {
            seedValidGroupedRoot(db)
            val store = CanonicalHierarchyPlacementGroupScopeSyncStore(db)

            store.mergeIncoming(null)

            val preserved =
                requireNotNull(
                    db.hierarchyPlacementGroupScopeDao()
                        .getByPlacementId(ROOT_PLACEMENT_ID),
                )
            assertEquals(GROUP_SUBJECT_ID, preserved.groupSubjectId)
            assertFalse(preserved.isDeleted)
        } finally {
            db.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).allowMainThreadQueries()
            .build()

    private suspend fun seedValidGroupedRoot(db: AppDatabase) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                subject(ROOT_SUBJECT_ID),
                subject(GROUP_SUBJECT_ID),
            ),
        )
        db.orientationDao().upsertOrientations(
            listOf(
                OrientationEntity(
                    subjectId = GROUP_SUBJECT_ID,
                    kind = OrientationKind.MAIN_BEACON_GROUP.name,
                    lifecycle = null,
                    lifecycleOrigin = "UNSET",
                ),
            ),
        )
        db.orientationDao().upsertLegacyMappings(
            listOf(
                LegacySubjectMappingEntity(
                    id = "group-mapping",
                    sourceType = LegacyOrientationSourceType.MAIN_BEACON_GROUP.name,
                    sourceId = "legacy-group",
                    subjectId = GROUP_SUBJECT_ID,
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
        db.orientationDao().upsertOrientationRelations(
            listOf(
                OrientationRelationEntity(
                    id = "part-of",
                    fromOrientationId = ROOT_SUBJECT_ID,
                    toOrientationId = GROUP_SUBJECT_ID,
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
        db.hierarchyPlacementDao().upsert(
            HierarchyPlacementEntity(
                id = ROOT_PLACEMENT_ID,
                hierarchyId = "GENERAL",
                targetType = HierarchyTargetType.MANAGED_SUBJECT.name,
                targetId = ROOT_SUBJECT_ID,
                parentPlacementId = null,
                placementKind = "PRIMARY",
                siblingOrder = 0L,
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            ),
        )
        db.hierarchyPlacementGroupScopeDao().upsert(
            HierarchyPlacementGroupScopeEntity(
                placementId = ROOT_PLACEMENT_ID,
                hierarchyId = "GENERAL",
                groupSubjectId = GROUP_SUBJECT_ID,
                createdAt = 1L,
                updatedAt = 1L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
            ),
        )
    }

    private fun subject(id: String) =
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
        )

    private companion object {
        const val ROOT_PLACEMENT_ID = "root-placement"
        const val ROOT_SUBJECT_ID = "root-subject"
        const val GROUP_SUBJECT_ID = "group-subject"
    }
}
