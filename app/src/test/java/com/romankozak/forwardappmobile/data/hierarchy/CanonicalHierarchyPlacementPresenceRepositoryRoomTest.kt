package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalHierarchyPlacementPresenceRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `PRIMARY LINK and repeated appearances produce typed distinct presence`(): Unit = runBlocking {
        val db = database()
        try {
            val dao = db.hierarchyPlacementDao()
            val repo = CanonicalHierarchyPlacementPresenceRepository(dao)

            assertThat(repo.observeVisiblePlacedTargets().first()).isEmpty()

            dao.upsertAll(
                listOf(
                    placement("p-subject", "MANAGED_SUBJECT", "same"),
                    placement("l-subject", "MANAGED_SUBJECT", "same", kind = "LINK"),
                    placement("l-aspect", "MANAGED_SUBJECT", "aspect", kind = "LINK"),
                    placement("p-workspace", "WORKSPACE", "same"),
                    placement("l-workspace", "WORKSPACE", "same", kind = "LINK"),
                ),
            )

            assertThat(repo.observeVisiblePlacedTargets().first())
                .containsExactly(
                    target(HierarchyTargetType.MANAGED_SUBJECT, "same"),
                    target(HierarchyTargetType.MANAGED_SUBJECT, "aspect"),
                    target(HierarchyTargetType.WORKSPACE, "same"),
                )

            assertThat(dao.observeVisiblePlacedTargets("GENERAL").first()).hasSize(3)
        } finally {
            db.close()
        }
    }

    @Test
    fun `tombstones scope and unknown target kinds do not leak into GENERAL presence`(): Unit = runBlocking {
        val db = database()
        try {
            val dao = db.hierarchyPlacementDao()

            // OTHER and FUTURE_KIND are deliberately inserted at the storage boundary.
            // Production canonical hierarchy validation does not admit these states.
            dao.upsertAll(
                listOf(
                    placement("live", "MANAGED_SUBJECT", "live"),
                    placement("dead", "MANAGED_SUBJECT", "dead", deleted = true),
                    placement("other", "MANAGED_SUBJECT", "other", hierarchy = "OTHER"),
                    placement("unknown", "FUTURE_KIND", "unknown"),
                ),
            )

            val repo = CanonicalHierarchyPlacementPresenceRepository(dao)
            assertThat(repo.observeVisiblePlacedTargets().first())
                .containsExactly(target(HierarchyTargetType.MANAGED_SUBJECT, "live"))

            assertThat(
                runCatching {
                    repo.observeVisiblePlacedTargets(HierarchyId("OTHER"))
                }.exceptionOrNull(),
            ).isInstanceOf(IllegalArgumentException::class.java)
        } finally {
            db.close()
        }
    }

    @Test
    fun `presence follows placement tombstone and restoration without deleting target`(): Unit = runBlocking {
        val db = database()
        try {
            val dao = db.hierarchyPlacementDao()
            val repo = CanonicalHierarchyPlacementPresenceRepository(dao)
            val original = placement("occurrence", "WORKSPACE", "workspace")

            dao.upsert(original)
            assertThat(repo.observeVisiblePlacedTargets().first())
                .containsExactly(target(HierarchyTargetType.WORKSPACE, "workspace"))

            val deleted = original.copy(isDeleted = true, version = 2L, updatedAt = 2L)
            dao.upsert(deleted)
            assertThat(repo.observeVisiblePlacedTargets().first()).isEmpty()

            dao.upsert(deleted.copy(isDeleted = false, version = 3L, updatedAt = 3L))
            assertThat(repo.observeVisiblePlacedTargets().first())
                .containsExactly(target(HierarchyTargetType.WORKSPACE, "workspace"))
        } finally {
            db.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun target(type: HierarchyTargetType, id: String) =
        HierarchyTargetRef(type, id)

    private fun placement(
        id: String,
        type: String,
        targetId: String,
        kind: String = "PRIMARY",
        hierarchy: String = "GENERAL",
        deleted: Boolean = false,
    ) = HierarchyPlacementEntity(
        id = id,
        hierarchyId = hierarchy,
        targetType = type,
        targetId = targetId,
        parentPlacementId = null,
        placementKind = kind,
        siblingOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = deleted,
        version = 1L,
    )
}
