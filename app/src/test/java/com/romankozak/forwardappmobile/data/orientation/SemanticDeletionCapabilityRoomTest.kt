package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class SemanticDeletionCapabilityRoomTest {
    @Test fun `ordinary Orientation and Aspect capability capture canonical title type version without writes`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val orientation = f.orientation()
            val aspect = f.aspect()
            val before = f.rows()
            assertThat(f.command.deletionTarget(orientation)).isEqualTo(SemanticEntityDeletionTarget(
                orientation, com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType.ORIENTATION, "Meaning", 1))
            assertThat(f.command.deletionTarget(aspect)?.title).isEqualTo("Health")
            assertThat(f.command.deletionTarget("missing")).isNull()
            assertThat(f.rows()).isEqualTo(before)
            f.delete(orientation)
            assertThat(f.command.deletionTarget(orientation)).isNull()
        }
    }

    @Test fun `every specialized or unknown Orientation kind fails closed`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val original = f.dao.getOrientation(id)!!
            for (kind in OrientationKind.entries.filterNot { it in genericDeletableOrientationKinds }.map { it.name } + "UNKNOWN") {
                f.dao.upsertOrientations(listOf(original.copy(kind = kind)))
                assertThat(f.command.deletionTarget(id)).isNull()
            }
        }
    }
}
