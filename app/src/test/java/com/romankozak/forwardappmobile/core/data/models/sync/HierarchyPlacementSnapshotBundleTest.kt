package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HierarchyPlacementSnapshotBundleTest {
    private val gson = Gson()

    @Test
    fun `A old bundle missing H1 field means collection absent`() {
        val parsed =
            gson.fromJson(
                """{"snapshotVersion":2}""",
                SnapshotBundle::class.java,
            )

        assertNull(parsed.hierarchyPlacements)
    }

    @Test
    fun `C present empty H1 field stays distinguishable from absence`() {
        val parsed =
            gson.fromJson(
                """{"snapshotVersion":2,"hierarchyPlacements":[]}""",
                SnapshotBundle::class.java,
            )

        assertEquals(emptyList<HierarchyPlacementSnapshot>(), parsed.hierarchyPlacements)
    }

    @Test
    fun `v176 old bundle missing Group scope field means collection absent`() {
        val parsed =
            gson.fromJson(
                """{"snapshotVersion":2,"hierarchyPlacements":[]}""",
                SnapshotBundle::class.java,
            )

        assertNull(parsed.hierarchyPlacementGroupScopes)
    }

    @Test
    fun `v176 explicit empty Group scope field stays distinguishable from absence`() {
        val parsed =
            gson.fromJson(
                """{"snapshotVersion":2,"hierarchyPlacementGroupScopes":[]}""",
                SnapshotBundle::class.java,
            )

        assertEquals(
            emptyList<HierarchyPlacementGroupScopeSnapshot>(),
            parsed.hierarchyPlacementGroupScopes,
        )
    }

    @Test
    fun `v176 Group scope transport round trip preserves explicit NoGroup and syncedAt`() {
        val row =
            HierarchyPlacementGroupScopeSnapshot(
                placementId = "arbitrary-placement-id",
                hierarchyId = "GENERAL",
                groupSubjectId = null,
                createdAt = 10L,
                updatedAt = 20L,
                syncedAt = 15L,
                isDeleted = false,
                version = 3L,
            )

        val parsed =
            gson.fromJson(
                gson.toJson(
                    SnapshotBundle(
                        version = 2,
                        hierarchyPlacementGroupScopes = listOf(row),
                    ),
                ),
                SnapshotBundle::class.java,
            )

        assertEquals(listOf(row), parsed.hierarchyPlacementGroupScopes)
    }

    @Test
    fun `D H1 transport round trip preserves every field including syncedAt`() {
        val row =
            HierarchyPlacementSnapshot(
                id = "placement-1",
                hierarchyId = "GENERAL",
                targetType = "WORKSPACE",
                targetId = "workspace-1",
                parentPlacementId = "placement-parent",
                placementKind = "LINK",
                siblingOrder = 42L,
                createdAt = 100L,
                updatedAt = 200L,
                syncedAt = 175L,
                isDeleted = true,
                version = 7L,
            )
        val source =
            SnapshotBundle(
                version = 2,
                hierarchyPlacements = listOf(row),
            )

        val parsed =
            gson.fromJson(
                gson.toJson(source),
                SnapshotBundle::class.java,
            )

        assertEquals(listOf(row), parsed.hierarchyPlacements)
    }
}
