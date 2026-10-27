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
class BigPictureContainerRenameRoomTest {
    @Test fun `rename changes only title and dirty version not mixed children or sources`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val subject = f.orientation()
            val section = f.h1.createContainer("Old", H1StructuralParent.Root, now = 20)
            val parent = H1StructuralParent.Node(H1TypedNode.Container(section))
            f.appearance(subject, parent)
            f.h1.createContainer("Nested", parent, now = 21)
            val repository = BigPicturePresentationRepository(f.db)
            val before = repository.snapshot()
            val placements = f.db.hierarchyPlacementDao().getAll()
            val subjects = f.dao.getAllManagedSubjects()
            val original = before.containers.single { it.id == section }
            f.h1.renameContainer(section, "  New title  ", now = 40)
            val after = repository.snapshot()
            val renamed = after.containers.single { it.id == section }
            assertThat(renamed.title).isEqualTo("New title")
            assertThat(renamed.version).isEqualTo(original.version + 1)
            assertThat(renamed.updatedAt).isGreaterThan(original.updatedAt)
            assertThat(renamed.syncedAt).isNull()
            assertThat(renamed.copy(title = original.title, version = original.version,
                updatedAt = original.updatedAt, syncedAt = original.syncedAt)).isEqualTo(original)
            assertThat(after.memberships).isEqualTo(before.memberships)
            assertThat(after.containers.filter { it.id != section }).isEqualTo(before.containers.filter { it.id != section })
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEqualTo(placements)
            assertThat(f.dao.getAllManagedSubjects()).isEqualTo(subjects)
            f.db.bigPicturePresentationDao().acknowledgeContainer(section, original.version, 50)
            assertThat(repository.snapshot().containers.single { it.id == section }.syncedAt).isNull()
            f.h1.renameContainer(section, " New title ", now = 60)
            assertThat(repository.snapshot()).isEqualTo(after)
        }
    }

    @Test fun `blank and deleted container rename fail without partial writes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val section = f.h1.createContainer("Title", H1StructuralParent.Root, now = 20)
            val repository = BigPicturePresentationRepository(f.db)
            val before = repository.snapshot()
            check(runCatching { f.h1.renameContainer(section, " \n ", now = 30) }.isFailure)
            assertThat(repository.snapshot()).isEqualTo(before)
            f.h1.deleteContainer(section, now = 40)
            val deleted = repository.snapshot()
            check(runCatching { f.h1.renameContainer(section, "Resurrect", now = 50) }.isFailure)
            assertThat(repository.snapshot()).isEqualTo(deleted)
        }
    }
}
