package com.romankozak.forwardappmobile.data.hierarchy

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigPictureRecursiveRemovalRoomTest {
    @Test fun `removing LINK subtree retires nested mixed structure not sources or outside appearances`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val orientation = f.orientation()
            val aspect = f.aspect()
            val target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace")
            val primary = f.h1.addAppearance(target, H1StructuralParent.Root, now = 20)
            val link = f.h1.addAppearance(target, H1StructuralParent.Root, now = 21)
            val outside = f.appearance(orientation)
            val section = f.h1.createContainer("Section", H1StructuralParent.Node(H1TypedNode.Occurrence(link.value)), now = 22)
            val nested = f.h1.createContainer("Nested", H1StructuralParent.Node(H1TypedNode.Container(section)), now = 23)
            val inside = f.appearance(orientation, H1StructuralParent.Node(H1TypedNode.Container(nested)))
            val aspectAppearance = f.appearance(aspect, H1StructuralParent.Node(H1TypedNode.Occurrence(link.value)))
            val subjects = f.dao.getAllManagedSubjects()
            val workspaces = f.db.workspaceDao().getAll()
            val before = f.db.hierarchyPlacementDao().getAll().associateBy { it.id }
            val retired = f.h1.removeOccurrenceSubtree(link, now = 40)
            assertThat(retired).containsExactly(link, inside, aspectAppearance)
            for (id in retired) {
                val row = f.db.hierarchyPlacementDao().getById(id.value)!!
                assertThat(row.isDeleted).isTrue()
                assertThat(row.version).isEqualTo(before.getValue(id.value).version + 1)
                assertThat(row.syncedAt).isNull()
            }
            assertThat(f.db.hierarchyPlacementDao().getById(primary.value)!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getById(outside.value)!!.isDeleted).isFalse()
            val presentation = BigPicturePresentationRepository(f.db).snapshot()
            assertThat(presentation.containers.filter { it.id in setOf(section, nested) }.all { it.isDeleted }).isTrue()
            assertThat(presentation.memberships.single { it.placementId == inside.value }.isDeleted).isTrue()
            assertThat(f.dao.getAllManagedSubjects()).isEqualTo(subjects)
            assertThat(f.db.workspaceDao().getAll()).isEqualTo(workspaces)
        }
    }
}
