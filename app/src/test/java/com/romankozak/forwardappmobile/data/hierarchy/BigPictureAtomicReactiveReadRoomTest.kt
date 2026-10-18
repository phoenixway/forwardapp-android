package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigPictureAtomicReactiveReadRoomTest {
    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private suspend fun seed(db: AppDatabase, vararg ids: String) {
        db.workspaceDao().upsert(ids.map { id ->
            WorkspaceEntity(
                id, id, null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                sourceContextId = null,
            )
        })
    }

    private suspend fun occurrence(
        h1: CanonicalHierarchyPlacementRepository,
        owner: String,
        parent: PlacementId? = null,
        now: Long,
    ): PlacementId =
        h1.createPrimaryAppearance(
            HierarchyTargetRef(HierarchyTargetType.WORKSPACE, owner),
            parentPlacementId = parent,
            now = now,
        )

    /**
     * Every emitted frame must agree about the H1 sibling location of each
     * active membership. A split-flow combine could emit a mixed version
     * after a transaction, even though the DB itself is consistent.
     */
    private fun assertFrameCoherent(frame: BigPictureStructure) {
        val placements = frame.placements.associateBy { it.id.value }
        val containers = frame.presentation.containers.associateBy { it.id }

        frame.presentation.memberships
            .filterNot { it.isDeleted }
            .forEach { member ->
                val placement = requireNotNull(placements[member.placementId])
                val container = requireNotNull(containers[member.containerId])
                if (!container.isDeleted) {
                    assertThat(placement.parentPlacementId?.value)
                        .isEqualTo(container.anchorPlacementId)
                }
            }
    }

    @Test
    fun `H1 move emits only coherent H1 and presentation frames`() = runBlocking {
        val db = database()
        try {
            seed(db, "atomic-a", "atomic-b", "atomic-child")
            val h1 = CanonicalHierarchyPlacementRepository(db)
            val a = occurrence(h1, "atomic-a", now = 1)
            val b = occurrence(h1, "atomic-b", now = 2)
            val child = occurrence(h1, "atomic-child", a, now = 3)

            val presentation = BigPicturePresentationRepository(db)
            val section = presentation.create(
                "At A", anchorPlacementId = a.value, now = 10,
            )
            presentation.assign(child.value, section, now = 11)

            val reader = BigPictureStructureRepository(db)
            val initial = withTimeout(10_000) { reader.observe().first() }
            assertFrameCoherent(initial)
            assertThat(initial.presentation.memberships.single().isDeleted)
                .isFalse()

            val started = CompletableDeferred<Unit>()
            val next = async(Dispatchers.Default) {
                withTimeout(10_000) {
                    reader.observe()
                        .onEach { frame ->
                            assertFrameCoherent(frame)
                            started.complete(Unit)
                        }
                        .first { frame ->
                            frame.placements.any {
                                it.id == child && it.parentPlacementId == b
                            }
                        }
                }
            }
            withTimeout(10_000) { started.await() }

            h1.movePlacement(child, b, now = 30)

            val moved = next.await()
            assertThat(
                moved.placements.single { it.id == child }.parentPlacementId,
            ).isEqualTo(b)
            val membership = moved.presentation.memberships.single()
            assertThat(membership.placementId).isEqualTo(child.value)
            assertThat(membership.isDeleted).isTrue()
            assertThat(membership.version).isEqualTo(2)
            assertThat(membership.syncedAt).isNull()
            assertThat(moved.presentation.containers.single().isDeleted)
                .isFalse()
        } finally {
            db.close()
        }
    }

    @Test
    fun `presentation-only commit invalidates the atomic read stream`() = runBlocking {
        val db = database()
        try {
            val reader = BigPictureStructureRepository(db)
            val presentation = BigPicturePresentationRepository(db)
            val started = CompletableDeferred<Unit>()

            val next = async(Dispatchers.Default) {
                withTimeout(10_000) {
                    reader.observe()
                        .onEach { frame ->
                            assertFrameCoherent(frame)
                            started.complete(Unit)
                        }
                        .first { frame ->
                            frame.presentation.containers.any { it.id == "new-section" }
                        }
                }
            }
            withTimeout(10_000) { started.await() }

            presentation.create(
                "New section",
                now = 10,
                id = "new-section",
            )

            val frame = next.await()
            assertThat(frame.placements).isEmpty()
            assertThat(frame.presentation.containers.single().title)
                .isEqualTo("New section")
        } finally {
            db.close()
        }
    }
}
