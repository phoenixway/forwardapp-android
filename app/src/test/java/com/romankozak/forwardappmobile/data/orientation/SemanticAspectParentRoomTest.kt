package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.romankozak.forwardappmobile.features.semanticeditor.aspectDirectChildren
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SemanticAspectParentRoomTest {
    @Test fun `child reads follow semantic taxonomy not H1 and reflect rename reorder move and tombstone`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val parent = f.aspects.create("Parent", now = 10)
            val first = f.aspects.create("First", parentAspectId = parent, now = 11)
            val second = f.aspects.create("Second", parentAspectId = parent, now = 12)
            f.aspects.create("Grandchild", parentAspectId = first, now = 13)
            val h1Only = f.aspects.create("Only visually nested", now = 14)
            val occurrence = f.appearance(parent)
            f.appearance(h1Only, H1StructuralParent.Node(H1TypedNode.Occurrence(occurrence.value)))
            val before = f.rows()
            suspend fun children() = aspectDirectChildren(parent, f.aspects.observeLive().first())
            assertThat(children().map { it.aspectId }).containsExactly(first, second).inOrder()
            assertThat(f.rows()).isEqualTo(before)
            f.aspects.reorderSiblings(parent, listOf(second, first), now = 30)
            f.aspects.updateDetails(first, "Renamed", null, now = 31)
            assertThat(children().map { it.title }).containsExactly("Second", "Renamed").inOrder()
            f.aspects.move(second, null, now = 32)
            assertThat(children().map { it.aspectId }).containsExactly(first)
            f.aspects.tombstone(first, now = 33)
            assertThat(children()).isEmpty()
        }
    }

    @Test fun `parent change and make root preserve descendants H1 memberships bindings and semantic sources`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val parent = f.aspects.create("Parent", now = 10)
            val current = f.aspects.create("Current", now = 11)
            val child = f.aspects.create("Child", parentAspectId = current, now = 12)
            val orientation = f.orientation()
            f.workspace()
            f.appearance(current)
            f.appearance(child)
            f.links.linkOrientation(current, orientation, AspectOrientationRelationType.BELONGS_TO, now = 20)
            f.bindings.bind(current, "workspace", WorkspaceBindingType.EMBODIES, now = 21)
            val before = f.rows()
            val identity = f.dao.getManagedSubject(current)!!
            f.aspects.move(current, parent, now = 100)
            assertThat(f.dao.getAspect(current)!!.parentAspectId).isEqualTo(parent)
            assertThat(f.dao.getAspect(child)!!.parentAspectId).isEqualTo(current)
            val changed = f.dao.getManagedSubject(current)!!
            assertThat(changed.version).isEqualTo(identity.version + 1)
            assertThat(changed.updatedAt).isEqualTo(100L)
            assertThat(changed.syncedAt).isNull()
            assertThat(changed.title).isEqualTo(identity.title)
            val after = f.rows()
            before.filterKeys { it !in setOf("aspects", "managed_subjects") }.forEach { (table, rows) ->
                assertWithMessage(table).that(after[table]).isEqualTo(rows)
            }
            f.aspects.move(current, null, now = 101)
            assertThat(f.dao.getAspect(current)!!.parentAspectId).isNull()
            assertThat(f.dao.getAspect(child)!!.parentAspectId).isEqualTo(current)
            assertThat(f.dao.getManagedSubject(current)!!.version).isEqualTo(identity.version + 2)
            val rooted = f.rows()
            before.filterKeys { it !in setOf("aspects", "managed_subjects") }.forEach { (table, rows) ->
                assertWithMessage(table).that(rooted[table]).isEqualTo(rows)
            }
        }
    }

    @Test fun `self cycles and deleted parent reject without any partial version or graph write`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val parent = f.aspects.create("Parent", now = 10)
            val child = f.aspects.create("Child", parentAspectId = parent, now = 11)
            val descendant = f.aspects.create("Descendant", parentAspectId = child, now = 12)
            val deleted = f.aspects.create("Deleted", now = 13)
            f.aspects.tombstone(deleted, now = 14)
            f.appearance(parent)
            val before = f.rows()
            listOf(parent, descendant, deleted).forEach { invalid ->
                assertThat(runCatching { f.aspects.move(parent, invalid, now = 100) }.isFailure).isTrue()
                assertThat(f.rows()).isEqualTo(before)
            }
        }
    }
}
