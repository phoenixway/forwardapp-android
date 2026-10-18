package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigPictureHierarchyLifecycleRoomTest {

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    private suspend fun seed(db: AppDatabase, vararg ids: String) {
        db.workspaceDao().upsert(
            ids.map { id ->
                WorkspaceEntity(
                    id, id, null, null, 1, 1, null, false, 1,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                )
            },
        )
    }

    private suspend fun occurrence(
        h1: CanonicalHierarchyPlacementRepository,
        id: String,
        parent: PlacementId? = null,
        now: Long,
    ): PlacementId =
        h1.createPrimaryAppearance(
            target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id),
            parentPlacementId = parent,
            now = now,
        )

    @Test
    fun `same level move preserves membership but cross level move tombstones only the moved occurrence`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "level-a", "level-b", "moving", "staying")
                val h1 = CanonicalHierarchyPlacementRepository(db)
                val a = occurrence(h1, "level-a", now = 1)
                val b = occurrence(h1, "level-b", now = 2)
                val moving = occurrence(h1, "moving", parent = a, now = 3)
                val staying = occurrence(h1, "staying", parent = a, now = 4)
                val presentation = BigPicturePresentationRepository(db)
                val section = presentation.create(
                    "At A", anchorPlacementId = a.value, now = 10,
                )
                presentation.assign(moving.value, section, now = 11)
                presentation.assign(staying.value, section, now = 12)

                h1.movePlacement(moving, a, now = 20)
                val sameLevel = presentation.snapshot()
                assertThat(
                    sameLevel.memberships.single { it.placementId == moving.value }.version,
                ).isEqualTo(1)
                assertThat(
                    sameLevel.memberships.single { it.placementId == moving.value }.isDeleted,
                ).isFalse()

                h1.movePlacement(moving, b, now = 30)

                val snapshot = presentation.snapshot()
                val retired = snapshot.memberships.single {
                    it.placementId == moving.value
                }
                val retained = snapshot.memberships.single {
                    it.placementId == staying.value
                }
                assertThat(retired.isDeleted).isTrue()
                assertThat(retired.version).isEqualTo(2)
                assertThat(retired.syncedAt).isNull()
                assertThat(retained.isDeleted).isFalse()
                assertThat(retained.version).isEqualTo(1)
                assertThat(snapshot.containers.single().isDeleted).isFalse()
                assertThat(h1.getPlacement(moving)?.parentPlacementId).isEqualTo(b)
            } finally {
                db.close()
            }
        }

    @Test
    fun `subtree retirement tombstones anchored nested sections and member while preserving unrelated presentation`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "subtree-root", "subtree-child", "outside")
                val h1 = CanonicalHierarchyPlacementRepository(db)
                val root = occurrence(h1, "subtree-root", now = 1)
                val child = occurrence(h1, "subtree-child", root, now = 2)
                val outside = occurrence(h1, "outside", now = 3)
                val presentation = BigPicturePresentationRepository(db)
                val section = presentation.create(
                    "Inside", anchorPlacementId = root.value, now = 10,
                )
                val nested = presentation.create(
                    "Nested", anchorPlacementId = root.value,
                    parentContainerId = section, now = 11,
                )
                val outsideSection = presentation.create("Outside", now = 12)
                presentation.assign(child.value, nested, now = 13)
                presentation.assign(outside.value, outsideSection, now = 14)

                val removed = h1.removeOccurrenceSubtree(root, now = 30)

                assertThat(removed).containsExactly(root, child)
                assertThat(h1.getPlacement(root)).isNull()
                assertThat(h1.getPlacement(child)).isNull()
                val after = presentation.snapshot()
                assertThat(after.containers.single { it.id == section }.isDeleted).isTrue()
                assertThat(after.containers.single { it.id == nested }.isDeleted).isTrue()
                assertThat(after.containers.single { it.id == outsideSection }.isDeleted).isFalse()
                assertThat(
                    after.memberships.single { it.placementId == child.value }.isDeleted,
                ).isTrue()
                assertThat(
                    after.memberships.single { it.placementId == outside.value }.isDeleted,
                ).isFalse()
            } finally {
                db.close()
            }
        }

    @Test
    fun `single occurrence removal retires membership without deleting shared container`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "single-a", "single-b")
                val h1 = CanonicalHierarchyPlacementRepository(db)
                val a = occurrence(h1, "single-a", now = 1)
                val b = occurrence(h1, "single-b", now = 2)
                val presentation = BigPicturePresentationRepository(db)
                val section = presentation.create("Root section", now = 10)
                presentation.assign(a.value, section, now = 11)
                presentation.assign(b.value, section, now = 12)

                h1.removePlacement(a, now = 20)

                val after = presentation.snapshot()
                assertThat(
                    after.memberships.single { it.placementId == a.value }.isDeleted,
                ).isTrue()
                assertThat(
                    after.memberships.single { it.placementId == b.value }.isDeleted,
                ).isFalse()
                assertThat(after.containers.single().isDeleted).isFalse()
                assertThat(h1.getPlacement(a)).isNull()
                assertThat(h1.getPlacement(b)).isNotNull()
            } finally {
                db.close()
            }
        }

    @Test
    fun `presentation write failure rolls back H1 reparent and membership atomically`() =
        runBlocking {
            val db = database()
            try {
                seed(db, "rollback-a", "rollback-b", "rollback-child")
                val h1 = CanonicalHierarchyPlacementRepository(db)
                val a = occurrence(h1, "rollback-a", now = 1)
                val b = occurrence(h1, "rollback-b", now = 2)
                val child = occurrence(h1, "rollback-child", a, now = 3)
                val presentation = BigPicturePresentationRepository(db)
                val section = presentation.create(
                    "Rollback", anchorPlacementId = a.value, now = 10,
                )
                presentation.assign(child.value, section, now = 11)

                val beforeH1 = db.hierarchyPlacementDao().getAll()
                val beforePresentation = presentation.snapshot()

                // Inject a failure exactly at the presentation persistence
                // boundary, after H1 has been updated inside the transaction.
                db.openHelper.writableDatabase.execSQL(
                    "CREATE TRIGGER r2f_fail_bp_update " +
                        "BEFORE UPDATE ON big_picture_memberships " +
                        "BEGIN SELECT RAISE(ABORT, 'injected'); END",
                )
                db.openHelper.writableDatabase.execSQL(
                    "CREATE TRIGGER r2f_fail_bp_insert " +
                        "BEFORE INSERT ON big_picture_memberships " +
                        "BEGIN SELECT RAISE(ABORT, 'injected'); END",
                )

                val failure = runCatching {
                    h1.movePlacement(child, b, now = 30)
                }.exceptionOrNull()
                assertThat(failure).isNotNull()
                assertThat(db.hierarchyPlacementDao().getAll())
                    .containsExactlyElementsIn(beforeH1)
                assertThat(presentation.snapshot()).isEqualTo(beforePresentation)
                assertThat(h1.getPlacement(child)?.parentPlacementId).isEqualTo(a)
            } finally {
                db.close()
            }
        }
}
