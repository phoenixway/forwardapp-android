package com.romankozak.forwardappmobile.data.repository

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyReadSnapshotAssembler
import com.romankozak.forwardappmobile.data.hierarchy.GroupScopeApplicabilityMode
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalSearchHierarchyReadRoomTest {
    @Test fun `search admits native semantic roots without inventing GroupScope`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val orientation = f.orientation()
            val aspect = f.aspect()
            f.appearance(orientation)
            f.appearance(orientation)
            f.appearance(aspect)
            val before = f.rows()
            val read = loadCanonicalV2SearchHierarchyRead(f.db, emptyList(), CanonicalV2HierarchyReadSnapshotAssembler())
            assertThat(read.hierarchy.occurrences).hasSize(3)
            assertThat(read.nativePlacementIds).hasSize(3)
            assertThat(f.rows()).isEqualTo(before)
            assertThat(f.db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
        }
    }
}
