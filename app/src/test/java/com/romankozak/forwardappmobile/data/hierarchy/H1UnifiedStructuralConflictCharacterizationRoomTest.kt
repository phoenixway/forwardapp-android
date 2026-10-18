package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Room acceptance for strict schema-199 peer conflict admission and stale ACK.
 *
 * Divergent equal-version states must roll back regardless of updatedAt.
 * A later legitimate local version increment must defeat the old ACK.
 */
@RunWith(RobolectricTestRunner::class)
class H1UnifiedStructuralConflictCharacterizationRoomTest {
    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    @Test
    fun `higher timestamp divergent membership rejects and old ACK cannot clear new version`(): Unit =
        runBlocking {
            val db = database()
            try {
                db.workspaceDao().upsert(
                    listOf(
                        WorkspaceEntity(
                            "moving-workspace",
                            "Moving",
                            null, null, 1, 1, null, false, 1,
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val placement = CanonicalHierarchyPlacementRepository(db)
                    .createPrimaryAppearance(
                        HierarchyTargetRef(
                            HierarchyTargetType.WORKSPACE,
                            "moving-workspace",
                        ),
                        now = 1,
                    )

                val repository = BigPicturePresentationRepository(db)
                val x = repository.create("X", now = 10, id = "section-x")
                val y = repository.create("Y", now = 11, id = "section-y")
                repository.assign(placement.value, x, now = 20)

                val sent = repository.snapshot().memberships.single()
                assertThat(sent.containerId).isEqualTo(x)
                assertThat(sent.version).isEqualTo(1L)
                assertThat(sent.syncedAt).isNull()

                val divergent = sent.copy(
                    containerId = y,
                    updatedAt = sent.updatedAt + 100,
                    syncedAt = null,
                )

                val beforeConflict = repository.snapshot()
                val conflict = runCatching {
                    repository.merge(
                        BigPicturePresentationSnapshot(
                            memberships = listOf(divergent),
                        ),
                    )
                }.exceptionOrNull()
                assertThat(conflict).isInstanceOf(IllegalStateException::class.java)
                assertThat(repository.snapshot()).isEqualTo(beforeConflict)

                // Legitimate local reassignment increments the version.
                repository.assign(
                    placement.value, y, now = 200, expectedVersion = sent.version,
                )
                val moved = repository.snapshot().memberships.single()
                assertThat(moved.containerId).isEqualTo(y)
                assertThat(moved.version).isEqualTo(sent.version + 1)
                assertThat(moved.syncedAt).isNull()

                // The old ACK identifies the previously transmitted v1 only.
                repository.markSynced(
                    BigPicturePresentationAck(
                        memberships = listOf(
                            BigPictureRecordVersion(
                                sent.placementId,
                                sent.version,
                            ),
                        ),
                    ),
                    now = 500,
                )

                val afterAck = repository.snapshot().memberships.single()
                assertThat(afterAck.containerId).isEqualTo(y)
                assertThat(afterAck.version).isEqualTo(sent.version + 1)
                assertThat(afterAck.syncedAt).isNull()
                assertThat(repository.dirty().memberships)
                    .containsExactly(afterAck)
            } finally {
                db.close()
            }
        }

    @Test
    fun `higher timestamp divergent container parent rejects and old ACK cannot clear new version`(): Unit =
        runBlocking {
            val db = database()
            try {
                val repository = BigPicturePresentationRepository(db)
                val left = repository.create(
                    "Left", now = 10, id = "left-container",
                )
                val right = repository.create(
                    "Right", now = 11, id = "right-container",
                )
                val nested = repository.create(
                    "Nested",
                    parentContainerId = left,
                    now = 12,
                    id = "nested-container",
                )

                val sent = repository.snapshot().containers.single {
                    it.id == nested
                }
                assertThat(sent.parentContainerId).isEqualTo(left)
                assertThat(sent.version).isEqualTo(1L)

                val beforeConflict = repository.snapshot()
                val conflict = runCatching {
                    repository.merge(
                        BigPicturePresentationSnapshot(
                            containers = listOf(
                                sent.copy(
                                    parentContainerId = right,
                                    updatedAt = sent.updatedAt + 100,
                                    syncedAt = null,
                                ),
                            ),
                        ),
                    )
                }.exceptionOrNull()
                assertThat(conflict).isInstanceOf(IllegalStateException::class.java)
                assertThat(repository.snapshot()).isEqualTo(beforeConflict)

                repository.update(
                    id = nested,
                    title = sent.title,
                    parentContainerId = right,
                    siblingOrder = sent.siblingOrder,
                    expectedVersion = sent.version,
                    now = 200,
                )
                val moved = repository.snapshot().containers.single {
                    it.id == nested
                }
                assertThat(moved.parentContainerId).isEqualTo(right)
                assertThat(moved.version).isEqualTo(sent.version + 1)
                assertThat(moved.syncedAt).isNull()

                // A stale ACK for v1 cannot acknowledge the new v2 parent.
                repository.markSynced(
                    BigPicturePresentationAck(
                        containers = listOf(
                            BigPictureRecordVersion(nested, sent.version),
                        ),
                    ),
                    now = 500,
                )

                val afterAck = repository.snapshot().containers.single {
                    it.id == nested
                }
                assertThat(afterAck.parentContainerId).isEqualTo(right)
                assertThat(afterAck.version).isEqualTo(sent.version + 1)
                assertThat(afterAck.syncedAt).isNull()
                assertThat(repository.dirty().containers).contains(moved)
            } finally {
                db.close()
            }
        }

    @Test
    fun `container metadata divergence conflicts but ACK-only replay is idempotent`(): Unit =
        runBlocking {
            val db = database()
            try {
                val repository = BigPicturePresentationRepository(db)
                val id = repository.create("Original", now = 10, id = "metadata-container")
                val sent = repository.snapshot().containers.single()

                repository.markSynced(
                    BigPicturePresentationAck(
                        containers = listOf(BigPictureRecordVersion(id, sent.version)),
                    ),
                    now = 30,
                )
                val acknowledged = repository.snapshot().containers.single()

                repository.merge(
                    BigPicturePresentationSnapshot(
                        containers = listOf(sent.copy(syncedAt = 999)),
                    ),
                )
                assertThat(repository.snapshot().containers.single())
                    .isEqualTo(acknowledged)

                val conflict = runCatching {
                    repository.merge(
                        BigPicturePresentationSnapshot(
                            containers = listOf(
                                sent.copy(
                                    title = "Concurrent rename",
                                    updatedAt = sent.updatedAt + 100,
                                ),
                            ),
                        ),
                    )
                }.exceptionOrNull()
                assertThat(conflict).isInstanceOf(IllegalStateException::class.java)
                assertThat(repository.snapshot().containers.single())
                    .isEqualTo(acknowledged)
            } finally {
                db.close()
            }
        }
}
