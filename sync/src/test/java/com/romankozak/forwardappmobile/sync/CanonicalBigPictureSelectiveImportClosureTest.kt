package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureContainerSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPictureMembershipSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalBigPictureSelectiveImportClosureTest {

    @Test
    fun `nested selection includes only membership and container ancestry`() {
        val root = placement("root")
        val child = placement("child", parentId = "root")
        val other = placement("other")
        val presentation = BigPicturePresentationSnapshot(
            containers = listOf(
                container("top", anchorId = "root"),
                container("nested", parentId = "top", anchorId = "root"),
                container("unrelated"),
            ),
            memberships = listOf(
                member("child", "nested"),
                member("other", "unrelated"),
            ),
        )
        val source = source(listOf(root, child, other), presentation)
        val filtered = selected(listOf(root, child))
            .withCanonicalBigPictureSelectiveClosure(source)
            .bigPicturePresentation!!

        assertEquals(listOf("top", "nested"), filtered.containers.map { it.id })
        assertEquals(listOf("child"), filtered.memberships.map { it.placementId })
        assertEquals(1, filtered.formatVersion)
    }

    @Test
    fun `selection is by occurrence not shared Workspace identity`() {
        val first = placement("first").copy(targetId = "shared")
        val second = placement("second").copy(
            targetId = "shared",
            placementKind = "LINK",
        )
        val source = source(
            listOf(first, second),
            BigPicturePresentationSnapshot(
                containers = listOf(container("section")),
                memberships = listOf(
                    member("first", "section"),
                    member("second", "section"),
                ),
            ),
        )

        val filtered = selected(listOf(first))
            .withCanonicalBigPictureSelectiveClosure(source)
            .bigPicturePresentation!!

        assertEquals(listOf("first"), filtered.memberships.map { it.placementId })
        assertEquals(listOf("section"), filtered.containers.map { it.id })
    }

    @Test
    fun `selected tombstone retains its container dependency and version`() {
        val root = placement("root")
        val tombstone = member("root", "section", deleted = true)
            .copy(version = 8L, syncedAt = 7L)
        val source = source(
            listOf(root),
            BigPicturePresentationSnapshot(
                containers = listOf(container("section").copy(isDeleted = true)),
                memberships = listOf(tombstone),
            ),
        )

        val filtered = selected(listOf(root))
            .withCanonicalBigPictureSelectiveClosure(source)
            .bigPicturePresentation!!

        assertEquals(tombstone, filtered.memberships.single())
        assertTrue(filtered.containers.single().isDeleted)
    }

    @Test
    fun `empty H1 selection emits explicit empty presentation`() {
        val root = placement("root")
        val source = source(
            listOf(root),
            BigPicturePresentationSnapshot(
                containers = listOf(container("section")),
                memberships = listOf(member("root", "section")),
            ),
        )

        val filtered = selected(emptyList())
            .withCanonicalBigPictureSelectiveClosure(source)

        assertEquals(BigPicturePresentationSnapshot(), filtered.bigPicturePresentation)
    }

    @Test
    fun `malformed unselected container fails before narrowing`() {
        val root = placement("root")
        val source = source(
            listOf(root),
            BigPicturePresentationSnapshot(
                containers = listOf(
                    container("section"),
                    container("broken", parentId = "missing"),
                ),
                memberships = listOf(member("root", "section")),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            selected(listOf(root)).withCanonicalBigPictureSelectiveClosure(source)
        }
    }

    @Test
    fun `container anchor omitted from selected H1 fails closed`() {
        val root = placement("root")
        val child = placement("child", parentId = "root")
        val source = source(
            listOf(root, child),
            BigPicturePresentationSnapshot(
                containers = listOf(container("section", anchorId = "root")),
                memberships = listOf(member("child", "section")),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            selected(listOf(child)).withCanonicalBigPictureSelectiveClosure(source)
        }
    }

    @Test
    fun `historical absence stays absent but presentation without source H1 fails`() {
        val empty = selected(emptyList())

        assertNull(
            empty.withCanonicalBigPictureSelectiveClosure(SnapshotBundle())
                .bigPicturePresentation,
        )

        assertThrows(IllegalArgumentException::class.java) {
            empty.withCanonicalBigPictureSelectiveClosure(
                SnapshotBundle(
                    bigPicturePresentation = BigPicturePresentationSnapshot(),
                ),
            )
        }
    }

    @Test
    fun `source cycle and unsupported format both fail closed`() {
        val root = placement("root")
        val base = BigPicturePresentationSnapshot(
            containers = listOf(
                container("one", parentId = "two"),
                container("two", parentId = "one"),
            ),
        )

        for (invalid in listOf(base, base.copy(formatVersion = 99))) {
            assertThrows(IllegalArgumentException::class.java) {
                selected(listOf(root)).withCanonicalBigPictureSelectiveClosure(
                    source(listOf(root), invalid),
                )
            }
        }
    }

    private fun selected(placements: List<HierarchyPlacementSnapshot>) =
        SnapshotBundle(hierarchyPlacements = placements)

    private fun source(
        placements: List<HierarchyPlacementSnapshot>,
        presentation: BigPicturePresentationSnapshot,
    ) = SnapshotBundle(
        hierarchyPlacements = placements,
        bigPicturePresentation = presentation,
    )

    private fun placement(id: String, parentId: String? = null) =
        HierarchyPlacementSnapshot(
            id = id,
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = id,
            parentPlacementId = parentId,
            placementKind = if (parentId == null) "PRIMARY" else "LINK",
            siblingOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun container(
        id: String,
        parentId: String? = null,
        anchorId: String? = null,
    ) = BigPictureContainerSnapshot(
        id = id,
        hierarchyId = "GENERAL",
        title = id,
        anchorPlacementId = anchorId,
        parentContainerId = parentId,
        siblingOrder = 0L,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
    )

    private fun member(
        placementId: String,
        containerId: String,
        deleted: Boolean = false,
    ) = BigPictureMembershipSnapshot(
        placementId = placementId,
        hierarchyId = "GENERAL",
        containerId = containerId,
        createdAt = 1L,
        updatedAt = 1L,
        syncedAt = null,
        isDeleted = deleted,
        version = 1L,
    )
}
