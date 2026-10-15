package com.romankozak.forwardappmobile.data.planning

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
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
class CanonicalPlanningSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `transport round trip preserves stable role nullable priority provenance and tombstone`() =
        runBlocking {
            val db = database()
            try {
                seedOrientation(db, ORIENTATION)
                val store = CanonicalPlanningSyncStore(db)

                store.mergeIncoming(
                    scopes = listOf(scope()),
                    commitments =
                        listOf(
                            commitment(
                                role = "DAY.THEME",
                                priorityLevel = null,
                                provenanceKind = "IMPORTED",
                                provenanceSourceType = "fixture",
                                provenanceSourceId = "source-1",
                                isDeleted = true,
                            ),
                        ),
                )

                val storedScope = store.loadAllScopes().single()
                val storedCommitment = store.loadAllCommitments().single()

                assertEquals("TACTICAL_CYCLE", storedScope.kind)
                assertEquals("DAY.THEME", storedCommitment.role)
                assertNull(storedCommitment.priorityLevel)
                assertEquals("IMPORTED", storedCommitment.provenanceKind)
                assertEquals("fixture", storedCommitment.provenanceSourceType)
                assertEquals("source-1", storedCommitment.provenanceSourceId)
                assertTrue(storedCommitment.isDeleted)
            } finally {
                db.close()
            }
        }

    @Test
    fun `newer wins older loses and equal semantic state is idempotent despite syncedAt`() =
        runBlocking {
            val db = database()
            try {
                seedOrientation(db, ORIENTATION)
                val store = CanonicalPlanningSyncStore(db)

                store.mergeIncoming(
                    scopes = listOf(scope(version = 2L, title = "v2")),
                    commitments = emptyList(),
                )

                store.mergeIncoming(
                    scopes = listOf(scope(version = 1L, title = "stale")),
                    commitments = emptyList(),
                )

                store.mergeIncoming(
                    scopes =
                        listOf(
                            scope(
                                version = 2L,
                                title = "v2",
                                syncedAt = 999L,
                            ),
                        ),
                    commitments = emptyList(),
                )

                val stored = store.loadAllScopes().single()
                assertEquals(2L, stored.version)
                assertEquals("v2", stored.title)
                assertNull(stored.syncedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `equal version divergent scope fails closed`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            store.mergeIncoming(
                scopes = listOf(scope(version = 3L, title = "local")),
                commitments = emptyList(),
            )

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = listOf(scope(version = 3L, title = "remote")),
                    commitments = emptyList(),
                )
            }

            assertEquals("local", store.loadAllScopes().single().title)
        } finally {
            db.close()
        }
    }

    @Test
    fun `unknown enum wire codes fail closed`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = listOf(scope(kind = "NOT_A_SCOPE_KIND")),
                    commitments = emptyList(),
                )
            }

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = listOf(scope()),
                    commitments = listOf(commitment(status = "NOT_A_STATUS")),
                )
            }

            assertTrue(store.loadAllScopes().isEmpty())
            assertTrue(store.loadAllCommitments().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `changed since includes newly created rows with null updatedAt`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            store.mergeIncoming(
                scopes =
                    listOf(
                        scope(
                            createdAt = 200L,
                            updatedAt = null,
                            version = 1L,
                        ),
                    ),
                commitments =
                    listOf(
                        commitment(
                            createdAt = 201L,
                            updatedAt = null,
                            version = 1L,
                        ),
                    ),
            )

            assertEquals(
                listOf(SCOPE),
                store.loadScopesChangedSince(150L).map { it.id },
            )
            assertEquals(
                listOf(COMMITMENT),
                store.loadCommitmentsChangedSince(150L).map { it.id },
            )
            assertTrue(store.loadScopesChangedSince(250L).isEmpty())
            assertTrue(store.loadCommitmentsChangedSince(250L).isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `malformed role fails closed`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = listOf(scope()),
                    commitments = listOf(commitment(role = "bad-role")),
                )
            }

            assertTrue(store.loadAllScopes().isEmpty())
            assertTrue(store.loadAllCommitments().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `live commitment rejects missing scope and missing orientation`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = emptyList(),
                    commitments = listOf(commitment()),
                )
            }

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = listOf(scope()),
                    commitments =
                        listOf(
                            commitment(
                                orientationId = "missing-orientation",
                            ),
                        ),
                )
            }

            assertTrue(store.loadAllScopes().isEmpty())
            assertTrue(store.loadAllCommitments().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `duplicate live participation fails closed`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            assertFailsPlanning {
                store.mergeIncoming(
                    scopes = listOf(scope()),
                    commitments =
                        listOf(
                            commitment(id = "c1"),
                            commitment(id = "c2"),
                        ),
                )
            }

            assertTrue(store.loadAllScopes().isEmpty())
            assertTrue(store.loadAllCommitments().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `scope tombstone cannot strand live commitment and transaction rolls back`() =
        runBlocking {
            val db = database()
            try {
                seedOrientation(db, ORIENTATION)
                val store = CanonicalPlanningSyncStore(db)

                store.mergeIncoming(
                    scopes = listOf(scope(version = 1L)),
                    commitments = listOf(commitment(version = 1L)),
                )

                assertFailsPlanning {
                    store.mergeIncoming(
                        scopes =
                            listOf(
                                scope(
                                    version = 2L,
                                    isDeleted = true,
                                ),
                            ),
                        commitments = emptyList(),
                    )
                }

                assertFalse(store.loadAllScopes().single().isDeleted)
                assertFalse(store.loadAllCommitments().single().isDeleted)
            } finally {
                db.close()
            }
        }

    @Test
    fun `scope and commitment tombstones can advance together`() = runBlocking {
        val db = database()
        try {
            seedOrientation(db, ORIENTATION)
            val store = CanonicalPlanningSyncStore(db)

            store.mergeIncoming(
                scopes = listOf(scope(version = 1L)),
                commitments = listOf(commitment(version = 1L)),
            )

            store.mergeIncoming(
                scopes =
                    listOf(
                        scope(
                            version = 2L,
                            isDeleted = true,
                        ),
                    ),
                commitments =
                    listOf(
                        commitment(
                            version = 2L,
                            isDeleted = true,
                        ),
                    ),
            )

            assertTrue(store.loadAllScopes().single().isDeleted)
            assertTrue(store.loadAllCommitments().single().isDeleted)
        } finally {
            db.close()
        }
    }

    @Test
    fun `restore validation uses supplied orientation closure and preserves syncedAt`() =
        runBlocking {
            val db = database()
            try {
                seedOrientation(db, ORIENTATION)
                val store = CanonicalPlanningSyncStore(db)

                val decoded =
                    store.decodeAndValidateForRestore(
                        scopes = listOf(scope(syncedAt = 111L)),
                        commitments = listOf(commitment(syncedAt = 222L)),
                        liveOrientationIds = setOf(ORIENTATION),
                    )

                store.restoreExactDecoded(decoded)

                assertEquals(111L, store.loadAllScopes().single().syncedAt)
                assertEquals(222L, store.loadAllCommitments().single().syncedAt)
            } finally {
                db.close()
            }
        }

    @Test
    fun `restore rejects missing orientation before persistence`() = runBlocking {
        val db = database()
        try {
            val store = CanonicalPlanningSyncStore(db)

            assertFailsPlanning {
                store.decodeAndValidateForRestore(
                    scopes = listOf(scope()),
                    commitments = listOf(commitment()),
                    liveOrientationIds = emptySet(),
                )
            }

            assertTrue(store.loadAllScopes().isEmpty())
            assertTrue(store.loadAllCommitments().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `exact ack marks matching versions and stale ack cannot mark later versions`() =
        runBlocking {
            val db = database()
            try {
                seedOrientation(db, ORIENTATION)
                val store = CanonicalPlanningSyncStore(db)

                store.mergeIncoming(
                    scopes = listOf(scope(version = 3L)),
                    commitments = listOf(commitment(version = 4L)),
                )

                store.markSynced(
                    scopes =
                        listOf(
                            PlanningScopeSyncVersion(
                                id = SCOPE,
                                version = 3L,
                            ),
                        ),
                    commitments =
                        listOf(
                            PlanningCommitmentSyncVersion(
                                id = COMMITMENT,
                                version = 4L,
                            ),
                        ),
                )

                assertNotNull(store.loadAllScopes().single().syncedAt)
                assertNotNull(store.loadAllCommitments().single().syncedAt)

                db.planningDao().upsertScope(
                    requireNotNull(db.planningDao().getScope(SCOPE)).copy(
                        version = 4L,
                        syncedAt = null,
                    ),
                )
                db.planningDao().upsertCommitment(
                    requireNotNull(db.planningDao().getCommitment(COMMITMENT)).copy(
                        version = 5L,
                        syncedAt = null,
                    ),
                )

                store.markSynced(
                    scopes =
                        listOf(
                            PlanningScopeSyncVersion(
                                id = SCOPE,
                                version = 3L,
                            ),
                        ),
                    commitments =
                        listOf(
                            PlanningCommitmentSyncVersion(
                                id = COMMITMENT,
                                version = 4L,
                            ),
                        ),
                )

                assertNull(store.loadAllScopes().single().syncedAt)
                assertNull(store.loadAllCommitments().single().syncedAt)
            } finally {
                db.close()
            }
        }

    private suspend fun seedOrientation(
        db: AppDatabase,
        id: String,
    ) {
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = id,
                    subjectType = "ORIENTATION",
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
        db.orientationDao().upsertOrientations(
            listOf(
                OrientationEntity(
                    subjectId = id,
                    kind = "GOAL",
                    lifecycle = null,
                    lifecycleOrigin = "UNSET",
                ),
            ),
        )
    }

    private suspend fun assertFailsPlanning(
        block: suspend () -> Unit,
    ) {
        try {
            block()
            throw AssertionError("Expected canonical Planning rejection")
        } catch (_: IllegalArgumentException) {
            // Expected contract rejection.
        }
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).allowMainThreadQueries()
            .build()

    private fun scope(
        version: Long = 1L,
        title: String? = null,
        kind: String = "TACTICAL_CYCLE",
        createdAt: Long = 10L,
        updatedAt: Long? = 20L + version,
        syncedAt: Long? = 50L,
        isDeleted: Boolean = false,
    ) =
        PlanningScopeSnapshot(
            id = SCOPE,
            kind = kind,
            lifecycle = "ACTIVE",
            title = title,
            startsAt = 100L,
            endsAt = 200L,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private fun commitment(
        id: String = COMMITMENT,
        orientationId: String = ORIENTATION,
        role: String = "MISSION",
        priorityLevel: String? = "HIGH",
        status: String = "PLANNED",
        provenanceKind: String = "MANUAL",
        provenanceSourceType: String? = null,
        provenanceSourceId: String? = null,
        createdAt: Long = 11L,
        version: Long = 1L,
        updatedAt: Long? = 30L + version,
        syncedAt: Long? = 60L,
        isDeleted: Boolean = false,
    ) =
        PlanningCommitmentSnapshot(
            id = id,
            scopeId = SCOPE,
            orientationId = orientationId,
            role = role,
            order = 0L,
            priorityLevel = priorityLevel,
            status = status,
            provenanceKind = provenanceKind,
            provenanceSourceType = provenanceSourceType,
            provenanceSourceId = provenanceSourceId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            version = version,
            isDeleted = isDeleted,
        )

    private companion object {
        const val SCOPE = "scope"
        const val COMMITMENT = "commitment"
        const val ORIENTATION = "orientation"
    }
}
